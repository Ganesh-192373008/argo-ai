package com.example.agroassist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class GovSchemesAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val schemes = listOf(
            Pair("PM Kisan Samman Nidhi", "New installment of ₹2,000 has been credited to registered farmers."),
            Pair("Pradhan Mantri Fasal Bima Yojana", "Deadline to apply for Kharif crop insurance is approaching. Apply now!"),
            Pair("Paramparagat Krishi Vikas Yojana", "Apply for up to 50% subsidy on organic compost and bio-fertilizers."),
            Pair("Kisan Credit Card (KCC)", "Loan interest rates slashed to 4% for prompt repayment. Check details."),
            Pair("PM Krishi Sinchayee Yojana", "80% subsidy available for installing drip irrigation systems in your farm.")
        )

        val randomIndex = Random.nextInt(schemes.size)
        val selectedScheme = schemes[randomIndex]

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "gov_schemes_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Government Schemes Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily alerts and updates about government agricultural schemes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open GovSchemesActivity when tapped
        val openIntent = Intent(context, GovSchemesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1002, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🇮🇳 ${selectedScheme.first}"
        val message = selectedScheme.second

        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val timeStr = timeFormat.format(java.util.Date())

        val dbHelper = AgroDatabaseHelper(context)
        dbHelper.addNotification(title, message, "scheme", timeStr)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = 3000 + randomIndex
        notificationManager.notify(notificationId, builder.build())
    }
}
