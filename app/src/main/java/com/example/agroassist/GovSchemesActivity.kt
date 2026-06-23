package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class GovSchemesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gov_schemes)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val scheme1 = findViewById<CardView>(R.id.scheme1)
        scheme1.setOnClickListener {
            val intent = Intent(this, SchemeDetailsActivity::class.java)
            // Passing mock data via intent extras for the detail screen
            intent.putExtra("SCHEME_NAME", "PM Kisan Samman Nidhi")
            intent.putExtra("SCHEME_DESC", "Provides income support of ₹6,000 per year in three equal installments to all landholding farmer families.")
            intent.putExtra("SCHEME_ELIGIBILITY", "All landholding farmers")
            intent.putExtra("SCHEME_BENEFITS", "Financial support of ₹6000/year to support farming activities and household needs.")
            intent.putExtra("SCHEME_URL", "https://pmkisan.gov.in/")
            startActivity(intent)
        }

        val scheme2 = findViewById<CardView>(R.id.scheme2)
        scheme2.setOnClickListener {
            val intent = Intent(this, SchemeDetailsActivity::class.java)
            intent.putExtra("SCHEME_NAME", "Pradhan Mantri Fasal Bima Yojana (Crop Insurance)")
            intent.putExtra("SCHEME_DESC", "Provides comprehensive crop insurance cover against non-preventable natural risks from pre-sowing to post-harvest.")
            intent.putExtra("SCHEME_ELIGIBILITY", "All farmers (including sharecroppers and tenant farmers) growing notified crops in notified areas.")
            intent.putExtra("SCHEME_BENEFITS", "Insurance coverage against crop loss due to droughts, floods, pests, and storms, with low premium rates.")
            intent.putExtra("SCHEME_URL", "https://pmfby.gov.in/")
            startActivity(intent)
        }

        val scheme3 = findViewById<CardView>(R.id.scheme3)
        scheme3.setOnClickListener {
            val intent = Intent(this, SchemeDetailsActivity::class.java)
            intent.putExtra("SCHEME_NAME", "Paramparagat Krishi Vikas Yojana (PKVY)")
            intent.putExtra("SCHEME_DESC", "Promotes organic farming through a cluster approach and Participatory Guarantee System of certification.")
            intent.putExtra("SCHEME_ELIGIBILITY", "Organic Farmers / Groups of farmers forming a cluster of 50 or more acres.")
            intent.putExtra("SCHEME_BENEFITS", "Financial assistance of ₹50,000 per hectare for 3 years, cluster building support, and market link assistance.")
            intent.putExtra("SCHEME_URL", "https://pgsindia-ncof.gov.in/")
            startActivity(intent)
        }
    }
}
