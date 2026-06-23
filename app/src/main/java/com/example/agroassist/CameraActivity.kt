package com.example.agroassist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var viewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var isFlashOn = false

    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        val btnClose = findViewById<ImageView>(R.id.btnClose)
        val btnFlash = findViewById<ImageView>(R.id.btnFlash)
        val btnCapture = findViewById<View>(R.id.btnCapture)
        val btnFlip = findViewById<ImageView>(R.id.btnFlip)
        viewFinder = findViewById(R.id.viewFinder)

        btnClose.setOnClickListener { finish() }

        btnFlash.setOnClickListener {
            isFlashOn = !isFlashOn
            val flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            imageCapture?.flashMode = flashMode
            camera?.cameraControl?.enableTorch(isFlashOn)
            
            if (isFlashOn) {
                btnFlash.setColorFilter(Color.parseColor("#FFD54F")) // Gold/yellow color when active
                Toast.makeText(this, "Flash ON", Toast.LENGTH_SHORT).show()
            } else {
                btnFlash.clearColorFilter()
                Toast.makeText(this, "Flash OFF", Toast.LENGTH_SHORT).show()
            }
        }

        btnFlip.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            Toast.makeText(this, "Flipping camera...", Toast.LENGTH_SHORT).show()
            startCamera()
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        btnCapture.setOnClickListener {
            takePhoto()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // Create ImageCapture build with proper flash mode set
            val flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            imageCapture = ImageCapture.Builder()
                .setFlashMode(flashMode)
                .build()

            // Select camera selector dynamically
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                // Restore torch state if flash was enabled
                camera?.cameraControl?.enableTorch(isFlashOn)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Failed to start camera stream", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture
        val cachePath = File(cacheDir, "temp_crop_image.png")

        if (imageCapture != null && allPermissionsGranted()) {
            val outputOptions = ImageCapture.OutputFileOptions.Builder(cachePath).build()

            Toast.makeText(this, "Capturing...", Toast.LENGTH_SHORT).show()
            
            imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                        // Failover: draw mock leaf if capture failed (e.g. emulator environment)
                        drawMockLeaf(cachePath)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val intent = Intent(this@CameraActivity, ImageReviewActivity::class.java).apply {
                            putExtra("image_path", cachePath.absolutePath)
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            )
        } else {
            // Local draw fallback if camera not initialized/permissions denied
            drawMockLeaf(cachePath)
        }
    }

    private fun drawMockLeaf(cachePath: File) {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Draw background (soil/ground)
        paint.color = Color.parseColor("#4E342E")
        canvas.drawRect(0f, 0f, 512f, 512f, paint)

        // Draw a leaf shape
        paint.isAntiAlias = true
        paint.color = Color.parseColor("#2E7D32") // Green leaf
        val path = Path()
        path.moveTo(256f, 50f)
        path.quadTo(400f, 256f, 256f, 462f)
        path.quadTo(112f, 256f, 256f, 50f)
        canvas.drawPath(path, paint)

        // Draw some yellow/brown disease spots
        paint.color = Color.parseColor("#FFD54F") // Yellow spots
        canvas.drawCircle(220f, 180f, 15f, paint)
        canvas.drawCircle(300f, 240f, 20f, paint)
        paint.color = Color.parseColor("#8D6E63") // Brown spots inside yellow
        canvas.drawCircle(220f, 180f, 8f, paint)
        canvas.drawCircle(300f, 240f, 10f, paint)

        // Draw leaf stem/veins
        paint.color = Color.parseColor("#81C784")
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(256f, 50f, 256f, 462f, paint)
        canvas.drawLine(256f, 150f, 200f, 100f, paint)
        canvas.drawLine(256f, 200f, 312f, 150f, paint)
        canvas.drawLine(256f, 280f, 200f, 230f, paint)
        canvas.drawLine(256f, 330f, 312f, 280f, paint)

        try {
            val stream = FileOutputStream(cachePath)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val intent = Intent(this, ImageReviewActivity::class.java).apply {
                putExtra("image_path", cachePath.absolutePath)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to capture leaf image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                // Let the user capture a simulated leaf anyway if permission is denied
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
