package com.example.agroassist

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfileSetupActivity : AppCompatActivity() {

    private val selectedCrops = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val ageInput = findViewById<EditText>(R.id.ageInput)
        val nextButton = findViewById<Button>(R.id.nextButton)
        
        val nameContainer = findViewById<LinearLayout>(R.id.nameContainer)
        val ageContainer = findViewById<LinearLayout>(R.id.ageContainer)

        val cropViews = mapOf(
            "Rice" to findViewById<TextView>(R.id.cropRice),
            "Wheat" to findViewById<TextView>(R.id.cropWheat),
            "Tomato" to findViewById<TextView>(R.id.cropTomato),
            "Potato" to findViewById<TextView>(R.id.cropPotato),
            "Cotton" to findViewById<TextView>(R.id.cropCotton),
            "Sugarcane" to findViewById<TextView>(R.id.cropSugarcane)
        )

        backButton.setOnClickListener { finish() }

        nameInput.setOnFocusChangeListener { _, hasFocus ->
            nameContainer.setBackgroundResource(if (hasFocus) R.drawable.edit_text_bg_active else R.drawable.edit_text_bg)
        }
        ageInput.setOnFocusChangeListener { _, hasFocus ->
            ageContainer.setBackgroundResource(if (hasFocus) R.drawable.edit_text_bg_active else R.drawable.edit_text_bg)
        }

        fun validateForm() {
            val isNameValid = nameInput.text.isNotBlank()
            val isAgeValid = ageInput.text.isNotBlank()
            val hasSelectedCrop = selectedCrops.isNotEmpty()

            if (isNameValid && isAgeValid && hasSelectedCrop) {
                nextButton.setBackgroundColor(resources.getColor(R.color.primary_green, theme))
                nextButton.isEnabled = true
            } else {
                nextButton.setBackgroundColor(android.graphics.Color.parseColor("#D6D9E0"))
                nextButton.isEnabled = false
            }
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateForm()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        nameInput.addTextChangedListener(textWatcher)
        ageInput.addTextChangedListener(textWatcher)

        cropViews.forEach { (cropName, textView) ->
            textView.setOnClickListener {
                if (selectedCrops.contains(cropName)) {
                    selectedCrops.remove(cropName)
                    textView.setBackgroundResource(R.drawable.crop_pill_bg)
                    textView.setTextColor(resources.getColor(R.color.text_primary, theme))
                } else {
                    selectedCrops.add(cropName)
                    textView.setBackgroundResource(R.drawable.crop_pill_bg_selected)
                    textView.setTextColor(resources.getColor(R.color.primary_green, theme))
                }
                validateForm()
            }
        }

        nextButton.isEnabled = false

        nextButton.setOnClickListener {
            val dbHelper = AgroDatabaseHelper(this)
            val name = nameInput.text.toString().trim()
            val age = ageInput.text.toString().trim()
            val cropsList = selectedCrops.joinToString(", ")
            dbHelper.saveProfile(name, age, cropsList)
            
            Toast.makeText(this, "Profile Saved! Crops: $cropsList", Toast.LENGTH_LONG).show()
            
            // Navigate to Location Setup
            val intent = android.content.Intent(this, LocationSetupActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
