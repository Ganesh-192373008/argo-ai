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

class WeeklyReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_report)

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
        loadWeeklyReport(dbHelper)
    }

    private fun loadWeeklyReport(dbHelper: AgroDatabaseHelper) {
        val historyList = dbHelper.getHistory()
        val now = java.util.Date()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val sevenDaysMs = 7 * oneDayMs
        val fourteenDaysMs = 14 * oneDayMs

        // Current week items
        val weeklyItems = historyList.filter {
            val timestampStr = it["timestamp"] ?: ""
            val date = dbHelper.parseTimestamp(timestampStr)
            if (date != null) {
                val diff = now.time - date.time
                diff in 0..sevenDaysMs
            } else {
                false
            }
        }

        // Previous week items
        val prevWeeklyItems = historyList.filter {
            val timestampStr = it["timestamp"] ?: ""
            val date = dbHelper.parseTimestamp(timestampStr)
            if (date != null) {
                val diff = now.time - date.time
                diff in (sevenDaysMs + 1)..fourteenDaysMs
            } else {
                false
            }
        }

        val totalScans = weeklyItems.size
        var healthyScans = 0
        var issuesScans = 0

        for (item in weeklyItems) {
            val isHealthy = item["disease"]?.lowercase()?.contains("healthy") == true ||
                            item["disease"]?.lowercase()?.contains("no disease") == true
            if (isHealthy) healthyScans++ else issuesScans++
        }

        // 1. Date Header Subtitle
        val dateFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.US)
        val calendar = java.util.Calendar.getInstance()
        val endRangeStr = dateFormat.format(now)
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -6)
        val startRangeStr = dateFormat.format(calendar.time)
        val yearFormat = java.text.SimpleDateFormat("yyyy", java.util.Locale.US)
        val yearStr = yearFormat.format(now)
        findViewById<TextView>(R.id.txtWeeklyReportDate)?.text = "$startRangeStr - $endRangeStr, $yearStr"

        // 2. Update Stats
        findViewById<TextView>(R.id.txtWeeklyTotalScans)?.text = totalScans.toString()
        findViewById<TextView>(R.id.txtWeeklyHealthyScans)?.text = healthyScans.toString()
        findViewById<TextView>(R.id.txtWeeklyIssuesScans)?.text = issuesScans.toString()

        // 3. Weekly Trend calculations
        val currRate = if (totalScans > 0) (healthyScans.toFloat() / totalScans * 100) else 100f
        findViewById<TextView>(R.id.txtWeeklyHealthRate)?.text = String.format("%.1f%%", currRate)

        // Previous week stats
        var prevTotal = prevWeeklyItems.size
        var prevHealthy = 0
        for (item in prevWeeklyItems) {
            val isHealthy = item["disease"]?.lowercase()?.contains("healthy") == true ||
                            item["disease"]?.lowercase()?.contains("no disease") == true
            if (isHealthy) prevHealthy++
        }
        val prevRate = if (prevTotal > 0) (prevHealthy.toFloat() / prevTotal * 100) else 100f
        val diffRate = currRate - prevRate

        val trendPercentView = findViewById<TextView>(R.id.txtWeeklyTrendPercent)
        val trendSubtitleView = findViewById<TextView>(R.id.txtWeeklyTrendSubtitle)

        if (diffRate >= 0) {
            trendPercentView?.text = String.format("+%.1f%%", diffRate)
            trendPercentView?.setTextColor(Color.parseColor("#2E7D32"))
            trendPercentView?.setBackgroundColor(Color.parseColor("#E8F5E9"))
            trendSubtitleView?.text = String.format("Improved from %.1f%% last week", prevRate)
            trendSubtitleView?.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            trendPercentView?.text = String.format("%.1f%%", diffRate)
            trendPercentView?.setTextColor(Color.parseColor("#D32F2F"))
            trendPercentView?.setBackgroundColor(Color.parseColor("#FFEBEE"))
            trendSubtitleView?.text = String.format("Decreased from %.1f%% last week", prevRate)
            trendSubtitleView?.setTextColor(Color.parseColor("#D32F2F"))
        }

        // 4. Daily Chart Columns (Mon-Sun)
        val dayOfWeekHealthy = IntArray(7)
        val dayOfWeekIssues = IntArray(7)
        val dayOfWeekTotal = IntArray(7)

        for (item in weeklyItems) {
            val date = dbHelper.parseTimestamp(item["timestamp"] ?: "") ?: continue
            val cal = java.util.Calendar.getInstance().apply { time = date }
            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)

            val index = when (dayOfWeek) {
                java.util.Calendar.MONDAY -> 0
                java.util.Calendar.TUESDAY -> 1
                java.util.Calendar.WEDNESDAY -> 2
                java.util.Calendar.THURSDAY -> 3
                java.util.Calendar.FRIDAY -> 4
                java.util.Calendar.SATURDAY -> 5
                java.util.Calendar.SUNDAY -> 6
                else -> 0
            }

            val isHealthy = item["disease"]?.lowercase()?.contains("healthy") == true ||
                            item["disease"]?.lowercase()?.contains("no disease") == true
            if (isHealthy) {
                dayOfWeekHealthy[index]++
            } else {
                dayOfWeekIssues[index]++
            }
            dayOfWeekTotal[index]++
        }

        val healthyBarIds = arrayOf(
            R.id.barMonHealthy, R.id.barTueHealthy, R.id.barWedHealthy, R.id.barThuHealthy,
            R.id.barFriHealthy, R.id.barSatHealthy, R.id.barSunHealthy
        )
        val issuesBarIds = arrayOf(
            R.id.barMonIssues, R.id.barTueIssues, R.id.barWedIssues, R.id.barThuIssues,
            R.id.barFriIssues, R.id.barSatIssues, R.id.barSunIssues
        )
        val countTextIds = arrayOf(
            R.id.txtMonCount, R.id.txtTueCount, R.id.txtWedCount, R.id.txtThuCount,
            R.id.txtFriCount, R.id.txtSatCount, R.id.txtSunCount
        )

        val density = resources.displayMetrics.density

        for (i in 0..6) {
            val barHealthy = findViewById<View>(healthyBarIds[i]) ?: continue
            val barIssues = findViewById<View>(issuesBarIds[i]) ?: continue
            val textCount = findViewById<TextView>(countTextIds[i]) ?: continue

            val hCount = dayOfWeekHealthy[i]
            val iCount = dayOfWeekIssues[i]
            val tCount = dayOfWeekTotal[i]

            textCount.text = tCount.toString()

            val pHealthy = barHealthy.layoutParams
            pHealthy.height = (hCount * 12 * density).toInt()
            barHealthy.layoutParams = pHealthy
            barHealthy.visibility = if (hCount > 0) View.VISIBLE else View.GONE

            val pIssues = barIssues.layoutParams
            pIssues.height = (iCount * 12 * density).toInt()
            barIssues.layoutParams = pIssues
            barIssues.visibility = if (iCount > 0) View.VISIBLE else View.GONE

            if (tCount == 0) {
                textCount.text = "0"
                pHealthy.height = (4 * density).toInt()
                barHealthy.layoutParams = pHealthy
                barHealthy.setBackgroundColor(Color.parseColor("#E0E0E0"))
                barHealthy.visibility = View.VISIBLE
            } else {
                barHealthy.setBackgroundColor(Color.parseColor("#388E3C"))
            }
        }

        // 5. Top Issues This Week
        val issuesList = weeklyItems.filter {
            val disease = it["disease"] ?: ""
            !(disease.lowercase().contains("healthy") || disease.lowercase().contains("no disease"))
        }

        val diseaseCounts = issuesList.groupBy { it["disease"] ?: "Unknown" }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        val containerTopIssues = findViewById<LinearLayout>(R.id.containerWeeklyTopIssues)
        containerTopIssues.removeAllViews()

        if (diseaseCounts.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No issues detected this week."
                textSize = 14f
                setTextColor(Color.GRAY)
                setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
            }
            containerTopIssues.addView(emptyText)
        } else {
            val totalIssues = issuesList.size
            for (index in diseaseCounts.indices) {
                val (diseaseName, count) = diseaseCounts[index]
                val pct = (count.toFloat() / totalIssues * 100)

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
                    gravity = Gravity.CENTER_VERTICAL
                }

                val leftLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val title = TextView(this).apply {
                    text = diseaseName
                    setTextColor(resources.getColor(R.color.text_primary, theme))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textSize = 14f
                }

                val sub = TextView(this).apply {
                    text = String.format("%.1f%% of all issues detected", pct)
                    setTextColor(resources.getColor(R.color.text_secondary, theme))
                    textSize = 12f
                    setPadding(0, (4 * density).toInt(), 0, 0)
                }

                leftLayout.addView(title)
                leftLayout.addView(sub)

                val rightText = TextView(this).apply {
                    text = "$count cases"
                    setTextColor(Color.parseColor(if (index == 0) "#E53935" else "#FB8C00"))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textSize = 14f
                }

                row.addView(leftLayout)
                row.addView(rightText)

                containerTopIssues.addView(row)

                if (index < diseaseCounts.size - 1) {
                    val divider = View(this).apply {
                        setBackgroundColor(Color.parseColor("#EEEEEE"))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            (1 * density).toInt()
                        ).apply {
                            setMargins((16 * density).toInt(), 0, (16 * density).toInt(), 0)
                        }
                    }
                    containerTopIssues.addView(divider)
                }
            }
        }

        // 6. Crop Performance
        val cropGroups = weeklyItems.groupBy { it["crop"] ?: "Unknown" }
        val containerCropPerformance = findViewById<LinearLayout>(R.id.containerCropPerformance)
        containerCropPerformance.removeAllViews()

        val cropList = cropGroups.map { (cropName, list) ->
            val total = list.size
            val healthy = list.count {
                it["disease"]?.lowercase()?.contains("healthy") == true ||
                it["disease"]?.lowercase()?.contains("no disease") == true
            }
            val rate = (healthy.toFloat() / total * 100).toInt()
            Triple(cropName, total, healthy) to rate
        }.sortedByDescending { it.first.second }

        if (cropList.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No crop performance data."
                textSize = 14f
                setTextColor(Color.GRAY)
                setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
            }
            containerCropPerformance.addView(emptyText)
        } else {
            for (index in cropList.indices) {
                val (cropInfo, rate) = cropList[index]
                val (cropName, total, healthy) = cropInfo

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
                    gravity = Gravity.CENTER_VERTICAL
                }

                val leftLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val title = TextView(this).apply {
                    text = cropName
                    setTextColor(resources.getColor(R.color.text_primary, theme))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textSize = 14f
                }

                val sub = TextView(this).apply {
                    text = "$total scans • $healthy healthy"
                    setTextColor(resources.getColor(R.color.text_secondary, theme))
                    textSize = 12f
                    setPadding(0, (4 * density).toInt(), 0, 0)
                }

                leftLayout.addView(title)
                leftLayout.addView(sub)

                val rightText = TextView(this).apply {
                    text = "$rate%"
                    setTextColor(Color.parseColor(if (rate == 100) "#2E7D32" else if (rate >= 70) "#FB8C00" else "#D32F2F"))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textSize = 14f
                }

                row.addView(leftLayout)
                row.addView(rightText)

                containerCropPerformance.addView(row)

                if (index < cropList.size - 1) {
                    val divider = View(this).apply {
                        setBackgroundColor(Color.parseColor("#EEEEEE"))
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            (1 * density).toInt()
                        ).apply {
                            setMargins((16 * density).toInt(), 0, (16 * density).toInt(), 0)
                        }
                    }
                    containerCropPerformance.addView(divider)
                }
            }
        }

        // 7. Weekly Insights
        val containerInsights = findViewById<LinearLayout>(R.id.containerWeeklyInsights)
        buildWeeklyInsights(containerInsights, weeklyItems, currRate, prevRate, dayOfWeekTotal, cropList)
    }

    private fun buildWeeklyInsights(
        container: LinearLayout,
        weeklyItems: List<Map<String, String>>,
        currRate: Float,
        prevRate: Float,
        dayOfWeekTotal: IntArray,
        cropList: List<Pair<Triple<String, Int, Int>, Int>>
    ) {
        container.removeAllViews()
        val density = resources.displayMetrics.density

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (12 * density).toInt())
            }
        }

        val headerEmoji = TextView(this).apply {
            text = "📊"
            textSize = 16f
            setPadding(0, 0, (8 * density).toInt(), 0)
        }

        val headerTitle = TextView(this).apply {
            text = "Weekly Insights"
            setTextColor(Color.parseColor("#2E7D32"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = 16f
        }

        headerLayout.addView(headerEmoji)
        headerLayout.addView(headerTitle)
        container.addView(headerLayout)

        val diffRate = currRate - prevRate
        val bullet1Text = when {
            diffRate > 0 -> Pair("Improvement Detected", String.format("Health rate improved by %.1f%% compared to last week", diffRate))
            diffRate < 0 -> Pair("Health Decline", String.format("Health rate declined by %.1f%% compared to last week", -diffRate))
            else -> Pair("Health Stable", "Health rate remained stable compared to last week")
        }
        addInsightBullet(container, "✓ ", "#2E7D32", bullet1Text.first, bullet1Text.second)

        val days = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        var maxScans = -1
        var activeDay = "Thursday"
        for (i in 0..6) {
            if (dayOfWeekTotal[i] > maxScans) {
                maxScans = dayOfWeekTotal[i]
                activeDay = days[i]
            }
        }
        val bullet2Text = if (maxScans > 0) {
            Pair("Most Active Day", "$activeDay with $maxScans scans - Keep up the monitoring!")
        } else {
            Pair("Monitoring Activity", "Start scanning your crops to get daily activity insights")
        }
        addInsightBullet(container, "⚡ ", "#F57C00", bullet2Text.first, bullet2Text.second)

        val bestPerformers = cropList.filter { it.second == 100 }
        val bestCropStr = if (bestPerformers.isNotEmpty()) {
            bestPerformers.joinToString(" and ") { it.first.first }
        } else {
            cropList.maxByOrNull { it.second }?.first?.first ?: "All crops"
        }

        val bullet3Text = if (cropList.isNotEmpty()) {
            Pair("Best Performers", "$bestCropStr maintained the highest health success rate")
        } else {
            Pair("Best Performers", "All crops are healthy - No scans performed yet")
        }
        addInsightBullet(container, "🏆 ", "#1976D2", bullet3Text.first, bullet3Text.second)
    }

    private fun addInsightBullet(container: LinearLayout, prefix: String, colorStr: String, titleStr: String, descStr: String) {
        val density = resources.displayMetrics.density

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (8 * density).toInt())
            }
        }

        val prefixText = TextView(this).apply {
            text = prefix
            setTextColor(Color.parseColor(colorStr))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = titleStr
            setTextColor(Color.parseColor(colorStr))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = 14f
        }

        val desc = TextView(this).apply {
            text = descStr
            setTextColor(Color.parseColor(colorStr))
            textSize = 12f
            setPadding(0, (2 * density).toInt(), 0, 0)
        }

        textCol.addView(title)
        textCol.addView(desc)

        row.addView(prefixText)
        row.addView(textCol)

        container.addView(row)
    }
}
