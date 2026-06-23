package com.example.agroassist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NearbyShopsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_shops)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnOpenMap = findViewById<android.widget.LinearLayout>(R.id.btnOpenMap)

        backButton.setOnClickListener { finish() }
        
        btnOpenMap.setOnClickListener {
            startActivity(Intent(this, ShopMapActivity::class.java))
        }

        // Get saved user location
        val dbHelper = AgroDatabaseHelper(this)
        val profile = dbHelper.getProfile()
        val userLocation = profile["location"]?.ifEmpty { "Pune, Maharashtra" } ?: "Pune, Maharashtra"

        val address1 = "123 Main Street, $userLocation"
        val address2 = "456 Market Road, $userLocation"
        val address3 = "321 Village Road, $userLocation"

        // Address TextViews
        val tvShopAddress1 = findViewById<TextView>(R.id.tvShopAddress1)
        val tvShopAddress2 = findViewById<TextView>(R.id.tvShopAddress2)
        val tvShopAddress3 = findViewById<TextView>(R.id.tvShopAddress3)

        tvShopAddress1.text = "📍 $address1"
        tvShopAddress2.text = "📍 $address2"
        tvShopAddress3.text = "📍 $address3"

        // Call Buttons
        val btnCallShop1 = findViewById<Button>(R.id.btnCallShop1)
        val btnCallShop2 = findViewById<Button>(R.id.btnCallShop2)
        val btnCallShop3 = findViewById<Button>(R.id.btnCallShop3)

        // Directions Buttons
        val btnDirectionsShop1 = findViewById<Button>(R.id.btnDirectionsShop1)
        val btnDirectionsShop2 = findViewById<Button>(R.id.btnDirectionsShop2)
        val btnDirectionsShop3 = findViewById<Button>(R.id.btnDirectionsShop3)

        btnCallShop1.setOnClickListener { makeCall("+919876543200") }
        btnCallShop2.setOnClickListener { makeCall("+919876543201") }
        btnCallShop3.setOnClickListener { makeCall("+919876543202") }

        btnDirectionsShop1.setOnClickListener { showDirections(address1) }
        btnDirectionsShop2.setOnClickListener { showDirections(address2) }
        btnDirectionsShop3.setOnClickListener { showDirections(address3) }
    }

    private fun makeCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        startActivity(intent)
    }

    private fun showDirections(address: String) {
        val intentUri = Uri.parse("geo:0,0?q=" + Uri.encode(address))
        val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
        startActivity(mapIntent)
    }
}
