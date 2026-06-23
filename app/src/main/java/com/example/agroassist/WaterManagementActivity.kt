package com.example.agroassist

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class WaterManagementActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_management)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnCreateSchedule = findViewById<android.widget.Button>(R.id.btnCreateSchedule)
        
        backButton.setOnClickListener { finish() }
        
        btnCreateSchedule.setOnClickListener {
            startActivity(android.content.Intent(this, CreateScheduleActivity::class.java))
        }
    }
}
