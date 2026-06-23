package com.example.agroassist

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class AchievementsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        loadAchievementsData()
    }

    private fun loadAchievementsData() {
        val dbHelper = AgroDatabaseHelper(this)
        val profile = dbHelper.getProfile()
        val history = dbHelper.getHistory()
        val prefs = getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)

        // 1. Data counts
        val cropsStr = profile["crops"] ?: ""
        val cropsCount = cropsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.size
        val scansCount = history.size
        
        // Fetch stats from SharedPreferences (mock fallbacks matching mockup if not set)
        val aiQuestions = prefs.getInt("ai_questions_count", 7)
        val loginStreak = prefs.getInt("login_streak", 5)
        val isEarlyBird = prefs.getBoolean("early_bird_unlocked", true)

        // Containers
        val containerCompleted = findViewById<LinearLayout>(R.id.containerCompleted)
        val containerInProgress = findViewById<LinearLayout>(R.id.containerInProgress)

        var completedPoints = 0

        // Helper function to update and place achievement CardView
        fun processAchievement(
            card: CardView,
            pointsView: TextView,
            progressLayout: LinearLayout,
            progressBarView: View,
            progressTextView: TextView,
            currentProgress: Int,
            targetProgress: Int,
            pointsReward: Int,
            isInitiallyCompleted: Boolean
        ): Boolean {
            val isCompleted = isInitiallyCompleted || (currentProgress >= targetProgress)
            
            // Remove from parent layout
            val parent = card.parent as? ViewGroup
            parent?.removeView(card)

            if (isCompleted) {
                // Completed styling
                progressLayout.visibility = View.GONE
                pointsView.text = "+$pointsReward"
                pointsView.setTextColor(Color.parseColor("#F57C00")) // Orange style
                
                // Add to completed container
                containerCompleted.addView(card)
                completedPoints += pointsReward
                return true
            } else {
                // In progress styling
                progressLayout.visibility = View.VISIBLE
                pointsView.text = "$pointsReward pts"
                pointsView.setTextColor(Color.parseColor("#9E9E9E")) // Gray style
                progressTextView.text = "Progress $currentProgress/$targetProgress"
                
                // Set progress bar width/weight
                val layoutParams = progressBarView.layoutParams as LinearLayout.LayoutParams
                layoutParams.weight = currentProgress.toFloat()
                progressBarView.layoutParams = layoutParams
                
                // Update weightSum of parent container
                val progressContainer = progressBarView.parent as? LinearLayout
                progressContainer?.weightSum = targetProgress.toFloat()

                // Add to in-progress container
                containerInProgress.addView(card)
                return false
            }
        }

        // Process each achievement card
        val cardFirstScan = findViewById<CardView>(R.id.cardFirstScan)
        val tvFirstScanPoints = findViewById<TextView>(R.id.tvFirstScanPoints)
        val layoutFirstScanProgress = findViewById<LinearLayout>(R.id.layoutFirstScanProgress)
        val viewFirstScanProgress = findViewById<View>(R.id.viewFirstScanProgress)
        val tvFirstScanProgressText = findViewById<TextView>(R.id.tvFirstScanProgressText)
        
        processAchievement(
            card = cardFirstScan,
            pointsView = tvFirstScanPoints,
            progressLayout = layoutFirstScanProgress,
            progressBarView = viewFirstScanProgress,
            progressTextView = tvFirstScanProgressText,
            currentProgress = scansCount,
            targetProgress = 1,
            pointsReward = 10,
            isInitiallyCompleted = scansCount >= 1
        )

        val cardStreak = findViewById<CardView>(R.id.cardStreak)
        val tvStreakPoints = findViewById<TextView>(R.id.tvStreakPoints)
        val layoutStreakProgress = findViewById<LinearLayout>(R.id.layoutStreakProgress)
        val viewStreakProgress = findViewById<View>(R.id.viewStreakProgress)
        val tvStreakProgressText = findViewById<TextView>(R.id.tvStreakProgressText)
        
        processAchievement(
            card = cardStreak,
            pointsView = tvStreakPoints,
            progressLayout = layoutStreakProgress,
            progressBarView = viewStreakProgress,
            progressTextView = tvStreakProgressText,
            currentProgress = loginStreak,
            targetProgress = 5,
            pointsReward = 25,
            isInitiallyCompleted = loginStreak >= 5
        )

        val cardEarlyBird = findViewById<CardView>(R.id.cardEarlyBird)
        val tvEarlyBirdPoints = findViewById<TextView>(R.id.tvEarlyBirdPoints)
        val layoutEarlyBirdProgress = findViewById<LinearLayout>(R.id.layoutEarlyBirdProgress)
        val viewEarlyBirdProgress = findViewById<View>(R.id.viewEarlyBirdProgress)
        val tvEarlyBirdProgressText = findViewById<TextView>(R.id.tvEarlyBirdProgressText)
        
        processAchievement(
            card = cardEarlyBird,
            pointsView = tvEarlyBirdPoints,
            progressLayout = layoutEarlyBirdProgress,
            progressBarView = viewEarlyBirdProgress,
            progressTextView = tvEarlyBirdProgressText,
            currentProgress = if (isEarlyBird) 1 else 0,
            targetProgress = 1,
            pointsReward = 50,
            isInitiallyCompleted = isEarlyBird
        )

        val cardKnowledgeSeeker = findViewById<CardView>(R.id.cardKnowledgeSeeker)
        val tvKnowledgeSeekerPoints = findViewById<TextView>(R.id.tvKnowledgeSeekerPoints)
        val layoutKnowledgeSeekerProgress = findViewById<LinearLayout>(R.id.layoutKnowledgeSeekerProgress)
        val viewKnowledgeSeekerProgress = findViewById<View>(R.id.viewKnowledgeSeekerProgress)
        val tvKnowledgeSeekerProgressText = findViewById<TextView>(R.id.tvKnowledgeSeekerProgressText)
        
        processAchievement(
            card = cardKnowledgeSeeker,
            pointsView = tvKnowledgeSeekerPoints,
            progressLayout = layoutKnowledgeSeekerProgress,
            progressBarView = viewKnowledgeSeekerProgress,
            progressTextView = tvKnowledgeSeekerProgressText,
            currentProgress = aiQuestions,
            targetProgress = 10,
            pointsReward = 30,
            isInitiallyCompleted = aiQuestions >= 10
        )

        val cardCropMaster = findViewById<CardView>(R.id.cardCropMaster)
        val tvCropMasterPoints = findViewById<TextView>(R.id.tvCropMasterPoints)
        val layoutCropMasterProgress = findViewById<LinearLayout>(R.id.layoutCropMasterProgress)
        val viewCropMasterProgress = findViewById<View>(R.id.viewCropMasterProgress)
        val tvCropMasterProgressText = findViewById<TextView>(R.id.tvCropMasterProgressText)
        
        processAchievement(
            card = cardCropMaster,
            pointsView = tvCropMasterPoints,
            progressLayout = layoutCropMasterProgress,
            progressBarView = viewCropMasterProgress,
            progressTextView = tvCropMasterProgressText,
            currentProgress = cropsCount,
            targetProgress = 5,
            pointsReward = 20,
            isInitiallyCompleted = cropsCount >= 5
        )

        // Top Farmer is based on total points. Total target is 500.
        // Base points is 200 (completed items not shown like tutorial, login setup, etc.).
        val totalPoints = 200 + completedPoints
        val tvTotalPoints = findViewById<TextView>(R.id.tvTotalPoints)
        tvTotalPoints.text = totalPoints.toString()

        val cardTopFarmer = findViewById<CardView>(R.id.cardTopFarmer)
        val tvTopFarmerPoints = findViewById<TextView>(R.id.tvTopFarmerPoints)
        val layoutTopFarmerProgress = findViewById<LinearLayout>(R.id.layoutTopFarmerProgress)
        val viewTopFarmerProgress = findViewById<View>(R.id.viewTopFarmerProgress)
        val tvTopFarmerProgressText = findViewById<TextView>(R.id.tvTopFarmerProgressText)
        
        processAchievement(
            card = cardTopFarmer,
            pointsView = tvTopFarmerPoints,
            progressLayout = layoutTopFarmerProgress,
            progressBarView = viewTopFarmerProgress,
            progressTextView = tvTopFarmerProgressText,
            currentProgress = totalPoints,
            targetProgress = 500,
            pointsReward = 100,
            isInitiallyCompleted = totalPoints >= 500
        )
    }
}
