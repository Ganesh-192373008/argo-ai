package com.example.agroassist

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ImageReviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_review)

        val imagePreview = findViewById<ImageView>(R.id.imagePreview)
        val textCropLabel = findViewById<TextView>(R.id.textCropLabel)
        val btnRetake = findViewById<LinearLayout>(R.id.btnRetake)
        val btnConfirm = findViewById<LinearLayout>(R.id.btnConfirm)

        // Handle image from camera or copied from gallery
        val imagePath = intent.getStringExtra("image_path")
        if (imagePath != null) {
            val file = File(imagePath)
            if (file.exists()) {
                // Downsample image to avoid OutOfMemoryError on high-res gallery uploads
                val bitmap = decodeSampledBitmap(file.absolutePath, 1024, 1024)
                if (bitmap != null) {
                    imagePreview.setImageBitmap(bitmap)
                    imagePreview.clearColorFilter()
                }
            }
        }

        // Handle image from gallery URI directly if present
        val imageUriString = intent.getStringExtra("image_uri")
        if (imageUriString != null) {
            try {
                val uri = Uri.parse(imageUriString)
                imagePreview.setImageURI(uri)
                imagePreview.clearColorFilter()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Validate if the image is actually a leaf/crop image
        val isValidLeaf = isLeafImage(imagePath)

        if (isValidLeaf) {
            // Retrieve preferred crop from database profile
            val dbHelper = AgroDatabaseHelper(this)
            val profile = dbHelper.getProfile()
            val preferredCrop = profile["crops"]

            // Classify image to identify the crop
            val diseaseInfo = PlantVillageClassifier.classifyImage(imagePath, preferredCrop)
            val cropName = diseaseInfo.crop
            textCropLabel.text = "Detected Crop: $cropName Leaf"
            textCropLabel.setTextColor(Color.WHITE)
        } else {
            textCropLabel.text = "Invalid: Not a Crop/Leaf Image"
            textCropLabel.setTextColor(Color.parseColor("#FF5252")) // Red accent
        }

        btnRetake.setOnClickListener {
            // "Retake" simply goes back to the Camera Activity
            finish() 
        }

        btnConfirm.setOnClickListener {
            if (!isValidLeaf) {
                Toast.makeText(this, "Invalid Image: Please capture/upload a clear crop or leaf image.", Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(this, AnalysisActivity::class.java)
                intent.putExtra("image_path", imagePath)
                intent.putExtra("image_uri", imageUriString)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun isLeafImage(imagePath: String?): Boolean {
        if (imagePath == null) return false
        val file = File(imagePath)
        if (!file.exists()) return false

        try {
            // High downsample to run quickly and safely on large images
            val options = BitmapFactory.Options().apply {
                inSampleSize = 16
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return false
            var leafGreenPixels = 0
            val width = bitmap.width
            val height = bitmap.height
            val step = 2
            var sampledTotal = 0
            val hsv = FloatArray(3)

            for (x in 0 until width step step) {
                for (y in 0 until height step step) {
                    val pixel = bitmap.getPixel(x, y)
                    Color.colorToHSV(pixel, hsv)
                    val hue = hsv[0]
                    val saturation = hsv[1]
                    val value = hsv[2]

                    // Leaf green/yellow-green range is generally 40 to 165 degrees.
                    val isLeafColor = hue in 40f..165f && saturation >= 0.18f && value >= 0.18f

                    if (isLeafColor) {
                        leafGreenPixels++
                    }
                    sampledTotal++
                }
            }
            bitmap.recycle()

            val leafPercentage = if (sampledTotal > 0) (leafGreenPixels.toFloat() / sampledTotal) * 100 else 0f
            return leafPercentage >= 20f
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}



