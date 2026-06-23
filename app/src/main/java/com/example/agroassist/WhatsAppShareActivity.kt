package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WhatsAppShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whatsapp_share)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnCopyMessage = findViewById<Button>(R.id.btnCopyMessage)
        val btnGoBack = findViewById<LinearLayout>(R.id.btnGoBack)
        val btnOpenWhatsApp = findViewById<LinearLayout>(R.id.btnOpenWhatsApp)

        val reportText = "Crop Disease Detection Result\n\nEarly Blight detected in my Tomato crop. Confidence: 94%"

        backButton.setOnClickListener { finish() }
        btnGoBack.setOnClickListener { finish() }

        btnCopyMessage.setOnClickListener {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Crop Report", reportText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Message copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        btnOpenWhatsApp.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
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
                    Toast.makeText(this, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
