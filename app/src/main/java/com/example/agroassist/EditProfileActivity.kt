package com.example.agroassist

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var ivEditProfilePhoto: ImageView
    private lateinit var etFullName: EditText
    private lateinit var etMobileNumber: EditText
    private lateinit var etLocation: EditText
    private lateinit var etEmail: EditText
    
    private var selectedPhotoUriStr: String? = null
    private val selectedCrops = mutableSetOf<String>()
    
    private val pickProfilePhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val localUri = saveImageToInternalStorage(uri)
            if (localUri != null) {
                selectedPhotoUriStr = localUri.toString()
                loadProfilePhoto(localUri.toString())
                
                // Save photo to SharedPreferences in real time
                val prefs = getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)
                prefs.edit().putString("profile_photo_uri", selectedPhotoUriStr).apply()
                
                Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save photo.", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Location permission denied. Please enter manually.", Toast.LENGTH_LONG).show()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnGetLocation = findViewById<ImageView>(R.id.btnGetLocation)
        val layoutChangePhoto = findViewById<FrameLayout>(R.id.layoutChangePhoto)
        
        ivEditProfilePhoto = findViewById(R.id.ivEditProfilePhoto)
        etFullName = findViewById(R.id.etFullName)
        etMobileNumber = findViewById(R.id.etMobileNumber)
        etEmail = findViewById(R.id.etEmail)
        etLocation = findViewById(R.id.etLocation)

        val dbHelper = AgroDatabaseHelper(this)
        val profile = dbHelper.getProfile()
        val prefs = getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)

        // Load details from DB
        val currentName = profile["name"] ?: "Rajesh Kumar"
        val currentAge = profile["age"] ?: "35"
        val currentLocation = profile["location"] ?: "Maharashtra, India"
        val currentCrops = profile["crops"] ?: "Tomato, Rice, Wheat"
        val currentMobile = prefs.getString("mobile_number", "9876543210") ?: "9876543210"

        etFullName.setText(currentName)
        etLocation.setText(currentLocation)
        etMobileNumber.setText(currentMobile)
        etEmail.setText(prefs.getString("email_address", "") ?: "")

        // Parse and pre-select crops
        val initialCropsList = currentCrops.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        selectedCrops.addAll(initialCropsList)

        val cropViews = mapOf(
            "Rice" to findViewById<TextView>(R.id.cropRice),
            "Wheat" to findViewById<TextView>(R.id.cropWheat),
            "Tomato" to findViewById<TextView>(R.id.cropTomato),
            "Potato" to findViewById<TextView>(R.id.cropPotato),
            "Cotton" to findViewById<TextView>(R.id.cropCotton),
            "Sugarcane" to findViewById<TextView>(R.id.cropSugarcane),
            "Onion" to findViewById<TextView>(R.id.cropOnion),
            "Cabbage" to findViewById<TextView>(R.id.cropCabbage)
        )

        // Update visual states of crops
        cropViews.forEach { (cropName, textView) ->
            if (textView != null) {
                updateCropVisualState(cropName, textView)
                textView.setOnClickListener {
                    if (selectedCrops.contains(cropName)) {
                        selectedCrops.remove(cropName)
                    } else {
                        selectedCrops.add(cropName)
                    }
                    updateCropVisualState(cropName, textView)
                    
                    // Save crops in real time
                    val updatedName = etFullName.text.toString().trim()
                    dbHelper.saveProfile(updatedName, currentAge, selectedCrops.joinToString(", "))
                }
            }
        }

        // Real-time saving TextWatchers
        etFullName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val updatedName = s.toString().trim()
                if (updatedName.isNotEmpty()) {
                    dbHelper.saveProfile(updatedName, currentAge, selectedCrops.joinToString(", "))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val updatedLocation = s.toString().trim()
                dbHelper.saveLocation(updatedLocation, false)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etMobileNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val updatedMobile = s.toString().trim()
                prefs.edit().putString("mobile_number", updatedMobile).apply()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val updatedEmail = s.toString().trim()
                prefs.edit().putString("email_address", updatedEmail).apply()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Load photo
        selectedPhotoUriStr = prefs.getString("profile_photo_uri", null)
        if (!selectedPhotoUriStr.isNullOrEmpty()) {
            loadProfilePhoto(selectedPhotoUriStr!!)
        } else {
            showDefaultProfileIcon()
        }

        layoutChangePhoto.setOnClickListener {
            pickProfilePhoto.launch("image/*")
        }

        btnGetLocation?.setOnClickListener {
            checkAndRequestLocation()
        }

        backButton.setOnClickListener { finish() }
        
        btnSave.setOnClickListener {
            val updatedName = etFullName.text.toString().trim()
            val updatedLocation = etLocation.text.toString().trim()
            val updatedMobile = etMobileNumber.text.toString().trim()
            val updatedEmail = etEmail.text.toString().trim()
            val updatedCrops = selectedCrops.joinToString(", ")

            if (updatedName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save one final time to be safe
            dbHelper.saveProfile(updatedName, currentAge, updatedCrops)
            dbHelper.saveLocation(updatedLocation, false)
            prefs.edit().apply {
                putString("mobile_number", updatedMobile)
                putString("email_address", updatedEmail)
                if (selectedPhotoUriStr != null) {
                    putString("profile_photo_uri", selectedPhotoUriStr)
                }
                apply()
            }

            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun updateCropVisualState(cropName: String, textView: TextView) {
        if (selectedCrops.contains(cropName)) {
            textView.setBackgroundResource(R.drawable.crop_pill_bg_selected)
            textView.setTextColor(resources.getColor(R.color.primary_green, theme))
        } else {
            textView.setBackgroundResource(R.drawable.crop_pill_bg)
            textView.setTextColor(resources.getColor(R.color.text_secondary, theme))
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
                ivEditProfilePhoto.setImageBitmap(bitmap)
                ivEditProfilePhoto.setPadding(0, 0, 0, 0)
                ivEditProfilePhoto.imageTintList = null
                ivEditProfilePhoto.clearColorFilter()
            } else {
                showDefaultProfileIcon()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showDefaultProfileIcon()
        }
    }

    private fun showDefaultProfileIcon() {
        ivEditProfilePhoto.setImageResource(android.R.drawable.ic_menu_myplaces)
        ivEditProfilePhoto.setPadding(
            (24 * resources.displayMetrics.density).toInt(),
            (24 * resources.displayMetrics.density).toInt(),
            (24 * resources.displayMetrics.density).toInt(),
            (24 * resources.displayMetrics.density).toInt()
        )
        ivEditProfilePhoto.setColorFilter(resources.getColor(R.color.primary_green, theme))
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
                    etLocation.setText(resolvedName)
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
        etLocation.setText(resolvedName)
        Toast.makeText(this, "Location set to: $resolvedName", Toast.LENGTH_SHORT).show()
    }
}
