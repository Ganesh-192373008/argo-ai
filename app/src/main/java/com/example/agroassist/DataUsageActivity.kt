package com.example.agroassist

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class DataUsageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_usage)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnClearCache = findViewById<Button>(R.id.btnClearCache)
        val btnDeleteScans = findViewById<Button>(R.id.btnDeleteScans)

        backButton.setOnClickListener { finish() }

        btnClearCache.setOnClickListener {
            // Placeholder logic
            finish()
        }

        btnDeleteScans.setOnClickListener {
            // Placeholder logic
            finish()
        }
    }
}
