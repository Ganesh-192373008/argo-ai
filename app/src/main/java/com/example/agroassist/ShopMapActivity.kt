package com.example.agroassist

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.core.content.ContextCompat

class ShopMapActivity : AppCompatActivity() {

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup osmdroid configuration
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        setContentView(R.layout.activity_shop_map)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        // Initialize Map
        map = findViewById(R.id.mapView)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(14.0)
        
        // Get user location from database
        val dbHelper = AgroDatabaseHelper(this)
        val profile = dbHelper.getProfile()
        val userLocation = profile["location"]?.ifEmpty { "Pune, Maharashtra" } ?: "Pune, Maharashtra"
        
        val startPoint = getLatLngFromAddress(userLocation)
        mapController.setCenter(startPoint)

        // Add "You" marker
        val youMarker = Marker(map)
        youMarker.position = startPoint
        youMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        youMarker.title = "You ($userLocation)"
        
        val blueIcon = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.marker_default)?.mutate()
        blueIcon?.setTint(Color.parseColor("#2196F3"))
        youMarker.icon = blueIcon
        
        map.overlays.add(youMarker)

        // Add Shop Markers (scattered around base coordinate)
        addShopMarker(startPoint.latitude + 0.0046, startPoint.longitude + 0.0033, "AgroMart Store", "0.8 km", true)
        addShopMarker(startPoint.latitude - 0.0054, startPoint.longitude - 0.0067, "Farmer's Choice", "1.2 km", true)
        addShopMarker(startPoint.latitude - 0.0104, startPoint.longitude + 0.0133, "Green Valley Supplies", "2.1 km", true)
        addShopMarker(startPoint.latitude + 0.0096, startPoint.longitude - 0.0167, "Farm Fresh Store", "3.5 km", false)
        addShopMarker(startPoint.latitude + 0.0196, startPoint.longitude + 0.0083, "Krishi Kendra", "4.2 km", true)
    }

    private fun getLatLngFromAddress(addressStr: String): GeoPoint {
        try {
            val geocoder = android.location.Geocoder(this, java.util.Locale.getDefault())
            val addresses = geocoder.getFromLocationName(addressStr, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                return GeoPoint(address.latitude, address.longitude)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Fallback to Pune if geocoding fails
        return GeoPoint(18.5204, 73.8567)
    }

    private fun addShopMarker(lat: Double, lon: Double, name: String, distance: String, inStock: Boolean) {
        val marker = Marker(map)
        marker.position = GeoPoint(lat, lon)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = name
        marker.snippet = "$distance - ${if (inStock) "In Stock" else "Out of Stock"}"
        
        val tintColor = if (inStock) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        val shopIcon = ContextCompat.getDrawable(this, org.osmdroid.library.R.drawable.marker_default)?.mutate()
        shopIcon?.setTint(tintColor)
        marker.icon = shopIcon

        map.overlays.add(marker)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
