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
import android.preference.PreferenceManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MarketLocationActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var userLocationMarker: Marker? = null
    private var market1Marker: Marker? = null
    private var market2Marker: Marker? = null
    private var userLocationName = "Nashik"

    // Register location permission request
    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            enableGPSLocation()
        } else {
            Toast.makeText(this, "Location permission denied. Center map on default (Nashik).", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup osmdroid configuration
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        setContentView(R.layout.activity_market_location)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        // Initialize Map
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        mapController.setZoom(12.0)
        
        // Base center point: Nashik, Maharashtra
        var centerPoint = GeoPoint(19.9975, 73.7898)

        // Read saved location from database
        val dbHelper = AgroDatabaseHelper(this)
        val profile = dbHelper.getProfile()
        val savedLoc = profile["location"]
        if (!savedLoc.isNullOrEmpty()) {
            userLocationName = savedLoc
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(savedLoc, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    centerPoint = GeoPoint(address.latitude, address.longitude)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        mapController.setCenter(centerPoint)

        // Add User Location Marker
        userLocationMarker = Marker(mapView)
        userLocationMarker?.position = centerPoint
        userLocationMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        userLocationMarker?.title = "Your Location ($userLocationName)"
        mapView.overlays.add(userLocationMarker)

        // Add Market 1
        market1Marker = Marker(mapView)
        market1Marker?.position = GeoPoint(centerPoint.latitude + 0.0075, centerPoint.longitude + 0.0152)
        market1Marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        market1Marker?.title = "$userLocationName Mandi A"
        market1Marker?.snippet = "2.5 km - Best Price"
        mapView.overlays.add(market1Marker)

        // Add Market 2
        market2Marker = Marker(mapView)
        market2Marker?.position = GeoPoint(centerPoint.latitude + 0.0250, centerPoint.longitude - 0.0200)
        market2Marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        market2Marker?.title = "$userLocationName District Mandi B"
        market2Marker?.snippet = "5.8 km"
        mapView.overlays.add(market2Marker)

        // Request real GPS location
        checkAndRequestLocation()

        // Bind Google Maps intents
        val btnOpenGoogleMaps = findViewById<TextView>(R.id.btnOpenGoogleMaps)
        btnOpenGoogleMaps.setOnClickListener {
            openInGoogleMaps("agricultural mandi vegetable market near $userLocationName")
        }

        val cardMarket1 = findViewById<CardView>(R.id.cardMarket1)
        cardMarket1?.setOnClickListener {
            val marketName = market1Marker?.title ?: "Local Market A"
            openInGoogleMaps(marketName)
        }

        val cardMarket2 = findViewById<CardView>(R.id.cardMarket2)
        cardMarket2?.setOnClickListener {
            val marketName = market2Marker?.title ?: "District Mandi B"
            openInGoogleMaps(marketName)
        }

        // Setup crop comparison simulated spinner
        val btnSelectCrop = findViewById<android.view.View>(R.id.btnSelectCrop)
        val tvSelectedCrop = findViewById<TextView>(R.id.tvSelectedCrop)
        
        val tvBestMarketTitle = findViewById<TextView>(R.id.tvBestMarketTitle)
        val tvBestMarketDetails = findViewById<TextView>(R.id.tvBestMarketDetails)
        
        val tvMarket1Price = findViewById<TextView>(R.id.tvMarket1Price)
        val tvMarket1PriceChange = findViewById<TextView>(R.id.tvMarket1PriceChange)
        val tvMarket1High = findViewById<TextView>(R.id.tvMarket1High)
        val tvMarket1Low = findViewById<TextView>(R.id.tvMarket1Low)
        
        val tvMarket2Price = findViewById<TextView>(R.id.tvMarket2Price)
        val tvMarket2PriceChange = findViewById<TextView>(R.id.tvMarket2PriceChange)
        val tvMarket2High = findViewById<TextView>(R.id.tvMarket2High)
        val tvMarket2Low = findViewById<TextView>(R.id.tvMarket2Low)
        
        val btnSearchCropMaps = findViewById<Button>(R.id.btnSearchCropMaps)
        btnSearchCropMaps?.setOnClickListener {
            val selectedCrop = tvSelectedCrop?.text?.toString() ?: "Tomato"
            openInGoogleMaps("$selectedCrop market mandi near $userLocationName")
        }

        val btnBestMarketRecommendation = findViewById<android.view.View>(R.id.btnBestMarketRecommendation)
        btnBestMarketRecommendation?.setOnClickListener {
            val bestMarket = tvBestMarketTitle?.text?.toString() ?: ""
            val query = if (bestMarket.contains("(")) {
                bestMarket.substringBefore("(").trim()
            } else {
                bestMarket.trim()
            }
            if (query.isNotEmpty()) {
                openInGoogleMaps(query)
            }
        }

        val cropsList = arrayOf("Tomato", "Potato", "Onion", "Corn", "Wheat", "Rice", "Cotton", "Sugarcane", "Cabbage")
        
        btnSelectCrop?.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Select Crop")
            builder.setItems(cropsList) { dialog, which ->
                val selectedCrop = cropsList[which]
                tvSelectedCrop?.text = selectedCrop
                btnSearchCropMaps?.text = "Search $selectedCrop in Maps 🔍"
                updateMarketPrices(
                    selectedCrop,
                    tvBestMarketTitle, tvBestMarketDetails,
                    tvMarket1Price, tvMarket1PriceChange, tvMarket1High, tvMarket1Low,
                    tvMarket2Price, tvMarket2PriceChange, tvMarket2High, tvMarket2Low
                )
            }
            builder.show()
        }

        // Get SELECTED_CROP from intent and initialize prices on startup
        val selectedCropFromIntent = intent.getStringExtra("SELECTED_CROP")?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Tomato"
        tvSelectedCrop?.text = selectedCropFromIntent
        btnSearchCropMaps?.text = "Search $selectedCropFromIntent in Maps 🔍"
        
        updateMarketPrices(
            selectedCropFromIntent,
            tvBestMarketTitle, tvBestMarketDetails,
            tvMarket1Price, tvMarket1PriceChange, tvMarket1High, tvMarket1Low,
            tvMarket2Price, tvMarket2PriceChange, tvMarket2High, tvMarket2Low
        )
    }

    private fun updateMarketPrices(
        crop: String,
        tvBestTitle: TextView?, tvBestDetails: TextView?,
        tvM1Price: TextView?, tvM1Change: TextView?, tvM1High: TextView?, tvM1Low: TextView?,
        tvM2Price: TextView?, tvM2Change: TextView?, tvM2High: TextView?, tvM2Low: TextView?
    ) {
        val name1 = market1Marker?.title ?: "$userLocationName Mandi A"
        val name2 = market2Marker?.title ?: "$userLocationName District Mandi B"
        when (crop) {
            "Tomato" -> {
                tvBestTitle?.text = "$name1 (Rs. 22/kg)"
                tvBestDetails?.text = "2.5 km away - 10 mins travel"
                
                tvM1Price?.text = "Rs. 22/kg"
                tvM1Change?.text = "+ Rs. 2 (+10%)"
                tvM1Change?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                tvM1High?.text = "High: Rs. 24/kg"
                tvM1Low?.text = "Low: Rs. 18/kg"
                
                tvM2Price?.text = "Rs. 19/kg"
                tvM2Change?.text = "- Rs. 1 (-5%)"
                tvM2Change?.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                tvM2High?.text = "High: Rs. 21/kg"
                tvM2Low?.text = "Low: Rs. 17/kg"
            }
            "Potato" -> {
                tvBestTitle?.text = "$name2 (Rs. 25/kg)"
                tvBestDetails?.text = "5.8 km away - 20 mins travel"
                
                tvM1Price?.text = "Rs. 20/kg"
                tvM1Change?.text = "- Rs. 2 (-9%)"
                tvM1Change?.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                tvM1High?.text = "High: Rs. 22/kg"
                tvM1Low?.text = "Low: Rs. 17/kg"
                
                tvM2Price?.text = "Rs. 25/kg"
                tvM2Change?.text = "+ Rs. 3 (+13%)"
                tvM2Change?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                tvM2High?.text = "High: Rs. 27/kg"
                tvM2Low?.text = "Low: Rs. 20/kg"
            }
            "Corn" -> {
                tvBestTitle?.text = "$name1 (Rs. 15/kg)"
                tvBestDetails?.text = "2.5 km away - 10 mins travel"
                
                tvM1Price?.text = "Rs. 15/kg"
                tvM1Change?.text = "+ Rs. 1 (+7%)"
                tvM1Change?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                tvM1High?.text = "High: Rs. 17/kg"
                tvM1Low?.text = "Low: Rs. 12/kg"
                
                tvM2Price?.text = "Rs. 14/kg"
                tvM2Change?.text = "- Rs. 0.5 (-3%)"
                tvM2Change?.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                tvM2High?.text = "High: Rs. 16/kg"
                tvM2Low?.text = "Low: Rs. 11/kg"
            }
            "Wheat" -> {
                tvBestTitle?.text = "$name1 (Rs. 30/kg)"
                tvBestDetails?.text = "2.5 km away - 10 mins travel"
                
                tvM1Price?.text = "Rs. 30/kg"
                tvM1Change?.text = "+ Rs. 2.5 (+9%)"
                tvM1Change?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                tvM1High?.text = "High: Rs. 32/kg"
                tvM1Low?.text = "Low: Rs. 26/kg"
                
                tvM2Price?.text = "Rs. 28/kg"
                tvM2Change?.text = "+ Rs. 0.5 (+2%)"
                tvM2Change?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                tvM2High?.text = "High: Rs. 30/kg"
                tvM2Low?.text = "Low: Rs. 24/kg"
            }
            "Rice" -> {
                tvBestTitle?.text = "$name2 (Rs. 45/kg)"
                tvBestDetails?.text = "5.8 km away - 20 mins travel"
                
                tvM1Price?.text = "Rs. 42/kg"
                tvM1Change?.text = "+ Rs. 1 (+2%)"
                tvM1Change?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                tvM1High?.text = "High: Rs. 44/kg"
                tvM1Low?.text = "Low: Rs. 38/kg"
                
                tvM2Price?.text = "Rs. 45/kg"
                tvM2Change?.text = "+ Rs. 4 (+10%)"
                tvM2Change?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                tvM2High?.text = "High: Rs. 48/kg"
                tvM2Low?.text = "Low: Rs. 40/kg"
            }
            else -> {
                tvBestTitle?.text = "$name1 (Rs. 35/kg)"
                tvBestDetails?.text = "2.5 km away - 10 mins travel"
                
                tvM1Price?.text = "Rs. 35/kg"
                tvM1Change?.text = "+ Rs. 3 (+9%)"
                tvM1Change?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                tvM1High?.text = "High: Rs. 38/kg"
                tvM1Low?.text = "Low: Rs. 32/kg"
                
                tvM2Price?.text = "Rs. 33/kg"
                tvM2Change?.text = "- Rs. 1 (-3%)"
                tvM2Change?.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                tvM2High?.text = "High: Rs. 36/kg"
                tvM2Low?.text = "Low: Rs. 31/kg"
            }
        }
    }

    private fun openInGoogleMaps(query: String) {
        val intentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
        val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Fallback: Open in web browser
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
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
            enableGPSLocation()
        } else {
            requestLocationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun enableGPSLocation() {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    updateLocationAndMarkets(location.latitude, location.longitude)
                    locationManager.removeUpdates(this)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            // Register listener for both GPS and Network providers
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                val lastKnownGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val bestLocation = lastKnownGPS ?: lastKnownNetwork
                bestLocation?.let {
                    updateLocationAndMarkets(it.latitude, it.longitude)
                }

                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000L,
                        5f,
                        locationListener
                    )
                }
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000L,
                        5f,
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

    private fun updateLocationAndMarkets(latitude: Double, longitude: Double) {
        val userPoint = GeoPoint(latitude, longitude)
        userLocationMarker?.position = userPoint
        
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
                    userLocationName = resolvedName
                    val dbHelper = AgroDatabaseHelper(this)
                    dbHelper.saveLocation(resolvedName, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        userLocationMarker?.title = "Your Location ($userLocationName)"
        userLocationMarker?.snippet = "Lat: $latitude, Lng: $longitude"

        // Update Market Markers relative to user
        market1Marker?.position = GeoPoint(latitude + 0.0075, longitude + 0.0152)
        market1Marker?.title = "$userLocationName Mandi A"
        market1Marker?.snippet = "2.5 km - Best Price"

        market2Marker?.position = GeoPoint(latitude + 0.0250, longitude - 0.0200)
        market2Marker?.title = "$userLocationName District Mandi B"
        market2Marker?.snippet = "5.8 km"

        mapView.controller.animateTo(userPoint)
        mapView.invalidate()
        
        // Instantly refresh titles in UI to represent local market name
        findViewById<TextView>(R.id.tvMarket1Title)?.text = market1Marker?.title
        findViewById<TextView>(R.id.tvMarket2Title)?.text = market2Marker?.title
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
