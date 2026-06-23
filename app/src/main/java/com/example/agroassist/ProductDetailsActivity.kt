package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProductDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        // Read intent data
        val name = intent.getStringExtra("product_name") ?: "Copper Oxychloride 50% WP"
        val brand = intent.getStringExtra("product_brand") ?: "AgroTech"
        val rating = intent.getStringExtra("product_rating") ?: "⭐ 4.5  •  500+ reviews"
        val price = intent.getIntExtra("product_price", 450)
        val imageResId = intent.getIntExtra("product_image", R.drawable.copper_oxychloride)
        val description = intent.getStringExtra("product_description") 
            ?: "Broad spectrum fungicide effective against late blight, early blight, and other fungal diseases. Suitable for tomatoes, potatoes, and other vegetables."

        // Bind views
        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnAddToCart = findViewById<Button>(R.id.btnAddToCart)
        val btnFindShops = findViewById<Button>(R.id.btnFindShops)

        val tvProductName = findViewById<TextView>(R.id.tvProductName)
        val tvProductBrand = findViewById<TextView>(R.id.tvProductBrand)
        val tvProductRating = findViewById<TextView>(R.id.tvProductRating)
        val tvProductPrice = findViewById<TextView>(R.id.tvProductPrice)
        val tvProductDescription = findViewById<TextView>(R.id.tvProductDescription)
        val ivProductImage = findViewById<ImageView>(R.id.ivProductImage)

        // Populate views
        tvProductName.text = name
        tvProductBrand.text = "by $brand Industries"
        tvProductRating.text = rating
        tvProductPrice.text = "₹$price"
        tvProductDescription.text = description
        ivProductImage.setImageResource(imageResId)

        backButton.setOnClickListener { finish() }

        btnAddToCart.setOnClickListener {
            Toast.makeText(this, "$name added to cart", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, CartActivity::class.java).apply {
                putExtra("product_name", name)
                putExtra("product_price", price)
                putExtra("product_image", imageResId)
            }
            startActivity(intent)
        }

        btnFindShops.setOnClickListener {
            startActivity(Intent(this, NearbyShopsActivity::class.java))
        }
    }
}
