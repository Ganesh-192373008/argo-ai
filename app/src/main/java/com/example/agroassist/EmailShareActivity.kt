package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EmailShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_share)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnCopyMessage = findViewById<Button>(R.id.btnCopyMessage)
        val btnGoBack = findViewById<LinearLayout>(R.id.btnGoBack)
        val btnOpenEmail = findViewById<LinearLayout>(R.id.btnOpenEmail)

        val emailSubject = "Crop Disease Detection Result"
        val emailBody = "Crop Disease Detection Result\n\nEarly Blight detected in my Tomato crop. Confidence: 94%"

        backButton.setOnClickListener { finish() }
        btnGoBack.setOnClickListener { finish() }

        btnCopyMessage.setOnClickListener {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Crop Report", emailBody)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Email content copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        btnOpenEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                putExtra(Intent.EXTRA_TEXT, emailBody)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                        putExtra(Intent.EXTRA_TEXT, emailBody)
                    }
                    startActivity(Intent.createChooser(chooserIntent, "Send Email via"))
                } catch (ex: Exception) {
                    Toast.makeText(this, "No email client found.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
