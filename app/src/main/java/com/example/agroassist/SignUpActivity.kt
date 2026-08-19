package com.example.agroassist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val ageInput = findViewById<EditText>(R.id.ageInput)
        val locationInput = findViewById<EditText>(R.id.locationInput)
        val cropsInput = findViewById<EditText>(R.id.cropsInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val togglePassword = findViewById<ImageView>(R.id.togglePassword)
        val termsCheckBox = findViewById<CheckBox>(R.id.termsCheckBox)
        val createAccountButton = findViewById<Button>(R.id.createAccountButton)
        val signInText = findViewById<TextView>(R.id.signInText)

        val googleButton = findViewById<android.view.View>(R.id.googleButtonContainer)

        backButton.setOnClickListener { finish() }

        signInText.setOnClickListener {
            val intent = Intent(this, EmailLoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        googleButton?.setOnClickListener {
            Toast.makeText(this, "Opening Google Sign-In...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, EmailLoginActivity::class.java)
            startActivity(intent)
        }

        togglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            passwordInput.inputType = if (isPasswordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            togglePassword.alpha = if (isPasswordVisible) 0.5f else 1.0f
            passwordInput.setSelection(passwordInput.text.length)
        }

        createAccountButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val age = ageInput.text.toString().trim().ifEmpty { "30" }
            val location = locationInput.text.toString().trim().ifEmpty { "India" }
            val crops = cropsInput.text.toString().trim().ifEmpty { "Wheat, Rice" }
            val password = passwordInput.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your Full Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid Email Address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!termsCheckBox.isChecked) {
                Toast.makeText(this, "Please agree to the Terms of Service to proceed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Creating your AgroAssist account...", Toast.LENGTH_SHORT).show()

            BackendApiClient.register(this, name, email, password, age, crops, location) { success, message, token, userId ->
                if (success && token != null) {
                    SessionManager.saveSession(this, token, userId, email, name)
                    Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    if (message.lowercase().contains("already exists")) {
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Account Already Exists")
                            .setMessage("An account with email '$email' already exists. Please sign in.")
                            .setPositiveButton("Sign In") { _, _ ->
                                val intent = Intent(this, EmailLoginActivity::class.java).apply {
                                    putExtra("PREFILL_EMAIL", email)
                                }
                                startActivity(intent)
                                finish()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    } else {
                        Toast.makeText(this, "Registration Failed: $message", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
