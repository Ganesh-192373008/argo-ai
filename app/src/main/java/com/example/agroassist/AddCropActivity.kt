package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddCropActivity : AppCompatActivity() {

    private lateinit var dbHelper: AgroDatabaseHelper
    private lateinit var cropsContainer: LinearLayout
    private lateinit var textCropsAvailable: TextView
    private lateinit var searchCrops: EditText

    private val cropList = listOf(
        CropInfo("Potato", "🥔", "Root Vegetable", "Easy", "Rabi", 90),
        CropInfo("Corn", "🌽", "Cereal", "Medium", "Kharif", 90),
        CropInfo("Carrot", "🥕", "Root Vegetable", "Easy", "Rabi", 75),
        CropInfo("Pepper", "🫑", "Vegetable", "Medium", "Spring", 80),
        CropInfo("Tomato", "🍅", "Vegetable", "Medium", "Spring", 75),
        CropInfo("Rice", "🌾", "Cereal", "Medium", "Kharif", 120),
        CropInfo("Wheat", "🌾", "Cereal", "Easy", "Rabi", 120),
        CropInfo("Cucumber", "🥒", "Vegetable", "Easy", "Zaid", 60),
        CropInfo("Watermelon", "🍉", "Fruit", "Medium", "Zaid", 85),
        CropInfo("Lettuce", "🥬", "Leafy Green", "Easy", "Winter", 45),
        CropInfo("Onion", "🧅", "Root Vegetable", "Hard", "Rabi", 110),
        CropInfo("Cabbage", "🥬", "Leafy Green", "Medium", "Winter", 85)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_crop)

        dbHelper = AgroDatabaseHelper(this)

        val backButton = findViewById<ImageView>(R.id.backButton)
        cropsContainer = findViewById(R.id.cropsContainer)
        textCropsAvailable = findViewById(R.id.textCropsAvailable)
        searchCrops = findViewById(R.id.searchCrops)

        backButton.setOnClickListener { finish() }

        // Load complete list initially
        renderCrops("")

        // Search text watcher
        searchCrops.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderCrops(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun renderCrops(query: String) {
        cropsContainer.removeAllViews()
        val filtered = if (query.isEmpty()) {
            cropList
        } else {
            cropList.filter { 
                it.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault())) ||
                it.category.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault())) ||
                it.difficulty.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault())) ||
                it.season.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
            }
        }

        val countText = "${filtered.size} crops available"
        textCropsAvailable.text = countText

        val inflater = LayoutInflater.from(this)
        for (crop in filtered) {
            val itemView = inflater.inflate(R.layout.item_add_crop, cropsContainer, false)
            
            val emojiText = itemView.findViewById<TextView>(R.id.cropEmoji)
            val nameText = itemView.findViewById<TextView>(R.id.cropName)
            val categoryText = itemView.findViewById<TextView>(R.id.cropCategory)
            val diffTag = itemView.findViewById<TextView>(R.id.tagDifficulty)
            val seasonTag = itemView.findViewById<TextView>(R.id.tagSeason)
            val btnAdd = itemView.findViewById<ImageView>(R.id.btnAddCrop)

            emojiText.text = crop.emoji
            nameText.text = crop.name
            categoryText.text = crop.category
            diffTag.text = crop.difficulty
            seasonTag.text = crop.season

            // Add Click Listener
            val cropCard = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.cropCard)
            val addCropClickListener = View.OnClickListener {
                // Calculate dates
                val format = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
                val today = Calendar.getInstance()
                val plantedDate = format.format(today.time)

                val harvestCal = Calendar.getInstance()
                harvestCal.add(Calendar.DAY_OF_YEAR, crop.growingDays)
                val harvestDate = format.format(harvestCal.time)

                // Save to DB
                dbHelper.addTrackedCrop(crop.name, plantedDate, harvestDate)

                // Launch success screen
                val intent = Intent(this, CropAddedSuccessActivity::class.java).apply {
                    putExtra("CROP_NAME", crop.name)
                    putExtra("CROP_EMOJI", crop.emoji)
                }
                startActivity(intent)
                finish()
            }
            btnAdd.setOnClickListener(addCropClickListener)
            cropCard.setOnClickListener(addCropClickListener)

            cropsContainer.addView(itemView)
        }
    }

    data class CropInfo(
        val name: String,
        val emoji: String,
        val category: String,
        val difficulty: String,
        val season: String,
        val growingDays: Int
    )
}
