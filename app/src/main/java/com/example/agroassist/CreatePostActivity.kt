package com.example.agroassist

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class CreatePostActivity : AppCompatActivity() {

    private lateinit var dbHelper: AgroDatabaseHelper
    private var farmerName = "Rajesh Kumar"
    private var farmerState = "India"
    private var farmerAvatar = "👨‍🌾"
    private var selectedPhotoUriStr: String? = null

    // Register image picker launcher
    private val pickPostPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val localUri = saveImageToInternalStorage(uri)
            if (localUri != null) {
                selectedPhotoUriStr = localUri.toString()
                val ivAttachedPhoto = findViewById<ImageView>(R.id.ivAttachedPhoto)
                if (ivAttachedPhoto != null) {
                    ivAttachedPhoto.setImageURI(localUri)
                    ivAttachedPhoto.visibility = View.VISIBLE
                }
                Toast.makeText(this, "Photo attached successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to attach photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Register location permission request
    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchGPSLocation()
        } else {
            Toast.makeText(this, "Location permission denied. Please select profile location.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        dbHelper = AgroDatabaseHelper(this)

        val textAvatar = findViewById<TextView>(R.id.textAvatar)
        val textFarmerName = findViewById<TextView>(R.id.textFarmerName)
        val editPostContent = findViewById<EditText>(R.id.editPostContent)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnPost = findViewById<TextView>(R.id.btnPost)
        val btnAttachPhoto = findViewById<LinearLayout>(R.id.btnAttachPhoto)
        val btnAttachLocation = findViewById<LinearLayout>(R.id.btnAttachLocation)

        // Retrieve profile details
        try {
            val profile = dbHelper.getProfile()
            val savedName = profile["name"]
            if (!savedName.isNullOrEmpty()) {
                farmerName = savedName
            }
            val savedLocation = profile["location"]
            if (!savedLocation.isNullOrEmpty()) {
                farmerState = savedLocation
            }
            if (profile["crops"]?.contains("chili", ignoreCase = true) == true) {
                farmerAvatar = "👩‍🌾"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        textAvatar.text = farmerAvatar
        textFarmerName.text = farmerName

        btnBack.setOnClickListener {
            finish()
        }

        btnPost.setOnClickListener {
            val content = editPostContent.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Please write something before posting!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sync post directly to SQL Prisma ORM Backend API
            BackendApiClient.addCommunityPost(context = this, name = farmerName, content = content, avatar = farmerAvatar, state = farmerState) {}

            // Track achievement score for Community Post
            AchievementTracker.onCommunityPostCreated(this)

            val resultIntent = Intent().apply {
                putExtra("avatar", farmerAvatar)
                putExtra("name", farmerName)
                putExtra("state", farmerState)
                putExtra("content", content)
                putExtra("image_uri", selectedPhotoUriStr)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            Toast.makeText(this, "Post shared successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnAttachPhoto.setOnClickListener {
            pickPostPhoto.launch("image/*")
        }

        btnAttachLocation.setOnClickListener {
            checkAndRequestLocation()
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(filesDir, "post_photo_${System.currentTimeMillis()}.png")
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

    private fun checkAndRequestLocation() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            fetchGPSLocation()
        } else {
            requestLocationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchGPSLocation() {
        Toast.makeText(this, "Fetching GPS Location...", Toast.LENGTH_SHORT).show()
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    resolveLocationName(location.latitude, location.longitude)
                    locationManager.removeUpdates(this)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                val lastKnownGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val bestLocation = lastKnownGPS ?: lastKnownNetwork
                
                if (bestLocation != null) {
                    resolveLocationName(bestLocation.latitude, bestLocation.longitude)
                }

                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        0L,
                        0f,
                        locationListener
                    )
                } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0L,
                        0f,
                        locationListener
                    )
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resolveLocationName(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                val state = address.adminArea ?: ""
                val resolvedName = if (city.isNotEmpty() && state.isNotEmpty() && city != state) {
                    "$city, $state"
                } else if (city.isNotEmpty()) {
                    city
                } else {
                    state
                }
                
                if (resolvedName.isNotEmpty()) {
                    farmerState = resolvedName
                    val textLocationAttached = findViewById<TextView>(R.id.textLocationAttached)
                    if (textLocationAttached != null) {
                        textLocationAttached.text = resolvedName
                    }
                    Toast.makeText(this, "Location detected: $resolvedName", Toast.LENGTH_SHORT).show()
                } else {
                    setFallbackGPSLocation(latitude, longitude)
                }
            } else {
                setFallbackGPSLocation(latitude, longitude)
            }
        } catch (e: Exception) {
            setFallbackGPSLocation(latitude, longitude)
        }
    }

    private fun setFallbackGPSLocation(latitude: Double, longitude: Double) {
        val resolvedName = String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
        farmerState = resolvedName
        val textLocationAttached = findViewById<TextView>(R.id.textLocationAttached)
        if (textLocationAttached != null) {
            textLocationAttached.text = resolvedName
        }
        Toast.makeText(this, "Location set to: $resolvedName", Toast.LENGTH_SHORT).show()
    }
}
