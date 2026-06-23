package com.example.agroassist

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class InstagramShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instagram_share)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnCopyMessage = findViewById<Button>(R.id.btnCopyMessage)
        val btnGoBack = findViewById<LinearLayout>(R.id.btnGoBack)
        val btnCopyAgain = findViewById<LinearLayout>(R.id.btnCopyAgain)

        val reportText = "Crop Disease Detection Result\n\nEarly Blight detected in my Tomato crop. Confidence: 94%"

        backButton.setOnClickListener { finish() }
        btnGoBack.setOnClickListener { finish() }

        btnCopyMessage.setOnClickListener {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Crop Report", reportText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Content copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        btnCopyAgain.setOnClickListener {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Crop Report", reportText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Content copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    }
}
