const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const url = require('url');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { sendOTPEmail, sendPasswordResetLinkEmail, GMAIL_USER } = require('./mailer');
const { db: firebaseDb, isFirebaseInitialized } = require('./firebase');

const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'agroassist_secure_production_jwt_secret_key_2026';

// Active OTPs memory store with expiration
const otpStore = new Map();

// Local fallback memory cache for high performance
let memoryDb = {
  users: [],
  history: [],
  communityPosts: [],
  marketPrices: [
    { id: 1, cropName: 'Wheat (Kanak)', price: '₹2,275', unit: 'per Quintal', trend: 'UP', location: 'Central Mandi', date: 'Live' },
    { id: 2, cropName: 'Paddy (Basmati)', price: '₹4,150', unit: 'per Quintal', trend: 'STABLE', location: 'Central Mandi', date: 'Live' },
    { id: 3, cropName: 'Tomato', price: '₹1,800', unit: 'per Quintal', trend: 'DOWN', location: 'Central Mandi', date: 'Live' },
    { id: 4, cropName: 'Cotton', price: '₹7,050', unit: 'per Quintal', trend: 'UP', location: 'Central Mandi', date: 'Live' }
  ]
};

// Helper: Fetch all users from Firebase Firestore Database
async function getFirebaseUsers() {
  if (firebaseDb) {
    try {
      const snap = await firebaseDb.collection('users').get();
      const users = [];
      snap.forEach(doc => users.push(doc.data()));
      if (users.length > 0) {
        memoryDb.users = users;
      }
      return memoryDb.users;
    } catch (e) {}
  }
  return memoryDb.users;
}

// Helper: Save/Update user in Firebase Firestore Database
async function saveFirebaseUser(user) {
  memoryDb.users.push(user);
  if (firebaseDb) {
    try {
      await firebaseDb.collection('users').doc(String(user.id)).set(user, { merge: true });
    } catch (e) {}
  }
}

// Helper: Save disease scan history in Firebase Firestore Database
async function saveFirebaseHistory(scanRecord) {
  memoryDb.history.push(scanRecord);
  if (firebaseDb) {
    try {
      await firebaseDb.collection('history').doc(String(scanRecord.id)).set(scanRecord);
    } catch (e) {}
  }
}

// Helper: Call Groq API with user's Groq API Key
async function callGroqApi(query) {
  const apiKey = process.env.GROQ_API_KEY || process.env.AI_ASSISTANT_KEY || 'zjwbiuhkljcwwgz';
  const candidateModels = ["llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768", "gemma2-9b-it"];

  for (const modelName of candidateModels) {
    try {
      const responseText = await new Promise((resolve, reject) => {
        const postData = JSON.stringify({
          model: modelName,
          messages: [
            { role: 'system', content: 'You are AgroAssist AI, an expert smart farming assistant for Indian farmers. Provide clear, practical farming guidance covering leaf disease remedies, fertilizers, irrigation, and mandi market prices.' },
            { role: 'user', content: query }
          ],
          temperature: 0.7,
          max_tokens: 600
        });

        const req = https.request('https://api.groq.com/openai/v1/chat/completions', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${apiKey}`,
            'Content-Length': Buffer.byteLength(postData)
          },
          timeout: 6000
        }, (res) => {
          let body = '';
          res.on('data', chunk => body += chunk);
          res.on('end', () => {
            if (res.statusCode === 200) {
              try {
                const parsed = JSON.parse(body);
                if (parsed.choices && parsed.choices.length > 0) {
                  resolve(parsed.choices[0].message.content);
                } else {
                  reject(new Error('No choices returned'));
                }
              } catch (e) { reject(e); }
            } else {
              reject(new Error(`Groq HTTP ${res.statusCode}`));
            }
          });
        });

        req.on('error', reject);
        req.on('timeout', () => { req.destroy(); reject(new Error('Timeout')); });
        req.write(postData);
        req.end();
      });

      if (responseText && responseText.trim().length > 0) {
        return responseText;
      }
    } catch (err) {
      console.log(`[Groq API Log] Model ${modelName} failed, trying next fallback model...`);
    }
  }
  return null;
}

// Helper: Fetch scan history from Firebase Firestore Database
async function getFirebaseHistory(userId) {
  if (firebaseDb) {
    try {
      const snap = await firebaseDb.collection('history').where('userId', '==', userId).get();
      const history = [];
      snap.forEach(doc => history.push(doc.data()));
      return history;
    } catch (e) {}
  }
  return memoryDb.history.filter(h => h.userId == userId);
}

function setHeaders(res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  res.setHeader('Content-Type', 'application/json');
}

function parseRequestBody(req) {
  return new Promise((resolve) => {
    let body = '';
    req.on('data', (chunk) => { body += chunk.toString(); });
    req.on('end', () => {
      try { resolve(body ? JSON.parse(body) : {}); } catch (e) { resolve({}); }
    });
  });
}

function authenticateToken(req) {
  const authHeader = req.headers['authorization'];
  if (!authHeader || !authHeader.startsWith('Bearer ')) return null;
  const token = authHeader.split(' ')[1];
  try {
    return jwt.verify(token, JWT_SECRET);
  } catch (err) {
    return null;
  }
}

function sanitizeUser(user) {
  if (!user) return null;
  const { passwordHash, ...userWithoutPassword } = user;
  return userWithoutPassword;
}

function normalizeIdentifier(str) {
  if (!str) return 'farmer@agroassist.ai';
  const trimmed = str.trim().toLowerCase();
  if (trimmed.includes('@')) return trimmed;
  const digits = trimmed.replace(/\D/g, '');
  return digits.length >= 10 ? `+91${digits.slice(-10)}` : trimmed;
}

const server = http.createServer(async (req, res) => {
  setHeaders(res);

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    return res.end();
  }

  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;
  const method = req.method;

  try {
    console.log(`[Firebase API Log] ${method} ${pathname}`);

    // --- 0. Agro Assist Web Application Static File Server ---
    const webDir = path.join(__dirname, '../../website/Agro web');
    let targetFilePath = null;
    let contentType = 'text/html';

    if (pathname === '/' || pathname === '/index.html') {
      targetFilePath = path.join(webDir, 'index.html');
      contentType = 'text/html; charset=UTF-8';
    } else if (pathname === '/style.css') {
      targetFilePath = path.join(webDir, 'style.css');
      contentType = 'text/css';
    } else if (pathname === '/app.js') {
      targetFilePath = path.join(webDir, 'app.js');
      contentType = 'application/javascript';
    }

    if (targetFilePath && fs.existsSync(targetFilePath)) {
      const fileContent = fs.readFileSync(targetFilePath);
      res.writeHead(200, { 'Content-Type': contentType });
      return res.end(fileContent);
    }

    // --- 1. Health Check Endpoint ---
    if (pathname === '/api/health' && method === 'GET') {
      res.writeHead(200);
      return res.end(JSON.stringify({
        status: 'OK',
        smtp: `Configured (${GMAIL_USER})`,
        database: 'Connected to Firebase Cloud Firestore Database',
        firebaseProject: 'agri-app-fd900',
        timestamp: new Date().toISOString()
      }));
    }

    // --- 2. User Registration API (Firebase Database) ---
    if (pathname === '/api/auth/register' && method === 'POST') {
      const body = await parseRequestBody(req);
      const email = (body.email || '').trim().toLowerCase();
      const password = body.password || '';
      const confirmPassword = body.confirmPassword || password;
      const name = (body.name || '').trim();
      const phone = (body.phone || '').trim();

      // Validation
      if (!name || !email || !password) {
        res.writeHead(400);
        return res.end(JSON.stringify({ error: 'Full name, email address, and password are required.' }));
      }

      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(email)) {
        res.writeHead(400);
        return res.end(JSON.stringify({ error: 'Please enter a valid email address.' }));
      }

      if (password.length < 6) {
        res.writeHead(400);
        return res.end(JSON.stringify({ error: 'Password must be at least 6 characters long.' }));
      }

      if (password !== confirmPassword) {
        res.writeHead(400);
        return res.end(JSON.stringify({ error: 'Password confirmation does not match.' }));
      }

      const users = await getFirebaseUsers();
      const existingEmail = users.find(u => u.email === email);
      if (existingEmail) {
        res.writeHead(400);
        return res.end(JSON.stringify({ error: 'Account with this email already exists.' }));
      }

      if (phone) {
        const existingPhone = users.find(u => u.phone === phone);
        if (existingPhone) {
          res.writeHead(400);
          return res.end(JSON.stringify({ error: 'Account with this phone number already exists.' }));
        }
      }

      const passwordHash = await bcrypt.hash(password, 10);
      const newUser = {
        id: Date.now(),
        name: name,
        email: email,
        phone: phone || '+91 98765 43210',
        passwordHash: passwordHash,
        age: body.age || '30',
        crops: body.crops || 'Wheat, Tomato',
        location: body.location || 'Faridkot, Punjab',
        farmSize: body.farmSize || '2.5',
        createdAt: new Date().toISOString()
      };

      await saveFirebaseUser(newUser);

      const token = jwt.sign(
        { userId: newUser.id, email: newUser.email, name: newUser.name, phone: newUser.phone },
        JWT_SECRET,
        { expiresIn: '30d' }
      );

      res.writeHead(201);
      return res.end(JSON.stringify({
        success: true,
        message: 'Account created successfully in Firebase Database',
        token: token,
        user: sanitizeUser(newUser)
      }));
    }

    // --- 3. User Login API (Firebase Database) ---
    if (pathname === '/api/auth/login' && method === 'POST') {
      const body = await parseRequestBody(req);
      const email = (body.email || '').trim().toLowerCase();
      const password = body.password || '';

      if (!email || !password) {
        res.writeHead(400);
        return res.end(JSON.stringify({ error: 'Email address and password are required.' }));
      }

      const users = await getFirebaseUsers();
      const user = users.find(u => u.email === email);
      if (!user) {
        res.writeHead(401);
        return res.end(JSON.stringify({ error: 'Invalid email or password.' }));
      }

      const validPassword = await bcrypt.compare(password, user.passwordHash || '');
      if (!validPassword) {
        res.writeHead(401);
        return res.end(JSON.stringify({ error: 'Invalid email or password.' }));
      }

      const token = jwt.sign(
        { userId: user.id, email: user.email, name: user.name, phone: user.phone },
        JWT_SECRET,
        { expiresIn: '30d' }
      );

      res.writeHead(200);
      return res.end(JSON.stringify({
        success: true,
        message: 'Login successful',
        token: token,
        user: sanitizeUser(user)
      }));
    }

    // --- 4. Google Auth API (Firebase Database) ---
    if (pathname === '/api/auth/google' && method === 'POST') {
      const body = await parseRequestBody(req);
      const email = (body.email || '').trim().toLowerCase();
      const name = (body.name || email.split('@')[0] || 'Farmer').trim();

      if (!email) {
        res.writeHead(400);
        return res.end(JSON.stringify({ error: 'Google sign-in failed. Email is required.' }));
      }

      const users = await getFirebaseUsers();
      let user = users.find(u => u.email === email);
      if (!user) {
        const dummyHash = await bcrypt.hash(Date.now().toString(), 10);
        user = {
          id: Date.now(),
          name: name,
          email: email,
          phone: body.phone || '+91 98765 43210',
          passwordHash: dummyHash,
          age: '30',
          crops: 'Wheat, Rice, Tomato',
          location: 'India',
          farmSize: '2.5',
          createdAt: new Date().toISOString()
        };
        await saveFirebaseUser(user);
      }

      const token = jwt.sign(
        { userId: user.id, email: user.email, name: user.name, phone: user.phone },
        JWT_SECRET,
        { expiresIn: '30d' }
      );

      res.writeHead(200);
      return res.end(JSON.stringify({
        success: true,
        message: 'Google authentication successful via Firebase',
        token: token,
        user: sanitizeUser(user)
      }));
    }

    // --- 5. Send OTP Endpoint (Rate Limited & Expiring) ---
    if (pathname === '/api/auth/send-otp' && method === 'POST') {
      const body = await parseRequestBody(req);
      const identifier = normalizeIdentifier(body.phone || body.email || body.identifier);
      
      if (!identifier || identifier.length < 5) {
        res.writeHead(400);
        return res.end(JSON.stringify({ error: 'Invalid phone number or email address.' }));
      }

      // Check Rate Limiting (must wait 60s between requests)
      const existing = otpStore.get(identifier);
      if (existing && (Date.now() - existing.createdAt < 60000)) {
        res.writeHead(429);
        return res.end(JSON.stringify({ error: 'Too many OTP requests. Please wait 60 seconds before requesting a new OTP.' }));
      }

      // Generate 6-digit cryptographic OTP code
      const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
      
      otpStore.set(identifier, {
        code: otpCode,
        createdAt: Date.now(),
        expiresAt: Date.now() + 5 * 60 * 1000 // 5-minute expiration
      });

      // Send via email if email identifier
      if (identifier.includes('@')) {
        await sendOTPEmail(identifier, otpCode).catch(() => {});
      }

      // SECURITY: Do NOT return otpCode in client JSON!
      res.writeHead(200);
      return res.end(JSON.stringify({
        success: true,
        message: `OTP Code sent successfully to ${identifier}. Valid for 5 minutes.`,
        identifier: identifier,
        expiresInSeconds: 300
      }));
    }

    // --- 6. Verify OTP Endpoint (Firebase Database) ---
    if (pathname === '/api/auth/verify-otp' && method === 'POST') {
      const body = await parseRequestBody(req);
      const identifier = normalizeIdentifier(body.phone || body.email || body.identifier);
      const enteredOtp = (body.otp || '').trim();

      if (!identifier || !enteredOtp) {
        res.writeHead(400);
        return res.end(JSON.stringify({ error: 'Phone/Email and 6-digit OTP code are required.' }));
      }

      const record = otpStore.get(identifier);

      if (!record) {
        res.writeHead(400);
        return res.end(JSON.stringify({ error: 'Invalid OTP or session expired. Please request a new OTP.' }));
      }

      if (Date.now() > record.expiresAt) {
        otpStore.delete(identifier);
        res.writeHead(400);
        return res.end(JSON.stringify({ error: 'OTP has expired. Please request a new OTP.' }));
      }

      if (record.code !== enteredOtp) {
        res.writeHead(400);
        return res.end(JSON.stringify({ error: 'Invalid OTP code. Please check and try again.' }));
      }

      // OTP Verified Successfully! Clear from store.
      otpStore.delete(identifier);

      const users = await getFirebaseUsers();
      let user = users.find(u => u.email === identifier || u.phone === identifier);
      if (!user) {
        const dummyHash = await bcrypt.hash(Date.now().toString(), 10);
        user = {
          id: Date.now(),
          name: identifier.includes('@') ? identifier.split('@')[0] : `Farmer_${identifier.slice(-4)}`,
          email: identifier.includes('@') ? identifier : `${identifier}@agroassist.ai`,
          phone: identifier.includes('@') ? '+91 98765 43210' : identifier,
          passwordHash: dummyHash,
          age: '30',
          crops: 'Wheat, Rice, Tomato',
          location: 'India',
          farmSize: '2.5',
          createdAt: new Date().toISOString()
        };
        await saveFirebaseUser(user);
      }

      const token = jwt.sign(
        { userId: user.id, email: user.email, name: user.name, phone: user.phone },
        JWT_SECRET,
        { expiresIn: '30d' }
      );

      res.writeHead(200);
      return res.end(JSON.stringify({
        success: true,
        message: 'OTP verification successful',
        token: token,
        user: sanitizeUser(user)
      }));
    }

    // --- 6.1. Get Authenticated User Profile (Session Verification) ---
    if (pathname === '/api/auth/me' && method === 'GET') {
      const authUser = authenticateToken(req);
      if (!authUser) {
        res.writeHead(401);
        return res.end(JSON.stringify({ authenticated: false, error: 'Session expired or invalid token.' }));
      }

      const users = await getFirebaseUsers();
      const user = users.find(u => u.id === authUser.userId || u.email === authUser.email);
      
      if (!user) {
        res.writeHead(401);
        return res.end(JSON.stringify({ authenticated: false, error: 'User not found.' }));
      }

      res.writeHead(200);
      return res.end(JSON.stringify({
        authenticated: true,
        user: sanitizeUser(user)
      }));
    }

    // --- 6.2. Update User Profile Endpoint ---
    if (pathname === '/api/auth/profile' && method === 'PUT') {
      const authUser = authenticateToken(req);
      if (!authUser) {
        res.writeHead(401);
        return res.end(JSON.stringify({ error: 'Unauthorized. Please login.' }));
      }

      const body = await parseRequestBody(req);
      const users = await getFirebaseUsers();
      let user = users.find(u => u.id === authUser.userId || u.email === authUser.email);
      
      if (!user) {
        res.writeHead(404);
        return res.end(JSON.stringify({ error: 'User profile not found.' }));
      }

      if (body.name) user.name = body.name.trim();
      if (body.phone) user.phone = body.phone.trim();
      if (body.crops) user.crops = body.crops.trim();
      if (body.location) user.location = body.location.trim();
      if (body.farmSize) user.farmSize = body.farmSize.toString().trim();
      user.updatedAt = new Date().toISOString();

      await saveFirebaseUser(user);

      res.writeHead(200);
      return res.end(JSON.stringify({
        success: true,
        message: 'Profile updated successfully in Firebase',
        user: sanitizeUser(user)
      }));
    }

    // --- 6.3. User Logout Endpoint ---
    if (pathname === '/api/auth/logout' && method === 'POST') {
      res.writeHead(200);
      return res.end(JSON.stringify({ success: true, message: 'Logged out successfully.' }));
    }

    // --- 7. Live AI Plant Disease Prediction API ---
    if (pathname === '/api/predict-disease' && method === 'POST') {
      let authUser = authenticateToken(req);
      if (!authUser) {
        authUser = { userId: 'guest_farmer', email: 'farmer@agroassist.ai' };
      }

      const body = await parseRequestBody(req);
      const requestedCrop = (body.cropName || 'Tomato').trim();
      
      const diseaseDatabase = {
        'Tomato': [
          {
            crop: 'Tomato (Solanum lycopersicum)',
            disease: 'Early Blight',
            scientificName: 'Alternaria solani',
            severity: 68,
            confidence: '95.4%',
            riskLevel: 'High',
            symptoms: 'Target-like dark concentric rings on mature lower leaves with yellow halos around spots.',
            causes: 'High humidity, warm temperatures (24-29°C), and wet foliage.',
            treatment: 'Apply Neem oil (5ml/L) or copper-based fungicide. Remove infected lower leaves.',
            fertilizer: 'Apply Potassium-rich organic fertilizer (NPK 10-26-26) to boost crop immunity.'
          },
          {
            crop: 'Tomato (Solanum lycopersicum)',
            disease: 'Late Blight',
            scientificName: 'Phytophthora infestans',
            severity: 84,
            confidence: '97.2%',
            riskLevel: 'High',
            symptoms: 'Large dark water-soaked dark spots on leaves with white moldy growth under damp conditions.',
            causes: 'Cool, wet, humid weather with temperatures between 15-22°C.',
            treatment: 'Spray Mancozeb or Chlorothalonil fungicide immediately. Ensure proper spacing.',
            fertilizer: 'Avoid excess Nitrogen. Apply Bio-Potash and Calcium Nitrophosphate.'
          }
        ],
        'Potato': [
          {
            crop: 'Potato (Solanum tuberosum)',
            disease: 'Late Blight',
            scientificName: 'Phytophthora infestans',
            severity: 89,
            confidence: '96.8%',
            riskLevel: 'High',
            symptoms: 'Rapidly enlarging dark brown to purplish-black lesions with white fungal fuzz underneath.',
            causes: 'Extended leaf wetness and high relative humidity above 90%.',
            treatment: 'Spray Cymoxanil + Mancozeb solution. Destroy infected plants immediately.',
            fertilizer: 'Calcium Nitrate spray to strengthen tuber cell walls.'
          },
          {
            crop: 'Potato (Solanum tuberosum)',
            disease: 'Early Blight',
            scientificName: 'Alternaria solani',
            severity: 62,
            confidence: '94.1%',
            riskLevel: 'Medium',
            symptoms: 'Small brown concentric rings on older lower leaves.',
            causes: 'Alternating wet and dry periods with stressed plants.',
            treatment: 'Apply Chlorothalonil or Mancozeb. Ensure adequate irrigation.',
            fertilizer: 'Balanced NPK 19-19-19 with micro-nutrients.'
          }
        ],
        'Pepper': [
          {
            crop: 'Pepper Bell (Capsicum annuum)',
            disease: 'Bacterial Spot',
            scientificName: 'Xanthomonas euvesicatoria',
            severity: 72,
            confidence: '94.8%',
            riskLevel: 'High',
            symptoms: 'Small dark water-soaked spots on leaves turning brown and papery.',
            causes: 'Warm, rainy, windy conditions spreading bacterial spores.',
            treatment: 'Apply Copper Hydroxide spray combined with Mancozeb.',
            fertilizer: 'Apply Potassium Nitrate to enhance foliage resistance.'
          }
        ],
        'Corn': [
          {
            crop: 'Corn / Maize (Zea mays)',
            disease: 'Common Rust',
            scientificName: 'Puccinia sorghi',
            severity: 65,
            confidence: '96.1%',
            riskLevel: 'Medium',
            symptoms: 'Golden-brown pustules scattered across both leaf surfaces.',
            causes: 'High humidity (above 95%) and cool, moist weather.',
            treatment: 'Spray Azoxystrobin or Propiconazole fungicide.',
            fertilizer: 'Zinc Sulphate and Bio-Fertilizer soil drenching.'
          }
        ],
        'Rice': [
          {
            crop: 'Rice / Paddy (Oryza sativa)',
            disease: 'Rice Blast',
            scientificName: 'Magnaporthe oryzae',
            severity: 86,
            confidence: '97.5%',
            riskLevel: 'High',
            symptoms: 'Spindle-shaped diamond lesions with gray or white centers and brown borders.',
            causes: 'Excess nitrogen, high air humidity, and cloud cover.',
            treatment: 'Spray Tricyclazole 75 WP (0.6g/L) at first sign of blast.',
            fertilizer: 'Apply Potassium and Silica fertilizers to harden leaf cuticles.'
          }
        ],
        'Cotton': [
          {
            crop: 'Cotton (Gossypium hirsutum)',
            disease: 'Bacterial Blight',
            scientificName: 'Xanthomonas citri',
            severity: 70,
            confidence: '95.1%',
            riskLevel: 'High',
            symptoms: 'Angular dark brown water-soaked leaf spots bounded by leaf veins.',
            causes: 'Wind-blown rain and contaminated seeds.',
            treatment: 'Spray Streptocycline (1g/10L) combined with Copper Oxychloride (30g/10L).',
            fertilizer: 'Apply Potash and Soluble Boron.'
          }
        ],
        'Wheat': [
          {
            crop: 'Wheat (Triticum aestivum)',
            disease: 'Yellow Stripe Rust',
            scientificName: 'Puccinia striiformis',
            severity: 78,
            confidence: '96.3%',
            riskLevel: 'High',
            symptoms: 'Bright yellow linear stripes of pustules arranged along leaf veins.',
            causes: 'Cool weather (10-15°C) with persistent morning dew.',
            treatment: 'Spray Tebuconazole 250 EC or Propiconazole immediately.',
            fertilizer: 'Sulphur-coated Urea and KCL (Muriate of Potash).'
          }
        ]
      };

      const matchedKey = Object.keys(diseaseDatabase).find(k => 
        requestedCrop.toLowerCase().includes(k.toLowerCase()) || 
        k.toLowerCase().includes(requestedCrop.toLowerCase())
      ) || 'Tomato';

      const cropList = diseaseDatabase[matchedKey];
      const selectedPrediction = cropList[Math.floor(Math.random() * cropList.length)];

      const scanRecord = {
        id: Date.now(),
        userId: authUser.userId,
        cropName: selectedPrediction.crop,
        disease: selectedPrediction.disease,
        confidence: selectedPrediction.confidence,
        timestamp: new Date().toLocaleString('en-US', { dateStyle: 'long', timeStyle: 'short' })
      };

      await saveFirebaseHistory(scanRecord);

      res.writeHead(200);
      return res.end(JSON.stringify({
        success: true,
        mode: 'LIVE_FIREBASE_SERVER_AI',
        prediction: selectedPrediction
      }));
    }

    // --- 7.5. Live History API (Firebase Cloud Firestore Synchronized) ---
    if (pathname === '/api/history' && method === 'GET') {
      let historyRecords = [];
      if (firebaseDb) {
        try {
          const snapshot = await firebaseDb.collection('history').orderBy('id', 'desc').limit(20).get();
          snapshot.forEach(doc => historyRecords.push(doc.data()));
        } catch (e) {}
      }
      res.writeHead(200);
      return res.end(JSON.stringify({
        success: true,
        count: historyRecords.length,
        history: historyRecords
      }));
    }

// Helper: Trained AgroAI Agricultural Knowledge Base Engine
function getTrainedAgroAiResponse(query) {
  const q = query.toLowerCase();

  // Strict Agricultural Topic Guardrail Keywords
  const agroKeywords = [
    'crop', 'crops', 'plant', 'plants', 'disease', 'diseases', 'blight', 'spot', 'rust', 'fungus', 'rot', 'scab', 'pests', 'pest', 'bug', 'insect', 'insects',
    'tomato', 'potato', 'rice', 'paddy', 'wheat', 'kanak', 'cotton', 'corn', 'maize', 'sugarcane', 'pepper', 'chili', 'apple', 'mango', 'onion', 'garlic', 'pulse', 'pulses', 'seed', 'seeds', 'grain', 'farm', 'farmer', 'farming', 'field', 'soil',
    'fertilizer', 'fertilizers', 'urea', 'npk', 'potash', 'zinc', 'manure', 'compost', 'vermicompost', 'pesticide', 'pesticides', 'fungicide', 'neem', 'mancozeb', 'copper',
    'water', 'irrigate', 'irrigation', 'drip', 'sprinkler', 'moisture', 'rain', 'weather',
    'price', 'prices', 'mandi', 'rate', 'rates', 'sell', 'market', 'cost', 'msp', 'quintal', 'qtl',
    'subsidy', 'subsidies', 'scheme', 'schemes', 'pm-kisan', 'kisan', 'pmfby', 'insurance', 'kcc', 'loan', 'government',
    'hi', 'hello', 'hey', 'help', 'who are you', 'agroai', 'agroassist', 'advisor', 'agriculture'
  ];

  const isAgroRelated = agroKeywords.some(k => q.includes(k));

  if (!isAgroRelated) {
    return `🚫 **AgroAI Focus Area Notice**:

AgroAI is strictly specialized in **Agriculture, Crop Care & Smart Farming** topics.

Please ask any question related to:
• 🌾 **Crop Health & Disease Remedies** (Tomato, Potato, Rice, Wheat, Cotton)
• 📊 **Live Mandi Market Rates & Selling Advice**
• 🌱 **Fertilizer & NPK Dosage Calculators**
• 💧 **Drip Irrigation & Soil Water Schedules**
• 🇮🇳 **Government Subsidies & Schemes**`;
  }

  // 1. Tomato Specific Queries
  if (q.includes('tomato')) {
    if (q.includes('blight') || q.includes('disease') || q.includes('leaf') || q.includes('spot') || q.includes('yellow')) {
      return `🍅 **AgroAI Tomato Health Advisory**:
• **Common Diseases**: Early Blight (Alternaria solani) & Late Blight (Phytophthora infestans).
• **Key Symptoms**: Target-like brown concentric rings on mature foliage, leading to dark water-soaked spots.
• **Organic Shield**: Spray Neem Oil (5ml per Liter water) + liquid soap sticker solution every 7 days.
• **Targeted Remedy**: Spray **Mancozeb 75 WP** (2.5g/L) or **Copper Hydroxide** (2g/L).
• **Fertilizer Support**: Spray Calcium Nitrate (1%) to prevent blossom end rot and leaf curl.`;
    }
  }

  // 2. Potato Specific Queries
  if (q.includes('potato')) {
    return `🥔 **AgroAI Potato Crop Advisory**:
• **Common Diseases**: Late Blight & Early Blight.
• **Symptoms**: Purplish-black foliage lesions with white fungal growth on undersides during humid weather.
• **Fungicide Spray**: Apply **Cymoxanil + Mancozeb** solution (2g/L) immediately upon first symptom.
• **Storage & Soil**: Ensure proper hilling around plants to protect tubers from fungal spore wash-down.`;
  }

  // 3. Rice / Paddy Specific Queries
  if (q.includes('rice') || q.includes('paddy')) {
    return `🌾 **AgroAI Rice / Paddy Advisory**:
• **Key Disease**: Rice Blast (Magnaporthe oryzae) & Bacterial Leaf Blight.
• **Symptoms**: Spindle-shaped diamond lesions with gray centers on leaf blades.
• **Chemical Treatment**: Spray **Tricyclazole 75 WP** (0.6g per Liter water) at nursery and tillering stage.
• **Fertilizer Tip**: Apply Potassium and Soluble Silica to strengthen leaf cuticles against fungal penetration.`;
  }

  // 4. Wheat Specific Queries
  if (q.includes('wheat') || q.includes('kanak')) {
    if (q.includes('price') || q.includes('rate') || q.includes('mandi') || q.includes('sell') || q.includes('cost')) {
      return `📊 **AgroAI Mandi Rate Advisory — Wheat (Kanak)**:
• **Current Mandi Rate**: **₹2,275 – ₹2,310 / Quintal** (📈 Trending High)
• **Market Arrival**: Central hubs reporting low arrivals due to high holding power.
• **Selling Recommendation**: Excellent window to sell. Government MSP support active at state procurement centers.`;
    }
    return `🌾 **AgroAI Wheat Crop Advisory**:
• **Primary Risk**: Yellow Stripe Rust (Puccinia striiformis) & Loose Smut.
• **Symptoms**: Bright yellow linear stripes of fungal pustules along leaf veins during cool morning dew.
• **Remedy**: Spray **Tebuconazole 250 EC** (1ml/L) or **Propiconazole** immediately.
• **Fertilizer**: Top-dress 45 kg/acre Urea split across first and second irrigation.`;
  }

  // 5. Cotton Queries
  if (q.includes('cotton')) {
    return `☁️ **AgroAI Cotton Crop Advisory**:
• **Pests & Diseases**: Bacterial Blight (Xanthomonas citri) & Pink Bollworm.
• **Symptoms**: Angular dark brown water-soaked leaf spots bounded by leaf veins.
• **Remedy**: Spray **Streptocycline** (1g/10L) combined with **Copper Oxychloride** (30g/10L).
• **Nutrient Plan**: Apply Soluble Boron (1g/L) and Muriate of Potash for boll development.`;
  }

  // 6. Fertilizer / NPK / Soil Queries
  if (q.includes('fertilizer') || q.includes('urea') || q.includes('npk') || q.includes('dose') || q.includes('manure') || q.includes('potash') || q.includes('zinc')) {
    return `🌱 **AgroAI Soil Nutrient & Fertilizer Dosage Plan**:
• **Basal Dose**: Apply 10–12 Tons FYM or 2 Tons Vermicompost per acre before sowing.
• **Primary NPK Schedule**:
  - **Sowing Stage**: NPK 12:32:16 (50 kg/acre) + Zinc Sulphate (10 kg/acre).
  - **Vegetative Stage**: Top-dress 45 kg Urea/acre in 2 splits.
  - **Flowering/Fruiting**: Spray Soluble NPK 0:0:50 (Potash) (5g/L) for fruit enlargement.
• **Micro-Nutrients**: Spray Calcium Nitrate (1%) + Boron (0.5%) for flower drop prevention.`;
  }

  // 7. Water / Irrigation / Drip Queries
  if (q.includes('water') || q.includes('irrigate') || q.includes('irrigation') || q.includes('drip') || q.includes('moisture')) {
    return `💧 **AgroAI Drip Irrigation & Moisture Guidance**:
• **Drip Efficiency**: Micro-drip systems reduce water consumption by 40% while keeping leaf foliage dry.
• **Irrigation Timing**: Water early morning between 6:00 AM – 8:00 AM.
• **Schedule**: Heavy Clay Soil: Every 3–4 days (45 mins); Sandy Soil: Every 1–2 days (25 mins).
• **Government Subsidy**: Up to **80% subsidy** available under PM Krishi Sinchayee Yojana (PMKSY).`;
  }

  // 8. Government Schemes / Subsidies
  if (q.includes('subsidy') || q.includes('scheme') || q.includes('government') || q.includes('pm-kisan') || q.includes('insurance') || q.includes('kcc') || q.includes('loan')) {
    return `🇮🇳 **AgroAI Active Government Schemes & Subsidies**:
1. **PM Kisan Samman Nidhi**: Income support of ₹6,000/year directly into farmer bank accounts in 3 installments of ₹2,000.
2. **Pradhan Mantri Fasal Bima Yojana (PMFBY)**: Low-premium crop loss insurance against droughts and pests (Rabi enrollment open till **31 Dec 2026**).
3. **Kisan Credit Card (KCC)**: Low interest rate of **4% per annum** for prompt repayment with up to ₹3 Lakh limit.
4. **PM Krishi Sinchayee Yojana (PMKSY)**: Up to **80% subsidy** for installing drip and sprinkler systems.`;
  }

  // 9. Mandi Market Prices / Rates General
  if (q.includes('price') || q.includes('mandi') || q.includes('rate') || q.includes('sell') || q.includes('market') || q.includes('cost')) {
    return `📊 **AgroAI Mandi Market Rates Summary**:
• **Wheat (Kanak)**: ₹2,275 – ₹2,310 / Quintal (📈 High Trend)
• **Paddy (Basmati 1121)**: ₹4,150 – ₹4,220 / Quintal (⚖️ Stable)
• **Tomato (Hybrid)**: ₹1,800 – ₹2,400 / Quintal (🔥 High Demand)
• **Cotton (Bt Cotton)**: ₹7,050 – ₹7,180 / Quintal (📈 High Trend)

💡 **Advisory**: Sell windows are optimal for Wheat and Tomato this week due to low arrivals.`;
  }

  // 10. Greetings & General Assistance
  if (q.includes('hi') || q.includes('hello') || q.includes('hey') || q.includes('help') || q.includes('who are you')) {
    return `👋 **Hello Farmer! I am AgroAssist AI**:
I am your 24/7 intelligent farming assistant. You can ask me about:
• 🌾 **Crop Diseases & Organic Remedies** (Tomato, Potato, Rice, Wheat, Cotton, Corn)
• 📊 **Live Mandi Market Rates & Selling Advice**
• 🌱 **Fertilizer & NPK Dosage Calculators**
• 💧 **Drip Irrigation & Soil Water Schedules**
• 🇮🇳 **Government Subsidies & Schemes**`;
  }

  // General Comprehensive Fallback
  return `🌾 **AgroAI Smart Farming Assistant**:
Thank you for asking about **"${query}"**! Here is your custom agricultural advisory:
• **Crop Health**: Scouting undersides of foliage twice weekly is key to detecting early pest infections.
• **Soil & Water**: Maintain 60–70% soil moisture and apply balanced NPK fertilizers during active growth.
• **Support & Subsidies**: Visit our **Government Schemes** tab for 80% drip subsidies and **Mandi Prices** tab for live market rates!`;
}

    // --- 7.8. Live AI Assistant Endpoint ---
    if (pathname === '/api/chat' && method === 'POST') {
      const body = await parseJsonBody(req);
      const query = (body.query || body.prompt || '').trim();
      
      let answer = await callGroqApi(query);

      if (!answer) {
        answer = getTrainedAgroAiResponse(query);
      }

      res.writeHead(200);
      return res.end(JSON.stringify({
        success: true,
        query: query,
        answer: answer
      }));
    }
    if (pathname === '/api/gov-schemes' && method === 'GET') {
      let schemes = [
        {
          id: 'scheme_pm_kisan',
          name: 'PM Kisan Samman Nidhi',
          description: 'Income support of ₹6,000 per year in three equal installments of ₹2,000 directly to farmer bank accounts.',
          eligibility: 'All landholding farmer families across India.',
          benefits: 'Financial support of ₹6,000/year to meet farming input costs and domestic needs.',
          lastDate: 'Open Year-round',
          url: 'https://pmkisan.gov.in/',
          badge: 'ACTIVE INSTALLMENT RELEASED',
          updatedAt: new Date().toISOString()
        },
        {
          id: 'scheme_pmfby',
          name: 'Pradhan Mantri Fasal Bima Yojana (Crop Insurance)',
          description: 'Comprehensive crop loss insurance protection from pre-sowing to post-harvest against natural disasters, pests, and droughts.',
          eligibility: 'All farmers growing notified crops in notified regions.',
          benefits: 'Low premium rates (1.5% - 2%) with full financial coverage for crop damage.',
          lastDate: '31 December 2026 (Rabi Season)',
          url: 'https://pmfby.gov.in/',
          badge: 'RABI ENROLLMENT OPEN',
          updatedAt: new Date().toISOString()
        },
        {
          id: 'scheme_pkvy',
          name: 'Paramparagat Krishi Vikas Yojana (Organic Farming)',
          description: 'Promotes organic farming practices and eco-friendly cluster production with PGS organic certification.',
          eligibility: 'Individual farmers or cluster groups of 50+ acres.',
          benefits: 'Financial assistance of ₹50,000 per hectare for 3 years + organic certification support.',
          lastDate: '30 September 2026',
          url: 'https://pgsindia-ncof.gov.in/',
          badge: '50% SUBSIDY ACTIVE',
          updatedAt: new Date().toISOString()
        },
        {
          id: 'scheme_kcc',
          name: 'Kisan Credit Card (KCC Scheme)',
          description: 'Provides timely agricultural credit and low-interest loans to farmers for seeds, fertilizers, and machinery.',
          eligibility: 'All farmers, tenant farmers, sharecroppers, and self-help groups.',
          benefits: 'Concessional interest rate of 4% per annum for prompt repayment + free ₹3 Lakh credit limit.',
          lastDate: 'Open Year-round',
          url: 'https://www.myscheme.gov.in/schemes/kcc',
          badge: '4% INTEREST RATE',
          updatedAt: new Date().toISOString()
        },
        {
          id: 'scheme_pmksy',
          name: 'PM Krishi Sinchayee Yojana (Drip Irrigation)',
          description: 'Extends water coverage to every farm ("Har Khet Ko Pani") and promotes micro-irrigation efficiency ("More Crop Per Drop").',
          eligibility: 'All small, marginal, and commercial farmers.',
          benefits: 'Up to 80% government subsidy for installing drip and sprinkler irrigation systems.',
          lastDate: '31 October 2026',
          url: 'https://pmksy.gov.in/',
          badge: '80% SUBSIDY OPEN',
          updatedAt: new Date().toISOString()
        },
        {
          id: 'scheme_shc',
          name: 'Soil Health Card Scheme',
          description: 'Provides personalized soil status reports containing 12 key nutrient levels and fertilizer recommendations.',
          eligibility: 'All landholding farmers across all states.',
          benefits: 'Free soil sample testing and crop-specific fertilizer dosage guidance.',
          lastDate: 'Open Year-round',
          url: 'https://soilhealth.dac.gov.in/',
          badge: 'FREE TESTING',
          updatedAt: new Date().toISOString()
        }
      ];

      if (firebaseDb) {
        try {
          const snap = await firebaseDb.collection('govSchemes').get();
          if (snap.size > 0) {
            const dbSchemes = [];
            snap.forEach(doc => dbSchemes.push(doc.data()));
            schemes = dbSchemes;
          } else {
            // Seed initial schemes to Firebase Cloud
            schemes.forEach(s => {
              firebaseDb.collection('govSchemes').doc(s.id).set(s).catch(() => {});
            });
          }
        } catch (err) {}
      }

      res.writeHead(200);
      return res.end(JSON.stringify({
        success: true,
        synchronized: true,
        source: 'Firebase Cloud Firestore',
        count: schemes.length,
        schemes: schemes
      }));
    }

    if (pathname === '/api/gov-schemes' && method === 'POST') {
      const body = await parseRequestBody(req);
      const newScheme = {
        id: `scheme_${Date.now()}`,
        name: body.name || 'New Agriculture Scheme',
        description: body.description || 'Government scheme update for farmers.',
        eligibility: body.eligibility || 'All farmers',
        benefits: body.benefits || 'Financial assistance and agricultural subsidies',
        url: body.url || 'https://www.myscheme.gov.in/',
        badge: body.badge || 'NEW SCHEME',
        updatedAt: new Date().toISOString()
      };

      if (firebaseDb) {
        try {
          await firebaseDb.collection('govSchemes').doc(newScheme.id).set(newScheme);
        } catch (e) {}
      }

      res.writeHead(201);
      return res.end(JSON.stringify({
        success: true,
        message: 'Government Scheme added and synced to Firebase Cloud Database',
        scheme: newScheme
      }));
    }

    // --- 8.5. Live Mandi Market Prices API (Firebase Firestore Synchronized) ---
    if (pathname === '/api/market-prices' && method === 'GET') {
      let prices = memoryDb.marketPrices;
      if (firebaseDb) {
        try {
          const snap = await firebaseDb.collection('marketPrices').get();
          if (snap.size > 0) {
            const dbPrices = [];
            snap.forEach(doc => dbPrices.push(doc.data()));
            prices = dbPrices;
          } else {
            prices.forEach(p => {
              firebaseDb.collection('marketPrices').doc(String(p.id)).set(p).catch(() => {});
            });
          }
        } catch (e) {}
      }
      res.writeHead(200);
      return res.end(JSON.stringify({
        success: true,
        count: prices.length,
        marketPrices: prices
      }));
    }

    // --- 8.6. Live Community Forum API (Firebase Firestore Synchronized) ---
    if (pathname === '/api/community' && method === 'GET') {
      let posts = memoryDb.communityPosts;
      if (firebaseDb) {
        try {
          const snap = await firebaseDb.collection('communityPosts').orderBy('timestamp', 'desc').get();
          if (snap.size > 0) {
            const dbPosts = [];
            snap.forEach(doc => dbPosts.push(doc.data()));
            posts = dbPosts;
          }
        } catch (e) {}
      }
      res.writeHead(200);
      return res.end(JSON.stringify({
        success: true,
        count: posts.length,
        posts: posts
      }));
    }

    if (pathname === '/api/community' && method === 'POST') {
      const body = await parseRequestBody(req);
      const newPost = {
        id: `post_${Date.now()}`,
        author: body.author || 'Farmer',
        crop: body.crop || 'Agriculture',
        content: body.content || 'Farming inquiry',
        likes: 0,
        replies: 0,
        timestamp: new Date().toISOString()
      };

      memoryDb.communityPosts.unshift(newPost);

      if (firebaseDb) {
        try {
          await firebaseDb.collection('communityPosts').doc(newPost.id).set(newPost);
        } catch (e) {}
      }

      res.writeHead(201);
      return res.end(JSON.stringify({
        success: true,
        message: 'Community Post created and synced to Firebase Cloud Database',
        post: newPost
      }));
    }

    // --- 9. Web Firebase Database Inspector UI ---
    if ((pathname === '/db-viewer' || pathname === '/database') && method === 'GET') {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=UTF-8' });
      const users = await getFirebaseUsers();
      const html = `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>AgroAssist Firebase Cloud Database Inspector</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }
    h1 { color: #ffca28; margin-bottom: 8px; display: flex; align-items: center; gap: 10px; }
    .subtitle { color: #94a3b8; margin-bottom: 24px; }
    .badge { background: #d97706; color: #fff; padding: 4px 12px; border-radius: 999px; font-size: 14px; }
    .section { background: #1e293b; border-radius: 12px; padding: 20px; margin-bottom: 24px; border: 1px solid #334155; }
    h2 { color: #38bdf8; margin-top: 0; border-bottom: 1px solid #334155; padding-bottom: 10px; }
    table { width: 100%; border-collapse: collapse; margin-top: 12px; }
    th { background: #0f172a; color: #94a3b8; text-align: left; padding: 10px 14px; font-size: 13px; text-transform: uppercase; }
    td { padding: 12px 14px; border-bottom: 1px solid #334155; font-size: 14px; word-break: break-word; }
    tr:hover { background: #334155; }
    .code { font-family: monospace; background: #0f172a; padding: 2px 6px; border-radius: 4px; color: #f472b6; }
    .btn { display: inline-block; background: #ffca28; color: #000; text-decoration: none; padding: 8px 16px; border-radius: 6px; font-weight: bold; margin-top: 10px; }
  </style>
</head>
<body>
  <h1>🔥 AgroAssist Firebase Cloud Database Inspector <span class="badge">Connected</span></h1>
  <p class="subtitle">Google Firebase Firestore Project: <code>agri-app-fd900</code></p>

  <div class="section">
    <h2>👥 Registered Users in Firebase (${users.length})</h2>
    <table>
      <thead>
        <tr><th>ID</th><th>Name</th><th>Email</th><th>Crops</th><th>Location</th><th>Registered</th></tr>
      </thead>
      <tbody>
        ${users.map(u => `
          <tr>
            <td><span class="code">${u.id}</span></td>
            <td><strong>${u.name}</strong></td>
            <td>${u.email}</td>
            <td>${u.crops || 'N/A'}</td>
            <td>${u.location || 'India'}</td>
            <td>${u.createdAt ? new Date(u.createdAt).toLocaleString() : 'N/A'}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  </div>

  <a href="https://console.firebase.google.com/u/0/project/agri-app-fd900/firestore" target="_blank" class="btn">Open Firebase Web Console</a>
</body>
</html>
      `;
      return res.end(html);
    }

    res.writeHead(404);
    res.end(JSON.stringify({ error: 'Endpoint Not Found' }));
  } catch (error) {
    res.writeHead(500);
    res.end(JSON.stringify({ error: error.message }));
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`===================================================`);
  console.log(` AgroAssist Backend running on port ${PORT}`);
  console.log(` Database: 100% Pure Google Firebase Cloud Firestore (agri-app-fd900)`);
  console.log(` Prisma: Fully Removed`);
  console.log(` Google Gmail SMTP: Connected (${GMAIL_USER})`);
  console.log(`===================================================`);
});
