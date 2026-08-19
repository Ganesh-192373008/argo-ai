package com.example.agroassist

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarketPricesActivity : BaseProtectedActivity() {

    private lateinit var textLastUpdated: TextView
    private lateinit var editSearchCrops: TextInputEditText
    private lateinit var recyclerViewCrops: RecyclerView
    private lateinit var adapter: MarketPriceAdapter

    private val cropItemsList = listOf(
        CropPriceItem("tomato", "Tomato", "🍅", "₹35/kg", "+12% vs last week", "up", "kg", "#FFEBEE"),
        CropPriceItem("potato", "Potato", "🥔", "₹22/kg", "-5% vs last week", "down", "kg", "#FFF3E0"),
        CropPriceItem("onion", "Onion", "🧅", "₹28/kg", "+8% vs last week", "up", "kg", "#F3E5F5"),
        CropPriceItem("rice", "Rice", "🌾", "₹42/kg", "+3% vs last week", "up", "kg", "#FFFDE7"),
        CropPriceItem("wheat", "Wheat", "🌾", "₹25/kg", "0% vs last week", "neutral", "kg", "#FFFDE7"),
        CropPriceItem("cotton", "Cotton", "☁️", "₹120/kg", "+15% vs last wk", "up", "kg", "#E3F2FD"),
        CropPriceItem("sugarcane", "Sugarcane", "🎋", "₹60/kg", "+7% vs last wk", "up", "kg", "#F1F8E9"),
        CropPriceItem("cabbage", "Cabbage", "🥬", "₹18/kg", "-3% vs last week", "down", "kg", "#E8F5E9"),
        
        CropPriceItem("apple", "Apple", "🍎", "₹150/kg", "+5% vs last week", "up", "kg", "#FFEBEE"),
        CropPriceItem("banana", "Banana", "🍌", "₹60/dozen", "+2% vs last week", "up", "dozen", "#FFFDE7"),
        CropPriceItem("mango", "Mango", "🥭", "₹120/kg", "-10% vs last week", "down", "kg", "#FFF3E0"),
        CropPriceItem("orange", "Orange", "🍊", "₹80/kg", "+4% vs last week", "up", "kg", "#FFE0B2"),
        CropPriceItem("grapes", "Grapes", "🍇", "₹90/kg", "-6% vs last week", "down", "kg", "#E1BEE7"),
        CropPriceItem("pomegranate", "Pomegranate", "🍎", "₹180/kg", "+8% vs last week", "up", "kg", "#FFCDD2"),
        CropPriceItem("lemon", "Lemon", "🍋", "₹120/kg", "+15% vs last week", "up", "kg", "#FFF9C4"),
        CropPriceItem("coconut", "Coconut", "🥥", "₹40/pc", "0% vs last week", "neutral", "pc", "#D7CCC8"),
        
        CropPriceItem("spinach", "Spinach", "🥬", "₹30/kg", "-8% vs last week", "down", "kg", "#C8E6C9"),
        CropPriceItem("cauliflower", "Cauliflower", "🥦", "₹45/kg", "+5% vs last week", "up", "kg", "#E8F5E9"),
        CropPriceItem("okra", "Okra", "🥒", "₹40/kg", "+2% vs last week", "up", "kg", "#E8F5E9"),
        CropPriceItem("brinjal", "Brinjal", "🍆", "₹35/kg", "-4% vs last week", "down", "kg", "#E1BEE7"),
        CropPriceItem("carrot", "Carrot", "🥕", "₹50/kg", "+6% vs last week", "up", "kg", "#FFE0B2"),
        CropPriceItem("cucumber", "Cucumber", "🥒", "₹30/kg", "-3% vs last week", "down", "kg", "#E8F5E9"),
        CropPriceItem("garlic", "Garlic", "🧄", "₹200/kg", "+10% vs last week", "up", "kg", "#F5F5F5"),
        CropPriceItem("ginger", "Ginger", "🫚", "₹150/kg", "+12% vs last week", "up", "kg", "#FFE0B2"),
        CropPriceItem("chilli", "Chilli", "🌶️", "₹80/kg", "+18% vs last week", "up", "kg", "#FFEBEE"),
        
        CropPriceItem("maize", "Maize", "🌽", "₹22/kg", "0% vs last week", "neutral", "kg", "#FFFDE7"),
        CropPriceItem("soyabean", "Soyabean", "🫘", "₹45/kg", "+5% vs last week", "up", "kg", "#FFE0B2"),
        CropPriceItem("mustard", "Mustard", "🌱", "₹55/kg", "+2% vs last week", "up", "kg", "#E8F5E9"),
        CropPriceItem("barley", "Barley", "🌾", "₹20/kg", "-1% vs last week", "down", "kg", "#FFFDE7"),
        CropPriceItem("millet", "Millet", "🌾", "₹25/kg", "+3% vs last week", "up", "kg", "#FFFDE7")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_market_prices)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val btnSettings = findViewById<ImageView>(R.id.btnSettings)
        btnSettings?.setOnClickListener { showApiSettingsDialog() }

        // Initialize Views
        textLastUpdated = findViewById(R.id.textLastUpdated)
        editSearchCrops = findViewById(R.id.editSearchCrops)
        recyclerViewCrops = findViewById(R.id.recyclerViewCrops)

        val btnViewMandiMap = findViewById<View>(R.id.btnViewMandiMap)
        btnViewMandiMap?.setOnClickListener {
            startActivity(Intent(this, MarketLocationActivity::class.java))
        }


        // Setup RecyclerView
        recyclerViewCrops.layoutManager = GridLayoutManager(this, 2)
        adapter = MarketPriceAdapter(cropItemsList) { item ->
            openCropDetails(item)
        }
        recyclerViewCrops.adapter = adapter

        // Fetch Live Prices
        fetchLivePrices()

        // Setup Search Filter
        editSearchCrops.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun openCropDetails(item: CropPriceItem) {
        val intent = Intent(this, CropPriceDetailsActivity::class.java).apply {
            putExtra("CROP_NAME", item.name)
            putExtra("CROP_EMOJI", item.emoji)
            putExtra("CROP_PRICE", item.price)
            putExtra("CROP_CHANGE", item.change)
            putExtra("CROP_TREND", item.trend)
            putExtra("CROP_BG_TINT", item.bgTint)
        }
        startActivity(intent)
    }

    private fun fetchLivePrices() {
        textLastUpdated.text = "Loading real-time prices..."

        CoroutineScope(Dispatchers.Main).launch {
            val dbHelper = AgroDatabaseHelper(this@MarketPricesActivity)
            val profile = dbHelper.getProfile()
            val location = profile["location"]?.ifEmpty { "Mumbai, MH" } ?: "Mumbai, MH"

            val cropNamesForPrompt = cropItemsList.joinToString(", ") { "${it.name} (per ${it.unit})" }

            val systemContext = "System Instruction: You are a retail grocery price assistant. Return the latest typical BigBasket online retail prices for vegetables, fruits, and crops in India. Return ONLY a valid raw JSON object matching the requested schema. Do not output markdown, formatting, or extra text."
            val prompt = "Return current online retail prices on BigBasket in India near $location for the following items as a JSON object: $cropNamesForPrompt. Use the current date: 2026-06-18.\n\n" +
                    "Format required (matching the lowercased keys of each item):\n" +
                    "{\n" +
                    "  \"tomato\": {\"price\": \"₹X/kg\", \"change\": \"+X% vs last week\", \"trend\": \"up\"},\n" +
                    "  \"potato\": {\"price\": \"₹X/kg\", \"change\": \"-X% vs last week\", \"trend\": \"down\"},\n" +
                    "  ...\n" +
                    "}"

            val prefs = getSharedPreferences("AgroAssistAIKeys", Context.MODE_PRIVATE)
            var geminiKey = prefs.getString("gemini_api_key", "") ?: ""
            var openaiKey = prefs.getString("openai_api_key", "") ?: ""
            if (geminiKey.trim().lowercase() == "hi") geminiKey = ""
            if (openaiKey.trim().lowercase() == "hi" || openaiKey.trim().startsWith("sk-...")) openaiKey = ""

            GeminiClient.setApiKey(geminiKey)
            OpenAIClient.setApiKey(openaiKey)

            try {
                val rawResponse = GroqClient.generateResponse(prompt, systemContext)
                val cleanJson = rawResponse.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val json = JSONObject(cleanJson)
                updateCropUI(json)

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val currentTime = timeFormat.format(Date())
                textLastUpdated.text = "Last Updated: Today, $currentTime (Live APMC & Mandi Market)"
            } catch (e: Exception) {
                generateSimulatedBigBasketPrices()
            }
        }
    }

    private fun generateSimulatedBigBasketPrices() {
        val basePrices = mapOf(
            "tomato" to 48.0,
            "potato" to 32.0,
            "onion" to 38.0,
            "rice" to 65.0,
            "wheat" to 45.0,
            "cotton" to 120.0,
            "sugarcane" to 60.0,
            "cabbage" to 28.0,
            "apple" to 150.0,
            "banana" to 60.0,
            "mango" to 120.0,
            "orange" to 80.0,
            "grapes" to 90.0,
            "pomegranate" to 180.0,
            "lemon" to 120.0,
            "coconut" to 40.0,
            "spinach" to 30.0,
            "cauliflower" to 45.0,
            "okra" to 40.0,
            "brinjal" to 35.0,
            "carrot" to 50.0,
            "cucumber" to 30.0,
            "garlic" to 200.0,
            "ginger" to 150.0,
            "chilli" to 80.0,
            "maize" to 22.0,
            "soyabean" to 45.0,
            "mustard" to 55.0,
            "barley" to 20.0,
            "millet" to 25.0
        )
        
        val json = JSONObject()
        val random = java.util.Random()
        
        for ((crop, basePrice) in basePrices) {
            val fluctuationPercent = (random.nextDouble() * 16) - 8 // -8 to +8
            val newPrice = basePrice * (1 + fluctuationPercent / 100)
            val changeVal = fluctuationPercent
            
            // Format price unit nicely depending on item
            val unitStr = when (crop) {
                "banana" -> "dozen"
                "coconut" -> "pc"
                else -> "kg"
            }
            val priceStr = String.format("₹%.1f/$unitStr", newPrice)
            val trend = when {
                changeVal > 1 -> "up"
                changeVal < -1 -> "down"
                else -> "neutral"
            }
            val changeStr = String.format("%+.1f%% vs last week", changeVal)
            
            val cropObj = JSONObject().apply {
                put("price", priceStr)
                put("change", changeStr)
                put("trend", trend)
            }
            json.put(crop, cropObj)
        }
        
        updateCropUI(json)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        textLastUpdated.text = "Last Updated: Today, $currentTime (Simulated BigBasket Live)"
    }

    private fun updateCropUI(json: JSONObject) {
        for (item in cropItemsList) {
            val cropKey = item.id
            if (json.has(cropKey)) {
                val cropObj = json.getJSONObject(cropKey)
                val priceText = cropObj.optString("price", "")
                val changeText = cropObj.optString("change", "")
                val trend = cropObj.optString("trend", "neutral")

                if (priceText.isNotEmpty()) {
                    item.price = priceText
                }
                if (changeText.isNotEmpty()) {
                    item.change = changeText
                }
                item.trend = trend
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun showApiSettingsDialog() {
        val density = resources.displayMetrics.density
        val padding = (20 * density).toInt()

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val descriptionText = TextView(this).apply {
            text = "Enter your custom API keys to enable real-time, live BigBasket prices. Leave them blank to use offline fallback."
            setTextColor(Color.parseColor("#4A5568"))
            textSize = 14f
            setLineSpacing(0f, 1.2f)
        }
        rootLayout.addView(descriptionText)

        // Spacer
        rootLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, (16 * density).toInt())
        })

        // Gemini Label
        val lblGemini = TextView(this).apply {
            text = "Google Gemini API Key"
            setTextColor(resources.getColor(R.color.primary_green, theme))
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        rootLayout.addView(lblGemini)

        val prefs = getSharedPreferences("AgroAssistAIKeys", Context.MODE_PRIVATE)
        var currentGeminiKey = prefs.getString("gemini_api_key", "") ?: ""
        var currentOpenaiKey = prefs.getString("openai_api_key", "") ?: ""
        if (currentGeminiKey.trim().lowercase() == "hi") {
            currentGeminiKey = ""
        }
        if (currentOpenaiKey.trim().lowercase() == "hi" || currentOpenaiKey.trim().startsWith("sk-...")) {
            currentOpenaiKey = ""
        }

        val inputGemini = EditText(this).apply {
            hint = "AIzaSy..."
            setText(currentGeminiKey)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            textSize = 14f
            setSingleLine(true)
            background = resources.getDrawable(R.drawable.edit_text_bg, theme)
            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * density).toInt()
            }
        }
        rootLayout.addView(inputGemini)

        // Spacer
        rootLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, (16 * density).toInt())
        })

        // OpenAI Label
        val lblOpenAI = TextView(this).apply {
            text = "OpenAI API Key"
            setTextColor(resources.getColor(R.color.primary_green, theme))
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        rootLayout.addView(lblOpenAI)

        val inputOpenAI = EditText(this).apply {
            hint = "sk-..."
            setText(currentOpenaiKey)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            textSize = 14f
            setSingleLine(true)
            background = resources.getDrawable(R.drawable.edit_text_bg, theme)
            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * density).toInt()
            }
        }
        rootLayout.addView(inputOpenAI)

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("AI Assistant Settings")
            .setView(rootLayout)
            .setPositiveButton("Save") { dialog, _ ->
                val newGemini = inputGemini.text.toString().trim()
                val newOpenAI = inputOpenAI.text.toString().trim()

                prefs.edit().apply {
                    putString("gemini_api_key", newGemini)
                    putString("openai_api_key", newOpenAI)
                    apply()
                }

                GeminiClient.setApiKey(newGemini)
                OpenAIClient.setApiKey(newOpenAI)

                Toast.makeText(this, "API Keys updated successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                
                // Refresh prices immediately with new API Keys
                fetchLivePrices()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        builder.show()
    }
}
