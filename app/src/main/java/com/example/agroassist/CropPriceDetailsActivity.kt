package com.example.agroassist

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CropPriceDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_price_details)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        // Get the selected crop name from intent
        val cropName = intent.getStringExtra("CROP_NAME") ?: "Tomato"

        // Initialize UI Elements
        val textCropTitle = findViewById<TextView>(R.id.textCropTitle)
        val textCropEmoji = findViewById<TextView>(R.id.textCropEmoji)
        val textCropPrice = findViewById<TextView>(R.id.textCropPrice)
        val textCropChange = findViewById<TextView>(R.id.textCropChange)

        val textDay1Price = findViewById<TextView>(R.id.textDay1Price)
        val viewDay1Bar = findViewById<View>(R.id.viewDay1Bar)
        val textDay1Date = findViewById<TextView>(R.id.textDay1Date)

        val textDay2Price = findViewById<TextView>(R.id.textDay2Price)
        val viewDay2Bar = findViewById<View>(R.id.viewDay2Bar)
        val textDay2Date = findViewById<TextView>(R.id.textDay2Date)

        val textDay3Price = findViewById<TextView>(R.id.textDay3Price)
        val viewDay3Bar = findViewById<View>(R.id.viewDay3Bar)
        val textDay3Date = findViewById<TextView>(R.id.textDay3Date)

        val textDay4Price = findViewById<TextView>(R.id.textDay4Price)
        val viewDay4Bar = findViewById<View>(R.id.viewDay4Bar)
        val textDay4Date = findViewById<TextView>(R.id.textDay4Date)

        val textDay5Price = findViewById<TextView>(R.id.textDay5Price)
        val viewDay5Bar = findViewById<View>(R.id.viewDay5Bar)
        val textDay5Date = findViewById<TextView>(R.id.textDay5Date)

        val textMarket1Name = findViewById<TextView>(R.id.textMarket1Name)
        val textMarket1Price = findViewById<TextView>(R.id.textMarket1Price)
        val textMarket1Unit = findViewById<TextView>(R.id.textMarket1Unit)
        val textMarket2Name = findViewById<TextView>(R.id.textMarket2Name)
        val textMarket2Price = findViewById<TextView>(R.id.textMarket2Price)
        val textMarket2Unit = findViewById<TextView>(R.id.textMarket2Unit)
        val textMarket3Name = findViewById<TextView>(R.id.textMarket3Name)
        val textMarket3Price = findViewById<TextView>(R.id.textMarket3Price)
        val textMarket3Unit = findViewById<TextView>(R.id.textMarket3Unit)
        val textMarket4Name = findViewById<TextView>(R.id.textMarket4Name)
        val textMarket4Price = findViewById<TextView>(R.id.textMarket4Price)
        val textMarket4Unit = findViewById<TextView>(R.id.textMarket4Unit)

        val textMarketInsight = findViewById<TextView>(R.id.textMarketInsight)

        // Generate dynamic live dates for graph (Today and past 4 days)
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val d5 = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val d4 = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val d3 = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val d2 = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val d1 = dateFormat.format(cal.time)

        textDay1Date?.text = d1
        textDay2Date?.text = d2
        textDay3Date?.text = d3
        textDay4Date?.text = d4
        textDay5Date?.text = d5

        // Read location to localize market names
        val dbHelper = AgroDatabaseHelper(this)
        val profile = dbHelper.getProfile()
        val userLocation = profile["location"]?.ifEmpty { "APMC Market" } ?: "APMC Market"
        val locationCity = if (userLocation.contains(",")) userLocation.substringBefore(",") else userLocation

        textMarket1Name?.text = "APMC Market, $locationCity"
        textMarket2Name?.text = "Central Mandi, $locationCity"
        textMarket3Name?.text = "District Wholesale Market"
        textMarket4Name?.text = "Regional Farmer Hub"

        // Setup Fruit & Vegetable Data Maps
        val emojiMap = mapOf(
            "Tomato" to "🍅", "Potato" to "🥔", "Onion" to "🧅", "Rice" to "🌾",
            "Wheat" to "🌾", "Cotton" to "☁️", "Sugarcane" to "🎋", "Cabbage" to "🥬",
            "Apple" to "🍎", "Banana" to "🍌", "Mango" to "🥭", "Orange" to "🍊",
            "Grapes" to "🍇", "Pomegranate" to "🍎", "Lemon" to "🍋", "Coconut" to "🥥",
            "Spinach" to "🥬", "Cauliflower" to "🥦", "Okra" to "🥒", "Brinjal" to "🍆",
            "Carrot" to "🥕", "Cucumber" to "🥒", "Garlic" to "🧄", "Ginger" to "🫚",
            "Chilli" to "🌶️", "Papaya" to "🍐", "Watermelon" to "🍉", "Guava" to "🍈"
        )

        val colorTintMap = mapOf(
            "Tomato" to "#FFEBEE", "Potato" to "#FFF3E0", "Onion" to "#F3E5F5", "Rice" to "#FFFDE7",
            "Wheat" to "#FFFDE7", "Cotton" to "#E3F2FD", "Sugarcane" to "#F1F8E9", "Cabbage" to "#E8F5E9",
            "Apple" to "#FFEBEE", "Banana" to "#FFFDE7", "Mango" to "#FFF3E0", "Orange" to "#FFE0B2",
            "Grapes" to "#E1BEE7", "Pomegranate" to "#FFCDD2", "Lemon" to "#FFF9C4", "Coconut" to "#D7CCC8"
        )

        // Read extras passed from MarketPricesActivity
        val cropEmoji = intent.getStringExtra("CROP_EMOJI") ?: (emojiMap[cropName] ?: "🍅")
        val cropPrice = intent.getStringExtra("CROP_PRICE") ?: "₹45.0/kg"
        val cropChange = intent.getStringExtra("CROP_CHANGE") ?: "+4.5% vs last week"
        val cropTrend = intent.getStringExtra("CROP_TREND") ?: "up"
        val cropBgTint = intent.getStringExtra("CROP_BG_TINT") ?: (colorTintMap[cropName] ?: "#FFEBEE")

        textCropTitle.text = "$cropName Details"
        textCropEmoji.text = cropEmoji
        textCropEmoji.backgroundTintList = android.content.res.ColorStateList.valueOf(
            Color.parseColor(cropBgTint)
        )
        textCropPrice.text = cropPrice
        textCropChange.text = cropChange

        // Set trend styling
        when (cropTrend.lowercase(java.util.Locale.ROOT)) {
            "up" -> {
                textCropChange.setTextColor(Color.parseColor("#2E7D32"))
                textCropChange.setBackgroundColor(Color.parseColor("#E8F5E9"))
            }
            "down" -> {
                textCropChange.setTextColor(Color.parseColor("#C62828"))
                textCropChange.setBackgroundColor(Color.parseColor("#FFEBEE"))
            }
            else -> {
                textCropChange.setTextColor(Color.parseColor("#757575"))
                textCropChange.setBackgroundColor(Color.parseColor("#EEEEEE"))
            }
        }

        // Parse clean numeric price to compute dynamic 5-day graph bars
        val cleanPriceNum = cropPrice.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 45.0
        val isTrendUp = cropTrend.lowercase(java.util.Locale.ROOT) == "up"
        val isTrendDown = cropTrend.lowercase(java.util.Locale.ROOT) == "down"

        val p1 = cleanPriceNum * (if (isTrendUp) 0.88 else if (isTrendDown) 1.12 else 0.98)
        val p2 = cleanPriceNum * (if (isTrendUp) 0.92 else if (isTrendDown) 1.08 else 0.99)
        val p3 = cleanPriceNum * (if (isTrendUp) 0.90 else if (isTrendDown) 1.05 else 1.01)
        val p4 = cleanPriceNum * (if (isTrendUp) 0.96 else if (isTrendDown) 1.02 else 0.99)
        val p5 = cleanPriceNum

        textDay1Price.text = String.format("₹%.0f", p1)
        textDay2Price.text = String.format("₹%.0f", p2)
        textDay3Price.text = String.format("₹%.0f", p3)
        textDay4Price.text = String.format("₹%.0f", p4)
        textDay5Price.text = String.format("₹%.0f", p5)

        val h1 = if (isTrendUp) 55 else if (isTrendDown) 82 else 70
        val h2 = if (isTrendUp) 63 else if (isTrendDown) 76 else 72
        val h3 = if (isTrendUp) 60 else if (isTrendDown) 74 else 69
        val h4 = if (isTrendUp) 72 else if (isTrendDown) 66 else 71
        val h5 = 80

        val density = resources.displayMetrics.density
        viewDay1Bar.layoutParams = viewDay1Bar.layoutParams.apply { height = (h1 * density).toInt() }
        viewDay2Bar.layoutParams = viewDay2Bar.layoutParams.apply { height = (h2 * density).toInt() }
        viewDay3Bar.layoutParams = viewDay3Bar.layoutParams.apply { height = (h3 * density).toInt() }
        viewDay4Bar.layoutParams = viewDay4Bar.layoutParams.apply { height = (h4 * density).toInt() }
        viewDay5Bar.layoutParams = viewDay5Bar.layoutParams.apply { height = (h5 * density).toInt() }

        // Populate Mandi Market Prices
        val mp1 = String.format("₹%.1f", cleanPriceNum * 0.95)
        val mp2 = String.format("₹%.1f", cleanPriceNum * 1.04)
        val mp3 = String.format("₹%.1f", cleanPriceNum * 0.92)
        val mp4 = String.format("₹%.1f", cleanPriceNum * 0.89)

        textMarket1Price.text = mp1
        textMarket2Price.text = mp2
        textMarket3Price.text = mp3
        textMarket4Price.text = mp4

        val parsedUnit = if (cropPrice.contains("/")) cropPrice.substringAfter("/") else "kg"
        val unitStr = "per $parsedUnit"
        textMarket1Unit.text = unitStr
        textMarket2Unit.text = unitStr
        textMarket3Unit.text = unitStr
        textMarket4Unit.text = unitStr

        // Populate Live Market Insight
        textMarketInsight.text = "$cropName live market rates in $locationCity are experiencing active trading with a $cropChange. Harvesting and selling in primary wholesale mandis today offers optimal profit margins."

        val btnViewMandiMap = findViewById<View>(R.id.btnViewMandiMap)
        btnViewMandiMap?.setOnClickListener {
            val mapIntent = android.content.Intent(this, MarketLocationActivity::class.java).apply {
                putExtra("SELECTED_CROP", cropName)
            }
            startActivity(mapIntent)
        }
    }
}
