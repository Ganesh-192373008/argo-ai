package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PasswordSuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_success)

        val loginButton = findViewById<Button>(R.id.loginButton)

        val navigateToLogin = {
            val intent = Intent(this, EmailLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        loginButton.setOnClickListener {
            navigateToLogin()
        }

        // Auto redirect after 2.5 seconds matching design screen animation
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing && !isDestroyed) {
                navigateToLogin()
            }
        }, 2500)
    }
}
