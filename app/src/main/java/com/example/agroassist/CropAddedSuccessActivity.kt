package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CropAddedSuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_added_success)

        val cropName = intent.getStringExtra("CROP_NAME") ?: "Crop"
        val cropEmoji = intent.getStringExtra("CROP_EMOJI") ?: "🌱"

        val textSuccessTitle = findViewById<TextView>(R.id.textSuccessTitle)
        val textSuccessCropEmoji = findViewById<TextView>(R.id.textSuccessCropEmoji)
        val btnSuccessDone = findViewById<Button>(R.id.btnSuccessDone)

        val btnTrackPlantingSchedule = findViewById<LinearLayout>(R.id.btnTrackPlantingSchedule)
        val btnMonitorGrowthStages = findViewById<LinearLayout>(R.id.btnMonitorGrowthStages)
        val btnSetReminders = findViewById<LinearLayout>(R.id.btnSetReminders)

        val successTitleText = "$cropName Added!"
        textSuccessTitle.text = successTitleText
        textSuccessCropEmoji.text = cropEmoji

        btnTrackPlantingSchedule.setOnClickListener {
            startActivity(Intent(this, PlantingTimesActivity::class.java))
        }

        btnMonitorGrowthStages.setOnClickListener {
            startActivity(Intent(this, AnalyticsReportsActivity::class.java))
        }

        btnSetReminders.setOnClickListener {
            startActivity(Intent(this, CreateScheduleActivity::class.java))
        }

        btnSuccessDone.setOnClickListener {
            // Simply finish and return to CropCalendarActivity which was under this
            finish()
        }
    }
}
