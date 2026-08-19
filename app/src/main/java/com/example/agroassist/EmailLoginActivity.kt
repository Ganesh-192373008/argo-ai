package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class EmailLoginActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var fallbackGoogleSignInClient: GoogleSignInClient

    private val fallbackGoogleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: return@registerForActivityResult
            val name = account.displayName ?: email.substringBefore("@")

            Toast.makeText(this, "Authenticating Google Account...", Toast.LENGTH_SHORT).show()

            account.photoUrl?.let { url ->
                downloadAndSaveGooglePhoto(this, url.toString())
            }

            BackendApiClient.googleLogin(this, email, name) { success, message, token, userId ->
                if (success && token != null) {
                    SessionManager.saveSession(this, token, userId, email, name)
                    Toast.makeText(this, "Welcome $name!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Authentication failed: $message", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "fallbackSignInResult:failed code=" + e.statusCode)
            Toast.makeText(this, "Google Sign-In failed (Code ${e.statusCode}). Please check your Google account.", Toast.LENGTH_LONG).show()
        }
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: return@registerForActivityResult
            val name = account.displayName ?: email.substringBefore("@")

            Toast.makeText(this, "Authenticating Google Account...", Toast.LENGTH_SHORT).show()

            account.photoUrl?.let { url ->
                downloadAndSaveGooglePhoto(this, url.toString())
            }

            BackendApiClient.googleLogin(this, email, name) { success, message, token, userId ->
                if (success && token != null) {
                    SessionManager.saveSession(this, token, userId, email, name)
                    Toast.makeText(this, "Welcome $name!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Authentication failed: $message", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            if (e.statusCode == 10) {
                // Code 10: Web Client ID missing Android binding in Google Cloud Console.
                // Fall back to standard Google Sign-In to retrieve account email and authenticate via JWT
                val fallbackGso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()
                fallbackGoogleSignInClient = GoogleSignIn.getClient(this, fallbackGso)
                fallbackGoogleLauncher.launch(fallbackGoogleSignInClient.signInIntent)
            } else {
                Toast.makeText(this, "Google Sign-In failed (Code ${e.statusCode}). Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_login)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val togglePasswordVisibility = findViewById<ImageView>(R.id.togglePasswordVisibility)
        val signInButton = findViewById<Button>(R.id.signInButton)
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)
        val signUpText = findViewById<TextView>(R.id.signUpText)
        val googleButton = findViewById<FrameLayout>(R.id.googleButtonContainer)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleButton.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        backButton.setOnClickListener {
            finish()
        }

        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        signUpText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        togglePasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordVisibility.alpha = 0.5f
            } else {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordVisibility.alpha = 1.0f
            }
            passwordInput.setSelection(passwordInput.text.length)
        }

        // Prefill email if passed from SignUpActivity or SharedPreferences
        val prefs = getSharedPreferences("AgroAssistSettings", android.content.Context.MODE_PRIVATE)
        val prefilledEmail = intent.getStringExtra("PREFILL_EMAIL") 
            ?: prefs.getString("registered_email", "") 
            ?: prefs.getString("email_address", "")
        if (!prefilledEmail.isNullOrEmpty()) {
            emailInput.setText(prefilledEmail)
            emailInput.setSelection(emailInput.text.length)
        }

        signInButton.isEnabled = true

        signInButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Verifying credentials...", Toast.LENGTH_SHORT).show()

            BackendApiClient.login(this, email, password) { success, message, token, userId, name ->
                if (success && token != null) {
                    SessionManager.saveSession(this, token, userId, email, name)
                    Toast.makeText(this, "Welcome Back! Login Successful.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun downloadAndSaveGooglePhoto(context: android.content.Context, photoUrlStr: String) {
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
                val prefs = context.getSharedPreferences("AgroAssistSettings", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("profile_photo_uri", localUri.toString()).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
