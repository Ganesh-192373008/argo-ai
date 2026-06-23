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
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DailyReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_report)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnOtherReports = findViewById<Button>(R.id.btnOtherReports)
        val btnHome = findViewById<Button>(R.id.btnHome)

        backButton.setOnClickListener { finish() }
        btnOtherReports.setOnClickListener { finish() }

        btnHome.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        val dbHelper = AgroDatabaseHelper(this)
        loadDailyReport(dbHelper)
    }

    private fun loadDailyReport(dbHelper: AgroDatabaseHelper) {
        val historyList = dbHelper.getHistory()
        val now = java.util.Date()
        val oneDayMs = 24 * 60 * 60 * 1000L

        val dailyItems = historyList.filter {
            val timestampStr = it["timestamp"] ?: ""
            val date = dbHelper.parseTimestamp(timestampStr)
            if (date != null) {
                val diff = now.time - date.time
                diff in 0..oneDayMs
            } else {
                false
            }
        }

        val totalScans = dailyItems.size
        var healthyScans = 0
        var issuesScans = 0

        for (item in dailyItems) {
            val isHealthy = item["disease"]?.lowercase()?.contains("healthy") == true ||
                            item["disease"]?.lowercase()?.contains("no disease") == true
            if (isHealthy) healthyScans++ else issuesScans++
        }

        // 1. Update Date Header
        val dateFormatHeader = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        findViewById<TextView>(R.id.txtDailyReportDate)?.text = dateFormatHeader.format(now)

        // 2. Update Stats
        findViewById<TextView>(R.id.txtDailyTotalScans)?.text = totalScans.toString()
        findViewById<TextView>(R.id.txtDailyHealthyScans)?.text = healthyScans.toString()
        findViewById<TextView>(R.id.txtDailyIssuesScans)?.text = issuesScans.toString()

        // 3. Health Overview Success Rate
        val successRate = if (totalScans > 0) (healthyScans.toFloat() / totalScans * 100) else 100f
        val successText = String.format("%.1f%%", successRate)
        findViewById<TextView>(R.id.txtDailySuccessRate)?.text = successText

        // Progress Bar Ratio
        val dailyProgressBar = findViewById<LinearLayout>(R.id.dailyProgressBar)
        val progressGreen = findViewById<View>(R.id.dailyProgressBarGreen)
        val progressRed = findViewById<View>(R.id.dailyProgressBarRed)

        if (totalScans > 0) {
            dailyProgressBar.weightSum = totalScans.toFloat()

            val paramGreen = progressGreen.layoutParams as LinearLayout.LayoutParams
            paramGreen.weight = healthyScans.toFloat()
            paramGreen.width = 0
            progressGreen.layoutParams = paramGreen
            progressGreen.visibility = if (healthyScans > 0) View.VISIBLE else View.GONE

            val paramRed = progressRed.layoutParams as LinearLayout.LayoutParams
            paramRed.weight = issuesScans.toFloat()
            paramRed.width = 0
            progressRed.layoutParams = paramRed
            progressRed.visibility = if (issuesScans > 0) View.VISIBLE else View.GONE

            dailyProgressBar.visibility = View.VISIBLE
        } else {
            dailyProgressBar.visibility = View.GONE
        }

        findViewById<TextView>(R.id.txtDailyOverviewHealthyCount)?.text = healthyScans.toString()
        findViewById<TextView>(R.id.txtDailyOverviewHealthyPercent)?.text = if (totalScans > 0) {
            String.format("%.1f%% of scans", (healthyScans.toFloat() / totalScans * 100))
        } else {
            "0% of scans"
        }

        findViewById<TextView>(R.id.txtDailyOverviewIssuesCount)?.text = issuesScans.toString()
        findViewById<TextView>(R.id.txtDailyOverviewIssuesPercent)?.text = if (totalScans > 0) {
            String.format("%.1f%% of scans", (issuesScans.toFloat() / totalScans * 100))
        } else {
            "0% of scans"
        }

        // 4. Load Scans List
        val listContainer = findViewById<LinearLayout>(R.id.containerDailyActivity)
        listContainer.removeAllViews()

        val density = resources.displayMetrics.density
        if (dailyItems.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No scanning activity today."
                textSize = 14f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(0, (24 * density).toInt(), 0, (24 * density).toInt())
            }
            listContainer.addView(emptyText)
        } else {
            for (item in dailyItems) {
                addDailyActivityItem(listContainer, item, dbHelper)
            }
        }

        // 5. Hourly Activity Bar Chart
        val hourlyCounts = IntArray(12)
        val hourlyHasIssues = BooleanArray(12)

        for (item in dailyItems) {
            val date = dbHelper.parseTimestamp(item["timestamp"] ?: "") ?: continue
            val cal = java.util.Calendar.getInstance().apply { time = date }
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            if (hour in 8..19) {
                val index = hour - 8
                hourlyCounts[index]++
                val isHealthy = item["disease"]?.lowercase()?.contains("healthy") == true ||
                                item["disease"]?.lowercase()?.contains("no disease") == true
                if (!isHealthy) {
                    hourlyHasIssues[index] = true
                }
            }
        }

        val barIds = arrayOf(
            R.id.barHour8a, R.id.barHour9a, R.id.barHour10a, R.id.barHour11a,
            R.id.barHour12p, R.id.barHour1p, R.id.barHour2p, R.id.barHour3p,
            R.id.barHour4p, R.id.barHour5p, R.id.barHour6p, R.id.barHour7p
        )

        for (i in 0..11) {
            val barView = findViewById<View>(barIds[i]) ?: continue
            val count = hourlyCounts[i]
            val params = barView.layoutParams
            
            if (count > 0) {
                params.height = ((10 + count * 15) * density).toInt()
                barView.layoutParams = params
                if (hourlyHasIssues[i]) {
                    barView.setBackgroundColor(Color.parseColor("#E53935"))
                } else {
                    barView.setBackgroundColor(Color.parseColor("#388E3C"))
                }
            } else {
                params.height = (4 * density).toInt()
                barView.layoutParams = params
                barView.setBackgroundColor(Color.parseColor("#E0E0E0"))
            }
        }

        // 6. Contextual Recommendations
        val recContainer = findViewById<LinearLayout>(R.id.containerRecommendationsList)
        buildRecommendations(recContainer, dailyItems)
    }

    private fun addDailyActivityItem(container: LinearLayout, item: Map<String, String>, dbHelper: AgroDatabaseHelper) {
        val density = resources.displayMetrics.density

        val card = CardView(this).apply {
            radius = 8 * density
            cardElevation = 1 * density
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (8 * density).toInt())
            }
        }

        val itemLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
        }

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val cropText = TextView(this).apply {
            text = item["crop"] ?: "Crop"
            setTextColor(resources.getColor(R.color.text_primary, theme))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val space = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }

        val timestamp = item["timestamp"] ?: ""
        val timeOnly = try {
            val fullFormat = java.text.SimpleDateFormat("MMMM dd, yyyy hh:mm a", java.util.Locale.US)
            val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
            val date = fullFormat.parse(timestamp)
            if (date != null) timeFormat.format(date) else timestamp
        } catch (e: Exception) {
            timestamp
        }

        val timeText = TextView(this).apply {
            text = timeOnly
            setTextColor(resources.getColor(R.color.text_secondary, theme))
            textSize = 12f
        }

        row1.addView(cropText)
        row1.addView(space)
        row1.addView(timeText)

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, (4 * density).toInt(), 0, 0)
            }
        }

        val isHealthy = item["disease"]?.lowercase()?.contains("healthy") == true ||
                        item["disease"]?.lowercase()?.contains("no disease") == true

        val statusText = TextView(this).apply {
            text = if (isHealthy) "Healthy" else "${item["disease"]} Detected"
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())

            if (isHealthy) {
                setTextColor(Color.parseColor("#2E7D32"))
                val gd = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#E8F5E9"))
                    cornerRadius = 4 * density
                }
                background = gd
            } else {
                setTextColor(Color.parseColor("#D32F2F"))
                val gd = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#FFEBEE"))
                    cornerRadius = 4 * density
                }
                background = gd
            }
        }

        val space2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }

        val confidenceText = TextView(this).apply {
            text = "Confidence: ${item["confidence"] ?: "90%"}"
            setTextColor(resources.getColor(R.color.text_secondary, theme))
            textSize = 12f
        }

        row2.addView(statusText)
        row2.addView(space2)
        row2.addView(confidenceText)

        itemLayout.addView(row1)
        itemLayout.addView(row2)

        if (!isHealthy) {
            val viewDetailsText = TextView(this).apply {
                text = "View Details →"
                setTextColor(resources.getColor(R.color.primary_green, theme))
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, (8 * density).toInt(), 0, 0)
                }
                setOnClickListener {
                    val intent = Intent(this@DailyReportActivity, ReportActivity::class.java).apply {
                        putExtra("crop", item["crop"])
                        putExtra("disease", item["disease"])
                        val info = PlantVillageClassifier.classifyImage("", item["crop"])
                        putExtra("scientific", info.scientificName)
                        putExtra("symptoms", info.symptoms)
                        putExtra("causes", info.causes)
                        putExtra("treatment", info.treatment)
                    }
                    startActivity(intent)
                }
            }
            itemLayout.addView(viewDetailsText)
        }

        card.addView(itemLayout)
        container.addView(card)
    }

    private fun buildRecommendations(container: LinearLayout, dailyItems: List<Map<String, String>>) {
        container.removeAllViews()
        val density = resources.displayMetrics.density

        val issues = dailyItems.filter {
            val disease = it["disease"] ?: ""
            !(disease.lowercase().contains("healthy") || disease.lowercase().contains("no disease"))
        }

        val items = mutableListOf<Pair<String, String>>()
        if (issues.isNotEmpty()) {
            for (issue in issues) {
                val crop = issue["crop"] ?: "Crop"
                val disease = issue["disease"] ?: "Disease"
                items.add("Treat $disease on $crop" to "Apply recommended treatment or fungicide within 24 hours")
            }
            items.add("Continue Monitoring" to "Check other healthy crops every 2-3 days to prevent spread")
        } else {
            items.add("Continue Monitoring" to "Check your healthy crops every 2-3 days")
            items.add("Maintain Irrigation" to "Keep watering your crops based on the calendar guidelines")
        }

        for (index in items.indices) {
            val item = items[index]
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index < items.size - 1) {
                        setMargins(0, 0, 0, (12 * density).toInt())
                    }
                }
            }

            val numberCircle = TextView(this).apply {
                text = (index + 1).toString()
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER

                val gd = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor("#F57F17"))
                }
                background = gd
                layoutParams = LinearLayout.LayoutParams((24 * density).toInt(), (24 * density).toInt()).apply {
                    setMargins(0, 0, (8 * density).toInt(), 0)
                }
            }

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val title = TextView(this).apply {
                text = item.first
                setTextColor(Color.parseColor("#F57F17"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textSize = 14f
            }

            val sub = TextView(this).apply {
                text = item.second
                setTextColor(Color.parseColor("#F57F17"))
                textSize = 12f
                setPadding(0, (2 * density).toInt(), 0, 0)
            }

            textLayout.addView(title)
            textLayout.addView(sub)

            row.addView(numberCircle)
            row.addView(textLayout)

            container.addView(row)
        }
    }
}
