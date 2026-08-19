package com.example.agroassist

import android.content.Context
import android.widget.Toast

object AchievementTracker {

    /**
     * Add points when user completes any task anywhere in the app
     */
    fun addPoints(context: Context, points: Int, taskName: String = "Task") {
        val prefs = context.getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)
        val currentBonus = prefs.getInt("task_bonus_points", 0)
        val newBonus = currentBonus + points
        prefs.edit().putInt("task_bonus_points", newBonus).apply()

        Toast.makeText(context, "🎉 $taskName Completed! +$points Points Earned!", Toast.LENGTH_LONG).show()
    }

    /**
     * Increment AI question counter when user asks AI assistant
     */
    fun incrementAIQuestions(context: Context) {
        val prefs = context.getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)
        val current = prefs.getInt("ai_questions_count", 7)
        val next = current + 1
        prefs.edit().putInt("ai_questions_count", next).apply()

        if (next == 10) {
            addPoints(context, 30, "Knowledge Seeker Achievement")
        } else {
            addPoints(context, 5, "Asked AI Question")
        }
    }

    /**
     * Trigger when disease scan is completed
     */
    fun onDiseaseScanCompleted(context: Context) {
        addPoints(context, 15, "Disease Detection Scan")
    }

    /**
     * Trigger when user creates or completes a schedule task
     */
    fun onScheduleTaskCompleted(context: Context) {
        addPoints(context, 15, "Farming Task")
    }

    /**
     * Trigger when user shares a post in community
     */
    fun onCommunityPostCreated(context: Context) {
        addPoints(context, 10, "Community Post")
    }

    /**
     * Get total score including completed tasks and achievements
     */
    fun getTotalPoints(context: Context): Int {
        val dbHelper = AgroDatabaseHelper(context)
        val profile = dbHelper.getProfile()
        val history = dbHelper.getHistory()
        val prefs = context.getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)

        val cropsStr = profile["crops"] ?: ""
        val cropsCount = cropsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.size
        val scansCount = history.size
        val aiQuestions = prefs.getInt("ai_questions_count", 7)
        val loginStreak = prefs.getInt("login_streak", 5)
        val isEarlyBird = prefs.getBoolean("early_bird_unlocked", true)
        val taskBonus = prefs.getInt("task_bonus_points", 0)

        var points = 200 + taskBonus
        if (scansCount >= 1) points += 10
        if (loginStreak >= 5) points += 25
        if (isEarlyBird) points += 50
        if (aiQuestions >= 10) points += 30
        if (cropsCount >= 5) points += 20

        return points
    }
}
