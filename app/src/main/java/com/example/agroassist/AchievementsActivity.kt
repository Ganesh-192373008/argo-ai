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

    override fun onResume() {
        super.onResume()
        loadAchievementsData()
    }

    private fun loadAchievementsData() {
        val dbHelper = AgroDatabaseHelper(this)
        val profile = dbHelper.getProfile()
        val history = dbHelper.getHistory()
        val prefs = getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)

        val cropsStr = profile["crops"] ?: ""
        val cropsCount = cropsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.size
        val scansCount = history.size
        
        val aiQuestions = prefs.getInt("ai_questions_count", 7)
        val loginStreak = prefs.getInt("login_streak", 5)
        val isEarlyBird = prefs.getBoolean("early_bird_unlocked", true)
        val taskBonus = prefs.getInt("task_bonus_points", 0)

        val containerCompleted = findViewById<LinearLayout>(R.id.containerCompleted)
        val containerInProgress = findViewById<LinearLayout>(R.id.containerInProgress)

        var completedPoints = 0

        fun processAchievement(
            cardId: Int,
            pointsViewId: Int,
            progressLayoutId: Int,
            progressBarViewId: Int,
            progressTextViewId: Int,
            currentProgress: Int,
            targetProgress: Int,
            pointsReward: Int,
            isInitiallyCompleted: Boolean
        ) {
            val card = findViewById<CardView>(cardId) ?: return
            val pointsView = findViewById<TextView>(pointsViewId) ?: return
            val progressLayout = findViewById<LinearLayout>(progressLayoutId) ?: return
            val progressBarView = findViewById<View>(progressBarViewId) ?: return
            val progressTextView = findViewById<TextView>(progressTextViewId) ?: return

            val isCompleted = isInitiallyCompleted || (currentProgress >= targetProgress)
            val targetContainer = if (isCompleted) containerCompleted else containerInProgress

            // Move to correct container if not already there
            if (card.parent != targetContainer) {
                (card.parent as? ViewGroup)?.removeView(card)
                targetContainer?.addView(card)
            }

            if (isCompleted) {
                progressLayout.visibility = View.GONE
                pointsView.text = "+$pointsReward"
                pointsView.setTextColor(Color.parseColor("#F57C00"))
                completedPoints += pointsReward
            } else {
                progressLayout.visibility = View.VISIBLE
                pointsView.text = "$pointsReward pts"
                pointsView.setTextColor(Color.parseColor("#9E9E9E"))
                progressTextView.text = "Progress $currentProgress/$targetProgress"
                
                val safeProgress = currentProgress.coerceAtLeast(0).coerceAtMost(targetProgress)
                val layoutParams = progressBarView.layoutParams as? LinearLayout.LayoutParams
                if (layoutParams != null) {
                    layoutParams.weight = safeProgress.toFloat()
                    progressBarView.layoutParams = layoutParams
                }
                
                val progressContainer = progressBarView.parent as? LinearLayout
                progressContainer?.weightSum = targetProgress.toFloat()
            }
        }

        // 1. First Scan Achievement
        processAchievement(
            cardId = R.id.cardFirstScan,
            pointsViewId = R.id.tvFirstScanPoints,
            progressLayoutId = R.id.layoutFirstScanProgress,
            progressBarViewId = R.id.viewFirstScanProgress,
            progressTextViewId = R.id.tvFirstScanProgressText,
            currentProgress = scansCount,
            targetProgress = 1,
            pointsReward = 10,
            isInitiallyCompleted = scansCount >= 1
        )

        // 2. 5-Day Streak
        processAchievement(
            cardId = R.id.cardStreak,
            pointsViewId = R.id.tvStreakPoints,
            progressLayoutId = R.id.layoutStreakProgress,
            progressBarViewId = R.id.viewStreakProgress,
            progressTextViewId = R.id.tvStreakProgressText,
            currentProgress = loginStreak,
            targetProgress = 5,
            pointsReward = 25,
            isInitiallyCompleted = loginStreak >= 5
        )

        // 3. Early Bird
        processAchievement(
            cardId = R.id.cardEarlyBird,
            pointsViewId = R.id.tvEarlyBirdPoints,
            progressLayoutId = R.id.layoutEarlyBirdProgress,
            progressBarViewId = R.id.viewEarlyBirdProgress,
            progressTextViewId = R.id.tvEarlyBirdProgressText,
            currentProgress = if (isEarlyBird) 1 else 0,
            targetProgress = 1,
            pointsReward = 50,
            isInitiallyCompleted = isEarlyBird
        )

        // 4. Knowledge Seeker (10 AI questions)
        processAchievement(
            cardId = R.id.cardKnowledgeSeeker,
            pointsViewId = R.id.tvKnowledgeSeekerPoints,
            progressLayoutId = R.id.layoutKnowledgeSeekerProgress,
            progressBarViewId = R.id.viewKnowledgeSeekerProgress,
            progressTextViewId = R.id.tvKnowledgeSeekerProgressText,
            currentProgress = aiQuestions,
            targetProgress = 10,
            pointsReward = 30,
            isInitiallyCompleted = aiQuestions >= 10
        )

        // 5. Crop Master (5 crops)
        processAchievement(
            cardId = R.id.cardCropMaster,
            pointsViewId = R.id.tvCropMasterPoints,
            progressLayoutId = R.id.layoutCropMasterProgress,
            progressBarViewId = R.id.viewCropMasterProgress,
            progressTextViewId = R.id.tvCropMasterProgressText,
            currentProgress = cropsCount,
            targetProgress = 5,
            pointsReward = 20,
            isInitiallyCompleted = cropsCount >= 5
        )

        // Calculate total points
        val totalPoints = 200 + completedPoints + taskBonus
        val tvTotalPoints = findViewById<TextView>(R.id.tvTotalPoints)
        tvTotalPoints?.text = totalPoints.toString()

        // 6. Top Farmer
        processAchievement(
            cardId = R.id.cardTopFarmer,
            pointsViewId = R.id.tvTopFarmerPoints,
            progressLayoutId = R.id.layoutTopFarmerProgress,
            progressBarViewId = R.id.viewTopFarmerProgress,
            progressTextViewId = R.id.tvTopFarmerProgressText,
            currentProgress = totalPoints,
            targetProgress = 500,
            pointsReward = 100,
            isInitiallyCompleted = totalPoints >= 500
        )
    }
}
