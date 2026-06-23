package com.example.agroassist

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale

class NotificationsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        // Request notification permission on Android 13+ and trigger fetching
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            } else {
                fetchRealTimeAlerts()
            }
        } else {
            fetchRealTimeAlerts()
        }
    }

    private fun fetchRealTimeAlerts() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewNotifications)
        
        progressBar.visibility = View.VISIBLE
        
        val dbHelper = AgroDatabaseHelper(this)
        val profile = dbHelper.getProfile()
        val location = profile["location"]?.ifEmpty { "Nashik, Maharashtra" } ?: "Nashik, Maharashtra"
        val crops = profile["crops"]?.ifEmpty { "Tomato, Rice, Wheat" } ?: "Tomato, Rice, Wheat"
        
        CoroutineScope(Dispatchers.Main).launch {
            var items: List<NotificationItem>? = null
            
            // Set keys before calling API
            val prefs = getSharedPreferences("AgroAssistAIKeys", Context.MODE_PRIVATE)
            var geminiKey = prefs.getString("gemini_api_key", "") ?: ""
            var openaiKey = prefs.getString("openai_api_key", "") ?: ""
            if (geminiKey.trim().lowercase() == "hi") geminiKey = ""
            if (openaiKey.trim().lowercase() == "hi" || openaiKey.trim().startsWith("sk-...")) openaiKey = ""
            
            GeminiClient.setApiKey(geminiKey)
            OpenAIClient.setApiKey(openaiKey)
            
            val systemContext = "You are an AI agricultural assistant. " +
                    "Generate exactly 4 critical agricultural alerts for a farmer located in $location growing $crops. " +
                    "Provide alerts for: " +
                    "1. Market prices/trends " +
                    "2. Government schemes " +
                    "3. Weather warnings " +
                    "4. Farm operations (watering or fertilizer schedules) " +
                    "The response MUST be a valid JSON array containing exactly 4 objects. " +
                    "Each object must have fields: " +
                    "- 'title' (string) " +
                    "- 'message' (string) " +
                    "- 'category' (string: 'market', 'scheme', 'weather', or 'operation') " +
                    "- 'time' (string: e.g. 'Just now', '15 mins ago') " +
                    "Do NOT wrap the response in markdown backticks or include any text other than the JSON array."
            
            val prompt = "Please generate 4 agricultural alerts for $location with crops $crops."
            
            try {
                val rawResponse = try {
                    GeminiClient.generateResponse(prompt, systemContext)
                } catch (e: Exception) {
                    OpenAIClient.generateResponse(prompt, systemContext)
                }
                
                items = parseAlertsJson(rawResponse)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            if (items == null || items.isEmpty()) {
                items = getFallbackAlerts(location, crops)
            }
            
            // Fetch saved local alerts from database
            val savedNotifs = dbHelper.getUserNotifications()
            val finalItems = mutableListOf<NotificationItem>()
            
            for (notif in savedNotifs) {
                val id = notif["id"]?.toIntOrNull() ?: 0
                val title = notif["title"] ?: ""
                val message = notif["message"] ?: ""
                val category = notif["category"] ?: "operation"
                val time = notif["timestamp"] ?: "Just now"
                
                finalItems.add(NotificationItem(id, title, message, category, time))
            }
            
            if (items != null) {
                finalItems.addAll(items)
            }
            
            progressBar.visibility = View.GONE
            
            // Set up adapter with combined items
            recyclerView.layoutManager = LinearLayoutManager(this@NotificationsActivity)
            recyclerView.adapter = NotificationAdapter(finalItems)
            
            // Post system notifications only for newly fetched alerts
            if (items != null) {
                postSystemNotifications(items)
            }
        }
    }

    private fun parseAlertsJson(jsonStr: String): List<NotificationItem>? {
        val list = mutableListOf<NotificationItem>()
        try {
            var cleanStr = jsonStr.trim()
            if (cleanStr.startsWith("```json")) {
                cleanStr = cleanStr.substringAfter("```json")
            }
            if (cleanStr.startsWith("```")) {
                cleanStr = cleanStr.substringAfter("```")
            }
            if (cleanStr.endsWith("```")) {
                cleanStr = cleanStr.substringBeforeLast("```")
            }
            cleanStr = cleanStr.trim()
            
            val jsonArray = JSONArray(cleanStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.getString("title")
                val message = obj.getString("message")
                val category = obj.getString("category")
                val time = obj.optString("time", "Just now")
                list.add(NotificationItem(id = 100 + i, title = title, message = message, category = category, time = time))
            }
            return list
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getFallbackAlerts(location: String, crops: String): List<NotificationItem> {
        return listOf(
            NotificationItem(
                id = 101,
                title = "Market Alert: Tomato Prices in $location",
                message = "Prices at nearest Mandi have risen by 12%. Recommended to sell today!",
                category = "market",
                time = "Just now"
            ),
            NotificationItem(
                id = 102,
                title = "Government Scheme for $location",
                message = "New subsidy announced for $crops crop fertilizers in your region.",
                category = "scheme",
                time = "10 mins ago"
            ),
            NotificationItem(
                id = 103,
                title = "Weather Warning for $location",
                message = "Heavy thunderstorm expected in $location this evening. Secure your crops.",
                category = "weather",
                time = "1 hour ago"
            ),
            NotificationItem(
                id = 104,
                title = "Watering Schedule: $crops",
                message = "Irrigation is recommended tomorrow morning based on the local climate in $location.",
                category = "operation",
                time = "2 hours ago"
            )
        )
    }

    private fun postSystemNotifications(items: List<NotificationItem>) {
        val channelId = "agro_alerts_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AgroAssist Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Market prices, schemes, and weather alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        for (item in items) {
            val intent = when (item.category.lowercase(Locale.ROOT)) {
                "market" -> Intent(this, MarketPricesActivity::class.java)
                "scheme" -> Intent(this, GovSchemesActivity::class.java)
                "weather" -> Intent(this, WeatherDashboardActivity::class.java)
                else -> { // operation
                    if (item.title.lowercase(Locale.ROOT).contains("water")) {
                        Intent(this, WaterManagementActivity::class.java)
                    } else {
                        Intent(this, FertilizerScheduleActivity::class.java)
                    }
                }
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this,
                item.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(item.title)
                .setContentText(item.message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                
            try {
                notificationManager.notify(item.id, builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            fetchRealTimeAlerts()
        }
    }
}
