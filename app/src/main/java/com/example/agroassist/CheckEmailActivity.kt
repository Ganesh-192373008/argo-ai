package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CheckEmailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_email)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val tryAgainText = findViewById<TextView>(R.id.tryAgainText)
        val emailAddressText = findViewById<TextView>(R.id.emailAddressText)

        val email = intent.getStringExtra("EXTRA_EMAIL") ?: "ganeshgidda4@gmail.com"
        emailAddressText.text = email

        backButton.setOnClickListener {
            finish()
        }

        tryAgainText.setOnClickListener {
            Toast.makeText(this, "Resending reset email to $email...", Toast.LENGTH_SHORT).show()
            BackendApiClient.sendPasswordResetLink(email) { success ->
                Toast.makeText(this, "Password reset instructions resent to $email!", Toast.LENGTH_LONG).show()
            }
        }

        // Tapping email container or text opens password reset link completion screen
        emailAddressText.setOnClickListener {
            val intent = Intent(this, CreateNewPasswordActivity::class.java).apply {
                putExtra("email", email)
            }
            startActivity(intent)
        }
    }
}
