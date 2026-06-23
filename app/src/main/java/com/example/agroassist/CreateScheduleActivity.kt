package com.example.agroassist

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import java.util.*

class CreateScheduleActivity : AppCompatActivity() {

    private lateinit var btnSelectTime: Button
    private lateinit var btnSaveSchedule: Button
    private lateinit var etCropName: TextInputEditText
    
    private var selectedHour = 8
    private var selectedMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_schedule)

        val backButton = findViewById<ImageView>(R.id.backButton)
        btnSelectTime = findViewById(R.id.btnSelectTime)
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule)
        etCropName = findViewById(R.id.etCropName)

        backButton.setOnClickListener { finish() }

        // Request Notification Permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        btnSelectTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                
                val amPm = if (hourOfDay >= 12) "PM" else "AM"
                val hour12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                val minStr = String.format("%02d", minute)
                
                btnSelectTime.text = "Select Time (Currently: $hour12:$minStr $amPm)"
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
            timePickerDialog.show()
        }

        btnSaveSchedule.setOnClickListener {
            val cropName = etCropName.text.toString().trim()
            if (cropName.isEmpty()) {
                Toast.makeText(this, "Please enter a crop name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scheduleAlarm(cropName)
        }
    }

    private fun scheduleAlarm(cropName: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, WateringAlarmReceiver::class.java).apply {
            putExtra("CROP_NAME", cropName)
        }
        
        // We use a unique request code so multiple alarms don't overwrite each other
        val requestCode = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
        }

        // If time is in the past, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Check exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Please allow Exact Alarms in App Settings", Toast.LENGTH_LONG).show()
                return
            }
        }

        try {
            // Schedule the alarm daily
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            
            // Save to Database
            val dbHelper = AgroDatabaseHelper(this)
            val amPm = if (selectedHour >= 12) "PM" else "AM"
            val hour12 = if (selectedHour % 12 == 0) 12 else selectedHour % 12
            val minStr = String.format("%02d", selectedMinute)
            val timeStr = "$hour12:$minStr $amPm"
            
            dbHelper.addSchedule(cropName, "Watering", "Daily Watering", "Every Day", timeStr)

            Toast.makeText(this, "Alarm set! You will be notified at the scheduled time.", Toast.LENGTH_LONG).show()
            finish()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Missing Exact Alarm permission.", Toast.LENGTH_LONG).show()
        }
    }
}
