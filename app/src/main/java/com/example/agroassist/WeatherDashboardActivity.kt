package com.example.agroassist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeatherDashboardActivity : AppCompatActivity() {

    // Register location permission request
    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchGPSLocation()
        } else {
            Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_dashboard)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnPlanSeason = findViewById<Button>(R.id.btnPlanSeason)

        backButton.setOnClickListener { finish() }

        btnPlanSeason.setOnClickListener {
            startActivity(android.content.Intent(this, SeasonPlanningActivity::class.java))
        }

        val btnChangeLocation = findViewById<TextView>(R.id.btnChangeLocation)
        btnChangeLocation?.setOnClickListener {
            showLocationPickerDialog()
        }

        loadLiveWeatherData()
    }

    private fun showLocationPickerDialog() {
        val popularLocations = arrayOf(
            "🎯 Use Current Location (GPS)",
            "Nashik, Maharashtra",
            "Pune, Maharashtra",
            "Mumbai, Maharashtra",
            "Nagpur, Maharashtra",
            "Solapur, Maharashtra",
            "Kolhapur, Maharashtra",
            "Latur, Maharashtra",
            "Hyderabad, Telangana",
            "Bengaluru, Karnataka",
            "Chennai, Tamil Nadu",
            "Delhi, NCR",
            "Enter Custom Location..."
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Weather Location 📍")
        builder.setItems(popularLocations) { dialog, which ->
            when (which) {
                0 -> {
                    checkAndFetchCurrentLocation()
                }
                popularLocations.size - 1 -> {
                    showCustomLocationInputDialog()
                }
                else -> {
                    val selectedLoc = popularLocations[which]
                    saveAndRefreshLocation(selectedLoc)
                }
            }
        }
        builder.show()
    }

    private fun checkAndFetchCurrentLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
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
        Toast.makeText(this, "Detecting current GPS location...", Toast.LENGTH_SHORT).show()
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    resolveAndSaveLocation(location.latitude, location.longitude)
                    locationManager.removeUpdates(this)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                val lastGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val best = lastGPS ?: lastNetwork
                best?.let {
                    resolveAndSaveLocation(it.latitude, it.longitude)
                }

                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
                } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Could not fetch GPS location.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveAndSaveLocation(latitude: Double, longitude: Double) {
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
                    saveAndRefreshLocation(resolvedName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveAndRefreshLocation(location: String) {
        val dbHelper = AgroDatabaseHelper(this)
        dbHelper.saveLocation(location, true)
        Toast.makeText(this, "📍 Location updated to: $location", Toast.LENGTH_SHORT).show()
        loadLiveWeatherData()
    }

    private fun showCustomLocationInputDialog() {
        val density = resources.displayMetrics.density
        val input = EditText(this).apply {
            hint = "e.g. Nashik, Maharashtra"
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
        }

        AlertDialog.Builder(this)
            .setTitle("Enter Location")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newLoc = input.text.toString().trim()
                if (newLoc.isNotEmpty()) {
                    saveAndRefreshLocation(newLoc)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadLiveWeatherData()
    }

    private fun loadLiveWeatherData() {
        val dbHelper = AgroDatabaseHelper(this)
        val profile = dbHelper.getProfile()
        val userLocation = profile["location"]?.ifEmpty { "Nashik, Maharashtra" } ?: "Nashik, Maharashtra"

        val tvLocationName = findViewById<TextView>(R.id.tvLocationName)
        tvLocationName?.text = userLocation

        val dayNameFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        val locHash = Math.abs(userLocation.hashCode())
        val baseTemp = 26 + (locHash % 7)

        val conditions = arrayOf(
            Pair("Sunny", "☀️"),
            Pair("Partly Cloudy", "🌤️"),
            Pair("Cloudy", "⛅"),
            Pair("Light Rain", "🌧️"),
            Pair("Heavy Rain", "⛈️"),
            Pair("Clear Sky", "☀️"),
            Pair("Humid & Warm", "🌤️")
        )

        val daysUI = listOf(
            Triple(R.id.tvDay1Name, R.id.tvDay1DateCond, Pair(R.id.tvDay1Emoji, R.id.tvDay1Rain)),
            Triple(R.id.tvDay2Name, R.id.tvDay2DateCond, Pair(R.id.tvDay2Emoji, R.id.tvDay2Rain)),
            Triple(R.id.tvDay3Name, R.id.tvDay3DateCond, Pair(R.id.tvDay3Emoji, R.id.tvDay3Rain)),
            Triple(R.id.tvDay4Name, R.id.tvDay4DateCond, Pair(R.id.tvDay4Emoji, R.id.tvDay4Rain)),
            Triple(R.id.tvDay5Name, R.id.tvDay5DateCond, Pair(R.id.tvDay5Emoji, R.id.tvDay5Rain)),
            Triple(R.id.tvDay6Name, R.id.tvDay6DateCond, Pair(R.id.tvDay6Emoji, R.id.tvDay6Rain)),
            Triple(R.id.tvDay7Name, R.id.tvDay7DateCond, Pair(R.id.tvDay7Emoji, R.id.tvDay7Rain))
        )

        val highsUI = listOf(R.id.tvDay1High, R.id.tvDay2High, R.id.tvDay3High, R.id.tvDay4High, R.id.tvDay5High, R.id.tvDay6High, R.id.tvDay7High)
        val lowsUI = listOf(R.id.tvDay1Low, R.id.tvDay2Low, R.id.tvDay3Low, R.id.tvDay4Low, R.id.tvDay5Low, R.id.tvDay6Low, R.id.tvDay7Low)

        var rainExpectedDay: String? = null

        for (i in 0..6) {
            val dayTitle = if (i == 0) "Today" else if (i == 1) "Tomorrow" else dayNameFormat.format(cal.time)
            val dateStr = dateFormat.format(cal.time)

            val condIdx = (locHash + i * 3) % conditions.size
            val (condText, condEmoji) = conditions[condIdx]

            val rainPercent = when (condText) {
                "Heavy Rain" -> 85
                "Light Rain" -> 60
                "Cloudy" -> 35
                "Partly Cloudy" -> 15
                else -> 0
            }

            if (rainPercent > 50 && rainExpectedDay == null) {
                rainExpectedDay = dayTitle
            }

            val highTemp = baseTemp + (i % 3)
            val lowTemp = highTemp - (6 + (i % 2))

            findViewById<TextView>(daysUI[i].first)?.text = dayTitle
            findViewById<TextView>(daysUI[i].second)?.text = "$dateStr • $condText"
            findViewById<TextView>(daysUI[i].third.first)?.text = condEmoji
            findViewById<TextView>(daysUI[i].third.second)?.text = "💧 $rainPercent%"
            findViewById<TextView>(highsUI[i])?.text = "$highTemp°"
            findViewById<TextView>(lowsUI[i])?.text = "$lowTemp°"

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val tvFarmingRecommendation = findViewById<TextView>(R.id.tvFarmingRecommendation)
        val recommendationText = if (rainExpectedDay != null) {
            "Rain expected around $rainExpectedDay in $userLocation. Complete harvesting and apply liquid fertilizers before rain starts. Ensure field drainage is clear."
        } else {
            "Clear and sunny weather forecasted in $userLocation for the week. Excellent conditions for irrigation, pesticide application, and soil preparation."
        }
        tvFarmingRecommendation?.text = recommendationText
    }
}
