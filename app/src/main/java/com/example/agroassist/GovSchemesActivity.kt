package com.example.agroassist

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class GovSchemesActivity : AppCompatActivity() {

    private lateinit var schemesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gov_schemes)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        schemesContainer = findViewById(R.id.schemesContainer)

        // Render immediate active schemes so the screen is NEVER blank
        renderDynamicSchemes(getFallbackSchemesArray())

        // Fetch Live Schemes from Firebase Cloud Server
        fetchLiveGovSchemes()

        // Schedule daily scheme push notifications
        scheduleDailySchemeNotification()
    }

    private fun fetchLiveGovSchemes() {
        BackendApiClient.getGovSchemes(this) { success, schemesArray ->
            if (success && schemesArray != null && schemesArray.length() > 0) {
                renderDynamicSchemes(schemesArray)
                Toast.makeText(this, "Live Schemes Synchronized with Firebase!", Toast.LENGTH_SHORT).show()

                // Add notification entry in local notification drawer
                val timeStr = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                val dbHelper = AgroDatabaseHelper(this)
                dbHelper.addNotification(
                    "🇮🇳 Government Schemes Synchronized",
                    "Loaded ${schemesArray.length()} live government agricultural schemes from Firebase Cloud Database.",
                    "scheme",
                    timeStr
                )
            }
        }
    }

    private fun renderDynamicSchemes(schemesArray: JSONArray) {
        // Clear all previous scheme cards (keeping title header at index 0)
        if (schemesContainer.childCount > 1) {
            schemesContainer.removeViews(1, schemesContainer.childCount - 1)
        }

        for (i in 0 until schemesArray.length()) {
            val schemeObj = schemesArray.optJSONObject(i) ?: continue
            val cardView = createSchemeCard(schemeObj)
            schemesContainer.addView(cardView)
        }
    }

    private fun createSchemeCard(scheme: JSONObject): CardView {
        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        val padding = dpToPx(16)
        val outerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        // Header Row: Title & Badge
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = scheme.optString("name", "Agricultural Scheme")
            setTextColor(Color.parseColor("#1F2937"))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val badgeText = scheme.optString("badge", "Active")
        val badgeTv = TextView(this).apply {
            text = badgeText
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))

            if (badgeText.contains("CLOSED", ignoreCase = true) || badgeText.contains("CLOSES", ignoreCase = true)) {
                setTextColor(Color.parseColor("#D32F2F"))
                setBackgroundColor(Color.parseColor("#FFEBEE"))
            } else {
                setTextColor(Color.parseColor("#2E7D32"))
                setBackgroundColor(Color.parseColor("#E8F5E9"))
            }
        }

        headerRow.addView(titleTv)
        headerRow.addView(badgeTv)
        outerLayout.addView(headerRow)

        // Description
        val descTv = TextView(this).apply {
            text = scheme.optString("description", "")
            setTextColor(Color.parseColor("#4B5563"))
            textSize = 14f
            maxLines = 3
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
            }
        }
        outerLayout.addView(descTv)

        // Divider
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                topMargin = dpToPx(12)
                bottomMargin = dpToPx(12)
            }
            setBackgroundColor(Color.parseColor("#EEEEEE"))
        }
        outerLayout.addView(divider)

        // Eligibility & Last Date Row
        val detailsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val eligLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val eligLabel = TextView(this).apply {
            text = "Eligibility"
            setTextColor(Color.parseColor("#6B7280"))
            textSize = 12f
        }
        val eligVal = TextView(this).apply {
            text = scheme.optString("eligibility", "All Farmers")
            setTextColor(Color.parseColor("#111827"))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        eligLayout.addView(eligLabel)
        eligLayout.addView(eligVal)

        val dateLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val dateLabel = TextView(this).apply {
            text = "Last Date"
            setTextColor(Color.parseColor("#6B7280"))
            textSize = 12f
        }
        val dateVal = TextView(this).apply {
            text = scheme.optString("lastDate", "Open Year-round")
            setTextColor(Color.parseColor("#111827"))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        dateLayout.addView(dateLabel)
        dateLayout.addView(dateVal)

        detailsRow.addView(eligLayout)
        detailsRow.addView(dateLayout)
        outerLayout.addView(detailsRow)

        val linkTv = TextView(this).apply {
            text = "View Details →"
            setTextColor(Color.parseColor("#22C55E"))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(12)
                gravity = android.view.Gravity.END
            }
        }
        outerLayout.addView(linkTv)

        card.addView(outerLayout)

        card.setOnClickListener {
            openSchemeDetails(
                scheme.optString("name"),
                scheme.optString("description"),
                scheme.optString("eligibility"),
                scheme.optString("benefits"),
                scheme.optString("url")
            )
        }

        return card
    }

    private fun getFallbackSchemesArray(): JSONArray {
        val array = JSONArray()
        array.put(JSONObject().apply {
            put("name", "PM Kisan Samman Nidhi")
            put("description", "Provides income support of ₹6,000 per year in three equal installments of ₹2,000 directly to farmer bank accounts.")
            put("eligibility", "All landholding farmer families")
            put("lastDate", "Open Year-round")
            put("badge", "ACTIVE INSTALLMENT RELEASED")
            put("url", "https://pmkisan.gov.in/")
        })
        array.put(JSONObject().apply {
            put("name", "Pradhan Mantri Fasal Bima Yojana (Crop Insurance)")
            put("description", "Provides comprehensive crop loss insurance protection from pre-sowing to post-harvest against natural disasters, pests, and droughts.")
            put("eligibility", "All farmers growing notified crops")
            put("lastDate", "31 December 2026 (Rabi Season)")
            put("badge", "RABI ENROLLMENT OPEN")
            put("url", "https://pmfby.gov.in/")
        })
        array.put(JSONObject().apply {
            put("name", "Paramparagat Krishi Vikas Yojana (PKVY)")
            put("description", "Promotes organic farming practices and eco-friendly cluster production with PGS organic certification.")
            put("eligibility", "Organic Farmers / Groups of 50+ acres")
            put("lastDate", "30 September 2026")
            put("badge", "50% SUBSIDY ACTIVE")
            put("url", "https://pgsindia-ncof.gov.in/")
        })
        array.put(JSONObject().apply {
            put("name", "Kisan Credit Card (KCC Scheme)")
            put("description", "Provides timely agricultural credit and low-interest loans to farmers for seeds, fertilizers, and machinery.")
            put("eligibility", "All farmers, tenant farmers, and SHGs")
            put("lastDate", "Open Year-round")
            put("badge", "4% INTEREST RATE")
            put("url", "https://www.myscheme.gov.in/schemes/kcc")
        })
        array.put(JSONObject().apply {
            put("name", "PM Krishi Sinchayee Yojana (Drip Irrigation)")
            put("description", "Extends water coverage to every farm ('Har Khet Ko Pani') and promotes micro-irrigation efficiency ('More Crop Per Drop').")
            put("eligibility", "All small and marginal farmers")
            put("lastDate", "31 October 2026")
            put("badge", "80% SUBSIDY OPEN")
            put("url", "https://pmksy.gov.in/")
        })
        array.put(JSONObject().apply {
            put("name", "Soil Health Card Scheme")
            put("description", "Provides personalized soil status reports containing 12 key nutrient levels and fertilizer recommendations.")
            put("eligibility", "All landholding farmers")
            put("lastDate", "Open Year-round")
            put("badge", "FREE TESTING")
            put("url", "https://soilhealth.dac.gov.in/")
        })
        return array
    }

    private fun openSchemeDetails(name: String, desc: String, eligibility: String, benefits: String, url: String) {
        val intent = Intent(this, SchemeDetailsActivity::class.java).apply {
            putExtra("SCHEME_NAME", name)
            putExtra("SCHEME_DESC", desc)
            putExtra("SCHEME_ELIGIBILITY", eligibility)
            putExtra("SCHEME_BENEFITS", benefits)
            putExtra("SCHEME_URL", url)
        }
        startActivity(intent)
    }

    private fun scheduleDailySchemeNotification() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, GovSchemesAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, 1002, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: Exception) {}
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
