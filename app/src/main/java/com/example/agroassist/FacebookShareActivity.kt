package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FacebookShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_facebook_share)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnCopyMessage = findViewById<Button>(R.id.btnCopyMessage)
        val btnGoBack = findViewById<LinearLayout>(R.id.btnGoBack)
        val btnOpenFacebook = findViewById<LinearLayout>(R.id.btnOpenFacebook)

        val reportText = "Early Blight detected in my Tomato crop. Confidence: 94%"

        backButton.setOnClickListener { finish() }
        btnGoBack.setOnClickListener { finish() }

        btnCopyMessage.setOnClickListener {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Crop Report", reportText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Facebook post copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        btnOpenFacebook.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.facebook.katana")
                putExtra(Intent.EXTRA_TEXT, reportText)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, reportText)
                    }
                    startActivity(Intent.createChooser(fallbackIntent, "Share via"))
                } catch (ex: Exception) {
                    Toast.makeText(this, "Facebook is not installed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
