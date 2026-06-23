package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val backToSignInText = findViewById<TextView>(R.id.backToSignInText)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val sendLinkButton = findViewById<Button>(R.id.sendLinkButton)

        // Both the back arrow and "Back to Sign In" text close this screen
        backButton.setOnClickListener { finish() }
        backToSignInText.setOnClickListener { finish() }

        sendLinkButton.isEnabled = false

        emailInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val emailText = s.toString()
                val isEmailValid = emailText.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()

                // Enable button and turn it green if email is valid
                if (isEmailValid) {
                    sendLinkButton.setBackgroundColor(resources.getColor(R.color.primary_green, theme))
                    sendLinkButton.isEnabled = true
                } else {
                    sendLinkButton.setBackgroundColor(android.graphics.Color.parseColor("#D6D9E0"))
                    sendLinkButton.isEnabled = false
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        sendLinkButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            Toast.makeText(this, "Sending reset link...", Toast.LENGTH_SHORT).show()
            FirebaseAuthHelper.sendPasswordResetEmail(
                this,
                email,
                onSuccess = {
                    Toast.makeText(this, "Reset link sent! Please check your email inbox.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, CreateNewPasswordActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                },
                onFailure = { error ->
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
