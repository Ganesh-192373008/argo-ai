package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class AnalyticsReportsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics_reports)

        val prefs = getSharedPreferences("AgroAssistPrefs", MODE_PRIVATE)
        val dbHelper = AgroDatabaseHelper(this)
        if (!prefs.getBoolean("has_seeded_history", false)) {
            dbHelper.seedMockHistory()
            prefs.edit().putBoolean("has_seeded_history", true).apply()
        }

        val backButton = findViewById<ImageView>(R.id.backButton)
        val cardMonthlyReport = findViewById<CardView>(R.id.cardMonthlyReport)
        val cardWeeklyReport = findViewById<CardView>(R.id.cardWeeklyReport)
        val cardDailyReport = findViewById<CardView>(R.id.cardDailyReport)
        
        backButton.setOnClickListener { finish() }

        cardMonthlyReport.setOnClickListener {
            startActivity(Intent(this, MonthlyReportActivity::class.java))
        }
        
        cardWeeklyReport.setOnClickListener {
            startActivity(Intent(this, WeeklyReportActivity::class.java))
        }

        cardDailyReport.setOnClickListener {
            startActivity(Intent(this, DailyReportActivity::class.java))
        }

        updateStats(dbHelper)
    }

    private fun updateStats(dbHelper: AgroDatabaseHelper) {
        val historyList = dbHelper.getHistory()
        val now = java.util.Date()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val sevenDaysMs = 7 * oneDayMs
        val thirtyDaysMs = 30 * oneDayMs

        var dailyScans = 0
        var dailyHealthy = 0
        var dailyDiseases = 0

        var weeklyScans = 0
        var weeklyHealthy = 0
        var weeklyDiseases = 0

        var monthlyScans = 0
        var monthlyHealthy = 0
        var monthlyDiseases = 0

        for (item in historyList) {
            val timestampStr = item["timestamp"] ?: ""
            val date = dbHelper.parseTimestamp(timestampStr) ?: continue
            val diff = now.time - date.time
            if (diff < 0) continue

            val isHealthy = item["disease"]?.lowercase()?.contains("healthy") == true ||
                            item["disease"]?.lowercase()?.contains("no disease") == true

            if (diff <= oneDayMs) {
                dailyScans++
                if (isHealthy) dailyHealthy++ else dailyDiseases++
            }
            if (diff <= sevenDaysMs) {
                weeklyScans++
                if (isHealthy) weeklyHealthy++ else weeklyDiseases++
            }
            if (diff <= thirtyDaysMs) {
                monthlyScans++
                if (isHealthy) monthlyHealthy++ else monthlyDiseases++
            }
        }

        findViewById<android.widget.TextView>(R.id.txtDailyScans)?.text = dailyScans.toString()
        findViewById<android.widget.TextView>(R.id.txtDailyDiseases)?.text = dailyDiseases.toString()
        findViewById<android.widget.TextView>(R.id.txtDailyHealthy)?.text = dailyHealthy.toString()

        findViewById<android.widget.TextView>(R.id.txtWeeklyScans)?.text = weeklyScans.toString()
        findViewById<android.widget.TextView>(R.id.txtWeeklyDiseases)?.text = weeklyDiseases.toString()
        findViewById<android.widget.TextView>(R.id.txtWeeklyHealthy)?.text = weeklyHealthy.toString()

        findViewById<android.widget.TextView>(R.id.txtMonthlyScans)?.text = monthlyScans.toString()
        findViewById<android.widget.TextView>(R.id.txtMonthlyDiseases)?.text = monthlyDiseases.toString()
        findViewById<android.widget.TextView>(R.id.txtMonthlyHealthy)?.text = monthlyHealthy.toString()
    }
}
