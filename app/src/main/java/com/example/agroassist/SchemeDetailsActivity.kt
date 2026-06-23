package com.example.agroassist

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SchemeDetailsActivity : AppCompatActivity() {

    private lateinit var tvCurrentStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheme_details)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val tvSchemeName = findViewById<TextView>(R.id.tvSchemeName)
        val tvSchemeDesc = findViewById<TextView>(R.id.tvSchemeDesc)
        val tvSchemeEligibility = findViewById<TextView>(R.id.tvSchemeEligibility)
        val tvSchemeBenefits = findViewById<TextView>(R.id.tvSchemeBenefits)
        val btnApplyNow = findViewById<Button>(R.id.btnApplyNow)
        
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus)
        val btnStatusApplied = findViewById<Button>(R.id.btnStatusApplied)
        val btnStatusReview = findViewById<Button>(R.id.btnStatusReview)
        val btnStatusApproved = findViewById<Button>(R.id.btnStatusApproved)
        val btnStatusRejected = findViewById<Button>(R.id.btnStatusRejected)

        // Get data from intent
        val schemeName = intent.getStringExtra("SCHEME_NAME") ?: "Government Scheme"
        val schemeDesc = intent.getStringExtra("SCHEME_DESC") ?: "Details not available."
        val schemeElig = intent.getStringExtra("SCHEME_ELIGIBILITY") ?: "N/A"
        val schemeBen = intent.getStringExtra("SCHEME_BENEFITS") ?: "N/A"
        val schemeUrl = intent.getStringExtra("SCHEME_URL") ?: "https://www.india.gov.in"

        tvSchemeName.text = schemeName
        tvSchemeDesc.text = schemeDesc
        tvSchemeEligibility.text = schemeElig
        tvSchemeBenefits.text = schemeBen

        btnApplyNow.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(schemeUrl))
            startActivity(browserIntent)
        }

        // Status change listeners
        btnStatusApplied.setOnClickListener { updateStatus("Applied", "#4CAF50", "#E8F5E9") } // Green
        btnStatusReview.setOnClickListener { updateStatus("Under Review", "#2196F3", "#E3F2FD") } // Blue
        btnStatusApproved.setOnClickListener { updateStatus("Approved", "#1B5E20", "#C8E6C9") } // Dark Green
        btnStatusRejected.setOnClickListener { updateStatus("Rejected", "#D32F2F", "#FFEBEE") } // Red
    }

    private fun updateStatus(text: String, textColorCode: String, bgColorCode: String) {
        tvCurrentStatus.text = text
        tvCurrentStatus.setTextColor(Color.parseColor(textColorCode))
        tvCurrentStatus.setBackgroundColor(Color.parseColor(bgColorCode))
    }
}
