package com.example.agroassist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class FertilizerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val cropName = intent.getStringExtra("CROP_NAME") ?: "your crops"
        val fertilizerName = intent.getStringExtra("FERTILIZER_NAME") ?: "Fertilizer"
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "fertilizer_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Fertilizer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for applying fertilizer"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open the Fertilizer Dashboard when tapped
        val openIntent = Intent(context, FertilizerScheduleActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🌱 Fertilizer Reminder!"
        val message = "It is time to apply $fertilizerName to your $cropName. Don't miss your schedule!"
        
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val timeStr = timeFormat.format(java.util.Date())

        val dbHelper = AgroDatabaseHelper(context)
        dbHelper.addNotification(title, message, "operation", timeStr)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, builder.build())
    }
}
