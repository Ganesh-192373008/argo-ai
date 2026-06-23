package com.example.agroassist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val navPrivacyPolicy = findViewById<LinearLayout>(R.id.navPrivacyPolicy)
        val navDataUsage = findViewById<LinearLayout>(R.id.navDataUsage)
        val navClearAllData = findViewById<LinearLayout>(R.id.navClearAllData)

        val switchNotifications = findViewById<SwitchCompat>(R.id.switchNotifications)
        val switchDarkMode = findViewById<SwitchCompat>(R.id.switchDarkMode)
        val switchAutoSync = findViewById<SwitchCompat>(R.id.switchAutoSync)

        backButton.setOnClickListener { finish() }

        // Setup options navigation
        navPrivacyPolicy.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }

        navDataUsage?.setOnClickListener {
            startActivity(Intent(this, DataUsageActivity::class.java))
        }

        // Load saved preferences states
        switchNotifications.isChecked = prefs.getBoolean("notifications_enabled", true)
        switchDarkMode.isChecked = prefs.getBoolean("dark_mode_enabled", false)
        switchAutoSync.isChecked = prefs.getBoolean("auto_sync_enabled", true)

        // Bind switch listeners
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            Toast.makeText(this, if (isChecked) "Notifications Enabled" else "Notifications Silenced", Toast.LENGTH_SHORT).show()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode_enabled", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            Toast.makeText(this, if (isChecked) "Dark Mode Enabled" else "Dark Mode Disabled", Toast.LENGTH_SHORT).show()
        }

        switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_sync_enabled", isChecked).apply()
            Toast.makeText(this, if (isChecked) "Auto Sync Enabled" else "Auto Sync Disabled", Toast.LENGTH_SHORT).show()
        }

        // Implement Clear All Data functionality
        navClearAllData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("Are you sure you want to delete all your profiles, schedules, search history, community posts, and settings? This action cannot be undone.")
                .setPositiveButton("Clear Everything") { dialog, _ ->
                    // 1. Wipe database
                    try {
                        val dbHelper = AgroDatabaseHelper(this)
                        val db = dbHelper.writableDatabase
                        dbHelper.onUpgrade(db, 2, 2)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // 2. Wipe settings SharedPreferences
                    prefs.edit().clear().apply()

                    // 3. Wipe API keys SharedPreferences
                    getSharedPreferences("AgroAssistAIKeys", Context.MODE_PRIVATE).edit().clear().apply()

                    Toast.makeText(this, "All data has been cleared successfully!", Toast.LENGTH_LONG).show()
                    dialog.dismiss()

                    // 4. Force restart to login/setup flow
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}
