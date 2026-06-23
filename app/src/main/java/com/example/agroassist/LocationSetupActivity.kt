package com.example.agroassist

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class LocationSetupActivity : AppCompatActivity() {

    private var isGpsSelected = false

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_setup)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val gpsButton = findViewById<LinearLayout>(R.id.gpsButton)
        val locationInput = findViewById<EditText>(R.id.locationInput)
        val locationContainer = findViewById<LinearLayout>(R.id.locationContainer)
        val continueButton = findViewById<Button>(R.id.continueButton)

        backButton.setOnClickListener { finish() }

        fun updateContinueButton() {
            val hasManualLocation = locationInput.text.isNotBlank()
            
            if (isGpsSelected || hasManualLocation) {
                continueButton.setBackgroundColor(resources.getColor(R.color.primary_green, theme))
                continueButton.isEnabled = true
            } else {
                continueButton.setBackgroundColor(android.graphics.Color.parseColor("#D6D9E0"))
                continueButton.isEnabled = false
            }
        }

        gpsButton.setOnClickListener {
            checkAndRequestLocation()
        }

        locationInput.setOnFocusChangeListener { _, hasFocus ->
            locationContainer.setBackgroundResource(if (hasFocus) R.drawable.edit_text_bg_active else R.drawable.edit_text_bg)
            if (hasFocus) {
                isGpsSelected = false // Unset GPS if they start typing manually
                updateContinueButton()
            }
        }

        locationInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isGpsSelected = false
                updateContinueButton()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        continueButton.isEnabled = false

        continueButton.setOnClickListener {
            val dbHelper = AgroDatabaseHelper(this)
            val location = locationInput.text.toString().trim()
            dbHelper.saveLocation(location, isGpsSelected)

            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
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
                    val locationInput = findViewById<EditText>(R.id.locationInput)
                    locationInput.setText(resolvedName)
                    isGpsSelected = true
                    findViewById<Button>(R.id.continueButton).apply {
                        setBackgroundColor(resources.getColor(R.color.primary_green, theme))
                        isEnabled = true
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
        val locationInput = findViewById<EditText>(R.id.locationInput)
        locationInput.setText(resolvedName)
        isGpsSelected = true
        findViewById<Button>(R.id.continueButton).apply {
            setBackgroundColor(resources.getColor(R.color.primary_green, theme))
            isEnabled = true
        }
        Toast.makeText(this, "Location set to: $resolvedName", Toast.LENGTH_SHORT).show()
    }
}

