package com.example.agroassist

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DetectionHistoryActivity : AppCompatActivity() {

    private lateinit var dbHelper: AgroDatabaseHelper
    private lateinit var container: LinearLayout
    private lateinit var txtTotalScans: TextView
    private lateinit var txtHighRisk: TextView
    private lateinit var txtHealthy: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection_history)

        dbHelper = AgroDatabaseHelper(this)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnViewAnalytics = findViewById<Button>(R.id.btnViewAnalytics)
        val btnDeleteHistory = findViewById<Button>(R.id.btnDeleteHistory)
        container = findViewById(R.id.historyListContainer)
        txtTotalScans = findViewById(R.id.txtTotalScans)
        txtHighRisk = findViewById(R.id.txtHighRisk)
        txtHealthy = findViewById(R.id.txtHealthy)

        backButton.setOnClickListener { finish() }

        btnViewAnalytics.setOnClickListener {
            startActivity(Intent(this, AnalyticsReportsActivity::class.java))
        }

        btnDeleteHistory.setOnClickListener {
            // Confirm deletion
            AlertDialog.Builder(this)
                .setTitle("Delete History")
                .setMessage("Are you sure you want to clear your entire detection history?")
                .setPositiveButton("Delete") { _, _ ->
                    dbHelper.clearHistory()
                    Toast.makeText(this, "History cleared successfully", Toast.LENGTH_SHORT).show()
                    loadHistory()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        loadHistory()
    }

    private fun loadHistory() {
        container.removeAllViews()
        val historyList = dbHelper.getHistory()

        // Calculate stats
        val total = historyList.size
        var healthyCount = 0
        var highRiskCount = 0

        for (item in historyList) {
            val isHealthy = item["disease"]?.lowercase()?.contains("healthy") == true || 
                            item["disease"]?.lowercase()?.contains("no disease") == true
            if (isHealthy) {
                healthyCount++
            } else {
                highRiskCount++
            }
        }

        txtTotalScans.text = total.toString()
        txtHealthy.text = healthyCount.toString()
        txtHighRisk.text = highRiskCount.toString()

        val density = resources.displayMetrics.density

        if (historyList.isEmpty()) {
            val emptyTextView = TextView(this).apply {
                text = "No detection history found."
                textSize = 16f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, (32 * density).toInt(), 0, 0)
                }
            }
            container.addView(emptyTextView)
        } else {
            for (item in historyList) {
                val card = CardView(this).apply {
                    radius = 12 * density
                    cardElevation = 2 * density
                    useCompatPadding = true
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, (12 * density).toInt())
                    }
                }

                val itemLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
                }

                val rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val cropEmoji = when (item["crop"]?.lowercase()) {
                    "tomato" -> "🍅"
                    "potato" -> "🥔"
                    "wheat" -> "🌾"
                    "rice" -> "🌾"
                    else -> "🌿"
                }

                val emojiText = TextView(this).apply {
                    text = cropEmoji
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.parseColor("#E8F5E9"))
                    layoutParams = LinearLayout.LayoutParams((40 * density).toInt(), (40 * density).toInt())
                }

                val textColLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins((12 * density).toInt(), 0, 0, 0)
                    }
                }

                val titleText = TextView(this).apply {
                    text = item["disease"] ?: "Unknown disease"
                    setTextColor(resources.getColor(R.color.text_primary, theme))
                    textSize = 16f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                val subText = TextView(this).apply {
                    text = item["crop"] ?: "Unknown crop"
                    setTextColor(resources.getColor(R.color.text_secondary, theme))
                    textSize = 12f
                    setPadding(0, (2 * density).toInt(), 0, 0)
                }

                textColLayout.addView(titleText)
                textColLayout.addView(subText)

                val rightColLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.END
                }

                val confidenceText = TextView(this).apply {
                    text = item["confidence"] ?: "Confidence"
                    textSize = 12f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())
                    
                    val isHealthy = item["disease"]?.lowercase()?.contains("healthy") == true || item["disease"]?.lowercase()?.contains("no disease") == true
                    if (isHealthy) {
                        setBackgroundColor(Color.parseColor("#E8F5E9"))
                        setTextColor(Color.parseColor("#2E7D32"))
                    } else {
                        setBackgroundColor(Color.parseColor("#FFEBEE"))
                        setTextColor(Color.parseColor("#D32F2F"))
                    }
                }

                rightColLayout.addView(confidenceText)

                rowLayout.addView(emojiText)
                rowLayout.addView(textColLayout)
                rowLayout.addView(rightColLayout)

                val divider = View(this).apply {
                    setBackgroundColor(Color.parseColor("#EEEEEE"))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (1 * density).toInt()
                    ).apply {
                        setMargins(0, (12 * density).toInt(), 0, (12 * density).toInt())
                    }
                }

                val timeText = TextView(this).apply {
                    text = item["timestamp"] ?: ""
                    setTextColor(resources.getColor(R.color.text_secondary, theme))
                    textSize = 12f
                }

                itemLayout.addView(rowLayout)
                itemLayout.addView(divider)
                itemLayout.addView(timeText)

                card.addView(itemLayout)
                container.addView(card)
            }
        }
    }
}
