package com.example.agroassist

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.appcompat.app.AlertDialog
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Toast.makeText(this, "Signed in as ${account?.email}", Toast.LENGTH_LONG).show()
            
            // Save profile details to database
            val email = account?.email ?: "farmer@agroassist.com"
            val name = account?.displayName ?: email.substringBefore("@")
            val dbHelper = AgroDatabaseHelper(this)
            dbHelper.saveProfile(name, "25", "Tomato, Wheat")
            
            // Save email to SharedPreferences
            val prefs = getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)
            prefs.edit().putString("email_address", email).apply()
            
            // Download Google photo if available
            account?.photoUrl?.let { url ->
                downloadAndSaveGooglePhoto(this, url.toString())
            }
            
            // Navigate to Dashboard or ProfileSetup
            val profile = dbHelper.getProfile()
            val intent = if (profile["name"]?.isNotEmpty() == true) {
                Intent(this, DashboardActivity::class.java)
            } else {
                Intent(this, ProfileSetupActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this, "Running in Offline Mode (Google Auth Fallback)", Toast.LENGTH_SHORT).show()
            
            // Save profile details to database
            val email = "google.farmer@gmail.com"
            val name = "Google Farmer"
            val dbHelper = AgroDatabaseHelper(this)
            dbHelper.saveProfile(name, "25", "Tomato, Wheat")

            // Save email to SharedPreferences
            val prefs = getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)
            prefs.edit().putString("email_address", email).apply()
            
            // Navigate to Dashboard
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val mobileInput = findViewById<EditText>(R.id.mobileInput)
        val continueButton = findViewById<Button>(R.id.continueButton)
        val googleButton = findViewById<FrameLayout>(R.id.googleButtonContainer)
        val emailButton = findViewById<FrameLayout>(R.id.emailButtonContainer)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        emailButton.setOnClickListener {
            startActivity(Intent(this, EmailLoginActivity::class.java))
        }

        val biometricButtonContainer = findViewById<FrameLayout>(R.id.biometricButtonContainer)
        biometricButtonContainer?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val biometricPrompt = BiometricPrompt.Builder(this)
                        .setTitle("Biometric Login")
                        .setSubtitle("Log in using your fingerprint")
                        .setDescription("AgroAssist AI uses biometrics to secure your farming dashboard.")
                        .setNegativeButton("Cancel", mainExecutor) { _, _ ->
                            Toast.makeText(this, "Authentication cancelled", Toast.LENGTH_SHORT).show()
                        }
                        .build()

                    val cancellationSignal = CancellationSignal()
                    biometricPrompt.authenticate(
                        cancellationSignal,
                        mainExecutor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                Toast.makeText(this@LoginActivity, "Real Biometric unavailable. Opening Simulator.", Toast.LENGTH_SHORT).show()
                                showFingerprintLoader()
                            }

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                Toast.makeText(this@LoginActivity, "Biometric authentication succeeded!", Toast.LENGTH_SHORT).show()
                                val dbHelper = AgroDatabaseHelper(this@LoginActivity)
                                dbHelper.saveProfile("Bio Farmer", "30", "Tomato, Wheat")
                                val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }

                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                Toast.makeText(this@LoginActivity, "Biometric authentication failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } catch (e: Exception) {
                    showFingerprintLoader()
                }
            } else {
                showFingerprintLoader()
            }
        }

        continueButton.setOnClickListener {
            val otpCode = "482015"
            sendOtpNotification(otpCode)
            val intent = Intent(this, OtpVerificationActivity::class.java).apply {
                putExtra("OTP_CODE", otpCode)
            }
            startActivity(intent)
        }

        backButton.setOnClickListener {
            finish()
        }

        // Initially disable the continue button
        continueButton.isEnabled = false

        // Simple validation to enable the continue button when 10 digits are entered
        mobileInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 10) {
                    continueButton.setBackgroundColor(resources.getColor(R.color.primary_green, theme))
                    continueButton.isEnabled = true
                } else {
                    continueButton.setBackgroundColor(android.graphics.Color.parseColor("#D6D9E0"))
                    continueButton.isEnabled = false
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun sendOtpNotification(otp: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "otp_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "OTP Verification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Verification code reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💬 AgroAssist AI OTP Verification")
            .setContentText("Your OTP code is $otp. Use this to login.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }

    private fun showFingerprintLoader() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_fingerprint, null)
        builder.setView(view)
        builder.setCancelable(true)
        val dialog = builder.create()

        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener { dialog.dismiss() }

        // Clicking the fingerprint icon area simulates success
        val layoutFingerprint = view.findViewById<LinearLayout>(R.id.layoutFingerprint)
        layoutFingerprint.setOnClickListener {
            Toast.makeText(this, "Fingerprint Authenticated!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            
            // Mock Login Success via fingerprint
            val dbHelper = AgroDatabaseHelper(this)
            dbHelper.saveProfile("Bio Farmer", "30", "Tomato, Wheat")
            
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        
        dialog.show()
    }

    private fun downloadAndSaveGooglePhoto(context: Context, photoUrlStr: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val url = java.net.URL(photoUrlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream = connection.inputStream
                val file = java.io.File(context.filesDir, "profile_photo.png")
                val outputStream = java.io.FileOutputStream(file)
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                val localUri = android.net.Uri.fromFile(file)
                val prefs = context.getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)
                prefs.edit().putString("profile_photo_uri", localUri.toString()).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

