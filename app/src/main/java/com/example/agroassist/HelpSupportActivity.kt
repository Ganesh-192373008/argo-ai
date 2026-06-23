package com.example.agroassist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HelpSupportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_support)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val layoutLiveChat = findViewById<LinearLayout>(R.id.layoutLiveChat)
        val layoutCallUs = findViewById<LinearLayout>(R.id.layoutCallUs)
        val layoutEmailSupport = findViewById<LinearLayout>(R.id.layoutEmailSupport)

        layoutLiveChat?.setOnClickListener {
            // Live Chat opens the AI Chat Assistant
            startActivity(Intent(this, ChatAssistantActivity::class.java))
        }

        layoutCallUs?.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:18001234567")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No dialer application found", Toast.LENGTH_SHORT).show()
            }
        }

        layoutEmailSupport?.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@agroassist.com")
                    putExtra(Intent.EXTRA_SUBJECT, "AgroAssist App Support Request")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email client found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
