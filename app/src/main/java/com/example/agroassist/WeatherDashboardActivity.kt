package com.example.agroassist

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WeatherDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_dashboard)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnPlanSeason = findViewById<Button>(R.id.btnPlanSeason)

        backButton.setOnClickListener { finish() }

        btnPlanSeason.setOnClickListener {
            startActivity(android.content.Intent(this, SeasonPlanningActivity::class.java))
        }
    }
}
