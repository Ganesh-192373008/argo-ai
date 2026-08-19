package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class DetectionActivity : BaseProtectedActivity() {

    // Register Activity Result Launcher for opening the gallery
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val cachePath = java.io.File(cacheDir, "temp_crop_image.png")
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.io.FileOutputStream(cachePath).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                val intent = Intent(this, ImageReviewActivity::class.java)
                intent.putExtra("image_path", cachePath.absolutePath)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load selected image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            // Save bitmap to cache directory
            val cachePath = java.io.File(cacheDir, "temp_crop_image.png")
            try {
                val stream = java.io.FileOutputStream(cachePath)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                val intent = Intent(this, ImageReviewActivity::class.java)
                intent.putExtra("image_path", cachePath.absolutePath)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to save captured photo", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnTakePhoto = findViewById<LinearLayout>(R.id.btnTakePhoto)
        val btnUploadImage = findViewById<LinearLayout>(R.id.btnUploadImage)
        val btnMonthlyReport = findViewById<ImageView>(R.id.btnMonthlyReport)
        val navHome = findViewById<LinearLayout>(R.id.navHome)

        backButton.setOnClickListener { finish() }

        btnTakePhoto.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        btnUploadImage.setOnClickListener {
            pickImage.launch("image/*")
        }
        
        btnMonthlyReport?.setOnClickListener {
            startActivity(Intent(this, MonthlyReportActivity::class.java))
        }

        val navCommunity = findViewById<LinearLayout>(R.id.navCommunity)
        val navAssistant = findViewById<LinearLayout>(R.id.navAssistant)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        navHome?.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        navCommunity?.setOnClickListener {
            val intent = Intent(this, CommunityActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        navAssistant?.setOnClickListener {
            val intent = Intent(this, ChatAssistantActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        navProfile?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}
