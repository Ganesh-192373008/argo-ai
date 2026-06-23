package com.example.agroassist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val notificationBell = findViewById<ImageView>(R.id.notificationBell)
        val navDetection = findViewById<LinearLayout>(R.id.navDetection)
        val cardDetectDisease = findViewById<LinearLayout>(R.id.cardDetectDisease)
        
        val cardWeather = findViewById<LinearLayout>(R.id.cardWeather)
        val cardMarketPrices = findViewById<LinearLayout>(R.id.cardMarketPrices)
        val cardAssistant = findViewById<LinearLayout>(R.id.cardAssistant)
        val navAssistant = findViewById<LinearLayout>(R.id.navAssistant)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)
        val navCommunity = findViewById<LinearLayout>(R.id.navCommunity)
        
        val cardHistory = findViewById<LinearLayout>(R.id.cardHistory)
        val cardMyCrops = findViewById<LinearLayout>(R.id.cardMyCrops)
        val cardCalendar = findViewById<LinearLayout>(R.id.cardCalendar)
        val cardCommunity = findViewById<LinearLayout>(R.id.cardCommunity)
        val cardRecommendedProducts = findViewById<LinearLayout>(R.id.cardRecommendedProducts)
        val cardFertilizerAlarms = findViewById<LinearLayout>(R.id.cardFertilizerAlarms)
        val cardGovSchemes = findViewById<androidx.cardview.widget.CardView>(R.id.cardGovSchemes)

        cardWeather?.setOnClickListener {
            startActivity(Intent(this, WeatherDashboardActivity::class.java))
        }

        cardRecommendedProducts?.setOnClickListener {
            startActivity(Intent(this, RecommendedProductsActivity::class.java))
        }

        cardFertilizerAlarms?.setOnClickListener {
            startActivity(Intent(this, FertilizerScheduleActivity::class.java))
        }

        navDetection?.setOnClickListener {
            startActivity(Intent(this, DetectionActivity::class.java))
        }

        navAssistant?.setOnClickListener {
            startActivity(Intent(this, ChatAssistantActivity::class.java))
        }
        
        navProfile?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        navCommunity?.setOnClickListener {
            startActivity(Intent(this, CommunityActivity::class.java))
        }

        cardCommunity?.setOnClickListener {
            startActivity(Intent(this, CommunityActivity::class.java))
        }
        
        cardMarketPrices?.setOnClickListener {
            startActivity(Intent(this, MarketPricesActivity::class.java))
        }
        
        cardAssistant?.setOnClickListener {
            startActivity(Intent(this, ChatAssistantActivity::class.java))
        }

        cardHistory?.setOnClickListener {
            startActivity(Intent(this, DetectionHistoryActivity::class.java))
        }

        cardMyCrops?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        cardCalendar?.setOnClickListener {
            startActivity(Intent(this, CropCalendarActivity::class.java))
        }

        cardGovSchemes?.setOnClickListener {
            startActivity(Intent(this, GovSchemesActivity::class.java))
        }
        
        val detectionClickListener = android.view.View.OnClickListener {
            startActivity(Intent(this, DetectionActivity::class.java))
        }

        notificationBell?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        navDetection?.setOnClickListener(detectionClickListener)
        cardDetectDisease?.setOnClickListener(detectionClickListener)
        
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        navHome?.setOnClickListener {
            // Already on Home
            Toast.makeText(this, "You are already on Home", Toast.LENGTH_SHORT).show()
        }

        // Initialize Government Schemes Notification triggers
        val prefs = getSharedPreferences("AgroAssistPrefs", Context.MODE_PRIVATE)
        val shown = prefs.getBoolean("gov_schemes_alert_shown", false)
        if (!shown) {
            sendBroadcast(Intent(this, GovSchemesAlarmReceiver::class.java))
            prefs.edit().putBoolean("gov_schemes_alert_shown", true).apply()
        }
        scheduleDailyGovSchemesAlarm()
    }

    private fun scheduleDailyGovSchemesAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(this, GovSchemesAlarmReceiver::class.java)
        
        val requestCode = 9999
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 9) // 9:00 AM daily
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        try {
            alarmManager.setRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                android.app.AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
