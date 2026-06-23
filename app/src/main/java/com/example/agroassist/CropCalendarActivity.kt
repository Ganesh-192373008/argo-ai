package com.example.agroassist

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CropCalendarActivity : AppCompatActivity() {

    private lateinit var dbHelper: AgroDatabaseHelper
    private lateinit var emptyState: View
    private lateinit var cropList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_calendar)

        dbHelper = AgroDatabaseHelper(this)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnAddCropHeader = findViewById<ImageView>(R.id.btnAddCropHeader)
        val cardPlanSeason = findViewById<androidx.cardview.widget.CardView>(R.id.cardPlanSeason)
        emptyState = findViewById(R.id.emptyState)
        cropList = findViewById(R.id.cropList)
        val btnAddFirstCrop = findViewById<Button>(R.id.btnAddFirstCrop)

        backButton.setOnClickListener { finish() }

        val addCropListener = View.OnClickListener {
            startActivity(Intent(this, AddCropActivity::class.java))
        }
        btnAddCropHeader.setOnClickListener(addCropListener)
        btnAddFirstCrop.setOnClickListener(addCropListener)

        cardPlanSeason.setOnClickListener {
            startActivity(Intent(this, SeasonPlanningActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadTrackedCrops()
    }

    private fun loadTrackedCrops() {
        val crops = dbHelper.getTrackedCrops()
        if (crops.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            cropList.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            cropList.visibility = View.VISIBLE
            cropList.removeAllViews()

            val inflater = LayoutInflater.from(this)

            for (crop in crops) {
                val idStr = crop["id"] ?: continue
                val id = idStr.toIntOrNull() ?: continue
                val name = crop["crop_name"] ?: ""
                val plantedDate = crop["planted_date"] ?: ""
                val harvestDate = crop["expected_harvest_date"] ?: ""

                val itemView = inflater.inflate(R.layout.item_tracked_crop, cropList, false)
                
                val emojiText = itemView.findViewById<TextView>(R.id.cropEmoji)
                val nameText = itemView.findViewById<TextView>(R.id.cropName)
                val categoryText = itemView.findViewById<TextView>(R.id.cropCategory)
                val btnDeleteCrop = itemView.findViewById<ImageView>(R.id.btnDeleteCrop)
                val plantedText = itemView.findViewById<TextView>(R.id.plantedDateText)
                val harvestText = itemView.findViewById<TextView>(R.id.harvestDateText)

                nameText.text = name
                plantedText.text = plantedDate
                harvestText.text = harvestDate

                // Set metadata based on crop name
                val metadata = getCropMetadata(name)
                emojiText.text = metadata.emoji
                categoryText.text = metadata.category

                // Highlight Month Bar
                highlightMonthBar(itemView, plantedDate, isHarvest = false)
                highlightMonthBar(itemView, harvestDate, isHarvest = true)

                btnDeleteCrop.setOnClickListener {
                    dbHelper.deleteTrackedCrop(id)
                    Toast.makeText(this, "$name removed from calendar", Toast.LENGTH_SHORT).show()
                    loadTrackedCrops()
                }

                cropList.addView(itemView)
            }
        }
    }

    private fun highlightMonthBar(itemView: View, dateStr: String, isHarvest: Boolean) {
        try {
            val format = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
            val date = format.parse(dateStr) ?: return
            val cal = Calendar.getInstance()
            cal.time = date
            val month = cal.get(Calendar.MONTH) // 0 - 11

            val prefix = if (isHarvest) "h" else "p"
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val targetIdName = prefix + monthNames[month]
            
            val resId = resources.getIdentifier(targetIdName, "id", packageName)
            if (resId != 0) {
                val monthView = itemView.findViewById<TextView>(resId)
                if (monthView != null) {
                    monthView.setTextColor(Color.WHITE)
                    monthView.setBackgroundResource(R.drawable.circle_icon_bg_green)
                    val tintColor = if (isHarvest) "#F57C00" else "#2E7D32"
                    monthView.backgroundTintList = ColorStateList.valueOf(Color.parseColor(tintColor))
                    monthView.setTypeface(null, Typeface.BOLD)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCropMetadata(name: String): CropMetadata {
        return when (name.lowercase(Locale.getDefault())) {
            "potato" -> CropMetadata("🥔", "Root Vegetable")
            "corn" -> CropMetadata("🌽", "Cereal")
            "carrot" -> CropMetadata("🥕", "Root Vegetable")
            "pepper" -> CropMetadata("🫑", "Vegetable")
            "tomato" -> CropMetadata("🍅", "Vegetable")
            "rice" -> CropMetadata("🌾", "Cereal")
            "wheat" -> CropMetadata("🌾", "Cereal")
            "cucumber" -> CropMetadata("🥒", "Vegetable")
            "watermelon" -> CropMetadata("🍉", "Fruit")
            "lettuce" -> CropMetadata("🥬", "Leafy Green")
            "onion" -> CropMetadata("🧅", "Root Vegetable")
            "cabbage" -> CropMetadata("🥬", "Leafy Green")
            else -> CropMetadata("🌱", "Crop")
        }
    }

    data class CropMetadata(val emoji: String, val category: String)
}
