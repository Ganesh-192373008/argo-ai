package com.example.agroassist

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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
        val textDay2Price = findViewById<TextView>(R.id.textDay2Price)
        val viewDay2Bar = findViewById<View>(R.id.viewDay2Bar)
        val textDay3Price = findViewById<TextView>(R.id.textDay3Price)
        val viewDay3Bar = findViewById<View>(R.id.viewDay3Bar)
        val textDay4Price = findViewById<TextView>(R.id.textDay4Price)
        val viewDay4Bar = findViewById<View>(R.id.viewDay4Bar)
        val textDay5Price = findViewById<TextView>(R.id.textDay5Price)
        val viewDay5Bar = findViewById<View>(R.id.viewDay5Bar)

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

        // Setup Crop Information Data
        val emojiMap = mapOf(
            "Tomato" to "🍅", "Potato" to "🥔", "Onion" to "🧅", "Rice" to "🌾",
            "Wheat" to "🌾", "Cotton" to "☁️", "Sugarcane" to "🎋", "Cabbage" to "🥬"
        )
        val colorTintMap = mapOf(
            "Tomato" to "#FFEBEE", "Potato" to "#FFF3E0", "Onion" to "#F3E5F5", "Rice" to "#FFFDE7",
            "Wheat" to "#FFFDE7", "Cotton" to "#E3F2FD", "Sugarcane" to "#F1F8E9", "Cabbage" to "#E8F5E9"
        )
        val priceMap = mapOf(
            "Tomato" to "₹48.0/kg", "Potato" to "₹32.0/kg", "Onion" to "₹38.0/kg", "Rice" to "₹65.0/kg",
            "Wheat" to "₹45.0/kg", "Cotton" to "₹120.0/kg", "Sugarcane" to "₹60.0/kg", "Cabbage" to "₹28.0/kg"
        )
        val changeMap = mapOf(
            "Tomato" to "+12% this week", "Potato" to "-5% this week", "Onion" to "+8% this week", "Rice" to "+3% this week",
            "Wheat" to "0% this week", "Cotton" to "+15% this week", "Sugarcane" to "+7% this week", "Cabbage" to "-3% this week"
        )
        val graphPrices = mapOf(
            "Tomato" to listOf("₹42", "₹44", "₹43", "₹46", "₹48"),
            "Potato" to listOf("₹36", "₹35", "₹34", "₹33", "₹32"),
            "Onion" to listOf("₹34", "₹35", "₹37", "₹36", "₹38"),
            "Rice" to listOf("₹63", "₹62", "₹64", "₹64", "₹65"),
            "Wheat" to listOf("₹45", "₹45", "₹45", "₹45", "₹45"),
            "Cotton" to listOf("₹100", "₹105", "₹110", "₹115", "₹120"),
            "Sugarcane" to listOf("₹55", "₹56", "₹58", "₹59", "₹60"),
            "Cabbage" to listOf("₹31", "₹30", "₹29", "₹28.5", "₹28")
        )
        val graphHeights = mapOf(
            "Tomato" to listOf(60, 68, 64, 74, 80),
            "Potato" to listOf(80, 76, 70, 66, 60),
            "Onion" to listOf(60, 65, 75, 70, 80),
            "Rice" to listOf(74, 70, 78, 78, 80),
            "Wheat" to listOf(75, 75, 75, 75, 75),
            "Cotton" to listOf(55, 63, 70, 75, 80),
            "Sugarcane" to listOf(60, 64, 72, 76, 80),
            "Cabbage" to listOf(80, 75, 68, 65, 60)
        )
        val marketsMap = mapOf(
            "Tomato" to listOf("₹44", "₹48", "₹43", "₹42"),
            "Potato" to listOf("₹30", "₹32", "₹29", "₹28"),
            "Onion" to listOf("₹34", "₹38", "₹33", "₹32"),
            "Rice" to listOf("₹60", "₹65", "₹58", "₹59"),
            "Wheat" to listOf("₹42", "₹45", "₹40", "₹41"),
            "Cotton" to listOf("₹110", "₹120", "₹105", "₹108"),
            "Sugarcane" to listOf("₹54", "₹60", "₹52", "₹55"),
            "Cabbage" to listOf("₹25", "₹28", "₹24", "₹23")
        )
        val insightMap = mapOf(
            "Tomato" to "Tomato prices are on the rise due to seasonal monsoon rain delays affecting transport from major southern mandis. Harvest early if ready.",
            "Potato" to "Cold storage releases are steady, keeping potato prices stable with a slight downward trend as fresh supply reaches markets.",
            "Onion" to "Export demands and lower arrivals from Maharashtra have nudged onion online retail prices upward.",
            "Rice" to "Rice prices are highly stable. A small premium is observed on high-quality grain varieties.",
            "Wheat" to "Government wheat procurement is at its peak. Buffer stocks are strong, keeping retail prices completely flat.",
            "Cotton" to "High international demand and lower domestic yield estimates this season have pushed cotton rates up.",
            "Sugarcane" to "Crushing season demand from sugar mills remains robust, causing sugarcane rates to stay firm.",
            "Cabbage" to "Excellent local harvests have flooded the retail markets, causing cabbage rates to soften."
        )

        // Read extras passed from MarketPricesActivity
        val cropEmoji = intent.getStringExtra("CROP_EMOJI") ?: (emojiMap[cropName] ?: "🍅")
        val cropPrice = intent.getStringExtra("CROP_PRICE") ?: (priceMap[cropName] ?: "₹35.0/kg")
        val cropChange = intent.getStringExtra("CROP_CHANGE") ?: (changeMap[cropName] ?: "+0% this week")
        val cropTrend = intent.getStringExtra("CROP_TREND") ?: "neutral"
        val cropBgTint = intent.getStringExtra("CROP_BG_TINT") ?: (colorTintMap[cropName] ?: "#FFEBEE")

        // Populate dynamic views
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

        // Parse clean numeric price to generate dynamic graph and market rates if not in static list
        val cleanPriceNum = cropPrice.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 35.0
        val isTrendUpValue = cropTrend.lowercase(java.util.Locale.ROOT) == "up"
        val isTrendDownValue = cropTrend.lowercase(java.util.Locale.ROOT) == "down"

        val prices = graphPrices[cropName] ?: listOf(
            String.format("₹%.0f", cleanPriceNum * (if (isTrendUpValue) 0.90 else if (isTrendDownValue) 1.10 else 1.0)),
            String.format("₹%.0f", cleanPriceNum * (if (isTrendUpValue) 0.93 else if (isTrendDownValue) 1.07 else 1.0)),
            String.format("₹%.0f", cleanPriceNum * (if (isTrendUpValue) 0.91 else if (isTrendDownValue) 1.05 else 1.0)),
            String.format("₹%.0f", cleanPriceNum * (if (isTrendUpValue) 0.96 else if (isTrendDownValue) 1.02 else 1.0)),
            String.format("₹%.0f", cleanPriceNum)
        )

        val heights = graphHeights[cropName] ?: listOf(
            if (isTrendUpValue) 55 else if (isTrendDownValue) 80 else 70,
            if (isTrendUpValue) 62 else if (isTrendDownValue) 75 else 70,
            if (isTrendUpValue) 59 else if (isTrendDownValue) 72 else 70,
            if (isTrendUpValue) 70 else if (isTrendDownValue) 65 else 70,
            80
        )
        val density = resources.displayMetrics.density

        textDay1Price.text = prices[0]
        textDay2Price.text = prices[1]
        textDay3Price.text = prices[2]
        textDay4Price.text = prices[3]
        textDay5Price.text = prices[4]

        viewDay1Bar.layoutParams = viewDay1Bar.layoutParams.apply { height = (heights[0] * density).toInt() }
        viewDay2Bar.layoutParams = viewDay2Bar.layoutParams.apply { height = (heights[1] * density).toInt() }
        viewDay3Bar.layoutParams = viewDay3Bar.layoutParams.apply { height = (heights[2] * density).toInt() }
        viewDay4Bar.layoutParams = viewDay4Bar.layoutParams.apply { height = (heights[3] * density).toInt() }
        viewDay5Bar.layoutParams = viewDay5Bar.layoutParams.apply { height = (heights[4] * density).toInt() }

        // Populate Market Rates
        val mPrices = marketsMap[cropName] ?: listOf(
            String.format("₹%.1f", cleanPriceNum * 0.94),
            String.format("₹%.1f", cleanPriceNum * 1.05),
            String.format("₹%.1f", cleanPriceNum * 0.92),
            String.format("₹%.1f", cleanPriceNum * 0.90)
        )
        textMarket1Price.text = mPrices[0]
        textMarket2Price.text = mPrices[1]
        textMarket3Price.text = mPrices[2]
        textMarket4Price.text = mPrices[3]

        // Parse dynamic unit string (e.g. from ₹150.0/kg, extract "kg")
        val parsedUnit = if (cropPrice.contains("/")) cropPrice.substringAfter("/") else "kg"
        val unitStr = "per $parsedUnit"
        textMarket1Unit.text = unitStr
        textMarket2Unit.text = unitStr
        textMarket3Unit.text = unitStr
        textMarket4Unit.text = unitStr

        // Populate Insight
        textMarketInsight.text = insightMap[cropName] ?: "$cropName prices are observing a steady ${cropTrend.lowercase(java.util.Locale.ROOT)} trend in local mandis, influenced by transport availability and seasonal demands."

        val btnViewMandiMap = findViewById<View>(R.id.btnViewMandiMap)
        btnViewMandiMap?.setOnClickListener {
            val mapIntent = android.content.Intent(this, MarketLocationActivity::class.java).apply {
                putExtra("SELECTED_CROP", cropName)
            }
            startActivity(mapIntent)
        }
    }
}

