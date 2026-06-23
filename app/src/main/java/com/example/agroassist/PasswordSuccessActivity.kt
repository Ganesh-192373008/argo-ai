package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PasswordSuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_success)

        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            Toast.makeText(this, "Navigating to Login Screen...", Toast.LENGTH_SHORT).show()
            
            // Navigate back to the Email Login screen and clear the activity stack
            val intent = Intent(this, EmailLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
