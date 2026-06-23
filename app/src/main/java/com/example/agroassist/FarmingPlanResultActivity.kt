package com.example.agroassist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.NotificationCompat

class FarmingPlanResultActivity : AppCompatActivity() {

    private lateinit var farmSize: String
    private lateinit var soilType: String
    private lateinit var waterSource: String
    private lateinit var previousCrops: String
    private lateinit var targetMarket: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_farming_plan_result)

        // Find Input Views
        val txtResultFarmSize = findViewById<TextView>(R.id.txtResultFarmSize)
        val txtResultSoilType = findViewById<TextView>(R.id.txtResultSoilType)
        val txtResultWaterSource = findViewById<TextView>(R.id.txtResultWaterSource)
        val txtResultPreviousCrops = findViewById<TextView>(R.id.txtResultPreviousCrops)
        val txtResultTargetMarket = findViewById<TextView>(R.id.txtResultTargetMarket)

        // Retrieve passed inputs
        farmSize = intent.getStringExtra("farm_size") ?: "Less than 1 hectare"
        soilType = intent.getStringExtra("soil_type") ?: "Loamy"
        waterSource = intent.getStringExtra("water_source") ?: "Multiple sources"
        previousCrops = intent.getStringExtra("last_season_crops") ?: "Mixed crops"
        targetMarket = intent.getStringExtra("target_market") ?: "Direct consumers"

        // Set inputs
        txtResultFarmSize.text = farmSize
        txtResultSoilType.text = soilType
        txtResultWaterSource.text = waterSource
        txtResultPreviousCrops.text = previousCrops
        txtResultTargetMarket.text = targetMarket

        // Actions
        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnDownloadPDF = findViewById<AppCompatButton>(R.id.btnDownloadPDF)
        val btnSharePlan = findViewById<AppCompatButton>(R.id.btnSharePlan)
        val btnDone = findViewById<AppCompatButton>(R.id.btnDone)

        backButton.setOnClickListener { finish() }

        btnDownloadPDF.setOnClickListener {
            downloadPlanAsPDF()
        }

        btnSharePlan.setOnClickListener {
            sharePlanNatively()
        }

        btnDone.setOnClickListener {
            // Launch CropCalendarActivity and finish result screen
            startActivity(Intent(this, CropCalendarActivity::class.java))
            finish()
        }
    }

    private fun downloadPlanAsPDF() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Generating custom plan PDF...")
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            setCancelable(false)
            show()
        }

        // Simulate compiling PDF for 1.5 seconds
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postDelayed({
            progressDialog.dismiss()
            Toast.makeText(this, "Farming plan PDF downloaded successfully!", Toast.LENGTH_LONG).show()
            sendDownloadNotification()
        }, 1500)
    }

    private fun sharePlanNatively() {
        val planSummary = """
            🌱 My Personalized Farming Plan Details:
            ----------------------------------------
            📊 Inputs Profile:
            - Farm Size: $farmSize
            - Soil Type: $soilType
            - Water Source: $waterSource
            - Previous Crops: $previousCrops
            - Target Market: $targetMarket
            
            ✅ Key Recommendations:
            - Legume crop rotation for nitrogen levels
            - Precision drip irrigation installation
            - 3-stage organic fertilizer schedule
            - Weekly disease monitoring inspect routines
            - High-value crop selection matching market trends
            
            📅 Timeline Summary:
            - Week 1: Soil preparation & testing
            - Week 2: Install irrigation system
            - Week 3: Begin crop planting
            - Ongoing: Care & monitoring
            
            Generated using AgroAssist AI!
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My Custom Farming Plan")
            putExtra(Intent.EXTRA_TEXT, planSummary)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Farming Plan via"))
    }

    private fun sendDownloadNotification() {
        val channelId = "agro_alerts_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel if on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AgroAssist Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Market prices, schemes, downloads and alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, CropCalendarActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Farming Plan Download Complete")
            .setContentText("agro_assist_farming_plan.pdf saved successfully.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(999, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
