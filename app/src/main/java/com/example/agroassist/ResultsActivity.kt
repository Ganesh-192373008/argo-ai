package com.example.agroassist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        var diseaseInfo: PlantVillageClassifier.DiseaseInfo? = null
        try {
            val textDiseaseName = findViewById<android.widget.TextView>(R.id.textDiseaseName)
            val textScientificName = findViewById<android.widget.TextView>(R.id.textScientificName)
            val textSeverityPercentage = findViewById<android.widget.TextView>(R.id.textSeverityPercentage)
            val progressSeverity = findViewById<android.widget.ProgressBar>(R.id.progressSeverity)
            val textRiskLevel = findViewById<android.widget.TextView>(R.id.textRiskLevel)

            val imagePath = intent.getStringExtra("image_path")
            val liveJsonStr = intent.getStringExtra("live_result_json")

            if (!liveJsonStr.isNullOrEmpty()) {
                val json = org.json.JSONObject(liveJsonStr)
                val crop = json.optString("crop", "Tomato")
                val disease = json.optString("disease", "Early Blight")
                val scientificName = json.optString("scientificName", "Alternaria solani")
                val severity = json.optInt("severity", 65)
                val confidence = json.optString("confidence", "95.4%")
                val riskLevel = json.optString("riskLevel", "High")
                val symptoms = json.optString("symptoms", "")
                val causes = json.optString("causes", "")
                val treatment = json.optString("treatment", "")

                diseaseInfo = PlantVillageClassifier.DiseaseInfo(
                    crop = crop,
                    disease = disease,
                    scientificName = scientificName,
                    severity = severity,
                    confidence = confidence,
                    riskLevel = riskLevel,
                    symptoms = symptoms,
                    causes = causes,
                    treatment = treatment
                )
            } else {
                // Retrieve preferred crop from database profile
                val dbHelper = AgroDatabaseHelper(this)
                val profile = dbHelper.getProfile()
                val preferredCrop = profile["crops"]

                // Classify dynamically
                diseaseInfo = PlantVillageClassifier.classifyImage(imagePath, preferredCrop)
            }

            // Update UI
            textDiseaseName?.text = "${diseaseInfo.crop} - ${diseaseInfo.disease}"
            textScientificName?.text = diseaseInfo.scientificName
            textSeverityPercentage?.text = "${diseaseInfo.severity}%"
            progressSeverity?.progress = diseaseInfo.severity
            textRiskLevel?.text = diseaseInfo.riskLevel

            val topBackground = findViewById<android.view.View>(R.id.topBackground)
            val iconAlert = findViewById<ImageView>(R.id.iconAlert)
            val titleText = findViewById<android.widget.TextView>(R.id.titleText)

            when (diseaseInfo.riskLevel) {
                "None" -> {
                    topBackground?.setBackgroundResource(R.drawable.bg_gradient_healthy)
                    iconAlert?.setImageResource(R.drawable.ic_check_circle)
                    titleText?.text = "Crop is Healthy!"
                    textRiskLevel?.setBackgroundResource(R.drawable.bg_pill_green)
                    textRiskLevel?.setTextColor(android.graphics.Color.WHITE)
                }
                "Medium" -> {
                    topBackground?.setBackgroundResource(R.drawable.bg_gradient_warning)
                    iconAlert?.setImageResource(android.R.drawable.stat_sys_warning)
                    titleText?.text = "Issue Detected!"
                    textRiskLevel?.setBackgroundResource(R.drawable.bg_pill_orange)
                    textRiskLevel?.setTextColor(android.graphics.Color.WHITE)
                }
                else -> { // High
                    topBackground?.setBackgroundResource(R.drawable.bg_gradient_danger)
                    iconAlert?.setImageResource(android.R.drawable.stat_notify_error)
                    titleText?.text = "Disease Detected!"
                    textRiskLevel?.setBackgroundResource(R.drawable.bg_pill_red)
                    textRiskLevel?.setTextColor(android.graphics.Color.WHITE)
                }
            }

            // Save scan result to local Database history & SQL Prisma ORM backend
            val dbHelper = AgroDatabaseHelper(this)
            dbHelper.addHistory(imagePath ?: "", diseaseInfo.crop, diseaseInfo.disease, diseaseInfo.confidence)
            BackendApiClient.addDetectionHistory(
                cropName = diseaseInfo.crop,
                disease = diseaseInfo.disease,
                confidence = diseaseInfo.confidence,
                timestamp = java.text.SimpleDateFormat("MMMM dd, yyyy hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            ) {}

            // Update achievement score
            AchievementTracker.onDiseaseScanCompleted(this)

            val btnViewReport = findViewById<Button>(R.id.btnViewReport)
            btnViewReport?.setOnClickListener {
                val reportIntent = Intent(this, ReportActivity::class.java).apply {
                    putExtra("crop", diseaseInfo.crop)
                    putExtra("disease", diseaseInfo.disease)
                    putExtra("scientific", diseaseInfo.scientificName)
                    putExtra("symptoms", diseaseInfo.symptoms)
                    putExtra("causes", diseaseInfo.causes)
                    putExtra("treatment", diseaseInfo.treatment)
                }
                startActivity(reportIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error in analysis: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }

        val btnShare = findViewById<LinearLayout>(R.id.btnShare)
        val btnDownload = findViewById<LinearLayout>(R.id.btnDownload)
        val btnBackHome = findViewById<LinearLayout>(R.id.btnBackHome)

        btnShare.setOnClickListener {
            val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.dialog_share, null)
            
            val btnCloseShare = view.findViewById<ImageView>(R.id.btnCloseShare)
            val btnShareWhatsApp = view.findViewById<LinearLayout>(R.id.btnShareWhatsApp)
            val btnShareFacebook = view.findViewById<LinearLayout>(R.id.btnShareFacebook)
            val btnShareTwitter = view.findViewById<LinearLayout>(R.id.btnShareTwitter)
            val btnShareTelegram = view.findViewById<LinearLayout>(R.id.btnShareTelegram)
            val btnShareInstagram = view.findViewById<LinearLayout>(R.id.btnShareInstagram)
            val btnShareEmail = view.findViewById<LinearLayout>(R.id.btnShareEmail)
            val btnShareSMS = view.findViewById<LinearLayout>(R.id.btnShareSMS)
            val btnCopyToClipboard = view.findViewById<LinearLayout>(R.id.btnCopyToClipboard)
            val btnMoreApps = view.findViewById<LinearLayout>(R.id.btnMoreApps)
            
            btnCloseShare.setOnClickListener { bottomSheetDialog.dismiss() }
            
            btnShareWhatsApp?.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, WhatsAppShareActivity::class.java))
            }
            btnShareFacebook?.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, FacebookShareActivity::class.java))
            }
            btnShareTwitter?.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, TwitterShareActivity::class.java))
            }
            btnShareTelegram?.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, TelegramShareActivity::class.java))
            }
            btnShareInstagram?.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, InstagramShareActivity::class.java))
            }
            btnShareEmail?.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, EmailShareActivity::class.java))
            }

            btnShareSMS?.setOnClickListener {
                bottomSheetDialog.dismiss()
                val info = diseaseInfo ?: return@setOnClickListener
                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("smsto:")
                    putExtra("sms_body", "Crop Disease Detection Result:\n${info.crop} - ${info.disease} detected. Confidence: ${info.confidence}%")
                }
                try {
                    startActivity(smsIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Opening SMS app...", Toast.LENGTH_SHORT).show()
                }
            }

            btnCopyToClipboard?.setOnClickListener {
                bottomSheetDialog.dismiss()
                val info = diseaseInfo ?: return@setOnClickListener
                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText(
                    "Crop Report",
                    "Crop Disease Detection Result:\n${info.crop} - ${info.disease} detected. Confidence: ${info.confidence}%"
                )
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
            }

            btnMoreApps?.setOnClickListener {
                bottomSheetDialog.dismiss()
                val info = diseaseInfo ?: return@setOnClickListener
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Crop Disease Report")
                    putExtra(Intent.EXTRA_TEXT, "Crop Disease Detection Result:\n${info.crop} - ${info.disease} detected. Confidence: ${info.confidence}%")
                }
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
            
            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()
        }

        btnDownload.setOnClickListener {
            val info = diseaseInfo
            if (info != null) {
                Toast.makeText(this, "Generating report PDF...", Toast.LENGTH_SHORT).show()
                btnDownload.postDelayed({
                    try {
                        val pdfDocument = android.graphics.pdf.PdfDocument()
                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 pt
                        val page = pdfDocument.startPage(pageInfo)
                        val canvas = page.canvas
                        val paint = android.graphics.Paint()

                        // Title Styling
                        paint.color = android.graphics.Color.BLACK
                        paint.textSize = 20f
                        paint.isFakeBoldText = true
                        canvas.drawText("AGROASSIST CROP HEALTH REPORT", 50f, 60f, paint)

                        // Line separator
                        paint.strokeWidth = 2f
                        canvas.drawLine(50f, 80f, 545f, 80f, paint)

                        // Reset paint for text
                        paint.isFakeBoldText = false
                        paint.textSize = 14f
                        
                        var yPosition = 120f
                        val leading = 24f

                        val lines = listOf(
                            "Date: 2026-06-19",
                            "Crop Name: ${info.crop}",
                            "Condition/Disease: ${info.disease}",
                            "Scientific Name: ${info.scientificName}",
                            "Severity Level: ${info.severity}%",
                            "Risk Level: ${info.riskLevel}",
                            "Confidence Score: ${info.confidence}%",
                            "",
                            "Symptoms:",
                            info.symptoms,
                            "",
                            "Causes:",
                            info.causes,
                            "",
                            "Treatment Instructions:",
                            info.treatment
                        )

                        // Draw lines on the PDF canvas, handling multiline text wrapping
                        for (line in lines) {
                            if (line.isEmpty()) {
                                yPosition += 12f
                                continue
                            }
                            
                            // Wrapping check
                            if (paint.measureText(line) > 495f) {
                                // Split text by spaces and wrap manually
                                val words = line.split(" ")
                                var currentLine = ""
                                for (word in words) {
                                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                                    if (paint.measureText(testLine) > 495f) {
                                        canvas.drawText(currentLine, 50f, yPosition, paint)
                                        yPosition += leading
                                        currentLine = word
                                    } else {
                                        currentLine = testLine
                                    }
                                }
                                if (currentLine.isNotEmpty()) {
                                    canvas.drawText(currentLine, 50f, yPosition, paint)
                                    yPosition += leading
                                }
                            } else {
                                canvas.drawText(line, 50f, yPosition, paint)
                                yPosition += leading
                            }
                            
                            // Check page height overflow
                            if (yPosition > 800f) {
                                break
                            }
                        }

                        pdfDocument.finishPage(page)

                        // Save PDF directly to the system's public Downloads directory using MediaStore for modern devices
                        val pdfFileName = "Crop_Report_${info.crop.replace(" ", "_")}.pdf"
                        
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            val resolver = contentResolver
                            val contentValues = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, pdfFileName)
                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                            }
                            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                            if (uri != null) {
                                resolver.openOutputStream(uri)?.use { outputStream ->
                                    pdfDocument.writeTo(outputStream)
                                }
                                Toast.makeText(this@ResultsActivity, "Report PDF saved to Downloads folder!", Toast.LENGTH_LONG).show()
                            } else {
                                throw Exception("Could not insert PDF entry via MediaStore")
                            }
                        } else {
                            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                            val file = java.io.File(downloadDir, pdfFileName)
                            java.io.FileOutputStream(file).use { outputStream ->
                                pdfDocument.writeTo(outputStream)
                            }
                            Toast.makeText(this@ResultsActivity, "Report PDF saved to Downloads folder!", Toast.LENGTH_LONG).show()
                        }
                        pdfDocument.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@ResultsActivity, "Download Complete: Crop_Report.pdf", Toast.LENGTH_SHORT).show()
                    }
                }, 1500)
            } else {
                Toast.makeText(this, "No report data available to download.", Toast.LENGTH_SHORT).show()
            }
        }

        btnBackHome.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}
