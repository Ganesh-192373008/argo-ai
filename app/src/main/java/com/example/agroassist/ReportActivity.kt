package com.example.agroassist

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val btnRecommended = findViewById<Button>(R.id.btnRecommended)

        val textReportDiseaseTitle = findViewById<TextView>(R.id.textReportDiseaseTitle)
        val textReportDiseaseDescription = findViewById<TextView>(R.id.textReportDiseaseDescription)
        val textReportSymptoms = findViewById<TextView>(R.id.textReportSymptoms)
        val textReportCauses = findViewById<TextView>(R.id.textReportCauses)
        val textReportTreatment = findViewById<TextView>(R.id.textReportTreatment)

        // Get values passed from ResultsActivity
        val crop = intent.getStringExtra("crop") ?: "Tomato"
        val disease = intent.getStringExtra("disease") ?: "Late Blight"
        val scientific = intent.getStringExtra("scientific") ?: "Phytophthora infestans"
        val symptoms = intent.getStringExtra("symptoms") ?: "• Dark brown to black lesions on leaves\n• White fuzzy growth on leaf undersides\n• Brown, greasy-looking spots on fruits\n• Rapid wilting and death of affected parts\n• Characteristic musty odor"
        val causes = intent.getStringExtra("causes") ?: "• High humidity (above 90%)\n• Cool temperatures (15-20°C)\n• Prolonged leaf wetness\n• Poor air circulation\n• Infected plant debris"
        val treatment = intent.getStringExtra("treatment") ?: "1. Remove and destroy infected plant parts immediately\n\n2. Apply copper-based fungicides as preventive measure\n\n3. Improve air circulation between plants\n\n4. Avoid overhead watering, use drip irrigation\n\n5. Apply recommended fungicides every 7-10 days"

        // Bind data to views
        textReportDiseaseTitle.text = "$crop - $disease"
        textReportDiseaseDescription.text = "$disease is a condition affecting $crop plants. Scientific classification: $scientific."
        textReportSymptoms.text = symptoms
        textReportCauses.text = causes
        textReportTreatment.text = treatment

        backButton.setOnClickListener { finish() }

        btnRecommended.setOnClickListener {
            startActivity(android.content.Intent(this, RecommendedProductsActivity::class.java))
        }
    }
}
