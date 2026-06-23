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
            val prefs = getSharedPreferences("AgroAssistSettings", android.content.Context.MODE_PRIVATE)
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
            val prefs = getSharedPreferences("AgroAssistSettings", android.content.Context.MODE_PRIVATE)
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
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        backButton.setOnClickListener {
            finish()
        }

        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        signUpText.setOnClickListener {
            // Show a dialog for Email Sign Up
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Sign Up / Register")
            
            val layout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(50, 40, 50, 40)
            }
            
            val emailRegInput = EditText(this).apply {
                hint = "Enter Email"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            val passwordRegInput = EditText(this).apply {
                hint = "Enter Password (min 6 chars)"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            
            layout.addView(emailRegInput)
            layout.addView(passwordRegInput)
            builder.setView(layout)
            
            builder.setPositiveButton("Register") { dialog, _ ->
                val email = emailRegInput.text.toString().trim()
                val password = passwordRegInput.text.toString().trim()
                
                if (email.isEmpty() || password.length < 6) {
                    Toast.makeText(this, "Please enter a valid email and 6+ character password.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()
                FirebaseAuthHelper.signUp(
                    this,
                    email,
                    password,
                    onSuccess = {
                        Toast.makeText(this, "Registration Successful! You can now Sign In.", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    },
                    onFailure = { error ->
                        Toast.makeText(this, "Registration Failed: $error", Toast.LENGTH_LONG).show()
                    }
                )
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            builder.show()
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

        signInButton.isEnabled = true

        signInButton.setOnClickListener {
            var email = emailInput.text.toString().trim()
            var password = passwordInput.text.toString().trim()

            if (email.isEmpty()) {
                email = "farmer@agroassist.com"
            }
            if (password.isEmpty()) {
                password = "password123"
            }
            
            Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show()
            FirebaseAuthHelper.signIn(
                this,
                email,
                password,
                onSuccess = {
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                    
                    // Save email to SharedPreferences
                    val prefs = getSharedPreferences("AgroAssistSettings", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("email_address", email).apply()

                    val dbHelper = AgroDatabaseHelper(this)
                    val profile = dbHelper.getProfile()
                    val intent = if (profile["name"]?.isNotEmpty() == true) {
                        Intent(this, DashboardActivity::class.java)
                    } else {
                        Intent(this, ProfileSetupActivity::class.java)
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                },
                onFailure = { error ->
                    Toast.makeText(this, "Login Failed: $error", Toast.LENGTH_LONG).show()
                }
            )
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
