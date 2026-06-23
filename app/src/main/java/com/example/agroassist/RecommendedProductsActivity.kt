package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecommendedProductsActivity : AppCompatActivity() {

    private lateinit var recyclerViewProducts: RecyclerView
    private lateinit var adapter: RecommendedProductAdapter

    private val productList = listOf(
        RecommendedProduct(
            id = "copper_oxychloride",
            name = "Copper Oxychloride 50% WP",
            brand = "AgroTech",
            rating = "⭐ 4.5 • 500+ reviews",
            price = 450,
            imageResId = R.drawable.copper_oxychloride,
            description = "Broad spectrum fungicide effective against late blight, early blight, and other fungal diseases. Suitable for tomatoes, potatoes, and other vegetables."
        ),
        RecommendedProduct(
            id = "mancozeb",
            name = "Mancozeb 75% WP",
            brand = "FarmCare",
            rating = "⭐ 4.7 • 500+ reviews",
            price = 380,
            imageResId = R.drawable.mancozeb,
            description = "High-performance contact fungicide for preventive control of scab, blight, and downy mildew. Offers excellent crop safety and adhesion."
        ),
        RecommendedProduct(
            id = "chlorothalonil",
            name = "Chlorothalonil 75% WP",
            brand = "CropGuard",
            rating = "⭐ 4.3 • 500+ reviews",
            price = 520,
            imageResId = R.drawable.chlorothalonil,
            description = "Multi-site protectant fungicide providing broad-spectrum control of leaf spots, rust, and anthracnose. Resists rain wash-off."
        ),
        RecommendedProduct(
            id = "metalaxyl",
            name = "Metalaxyl 8% + Mancozeb",
            brand = "BioFarm",
            rating = "⭐ 4.8 • 500+ reviews",
            price = 680,
            imageResId = R.drawable.metalaxyl,
            description = "Dual-action systemic and contact fungicide offering dual protection. Targets downy mildew and phytophthora in potato, grape, and mustard crops."
        ),
        RecommendedProduct(
            id = "urea",
            name = "Urea Fertilizer (46% Nitrogen)",
            brand = "AgroTech",
            rating = "⭐ 4.6 • 400+ reviews",
            price = 280,
            imageResId = R.drawable.urea_fertilizer,
            description = "High-nitrogen fertilizer designed to promote robust vegetative growth and deep green foliage. Suitable for cereal and leafy vegetable crops."
        ),
        RecommendedProduct(
            id = "dap",
            name = "DAP Fertilizer (18-46-0)",
            brand = "FarmCare",
            rating = "⭐ 4.7 • 350+ reviews",
            price = 550,
            imageResId = R.drawable.dap_fertilizer,
            description = "Premium phosphate fertilizer providing necessary nutrition to root structures and early seed development. Highly soluble and efficient."
        ),
        RecommendedProduct(
            id = "npk",
            name = "NPK 19-19-19 Soluble",
            brand = "BioFarm",
            rating = "⭐ 4.8 • 600+ reviews",
            price = 320,
            imageResId = R.drawable.npk_fertilizer,
            description = "Perfect balance of Nitrogen, Phosphorus, and Potassium. Fully water-soluble fertilizer for foliar application and fertigation."
        ),
        RecommendedProduct(
            id = "neem_oil",
            name = "Neem Oil Organic Insecticide",
            brand = "NatureShield",
            rating = "⭐ 4.5 • 250+ reviews",
            price = 290,
            imageResId = R.drawable.neem_oil,
            description = "100% cold-pressed organic insecticide and acaricide. Safely controls aphids, whiteflies, spider mites, and scale insects."
        ),
        RecommendedProduct(
            id = "imidacloprid",
            name = "Imidacloprid 17.8% SL",
            brand = "CropGuard",
            rating = "⭐ 4.4 • 480+ reviews",
            price = 410,
            imageResId = R.drawable.imidacloprid,
            description = "Highly systemic insecticide for controlling sucking insects, termites, and beetles. Fast-acting protective shield."
        ),
        RecommendedProduct(
            id = "tomato_seeds",
            name = "Premium Hybrid Tomato Seeds",
            brand = "Sementis",
            rating = "⭐ 4.9 • 300+ reviews",
            price = 150,
            imageResId = R.drawable.tomato_seeds,
            description = "F1 hybrid seeds offering excellent yield, disease tolerance, and firm, deep red fruits. Perfect for kitchen gardens or farms."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommended_products)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnViewCart = findViewById<ImageView>(R.id.btnViewCart)

        backButton.setOnClickListener { finish() }
        
        btnViewCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        recyclerViewProducts = findViewById(R.id.recyclerViewRecommendedProducts)
        recyclerViewProducts.layoutManager = LinearLayoutManager(this)
        
        adapter = RecommendedProductAdapter(
            products = productList,
            onCardClick = { product ->
                launchProductDetails(product)
            },
            onAddClick = { product ->
                addToCart(product)
            }
        )
        recyclerViewProducts.adapter = adapter
    }

    private fun launchProductDetails(product: RecommendedProduct) {
        val intent = Intent(this, ProductDetailsActivity::class.java).apply {
            putExtra("product_name", product.name)
            putExtra("product_brand", product.brand)
            putExtra("product_rating", product.rating)
            putExtra("product_price", product.price)
            putExtra("product_image", product.imageResId)
            putExtra("product_description", product.description)
        }
        startActivity(intent)
    }

    private fun addToCart(product: RecommendedProduct) {
        val intent = Intent(this, CartActivity::class.java).apply {
            putExtra("product_name", product.name)
            putExtra("product_price", product.price)
            putExtra("product_image", product.imageResId)
        }
        startActivity(intent)
    }
}
