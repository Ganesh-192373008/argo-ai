package com.example.agroassist

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
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

class AddFertilizerScheduleActivity : AppCompatActivity() {

    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button
    private lateinit var etCropName: TextInputEditText
    private lateinit var etFertilizerName: TextInputEditText
    
    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0
    private var selectedHour = 0
    private var selectedMinute = 0
    
    private var isDateSet = false
    private var isTimeSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_fertilizer_schedule)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnSubmitSchedule = findViewById<Button>(R.id.btnSubmitSchedule)
        
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectTime = findViewById(R.id.btnSelectTime)
        etCropName = findViewById(R.id.etCropName)
        etFertilizerName = findViewById(R.id.etFertilizerName)

        backButton.setOnClickListener { finish() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedYear = year
                selectedMonth = month
                selectedDay = dayOfMonth
                isDateSet = true
                btnSelectDate.text = "$dayOfMonth/${month + 1}/$year"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSelectTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                isTimeSet = true
                val amPm = if (hourOfDay >= 12) "PM" else "AM"
                val hour12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                val minStr = String.format("%02d", minute)
                btnSelectTime.text = "$hour12:$minStr $amPm"
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        btnSubmitSchedule.setOnClickListener {
            val cropName = etCropName.text.toString().trim()
            val fertilizerName = etFertilizerName.text.toString().trim()
            
            if (cropName.isEmpty() || fertilizerName.isEmpty()) {
                Toast.makeText(this, "Please enter Crop Name and Fertilizer Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!isDateSet || !isTimeSet) {
                Toast.makeText(this, "Please select Date and Time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scheduleFertilizerAlarm(cropName, fertilizerName)
        }
    }
    
    private fun scheduleFertilizerAlarm(cropName: String, fertilizerName: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, FertilizerAlarmReceiver::class.java).apply {
            putExtra("CROP_NAME", cropName)
            putExtra("FERTILIZER_NAME", fertilizerName)
        }
        
        val requestCode = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, selectedDay)
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Please allow Exact Alarms in App Settings", Toast.LENGTH_LONG).show()
                return
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            
            // Save to Database
            val dbHelper = AgroDatabaseHelper(this)
            val dateStr = btnSelectDate.text.toString()
            val timeStr = btnSelectTime.text.toString()
            dbHelper.addSchedule(cropName, "Fertilizer", fertilizerName, dateStr, timeStr)
            
            Toast.makeText(this, "Fertilizer Schedule Created! Alarm Set.", Toast.LENGTH_LONG).show()
            finish()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Missing Exact Alarm permission.", Toast.LENGTH_LONG).show()
        }
    }
}
