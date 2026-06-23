package com.example.agroassist

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CustomPlanGeneratorActivity : AppCompatActivity() {

    private lateinit var dbHelper: AgroDatabaseHelper
    private lateinit var textProgress: TextView
    private lateinit var progressWizard: ProgressBar
    private lateinit var btnAction: Button

    private lateinit var rgFarmSize: RadioGroup
    private lateinit var rgSoilType: RadioGroup
    private lateinit var rgWaterSource: RadioGroup
    private lateinit var rgLastSeasonCrops: RadioGroup
    private lateinit var rgTargetMarket: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_plan_generator)

        dbHelper = AgroDatabaseHelper(this)

        val backButton = findViewById<ImageView>(R.id.backButton)
        textProgress = findViewById(R.id.textProgress)
        progressWizard = findViewById(R.id.progressWizard)
        btnAction = findViewById(R.id.btnAction)

        rgFarmSize = findViewById(R.id.rgFarmSize)
        rgSoilType = findViewById(R.id.rgSoilType)
        rgWaterSource = findViewById(R.id.rgWaterSource)
        rgLastSeasonCrops = findViewById(R.id.rgLastSeasonCrops)
        rgTargetMarket = findViewById(R.id.rgTargetMarket)

        backButton.setOnClickListener { finish() }

        val checkChangeListener = RadioGroup.OnCheckedChangeListener { _, _ ->
            updateProgress()
        }

        rgFarmSize.setOnCheckedChangeListener(checkChangeListener)
        rgSoilType.setOnCheckedChangeListener(checkChangeListener)
        rgWaterSource.setOnCheckedChangeListener(checkChangeListener)
        rgLastSeasonCrops.setOnCheckedChangeListener(checkChangeListener)
        rgTargetMarket.setOnCheckedChangeListener(checkChangeListener)

        // Initialize state
        updateProgress()

        btnAction.setOnClickListener {
            generateFarmingPlan()
        }
    }

    private fun updateProgress() {
        var count = 0
        if (rgFarmSize.checkedRadioButtonId != -1) count++
        if (rgSoilType.checkedRadioButtonId != -1) count++
        if (rgWaterSource.checkedRadioButtonId != -1) count++
        if (rgLastSeasonCrops.checkedRadioButtonId != -1) count++
        if (rgTargetMarket.checkedRadioButtonId != -1) count++

        progressWizard.progress = count
        textProgress.text = "Progress: $count/5 completed"

        if (count < 5) {
            val remaining = 5 - count
            val suffix = if (remaining == 1) "Question" else "Questions"
            btnAction.text = "Answer $remaining More $suffix"
            btnAction.isEnabled = false
            btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B0BEC5"))
        } else {
            btnAction.text = "Generate Custom Plan"
            btnAction.isEnabled = true
            btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E7D32"))
        }
    }

    private fun generateFarmingPlan() {
        val cropsToSeed = mutableListOf<String>()
        val soilId = rgSoilType.checkedRadioButtonId

        // Personalize crops based on soil type selection
        if (soilId == R.id.rbSoilClay) {
            cropsToSeed.add("Rice")
            cropsToSeed.add("Cabbage")
        } else if (soilId == R.id.rbSoilSandy) {
            cropsToSeed.add("Watermelon")
            cropsToSeed.add("Carrot")
        } else {
            cropsToSeed.add("Potato")
            cropsToSeed.add("Tomato")
            cropsToSeed.add("Corn")
        }

        // Save selected crops to the Database Calendar
        val format = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
        for (cropName in cropsToSeed) {
            val today = Calendar.getInstance()
            val plantedDate = format.format(today.time)

            val growingDays = when (cropName.lowercase(Locale.getDefault())) {
                "rice" -> 120
                "cabbage" -> 85
                "watermelon" -> 85
                "carrot" -> 75
                "potato" -> 90
                "tomato" -> 75
                "corn" -> 90
                else -> 90
            }

            val harvestCal = Calendar.getInstance()
            harvestCal.add(Calendar.DAY_OF_YEAR, growingDays)
            val harvestDate = format.format(harvestCal.time)

            dbHelper.addTrackedCrop(cropName, plantedDate, harvestDate)
        }

        Toast.makeText(this, "Plan generated! Seeded ${cropsToSeed.size} crops to your calendar.", Toast.LENGTH_LONG).show()

        // Get selections
        val farmSizeText = getSelectedText(rgFarmSize)
        val soilTypeText = getSelectedText(rgSoilType)
        val waterSourceText = getSelectedText(rgWaterSource)
        val lastSeasonCropsText = getSelectedText(rgLastSeasonCrops)
        val targetMarketText = getSelectedText(rgTargetMarket)

        // Launch FarmingPlanResultActivity and finish
        val intent = Intent(this, FarmingPlanResultActivity::class.java).apply {
            putExtra("farm_size", farmSizeText)
            putExtra("soil_type", soilTypeText)
            putExtra("water_source", waterSourceText)
            putExtra("last_season_crops", lastSeasonCropsText)
            putExtra("target_market", targetMarketText)
        }
        startActivity(intent)
        finish()
    }

    private fun getSelectedText(radioGroup: RadioGroup): String {
        val id = radioGroup.checkedRadioButtonId
        if (id == -1) return ""
        val radioButton = radioGroup.findViewById<android.widget.RadioButton>(id)
        return radioButton?.text?.toString() ?: ""
    }
}
