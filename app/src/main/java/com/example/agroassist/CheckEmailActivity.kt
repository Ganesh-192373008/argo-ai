package com.example.agroassist

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

        // Ideally, the email address is passed via Intent from the previous screen
        val email = intent.getStringExtra("EXTRA_EMAIL") ?: "ganeshgidda4@gmail.com"
        emailAddressText.text = email

        backButton.setOnClickListener {
            finish()
        }

        tryAgainText.setOnClickListener {
            Toast.makeText(this, "Resending email...", Toast.LENGTH_SHORT).show()
        }
    }
}
