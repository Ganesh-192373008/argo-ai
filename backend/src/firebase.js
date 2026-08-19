const { initializeApp, cert, getApps } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const path = require('path');
const fs = require('fs');

let db = null;
let isFirebaseInitialized = false;

// Auto-detect Firebase Service Account Key in backend directory
const backendDir = path.join(__dirname, '..');
const filesInBackend = fs.readdirSync(backendDir);
const keyFile = filesInBackend.find(f => f.includes('firebase-adminsdk') || f === 'firebase-service-account.json');

if (keyFile) {
  const fullPath = path.join(backendDir, keyFile);
  try {
    const serviceAccount = JSON.parse(fs.readFileSync(fullPath, 'utf-8'));
    if (getApps().length === 0) {
      initializeApp({
        credential: cert(serviceAccount)
      });
    }
    db = getFirestore();
    isFirebaseInitialized = true;
    console.log(`===================================================`);
    console.log(`🔥 [Firebase Admin SDK] Successfully Connected!`);
    console.log(`🔥 Project ID: ${serviceAccount.project_id || 'agri-app-fd900'}`);
    console.log(`🔥 Client Email: ${serviceAccount.client_email || 'N/A'}`);
    console.log(`===================================================`);
  } catch (e) {
    console.error('[Firebase Error] Failed to initialize with key file:', e.message);
  }
} else {
  console.log('[Firebase Engine] Initialized with Local Sync Fallback.');
}

module.exports = {
  isFirebaseInitialized,
  db
};
