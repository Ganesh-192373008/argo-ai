package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class VoiceAssistantActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_assistant)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnStartListening = findViewById<Button>(R.id.btnStartListening)
        val micIcon = findViewById<ImageView>(R.id.micIcon)

        backButton.setOnClickListener { finish() }

        val startListeningAction = {
            startActivity(Intent(this, VoiceListeningActivity::class.java))
        }

        btnStartListening.setOnClickListener { startListeningAction() }
        micIcon.setOnClickListener { startListeningAction() }
    }
}
