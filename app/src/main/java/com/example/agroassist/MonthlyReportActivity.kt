package com.example.agroassist

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MonthlyReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_report)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val dbHelper = AgroDatabaseHelper(this)
        loadMonthlyReport(dbHelper)
    }

    private fun loadMonthlyReport(dbHelper: AgroDatabaseHelper) {
        val historyList = dbHelper.getHistory()
        val now = java.util.Date()
        val thirtyDaysMs = 30 * 24 * 60 * 60 * 1000L

        val monthlyItems = historyList.filter {
            val timestampStr = it["timestamp"] ?: ""
            val date = dbHelper.parseTimestamp(timestampStr)
            if (date != null) {
                val diff = now.time - date.time
                diff in 0..thirtyDaysMs
            } else {
                false
            }
        }

        val totalScans = monthlyItems.size
        var healthyScans = 0
        var issuesScans = 0

        for (item in monthlyItems) {
            val isHealthy = item["disease"]?.lowercase()?.contains("healthy") == true ||
                            item["disease"]?.lowercase()?.contains("no disease") == true
            if (isHealthy) healthyScans++ else issuesScans++
        }

        // 1. Current Month filter label
        val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val calendar = java.util.Calendar.getInstance()
        val currentMonthIndex = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        findViewById<TextView>(R.id.txtMonthlyReportFilter)?.text = "${monthNames[currentMonthIndex]} $currentYear"

        // 2. Stats Overview
        findViewById<TextView>(R.id.txtMonthlyTotalScans)?.text = totalScans.toString()
        val successRate = if (totalScans > 0) (healthyScans.toFloat() / totalScans * 100) else 100f
        findViewById<TextView>(R.id.txtMonthlySuccessRate)?.text = String.format("%.0f%%", successRate)

        // 3. Stacked Chart monthly trend for last 4 months
        val shortMonthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        
        // Month 4 (Current)
        val m4Index = calendar.get(java.util.Calendar.MONTH)
        val m4Year = calendar.get(java.util.Calendar.YEAR)
        val m4Name = shortMonthNames[m4Index]

        calendar.add(java.util.Calendar.MONTH, -1)
        val m3Index = calendar.get(java.util.Calendar.MONTH)
        val m3Year = calendar.get(java.util.Calendar.YEAR)
        val m3Name = shortMonthNames[m3Index]

        calendar.add(java.util.Calendar.MONTH, -1)
        val m2Index = calendar.get(java.util.Calendar.MONTH)
        val m2Year = calendar.get(java.util.Calendar.YEAR)
        val m2Name = shortMonthNames[m2Index]

        calendar.add(java.util.Calendar.MONTH, -1)
        val m1Index = calendar.get(java.util.Calendar.MONTH)
        val m1Year = calendar.get(java.util.Calendar.YEAR)
        val m1Name = shortMonthNames[m1Index]

        // Update Labels
        findViewById<TextView>(R.id.txtMonthLabel1)?.text = m1Name
        findViewById<TextView>(R.id.txtMonthLabel2)?.text = m2Name
        findViewById<TextView>(R.id.txtMonthLabel3)?.text = m3Name
        findViewById<TextView>(R.id.txtMonthLabel4)?.text = m4Name

        var m1Healthy = 0
        var m1Diseased = 0
        var m2Healthy = 0
        var m2Diseased = 0
        var m3Healthy = 0
        var m3Diseased = 0
        var m4Healthy = 0
        var m4Diseased = 0

        for (item in historyList) {
            val date = dbHelper.parseTimestamp(item["timestamp"] ?: "") ?: continue
            val cal = java.util.Calendar.getInstance().apply { time = date }
            val m = cal.get(java.util.Calendar.MONTH)
            val y = cal.get(java.util.Calendar.YEAR)

            val isHealthy = item["disease"]?.lowercase()?.contains("healthy") == true ||
                            item["disease"]?.lowercase()?.contains("no disease") == true

            if (m == m1Index && y == m1Year) {
                if (isHealthy) m1Healthy++ else m1Diseased++
            } else if (m == m2Index && y == m2Year) {
                if (isHealthy) m2Healthy++ else m2Diseased++
            } else if (m == m3Index && y == m3Year) {
                if (isHealthy) m3Healthy++ else m3Diseased++
            } else if (m == m4Index && y == m4Year) {
                if (isHealthy) m4Healthy++ else m4Diseased++
            }
        }

        val density = resources.displayMetrics.density

        setMonthBarHeights(
            findViewById(R.id.barMonth1Diseased), findViewById(R.id.barMonth1Healthy),
            findViewById(R.id.txtMonth1DiseasedCount), findViewById(R.id.txtMonth1HealthyCount),
            m1Healthy, m1Diseased, density
        )
        setMonthBarHeights(
            findViewById(R.id.barMonth2Diseased), findViewById(R.id.barMonth2Healthy),
            findViewById(R.id.txtMonth2DiseasedCount), findViewById(R.id.txtMonth2HealthyCount),
            m2Healthy, m2Diseased, density
        )
        setMonthBarHeights(
            findViewById(R.id.barMonth3Diseased), findViewById(R.id.barMonth3Healthy),
            findViewById(R.id.txtMonth3DiseasedCount), findViewById(R.id.txtMonth3HealthyCount),
            m3Healthy, m3Diseased, density
        )
        setMonthBarHeights(
            findViewById(R.id.barMonth4Diseased), findViewById(R.id.barMonth4Healthy),
            findViewById(R.id.txtMonth4DiseasedCount), findViewById(R.id.txtMonth4HealthyCount),
            m4Healthy, m4Diseased, density
        )

        // 4. Top Detected Diseases
        val issuesList = monthlyItems.filter {
            val disease = it["disease"] ?: ""
            !(disease.lowercase().contains("healthy") || disease.lowercase().contains("no disease"))
        }

        val diseaseCounts = issuesList.groupBy { it["disease"] ?: "Unknown" }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        val containerTopDiseases = findViewById<LinearLayout>(R.id.containerMonthlyTopDiseases)
        containerTopDiseases.removeAllViews()

        if (diseaseCounts.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No diseases detected this month."
                textSize = 14f
                setTextColor(Color.GRAY)
                setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
            }
            containerTopDiseases.addView(emptyText)
        } else {
            for (index in diseaseCounts.indices) {
                val (diseaseName, count) = diseaseCounts[index]
                val labelText = if (count == 1) "1 detection" else "$count detections"

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
                    gravity = Gravity.CENTER_VERTICAL
                }

                val title = TextView(this).apply {
                    text = diseaseName
                    setTextColor(resources.getColor(R.color.text_primary, theme))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val countView = TextView(this).apply {
                    text = labelText
                    setTextColor(Color.parseColor(if (index == 0) "#E53935" else "#FB8C00"))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textSize = 14f
                }

                row.addView(title)
                row.addView(countView)

                containerTopDiseases.addView(row)

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
                    containerTopDiseases.addView(divider)
                }
            }
        }

        // 5. Dynamic Analysis Summary Text
        val txtAnalysis = findViewById<TextView>(R.id.txtMonthlyAnalysis)
        if (monthlyItems.isEmpty()) {
            txtAnalysis?.text = "No analytics available for this month. Start scanning crops to build your monthly analysis report."
        } else {
            val mostCommonDisease = if (diseaseCounts.isNotEmpty()) diseaseCounts[0].first else null
            if (mostCommonDisease != null) {
                txtAnalysis?.text = String.format("Your crops show a %.1f%% health success rate this month. %s is the most common issue. Consider preventive measures during monsoon season.", successRate, mostCommonDisease)
            } else {
                txtAnalysis?.text = String.format("Your crops show a %.1f%% health success rate this month. Excellent crop health maintained overall. Keep up the monitoring!", successRate)
            }
        }
    }

    private fun setMonthBarHeights(
        diseasedView: View?, healthyView: View?,
        diseasedCountText: TextView?, healthyCountText: TextView?,
        healthyCount: Int, diseasedCount: Int, density: Float
    ) {
        if (diseasedCountText != null) diseasedCountText.text = diseasedCount.toString()
        if (healthyCountText != null) healthyCountText.text = healthyCount.toString()

        if (diseasedView != null) {
            val p = diseasedView.layoutParams
            p.height = (diseasedCount * 8 * density).toInt()
            diseasedView.layoutParams = p
            diseasedView.visibility = if (diseasedCount > 0) View.VISIBLE else View.GONE
        }

        if (healthyView != null) {
            val p = healthyView.layoutParams
            p.height = (healthyCount * 8 * density).toInt()
            healthyView.layoutParams = p
            healthyView.visibility = if (healthyCount > 0) View.VISIBLE else View.GONE
        }

        if (healthyCount == 0 && diseasedCount == 0) {
            if (healthyView != null) {
                val p = healthyView.layoutParams
                p.height = (4 * density).toInt()
                healthyView.layoutParams = p
                healthyView.setBackgroundColor(Color.parseColor("#E0E0E0"))
                healthyView.visibility = View.VISIBLE
            }
            if (healthyCountText != null) healthyCountText.text = "0"
            if (diseasedCountText != null) diseasedCountText.text = "0"
        } else {
            healthyView?.setBackgroundColor(Color.parseColor("#388E3C"))
        }
    }
}
