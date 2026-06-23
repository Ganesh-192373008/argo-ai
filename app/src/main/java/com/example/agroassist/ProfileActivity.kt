package com.example.agroassist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfilePhoto: ImageView

    private val pickProfilePhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val localUri = saveImageToInternalStorage(uri)
            if (localUri != null) {
                val prefs = getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)
                prefs.edit().putString("profile_photo_uri", localUri.toString()).apply()
                loadProfilePhoto(localUri.toString())
                Toast.makeText(this, "Profile photo updated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save profile photo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(filesDir, "profile_photo.png")
            val outputStream = java.io.FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val prefs = getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)

        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navDetection = findViewById<LinearLayout>(R.id.navDetection)
        val navAssistant = findViewById<LinearLayout>(R.id.navAssistant)
        val navHelpSupport = findViewById<LinearLayout>(R.id.navHelpSupport)
        val navSettings = findViewById<LinearLayout>(R.id.navSettings)
        val navAchievements = findViewById<LinearLayout>(R.id.navAchievements)
        val navLanguage = findViewById<LinearLayout>(R.id.navLanguage)
        val btnEditProfile = findViewById<Button>(R.id.btnEditProfile)
        
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)

        // Profile data loading is handled in onResume()

        ivProfilePhoto.setOnClickListener {
            pickProfilePhoto.launch("image/*")
        }

        navHome.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        
        navAchievements?.setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }
        
        btnEditProfile?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
        
        navSettings?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        navHelpSupport?.setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }

        navLanguage.setOnClickListener {
            val languages = arrayOf("English", "Hindi (हिन्दी)", "Tamil (தமிழ்)", "Telugu (తెలుగు)", "Kannada (ಕನ್ನಡ)", "Marathi (मराठी)", "Bengali (বাংলা)", "Spanish (Español)")
            val currentLanguageIndex = when (prefs.getString("app_language", "English")) {
                "Hindi" -> 1
                "Tamil" -> 2
                "Telugu" -> 3
                "Kannada" -> 4
                "Marathi" -> 5
                "Bengali" -> 6
                "Spanish" -> 7
                else -> 0
            }
            
            AlertDialog.Builder(this)
                .setTitle("Select App Language")
                .setSingleChoiceItems(languages, currentLanguageIndex, android.content.DialogInterface.OnClickListener { dialog, which ->
                    val selectedLang = when (which) {
                        1 -> "Hindi"
                        2 -> "Tamil"
                        3 -> "Telugu"
                        4 -> "Kannada"
                        5 -> "Marathi"
                        6 -> "Bengali"
                        7 -> "Spanish"
                        else -> "English"
                    }
                    prefs.edit().putString("app_language", selectedLang).apply()
                    Toast.makeText(this, "Language set to ${languages[which]}", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                    
                    // Restart base setup to apply locale changes
                    recreate()
                })
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        navDetection.setOnClickListener {
            val intent = Intent(this, DetectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        navAssistant.setOnClickListener {
            val intent = Intent(this, ChatAssistantActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        val navLogout = findViewById<LinearLayout>(R.id.navLogout)
        navLogout?.setOnClickListener {
            AgroDatabaseHelper(this).saveProfile("", "", "")
            
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }

    private fun loadProfileData() {
        val prefs = getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)
        val dbHelper = AgroDatabaseHelper(this)
        val profile = dbHelper.getProfile()
        if (profile.isNotEmpty()) {
            val name = profile["name"]
            val location = profile["location"]
            val crops = profile["crops"]

            if (!name.isNullOrEmpty()) {
                findViewById<TextView>(R.id.tvProfileName)?.text = name
            }
            if (!location.isNullOrEmpty()) {
                findViewById<TextView>(R.id.tvProfileLocation)?.text = location
            }
            if (!crops.isNullOrEmpty()) {
                findViewById<TextView>(R.id.tvProfileCrops)?.text = crops
            }
        }

        val mobile = prefs.getString("mobile_number", "9876543210")
        findViewById<TextView>(R.id.tvProfileMobile)?.text = if (mobile.isNullOrEmpty()) "+91 98765 43210" else if (mobile.startsWith("+")) mobile else "+91 $mobile"

        val email = prefs.getString("email_address", "")
        val tvProfileEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val layoutProfileEmail = findViewById<android.view.View>(R.id.layoutProfileEmail)
        val dividerProfileEmail = findViewById<android.view.View>(R.id.dividerProfileEmail)

        if (email.isNullOrEmpty()) {
            layoutProfileEmail?.visibility = android.view.View.GONE
            dividerProfileEmail?.visibility = android.view.View.GONE
        } else {
            layoutProfileEmail?.visibility = android.view.View.VISIBLE
            dividerProfileEmail?.visibility = android.view.View.VISIBLE
            tvProfileEmail?.text = email
        }


        val savedPhotoUri = prefs.getString("profile_photo_uri", null)
        if (!savedPhotoUri.isNullOrEmpty()) {
            loadProfilePhoto(savedPhotoUri)
        } else {
            showDefaultProfileIcon()
        }
    }

    private fun loadProfilePhoto(uriStr: String) {
        try {
            val uri = Uri.parse(uriStr)
            val bitmap = if (uri.scheme == "file") {
                val file = java.io.File(uri.path ?: "")
                if (file.exists()) {
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    null
                }
            } else {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    android.graphics.BitmapFactory.decodeStream(inputStream)
                }
            }

            if (bitmap != null) {
                ivProfilePhoto.setImageBitmap(bitmap)
                ivProfilePhoto.setPadding(0, 0, 0, 0) // Full scale circular fitting
                ivProfilePhoto.imageTintList = null   // Clear XML tint list!
                ivProfilePhoto.clearColorFilter()
            } else {
                showDefaultProfileIcon()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showDefaultProfileIcon()
        }
    }

    private fun showDefaultProfileIcon() {
        ivProfilePhoto.setImageResource(android.R.drawable.ic_menu_myplaces)
        ivProfilePhoto.setPadding(
            (16 * resources.displayMetrics.density).toInt(),
            (16 * resources.displayMetrics.density).toInt(),
            (16 * resources.displayMetrics.density).toInt(),
            (16 * resources.displayMetrics.density).toInt()
        )
        ivProfilePhoto.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.white, theme))
        ivProfilePhoto.setColorFilter(resources.getColor(R.color.white, theme))
    }
}
