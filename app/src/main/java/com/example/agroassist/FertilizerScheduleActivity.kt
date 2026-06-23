package com.example.agroassist

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Locale

class FertilizerScheduleActivity : AppCompatActivity() {

    data class ScheduleItem(
        val id: String,
        val crop: String,
        val type: String,
        val detail: String,
        val date: String,
        val time: String,
        val isStatic: Boolean = false,
        var isCompleted: Boolean = false
    )

    private val schedulesList = mutableListOf<ScheduleItem>()
    private var currentTab = TabType.ALL

    enum class TabType {
        UPCOMING, TODAY, MISSED, ALL
    }

    private lateinit var tvTotalSchedules: TextView
    private lateinit var tvStatUpcomingVal: TextView
    private lateinit var tvStatTodayVal: TextView
    private lateinit var tvStatMissedVal: TextView

    private lateinit var tabUpcoming: TextView
    private lateinit var tabToday: TextView
    private lateinit var tabMissed: TextView
    private lateinit var tabAll: TextView

    private lateinit var indicatorUpcoming: View
    private lateinit var indicatorToday: View
    private lateinit var indicatorMissed: View
    private lateinit var indicatorAll: View

    private lateinit var cardMissedAlert: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fertilizer_schedule)

        // Initialize Views
        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnCreateFertilizerSchedule = findViewById<Button>(R.id.btnCreateFertilizerSchedule)

        tvTotalSchedules = findViewById(R.id.tvTotalSchedules)
        tvStatUpcomingVal = findViewById(R.id.tvStatUpcomingVal)
        tvStatTodayVal = findViewById(R.id.tvStatTodayVal)
        tvStatMissedVal = findViewById(R.id.tvStatMissedVal)

        tabUpcoming = findViewById(R.id.tabUpcoming)
        tabToday = findViewById(R.id.tabToday)
        tabMissed = findViewById(R.id.tabMissed)
        tabAll = findViewById(R.id.tabAll)

        indicatorUpcoming = findViewById(R.id.indicatorUpcoming)
        indicatorToday = findViewById(R.id.indicatorToday)
        indicatorMissed = findViewById(R.id.indicatorMissed)
        indicatorAll = findViewById(R.id.indicatorAll)

        cardMissedAlert = findViewById(R.id.cardMissedAlert)

        // Setup Tab Layouts
        val layoutTabUpcoming = findViewById<LinearLayout>(R.id.layoutTabUpcoming)
        val layoutTabToday = findViewById<LinearLayout>(R.id.layoutTabToday)
        val layoutTabMissed = findViewById<LinearLayout>(R.id.layoutTabMissed)
        val layoutTabAll = findViewById<LinearLayout>(R.id.layoutTabAll)

        // Setup Stats Cards
        val cardStatUpcoming = findViewById<CardView>(R.id.cardStatUpcoming)
        val cardStatToday = findViewById<CardView>(R.id.cardStatToday)
        val cardStatMissed = findViewById<CardView>(R.id.cardStatMissed)

        // Click Listeners
        backButton.setOnClickListener { finish() }

        btnCreateFertilizerSchedule.setOnClickListener {
            startActivity(Intent(this, AddFertilizerScheduleActivity::class.java))
        }

        // Tab click listeners
        layoutTabUpcoming.setOnClickListener { selectTab(TabType.UPCOMING) }
        layoutTabToday.setOnClickListener { selectTab(TabType.TODAY) }
        layoutTabMissed.setOnClickListener { selectTab(TabType.MISSED) }
        layoutTabAll.setOnClickListener { selectTab(TabType.ALL) }

        // Stats card click listeners
        cardStatUpcoming.setOnClickListener { selectTab(TabType.UPCOMING) }
        cardStatToday.setOnClickListener { selectTab(TabType.TODAY) }
        cardStatMissed.setOnClickListener { selectTab(TabType.MISSED) }

        // Alert Banner click listener
        cardMissedAlert.setOnClickListener { selectTab(TabType.MISSED) }
    }

    override fun onResume() {
        super.onResume()
        loadSchedules()
        updateUI()
        selectTab(currentTab)
    }

    private fun loadSchedules() {
        val static1Completed = schedulesList.find { it.id == "static_1" }?.isCompleted ?: false
        val static2Completed = schedulesList.find { it.id == "static_2" }?.isCompleted ?: false

        schedulesList.clear()

        // 1. Add static mock items
        schedulesList.add(
            ScheduleItem(
                id = "static_1",
                crop = "Potato",
                type = "Fertilizer",
                detail = "Urea",
                date = "25 Apr 2026",
                time = "1:03 PM",
                isStatic = true,
                isCompleted = static1Completed
            )
        )
        schedulesList.add(
            ScheduleItem(
                id = "static_2",
                crop = "Potato",
                type = "Fertilizer",
                detail = "Ferrous Sulphate",
                date = "30 Apr 2026",
                time = "1:37 PM",
                isStatic = true,
                isCompleted = static2Completed
            )
        )

        // 2. Load database items
        val dbHelper = AgroDatabaseHelper(this)
        val dbSchedules = dbHelper.getSchedules()
        for (item in dbSchedules) {
            val id = item["id"] ?: ""
            val crop = item["crop"] ?: ""
            val type = item["type"] ?: ""
            val detail = item["detail"] ?: ""
            val date = item["date"] ?: ""
            val time = item["time"] ?: ""
            
            schedulesList.add(
                ScheduleItem(
                    id = id,
                    crop = crop,
                    type = type,
                    detail = detail,
                    date = date,
                    time = time,
                    isStatic = false,
                    isCompleted = false
                )
            )
        }
    }

    private fun updateUI() {
        var missedCount = 0
        var upcomingCount = 0
        var todayCount = 0
        var totalActiveCount = 0

        for (item in schedulesList) {
            if (item.isCompleted) continue
            
            totalActiveCount++
            val status = getScheduleStatus(item)
            when (status) {
                TabType.MISSED -> missedCount++
                TabType.UPCOMING -> upcomingCount++
                TabType.TODAY -> todayCount++
                TabType.ALL -> {}
            }
        }

        tvTotalSchedules.text = "$totalActiveCount total schedules"
        tvStatUpcomingVal.text = upcomingCount.toString()
        tvStatTodayVal.text = todayCount.toString()
        tvStatMissedVal.text = missedCount.toString()

        tabUpcoming.text = "Upcoming ($upcomingCount)"
        tabToday.text = "Today ($todayCount)"
        tabMissed.text = "Missed ($missedCount)"
        tabAll.text = "All ($totalActiveCount)"

        if (missedCount == 0) {
            cardMissedAlert.visibility = View.GONE
        } else {
            cardMissedAlert.visibility = View.VISIBLE
        }
    }

    private fun selectTab(tabType: TabType) {
        currentTab = tabType

        // Update tab styles
        updateTabSelectionUI(tabType)

        // Clear dynamic layout
        val layoutScheduleList = findViewById<LinearLayout>(R.id.layoutScheduleList)
        layoutScheduleList.removeAllViews()

        // Filter and display cards
        for (item in schedulesList) {
            if (item.isCompleted) continue

            val status = getScheduleStatus(item)
            val shouldShow = when (tabType) {
                TabType.ALL -> true
                TabType.UPCOMING -> status == TabType.UPCOMING
                TabType.TODAY -> status == TabType.TODAY
                TabType.MISSED -> status == TabType.MISSED
            }

            if (shouldShow) {
                val cardView = layoutInflater.inflate(R.layout.item_fertilizer_schedule, layoutScheduleList, false)
                
                val tvCropEmoji = cardView.findViewById<TextView>(R.id.tvCropEmoji)
                val tvCropName = cardView.findViewById<TextView>(R.id.tvCropName)
                val tvStatusTag = cardView.findViewById<TextView>(R.id.tvStatusTag)
                val tvFertilizerName = cardView.findViewById<TextView>(R.id.tvFertilizerName)
                val tvDetailLabel = cardView.findViewById<TextView>(R.id.tvDetailLabel)
                val tvDetailValue = cardView.findViewById<TextView>(R.id.tvDetailValue)
                val tvScheduleDate = cardView.findViewById<TextView>(R.id.tvScheduleDate)
                val tvScheduleTime = cardView.findViewById<TextView>(R.id.tvScheduleTime)
                val btnMarkComplete = cardView.findViewById<Button>(R.id.btnMarkComplete)

                // Bind content
                tvCropName.text = item.crop
                tvFertilizerName.text = item.detail
                tvScheduleDate.text = "📅 ${item.date}"
                tvScheduleTime.text = "⏰ ${item.time}"

                // Dynamic Crop Emoji
                tvCropEmoji.text = getCropEmoji(item.crop)

                // Style based on status
                when (status) {
                    TabType.MISSED -> {
                        tvCropEmoji.setBackgroundColor(Color.parseColor("#FFEBEE"))
                        
                        tvStatusTag.text = "Missed"
                        tvStatusTag.setTextColor(Color.parseColor("#D32F2F"))
                        tvStatusTag.setBackgroundColor(Color.parseColor("#FFEBEE"))
                        
                        tvScheduleDate.setTextColor(Color.parseColor("#D32F2F"))
                        tvScheduleTime.setTextColor(Color.parseColor("#D32F2F"))
                        
                        btnMarkComplete.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F"))
                    }
                    TabType.TODAY -> {
                        tvCropEmoji.setBackgroundColor(Color.parseColor("#E8F5E9"))
                        
                        tvStatusTag.text = "Today"
                        tvStatusTag.setTextColor(Color.parseColor("#2E7D32"))
                        tvStatusTag.setBackgroundColor(Color.parseColor("#E8F5E9"))
                        
                        tvScheduleDate.setTextColor(Color.parseColor("#2E7D32"))
                        tvScheduleTime.setTextColor(Color.parseColor("#2E7D32"))
                        
                        btnMarkComplete.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D32"))
                    }
                    TabType.UPCOMING -> {
                        tvCropEmoji.setBackgroundColor(Color.parseColor("#E3F2FD"))
                        
                        tvStatusTag.text = "Upcoming"
                        tvStatusTag.setTextColor(Color.parseColor("#1565C0"))
                        tvStatusTag.setBackgroundColor(Color.parseColor("#E3F2FD"))
                        
                        tvScheduleDate.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                        tvScheduleTime.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                        
                        btnMarkComplete.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_green))
                    }
                    else -> {}
                }

                if (item.isStatic) {
                    tvDetailLabel.text = "Quantity"
                    tvDetailValue.text = if (item.id == "static_1") "2" else "5"
                } else {
                    tvDetailLabel.text = "Type"
                    tvDetailValue.text = item.type
                }

                btnMarkComplete.setOnClickListener {
                    item.isCompleted = true
                    if (item.isStatic) {
                        Toast.makeText(this, "${item.crop} schedule marked as complete", Toast.LENGTH_SHORT).show()
                    } else {
                        val db = AgroDatabaseHelper(this)
                        db.deleteSchedule(item.id.toInt())
                        Toast.makeText(this, "${item.crop} schedule marked as complete", Toast.LENGTH_SHORT).show()
                    }
                    updateUI()
                    selectTab(currentTab)
                }

                layoutScheduleList.addView(cardView)
            }
        }
    }

    private fun updateTabSelectionUI(selectedTab: TabType) {
        val activeColor = ContextCompat.getColor(this, R.color.primary_green)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)

        tabUpcoming.setTextColor(inactiveColor)
        tabUpcoming.typeface = Typeface.DEFAULT
        indicatorUpcoming.visibility = View.INVISIBLE

        tabToday.setTextColor(inactiveColor)
        tabToday.typeface = Typeface.DEFAULT
        indicatorToday.visibility = View.INVISIBLE

        tabMissed.setTextColor(inactiveColor)
        tabMissed.typeface = Typeface.DEFAULT
        indicatorMissed.visibility = View.INVISIBLE

        tabAll.setTextColor(inactiveColor)
        tabAll.typeface = Typeface.DEFAULT
        indicatorAll.visibility = View.INVISIBLE

        when (selectedTab) {
            TabType.UPCOMING -> {
                tabUpcoming.setTextColor(activeColor)
                tabUpcoming.typeface = Typeface.DEFAULT_BOLD
                indicatorUpcoming.visibility = View.VISIBLE
            }
            TabType.TODAY -> {
                tabToday.setTextColor(activeColor)
                tabToday.typeface = Typeface.DEFAULT_BOLD
                indicatorToday.visibility = View.VISIBLE
            }
            TabType.MISSED -> {
                tabMissed.setTextColor(activeColor)
                tabMissed.typeface = Typeface.DEFAULT_BOLD
                indicatorMissed.visibility = View.VISIBLE
            }
            TabType.ALL -> {
                tabAll.setTextColor(activeColor)
                tabAll.typeface = Typeface.DEFAULT_BOLD
                indicatorAll.visibility = View.VISIBLE
            }
        }
    }

    private fun getScheduleStatus(item: ScheduleItem): TabType {
        if (item.id == "static_1") return TabType.MISSED
        if (item.id == "static_2") return TabType.UPCOMING
        
        val cal = parseDateTime(item.date, item.time) ?: return TabType.UPCOMING
        val now = Calendar.getInstance()

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val calDate = Calendar.getInstance().apply {
            time = cal.time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return when {
            calDate.timeInMillis < today.timeInMillis -> {
                TabType.MISSED
            }
            calDate.timeInMillis == today.timeInMillis -> {
                TabType.TODAY
            }
            else -> {
                TabType.UPCOMING
            }
        }
    }

    private fun parseDateTime(dateStr: String, timeStr: String): Calendar? {
        try {
            val calendar = Calendar.getInstance()
            if (dateStr.contains("/")) {
                val dateParts = dateStr.split("/")
                if (dateParts.size == 3) {
                    val day = dateParts[0].toInt()
                    val month = dateParts[1].toInt() - 1
                    val year = dateParts[2].toInt()
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                }
            } else {
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy", Locale.US)
                val date = sdf.parse(dateStr) ?: return null
                calendar.time = date
            }

            val timeParts = timeStr.split(" ")
            if (timeParts.size == 2) {
                val isPm = timeParts[1].equals("PM", ignoreCase = true)
                val hm = timeParts[0].split(":")
                if (hm.size == 2) {
                    var hour = hm[0].toInt()
                    val minute = hm[1].toInt()
                    if (isPm && hour < 12) hour += 12
                    if (!isPm && hour == 12) hour = 0
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                }
            }
            return calendar
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getCropEmoji(cropName: String): String {
        val cropLower = cropName.lowercase(Locale.getDefault())
        return when {
            cropLower.contains("potato") -> "🥔"
            cropLower.contains("rice") || cropLower.contains("paddy") -> "🌾"
            cropLower.contains("wheat") -> "🌾"
            cropLower.contains("tomato") -> "🍅"
            cropLower.contains("cotton") -> "☁️"
            cropLower.contains("corn") || cropLower.contains("maize") -> "🌽"
            cropLower.contains("onion") -> "🧅"
            cropLower.contains("chilli") || cropLower.contains("chili") || cropLower.contains("pepper") -> "🌶️"
            else -> "🌿"
        }
    }
}

