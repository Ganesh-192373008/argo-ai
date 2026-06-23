package com.example.agroassist

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap

class ChatAssistantActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private lateinit var chatInput: EditText
    private lateinit var btnSend: ImageView

    // Camera capture launcher
    private val capturePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            handleSelectedImage(bitmap)
        } else {
            Toast.makeText(this, "Camera capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery / Photo picker launcher
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    handleSelectedImage(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // File picker launcher
    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    handleSelectedImage(bitmap)
                } else {
                    addUserMessage("Shared file: ${uri.lastPathSegment}")
                    showTypingIndicator()
                    Handler(Looper.getMainLooper()).postDelayed({
                        hideTypingIndicator()
                        addAIMessage("I've received your file. I can analyze crop leaves and agricultural images. Please upload an image of your crop leaf for a disease diagnosis!")
                    }, 1500)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSelectedImage(bitmap: Bitmap) {
        // 1. Add image to user chat bubble
        addUserImageMessage(bitmap)

        // 2. Show typing indicator
        showTypingIndicator()

        // 3. Process image in coroutine (save to temp file and run classifier)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val cachePath = java.io.File(cacheDir, "temp_chat_image.png")
                val stream = java.io.FileOutputStream(cachePath)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                val dbHelper = AgroDatabaseHelper(this@ChatAssistantActivity)
                val profile = dbHelper.getProfile()
                val preferredCrop = profile["crops"]

                // Classify image dynamically
                val diseaseInfo = PlantVillageClassifier.classifyImage(cachePath.absolutePath, preferredCrop)

                // Build detailed diagnostic response
                val diagnosisReport = "🔍 **AI Crop Diagnosis Report**\n\n" +
                    "• **Crop Detected**: ${diseaseInfo.crop}\n" +
                    "• **Status/Condition**: ${diseaseInfo.disease}\n" +
                    "• **Scientific Name**: *${diseaseInfo.scientificName}*\n" +
                    "• **Confidence/Severity**: ${diseaseInfo.severity}% (${diseaseInfo.confidence})\n" +
                    "• **Risk Level**: ${diseaseInfo.riskLevel}\n\n" +
                    "📝 **Symptoms**:\n${diseaseInfo.symptoms}\n\n" +
                    "🧬 **Causes**:\n${diseaseInfo.causes}\n\n" +
                    "🛠️ **Recommended Action**:\n${diseaseInfo.treatment}"

                hideTypingIndicator()
                addAIMessage(diagnosisReport)
            } catch (e: java.lang.Exception) {
                hideTypingIndicator()
                addAIMessage("Sorry, I could not analyze this image. Please ensure it is a clear picture of a crop or leaf.")
            }
        }
    }

    private fun addUserImageMessage(bitmap: Bitmap) {
        val density = resources.displayMetrics.density
        
        val bubbleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (16 * density).toInt())
            }
        }

        // Add spacer on the left to push the layout to the right and constrain width
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                1,
                1f
            )
        }
        bubbleLayout.addView(spacer)

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_bubble_user)
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = (64 * density).toInt()
            }
        }

        val imageView = ImageView(this).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
            maxHeight = (200 * density).toInt()
            maxWidth = (200 * density).toInt()
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val timeText = TextView(this).apply {
            text = getCurrentTime()
            setTextColor(Color.parseColor("#E8F5E9"))
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                topMargin = (4 * density).toInt()
            }
        }

        textContainer.addView(imageView)
        textContainer.addView(timeText)
        bubbleLayout.addView(textContainer)
        
        chatContainer.addView(bubbleLayout)
        
        // Slide up + Fade in animation
        bubbleLayout.alpha = 0f
        bubbleLayout.translationY = 40f * density
        bubbleLayout.animate().alpha(1f).translationY(0f).setDuration(300).start()
        
        scrollToBottom()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_assistant)

        // Load dynamic API keys from SharedPreferences
        val prefs = getSharedPreferences("AgroAssistAIKeys", android.content.Context.MODE_PRIVATE)
        var geminiKey = prefs.getString("gemini_api_key", "") ?: ""
        var openaiKey = prefs.getString("openai_api_key", "") ?: ""
        
        if (geminiKey.trim().lowercase() == "hi") {
            geminiKey = ""
            prefs.edit().putString("gemini_api_key", "").apply()
        }
        if (openaiKey.trim().lowercase() == "hi" || openaiKey.trim().startsWith("sk-...")) {
            openaiKey = ""
            prefs.edit().putString("openai_api_key", "").apply()
        }
        
        GeminiClient.setApiKey(geminiKey)
        OpenAIClient.setApiKey(openaiKey)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        chatContainer = findViewById(R.id.chatContainer)
        chatScrollView = findViewById(R.id.chatScrollView)
        chatInput = findViewById(R.id.chatInput)
        btnSend = findViewById(R.id.btnSend)
        val btnAttach = findViewById<ImageView>(R.id.btnAttach)

        btnAttach.setOnClickListener {
            showAttachmentPopup(it)
        }

        // Set click listeners for the settings gear button
        val btnSettings = findViewById<ImageView>(R.id.btnSettings)
        btnSettings?.setOnClickListener {
            showApiSettingsDialog()
        }

        // Set click listeners for the microphone icons
        val btnHeaderMic = findViewById<ImageView>(R.id.btnHeaderMic)
        val btnInputMic = findViewById<ImageView>(R.id.btnInputMic)

        val launchVoiceAssistant = {
            val voiceIntent = Intent(this, VoiceListeningActivity::class.java)
            startActivity(voiceIntent)
        }
        btnHeaderMic?.setOnClickListener { launchVoiceAssistant() }
        btnInputMic?.setOnClickListener { launchVoiceAssistant() }

        // Set click listeners for the suggestion chips
        val chipDetectDisease = findViewById<TextView>(R.id.chipDetectDisease)
        val chipTreatmentAdvice = findViewById<TextView>(R.id.chipTreatmentAdvice)
        val chipPreventionTips = findViewById<TextView>(R.id.chipPreventionTips)
        val chipFertilizerGuide = findViewById<TextView>(R.id.chipFertilizerGuide)
        val chipWeatherHelp = findViewById<TextView>(R.id.chipWeatherHelp)

        val handleChipClick = { queryText: String ->
            chatInput.setText(queryText)
            btnSend.performClick()
        }

        chipDetectDisease?.setOnClickListener { handleChipClick("How do I detect diseases in my crops?") }
        chipTreatmentAdvice?.setOnClickListener { handleChipClick("What treatment is recommended for crop diseases?") }
        chipPreventionTips?.setOnClickListener { handleChipClick("What are the best crop disease prevention tips?") }
        chipFertilizerGuide?.setOnClickListener { handleChipClick("How should I fertilize my crops?") }
        chipWeatherHelp?.setOnClickListener { handleChipClick("Give me weather-based farming suggestions.") }

        chatInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val context = this@ChatAssistantActivity
                if (s.toString().trim().isNotEmpty()) {
                    btnSend.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        context.resources.getColor(R.color.primary_green, context.theme)
                    )
                } else {
                    btnSend.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#D2D6DC")
                    )
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navDetection = findViewById<LinearLayout>(R.id.navDetection)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        navHome?.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        navDetection?.setOnClickListener {
            val intent = Intent(this, DetectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        navProfile?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        btnSend.setOnClickListener {
            val query = chatInput.text.toString().trim()
            if (query.isNotEmpty()) {
                addUserMessage(query)
                chatInput.text.clear()
                
                // Show typing indicator
                showTypingIndicator()
                
                // Call Gemini / Local Engine in a Coroutine
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val dbHelper = AgroDatabaseHelper(this@ChatAssistantActivity)
                        val profile = dbHelper.getProfile()
                        val location = profile["location"]?.ifEmpty { "Noida, UP" } ?: "Noida, UP"
                        val crops = profile["crops"]?.ifEmpty { "Tomato, Rice" } ?: "Tomato, Rice"
                        val historyList = dbHelper.getHistory()
                        val recentDisease = if (historyList.isNotEmpty()) historyList[0]["disease"] ?: "Healthy" else "Healthy"
                        
                        val systemContext = "System Instruction: You are AgroAI, an intelligent agricultural assistant integrated into a Crop Disease Detection System.\n" +
                                            "Your primary role is to assist farmers by analyzing crop disease detection results and providing accurate agricultural guidance.\n" +
                                            "Responsibilities:\n" +
                                            "1. Analyze crop disease predictions generated by the AI model.\n" +
                                            "2. Explain detected diseases in simple and farmer-friendly language.\n" +
                                            "3. Provide: Disease name, Disease severity level, Symptoms, Causes, Preventive measures, and Treatment recommendations.\n" +
                                            "4. Recommend suitable pesticides, fungicides, or organic treatments when appropriate.\n" +
                                            "5. Suggest dosage, application methods, and safety precautions (e.g. wearing masks/gloves, spray timing).\n" +
                                            "6. Support multiple crops including Tomato, Potato, Rice, Wheat, Corn, Cotton, Sugarcane, Banana, Mango, and other crops.\n" +
                                            "7. Answer any agriculture-related questions from users.\n" +
                                            "8. Support multiple languages (e.g., English, Hindi, Spanish) and automatically respond in the user's language.\n" +
                                            "9. If the disease confidence score is low (or if the user's input/image prediction confidence is low/uncertain), advise the user to consult an agricultural expert.\n" +
                                            "10. Provide weather-based farming suggestions when weather data is available.\n" +
                                            "11. Suggest irrigation, fertilization, and crop management practices.\n" +
                                            "12. Compare healthy and diseased plant conditions.\n" +
                                            "13. Help identify possible reasons for crop damage such as Fungal infection, Bacterial infection, Viral infection, Nutrient deficiency, Pest attack, or Environmental stress.\n" +
                                            "14. Give step-by-step solutions to reduce crop loss.\n" +
                                            "15. Use simple, clear, and practical language suitable for farmers.\n" +
                                            "Always prioritize farmer safety, sustainable farming practices, and accurate agricultural recommendations.\n\n" +
                                            "Farmer Context:\n" +
                                            "- Location: $location\n" +
                                            "- Selected Crops: $crops\n" +
                                            "- Recent Scan Issue: $recentDisease\n" +
                                            "- Current Weather: 29°C, Humid, sunny/occasional showers\n" +
                                            "Answer the user's question accurately using this context when relevant."

                        val aiResponse = try {
                            GeminiClient.generateResponse(query, systemContext)
                        } catch (e: Exception) {
                            try {
                                OpenAIClient.generateResponse(query, systemContext)
                            } catch (ex: Exception) {
                                // Show toast to inform user why it failed
                                val errorMsg = e.localizedMessage ?: "Invalid Key"
                                Toast.makeText(this@ChatAssistantActivity, "API Key Failed: $errorMsg. Using offline fallback.", Toast.LENGTH_LONG).show()
                                getAIResponseLocal(query, location, crops, recentDisease)
                            }
                        }
                        
                        hideTypingIndicator()
                        addAIMessage(aiResponse)
                    } catch (e: Exception) {
                        hideTypingIndicator()
                        addAIMessage("Sorry, I encountered an issue. Please check your network connection.")
                    }
                }
            }
        }

        // Populate initial mock message history
        addAIMessage("Hello! I'm AgroAI, your intelligent agricultural assistant. How can I help you today?")
        addUserMessage("How can I prevent tomato blight?")
        addAIMessage("To prevent tomato blight:\n\n1. Space plants properly for air circulation\n2. Water at soil level (drip irrigation), not overhead\n3. Apply copper-based organic fungicides preventively\n4. Remove and destroy infected leaves immediately\n5. Rotate crops yearly\n\nWould you like more details on dosage or safety precautions?")
    }

    private var typingIndicatorView: View? = null

    private fun showTypingIndicator() {
        val density = resources.displayMetrics.density
        val bubbleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (16 * density).toInt())
            }
        }

        val robotIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_assistant_bot)
            setColorFilter(resources.getColor(R.color.primary_green, theme))
            layoutParams = LinearLayout.LayoutParams((24 * density).toInt(), (24 * density).toInt()).apply {
                gravity = Gravity.BOTTOM
                rightMargin = (8 * density).toInt()
            }
        }

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_white)
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = (48 * density).toInt()
            }
        }

        val messageText = TextView(this).apply {
            text = "AgroAI is typing..."
            setTextColor(resources.getColor(R.color.text_secondary, theme))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.ITALIC)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        textContainer.addView(messageText)
        bubbleLayout.addView(robotIcon)
        bubbleLayout.addView(textContainer)
        
        typingIndicatorView = bubbleLayout
        chatContainer.addView(bubbleLayout)
        
        // Slide up + Fade in animation
        bubbleLayout.alpha = 0f
        bubbleLayout.translationY = 40f * density
        bubbleLayout.animate().alpha(1f).translationY(0f).setDuration(250).start()
        
        scrollToBottom()
    }

    private fun hideTypingIndicator() {
        typingIndicatorView?.let {
            chatContainer.removeView(it)
            typingIndicatorView = null
        }
    }

    private fun showAttachmentPopup(anchorView: View) {
        val popupView = layoutInflater.inflate(R.layout.dialog_chat_attachments, null)
        val popupWindow = android.widget.PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 8f

        popupView.findViewById<View>(R.id.attachCamera).setOnClickListener {
            popupWindow.dismiss()
            capturePhoto.launch(null)
        }

        popupView.findViewById<View>(R.id.attachPhotos).setOnClickListener {
            popupWindow.dismiss()
            pickImage.launch("image/*")
        }

        popupView.findViewById<View>(R.id.attachFiles).setOnClickListener {
            popupWindow.dismiss()
            pickFile.launch("image/*")
        }

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupHeight = popupView.measuredHeight
        
        // Position above the attachment button
        popupWindow.showAsDropDown(anchorView, 0, -popupHeight - anchorView.height)
    }

    private fun showApiSettingsDialog() {
        val density = resources.displayMetrics.density
        val padding = (20 * density).toInt()

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val descriptionText = TextView(this).apply {
            text = "Enter your custom API keys to enable real-time, live answers from Gemini or OpenAI. Leave them blank to fall back to the local farming expert engine."
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

        val prefs = getSharedPreferences("AgroAssistAIKeys", android.content.Context.MODE_PRIVATE)
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
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        builder.show()
    }


    private fun addUserMessage(message: String) {
        val density = resources.displayMetrics.density
        
        val bubbleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (16 * density).toInt())
            }
        }

        // Add spacer on the left to push the layout to the right and constrain width
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                1,
                1f
            )
        }
        bubbleLayout.addView(spacer)

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_bubble_user)
            setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = (64 * density).toInt()
            }
        }

        val messageText = TextView(this).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val timeText = TextView(this).apply {
            text = getCurrentTime()
            setTextColor(Color.parseColor("#E8F5E9"))
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                topMargin = (4 * density).toInt()
            }
        }

        textContainer.addView(messageText)
        textContainer.addView(timeText)
        
        textContainer.setOnClickListener {
            chatInput.setText(message)
            chatInput.setSelection(message.length)
            chatInput.requestFocus()
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(chatInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            Toast.makeText(this, "Message loaded into input field to edit", Toast.LENGTH_SHORT).show()
        }

        bubbleLayout.addView(textContainer)
        
        chatContainer.addView(bubbleLayout)
        
        // Slide up + Fade in animation
        bubbleLayout.alpha = 0f
        bubbleLayout.translationY = 40f * density
        bubbleLayout.animate().alpha(1f).translationY(0f).setDuration(300).start()
        
        scrollToBottom()
    }

    private fun addAIMessage(message: String) {
        val density = resources.displayMetrics.density

        val bubbleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (16 * density).toInt())
            }
        }

        val robotIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_assistant_bot)
            setColorFilter(resources.getColor(R.color.primary_green, theme))
            layoutParams = LinearLayout.LayoutParams((24 * density).toInt(), (24 * density).toInt()).apply {
                gravity = Gravity.BOTTOM
                rightMargin = (8 * density).toInt()
            }
        }

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_white)
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                rightMargin = (48 * density).toInt()
            }
        }

        val messageText = TextView(this).apply {
            text = message
            setTextColor(resources.getColor(R.color.text_primary, theme))
            textSize = 14f
            setLineSpacing(0f, 1.2f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val timeText = TextView(this).apply {
            text = getCurrentTime()
            setTextColor(resources.getColor(R.color.text_secondary, theme))
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                topMargin = (4 * density).toInt()
            }
        }

        textContainer.addView(messageText)
        textContainer.addView(timeText)
        
        bubbleLayout.addView(robotIcon)
        bubbleLayout.addView(textContainer)
        
        chatContainer.addView(bubbleLayout)
        
        // Slide up + Fade in animation
        bubbleLayout.alpha = 0f
        bubbleLayout.translationY = 40f * density
        bubbleLayout.animate().alpha(1f).translationY(0f).setDuration(300).start()
        
        scrollToBottom()
    }

    private fun getAIResponseLocal(query: String, location: String, crops: String, recentDisease: String): String {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return "Please type a question or query about your crops."
        
        val isPest = q.contains("pest") || q.contains("insect") || q.contains("bug") || q.contains("worm") || q.contains("caterpillar") || q.contains("कीड़ा") || q.contains("plaga") || q.contains("பூச்சி") || q.contains("புరుగు") || q.contains("ಕೀಟ") || q.contains("कीड") || q.contains("পোকা")
        val isWatering = q.contains("water") || q.contains("irrigation") || q.contains("watering") || q.contains("dry") || q.contains("पानी") || q.contains("सिंचाई") || q.contains("riego") || q.contains("தண்ணீர்") || q.contains("నీరు") || q.contains("ನೀರಾವರಿ") || q.contains("सिंचन") || q.contains("সেচ")
        val isFertilizer = q.contains("fertilizer") || q.contains("manure") || q.contains("npk") || q.contains("urea") || q.contains("खाद") || q.contains("उर्वरक") || q.contains("abono") || q.contains("fertilizante") || q.contains("உரம்") || q.contains("ఎరువు") || q.contains("ಗೊಬ್ಬರ") || q.contains("खत") || q.contains("সার")

        
        // 1. Language detection
        var lang = "en"
        if (q.contains("नमस्ते") || q.contains("नमस्कार") || q.contains("टमाटर") || q.contains("आलू") || q.contains("धान") || q.contains("गेहूं") || q.contains("गन्ना") || q.contains("केला") || q.contains("आम") || q.contains("रोग") || q.contains("कीड़ा") || q.contains("पानी") || q.contains("खाद") || q.contains("कृषि") || q.contains("उपचार")) {
            lang = "hi"
        } else if (q.contains("hola") || q.contains("tomate") || q.contains("patata") || q.contains("papa") || q.contains("arroz") || q.contains("trigo") || q.contains("maiz") || q.contains("algodon") || q.contains("enfermedad") || q.contains("plaga") || q.contains("agua") || q.contains("abono")) {
            lang = "es"
        } else if (q.contains("வணக்கம்") || q.contains("தக்காளி") || q.contains("உருளை") || q.contains("நெல்") || q.contains("அரிசி") || q.contains("கோதுமை") || q.contains("கரும்பு") || q.contains("வாழை") || q.contains("மாம்பழம்") || q.contains("பூச்சி") || q.contains("தண்ணீர்") || q.contains("உரம்")) {
            lang = "ta"
        } else if (q.contains("నమస్కారం") || q.contains("టమోటా") || q.contains("బంగాళాదుంప") || q.contains("వరి") || q.contains("బియ్యం") || q.contains("గోధుమ") || q.contains("చెరకు") || q.contains("అరటి") || q.contains("మామిడి") || q.contains("తెగులు") || q.contains("పురుగు") || q.contains("నీరు") || q.contains("ఎరువు")) {
            lang = "te"
        } else if (q.contains("ನಮಸ್ಕಾರ") || q.contains("ಟೊಮೆಟೊ") || q.contains("ಆಲೂಗೆಡ್ಡೆ") || q.contains("ಭತ್ತ") || q.contains("ಅಕ್ಕಿ") || q.contains("ಗೋಧಿ") || q.contains("ಕಬ್ಬು") || q.contains("ಬಾಳೆಹಣ್ಣು") || q.contains("ಮಾವಿನಹಣ್ಣು") || q.contains("ರೋಗ") || q.contains("ಕೀಟ") || q.contains("ಗೊಬ್ಬರ")) {
            lang = "kn"
        } else if (q.contains("टोमॅटो") || q.contains("बटाटा") || q.contains("तांदूळ") || q.contains("गहू") || q.contains("ऊस") || q.contains("केळी") || q.contains("आंबा") || q.contains("खत") || q.contains("नमस्कार") || q.contains("कीड")) {
            lang = "mr"
        } else if (q.contains("নমস্কার") || q.contains("টমেটো") || q.contains("আলু") || q.contains("ধান") || q.contains("চাল") || q.contains("গম") || q.contains("আখ") || q.contains("কলা") || q.contains("আম") || q.contains("রোগ") || q.contains("পোকা") || q.contains("সার") || q.contains("জল")) {
            lang = "bn"
        }
 
        // 2. Greeting checks
        val isGreeting = q == "hi" || q == "hello" || q == "hey" || q == "greeting" || q == "hey there" || q.contains("नमस्ते") || q.contains("नमस्कार") || q.contains("hola") || q.contains("வணக்கம்") || q.contains("నమస్కారం") || q.contains("ನಮಸ್ಕಾರ") || q.contains("নমস্কার")
        
        if (isGreeting) {
            return when (lang) {
                "hi" -> "नमस्ते! मैं एग्रोएआई (AgroAI) हूँ, आपका स्मार्ट कृषि सहायक। 🌾\n\nमैं फसल के रोगों का विश्लेषण कर सकता हूँ, सिंचाई/खाद योजनाओं का सुझाव दे सकता हूँ और जैविक कीट प्रबंधन टिप्स दे सकता हूँ। आज मैं आपकी फसल की उपज बढ़ाने में कैसे मदद कर सकता हूँ?"
                "es" -> "¡Hola! Soy AgroAI, su asistente agrícola inteligente. 🌾\n\nPuedo analizar enfermedades de cultivos, sugerir planes de riego/fertilización y ofrecer consejos de manejo orgánico de plagas. ¿Cómo puedo ayudarle a mejorar su cosecha hoy?"
                "ta" -> "வணக்கம்! நான் அக்ரோஏஐ (AgroAI), உங்கள் ஸ்மார்ட் விவசாய உதவியாளர். 🌾\n\nநான் பயிர் நோய்களைக் கண்டறிந்து, நீர்ப்பாசனம்/உரமிடுதல் ஆலோசனைகளையும், இயற்கை பூச்சி மேலாண்மை குறிப்புகளையும் வழங்க முடியும். இன்று நான் உங்களுக்கு எவ்வாறு உதவ முடியும்?"
                "te" -> "నమస్కారం! నేను ఆగ్రోఏఐ (AgroAI), మీ స్మార్ట్ వ్యవసాయ సహాయకుడిని. 🌾\n\nనేను పంట తెగుళ్లను విશ્లేషించగలను, నీటి పారుదల/ఎరువుల ప్రణాళికలను సూచించగలను మరియు సేంద్రీయ తెగులు నివారణ చిట్కాలను అందించగలను. ఈ రోజు నేను మీకు ఎలా సహాయపడగలను?"
                "kn" -> "ನಮಸ್ಕಾರ! ನಾನು ಆಗ್ರೋಏಐ (AgroAI), ನಿಮ್ಮ ಸ್ಮಾರ್ಟ್ ಕೃಷಿ ಸಹಾಯಕ. 🌾\n\nನಾನು ಬೆಳೆ ರೋಗಗಳನ್ನು ವಿಶ್ಲೇಷಿಸಬಹುದು, ನೀರಾವರಿ/ಗೊಬ್ಬರ ಯೋಜನೆಗಳನ್ನು ಸೂಚಿಸಬಹುದು ಮತ್ತು ಸಾವಯವ ಕೀಟ ನಿಯಂತ್ರಣ ಸಲಹೆಗಳನ್ನು ನೀಡಬಹುದು. ಇಂದು ನಾನು ನಿಮಗೆ ಹೇಗೆ ಸಹಾಯ ಮಾಡಲಿ?"
                "mr" -> "नमस्कार! मी ऍग्रोएआय (AgroAI), तुमचा स्मार्ट कृषी सहाय्यक आहे. 🌾\n\nमी पिकांवरील रोगांचे विश्लेषण करू शकतो, सिंचन आणि खतांच्या नियोजनाची शिफारस करू शकतो आणि सेंद्रिय कीड व्यवस्थापन टिप्स देऊ शकतो. आज मी तुम्हाला कशी मदत करू शकतो?"
                "bn" -> "নমস্কার! আমি এগ্রোএআই (AgroAI), আপনার স্মার্ট কৃষি সহকারী। 🌾\n\nআমি ফসলের রোগ বিশ্লেষণ করতে পারি, সেচ ও সার প্রয়োগের পরামর্শ দিতে পারি এবং জৈব কীট দমনের টিপস দিতে পারি। আজ আমি আপনাকে কীভাবে সাহায্য করতে পারি?"
                else -> "Hello! I am AgroAI, your intelligent agricultural assistant. 🌾\n\nI can analyze crop diseases, suggest customized watering/fertilizer plans, explain soil health, and offer organic pest management tips. How can I help you grow a healthier yield today?"
            }
        }
 
        val sb = java.lang.StringBuilder()
 
        // 3. Process context info header
        when (lang) {
            "hi" -> sb.append("नमस्ते! एग्रोएआई (AgroAI) यहाँ है।\n\nआपके कृषि संदर्भ का विश्लेषण (स्थान: **$location**, फसलें: **$crops**, हालिया स्कैन समस्या: **$recentDisease**):\n")
            "es" -> sb.append("¡Hola! Soy AgroAI.\n\nAnalizando su consulta con su contexto agrícola (Ubicación: **$location**, Cultivos: **$crops**, Problema reciente: **$recentDisease**):\n")
            "ta" -> sb.append("வணக்கம்! அக்ரோஏஐ (AgroAI) இங்கே உள்ளது.\n\nஉங்கள் விவசாய சூழலை பகுப்பாய்வு செய்கிறது (இருப்பிடம்: **$location**, பயிர்கள்: **$crops**, சமீபத்திய ஸ்கேன் சிக்கல்: **$recentDisease**):\n")
            "te" -> sb.append("నమస్కారం! ఆగ్రోఏఐ (AgroAI) ఇక్కడ ఉంది.\n\nమీ వ్యవసాయ సందర్భాన్ని విશ્లేషిస్తోంది (స్థానం: **$location**, పంటలు: **$crops**, ఇటీవలి స్కాన్ సమస్య: **$recentDisease**):\n")
            "kn" -> sb.append("ನಮಸ್ಕಾರ! ಆಗ್ರೋಏಐ (AgroAI) ಇಲ್ಲಿದೆ.\n\nನಿಮ್ಮ ಕೃಷಿ ಸಂದರ್ಭವನ್ನು ವಿಶ್ಲೇಷಿಸುತ್ತಿದೆ (ಸ್ಥಳ: **$location**, ಬೆಳೆಗಳು: **$crops**, ಇತ್ತೀಚಿನ ಸ್ಕ್ಯಾನ್ ಸಮಸ್ಯೆ: **$recentDisease**):\n")
            "mr" -> sb.append("नमस्कार! ऍग्रोएआय (AgroAI) येथे आहे.\n\nतुमच्या शेतीच्या संदर्भाचे विश्लेषण करत आहे (स्थान: **$location**, पिके: **$crops**, अलीकडील स्कॅन समस्या: **$recentDisease**):\n")
            "bn" -> sb.append("নমস্কার! এগ্রোএআই (AgroAI) এখানে আছে।\n\nআপনার কৃষি প্রেক্ষাপট বিশ্লেষণ করা হচ্ছে (স্থান: **$location**, ফসল: **$crops**, সাম্প্রতিক স্ক্যান সমস্যা: **$recentDisease**):\n")
            else -> sb.append("Hi there! AgroAI here.\n\nAnalyzing your query with your farm context in **$location** growing **$crops** (Recent scan: **$recentDisease**):\n")
        }
 
        // 4. Low confidence advise
        val isLowConfidence = q.contains("low confidence") || q.contains("not sure") || q.contains("uncertain") || q.contains("unclear") || q.contains("कम विश्वास") || q.contains("संदेह") || q.contains("baja confianza") || q.contains("duda") || q.contains("expert") || q.contains("विशेषज्ञ") || q.contains("experto")
        if (isLowConfidence) {
            when (lang) {
                "hi" -> sb.append("\n⚠️ **सलाह**: चूंकि रोग की पुष्टि का आत्मविश्वास स्कोर कम है, हम दृढ़ता से अनुशंसा करते हैं कि किसी भी रासायनिक उपचार को लागू करने से पहले आप एक स्थानीय कृषि विस्तार अधिकारी या फसल विशेषज्ञ से परामर्श करें।\n")
                "es" -> sb.append("\n⚠️ **Consejo**: Dado que el nivel de confianza de la predicción es bajo, le recomendamos encarecidamente consultar a un experto agrícola local o extensionista antes de aplicar cualquier tratamiento químico.\n")
                "ta" -> sb.append("\n⚠️ **அறிவுரை**: நோயைக் கண்டறிவதில் நம்பிக்கை அளவு குறைவாக உள்ளதால், ஏதேனும் இரசாயன சிகிச்சையைப் பயன்படுத்துவதற்கு முன்பு உள்ளூர் விவசாய அதிகாரி அல்லது பயிர் நிபுணரை அணுகுமாறு பரிந்துரைக்கிறோம்.\n")
                "te" -> sb.append("\n⚠️ **సలహా**: తెగులు నిర్ధారణ ఖచ్చితత్వం తక్కువగా ఉన్నందున, ఎటువంటి రసాయన చికిత్సలను ఉపయోగించే ముందే స్థానిక వ్యవసాయ విస్తరణ అధికారి లేదా పంట నిపుణుడిని సంప్రదించాల్సిందిగా గట్టిగా సిఫార్సు చేస్తున్నాము.\n")
                "kn" -> sb.append("\n⚠️ **ಸಲಹೆ**: ರೋಗದ ಮುನ್ಸೂಚನೆಯ ನಿಖರತೆ ಕಡಿಮೆ ಇರುವುದರಿಂದ, ಯಾವುದೇ ರಾಸಾಯನಿಕ ಚಿಕಿತ್ಸೆಯನ್ನು ಅನ್ವಯಿಸುವ ಮೊದಲು ಸ್ಥಳೀಯ ಕೃಷಿ ವಿಸ್ತರಣಾ ಅಧಿಕಾರಿ ಅಥವಾ ಬೆಳೆ ತಜ್ಞರನ್ನು ಸಂಪರ್ಕಿಸಲು ನಾವು ಬಲವಾಗಿ ಶಿಫಾರಸು ಮಾಡುತ್ತೇವೆ.\n")
                "mr" -> sb.append("\n⚠️ **सल्ला**: पिकावरील रोगाच्या निदानाचा आत्मविश्वास स्कोर कमी असल्याने, कोणताही रासायनिक उपचार करण्यापूर्वी आपण स्थानिक कृषी विस्तार अधिकारी किंवा पीक तज्ञांचा सल्ला घ्यावा ही नम्र विनंती.\n")
                "bn" -> sb.append("\n⚠️ **পরামর্শ**: যেহেতু রোগের পূর্বাভাসের নির্ভুলতার হার কম, তাই যে কোনো রাসায়নিক চিকিৎসা প্রয়োগ করার আগে আমরা আপনাকে স্থানীয় কৃষি সম্প্রসারণ কর্মকর্তা বা ফসল বিশেষজ্ঞের সাথে পরামর্শ করার জন্য দৃঢ়ভাবে সুপারিশ করছি।\n")
                else -> sb.append("\n⚠️ **Advice**: Since the disease prediction confidence level is low, we strongly recommend consulting a local agricultural extension officer or crop expert before applying any chemical treatment.\n")
            }
        }
 
        // 5. Compare healthy vs diseased conditions if requested
        val isComparison = q.contains("healthy") && (q.contains("diseased") || q.contains("compare") || q.contains("sick") || q.contains("spots")) || q.contains("तुलना") || q.contains("comparar") || q.contains("स्वस्थ") || q.contains("बीमार")
        if (isComparison) {
            when (lang) {
                "hi" -> sb.append("\n⚖️ **स्वस्थ बनाम रोगग्रस्त स्थिति की तुलना**:\n• **स्वस्थ पौधे**: पत्तियाँ चमकदार हरी, धब्बों रहित, मजबूत तना और सामान्य विकास दर्शाती हैं।\n• **रोगग्रस्त पौधे**: पत्तियों पर पीले/भूरे धब्बे, फफूंद का विकास, मुरझाना, तने का सड़ना या कीटों के छेद दिखते हैं, जिससे प्रकाश संश्लेषण बाधित होता है।\n")
                "es" -> sb.append("\n⚖️ **Comparación de Estado Sano vs Enfermo**:\n• **Plantas Sanas**: Hojas de color verde vibrante y sin manchas, tallos fuertes y erguidos, y desarrollo foliar normal.\n• **Plantas Enfermas**: Manchas foliares amarillas/marrones, moho, marchitez, pudrición del tallo o agujeros de insectos que reducen la fotosíntesis.\n")
                "ta" -> sb.append("\n⚖️ **ஆரோக்கியமான vs நோயுற்ற பயிர் ஒப்பீடு**:\n• **ஆரோக்கியமான பயிர்கள்**: பளபளப்பான பச்சை மற்றும் புள்ளிகள் இல்லாத இலைகள், வலுவான தண்டுகள் மற்றும் இயல்பான வளர்ச்சி.\n• **நோயுற்ற பயிர்கள்**: இலைகளில் மஞ்சள்/பழுப்பு புள்ளிகள், பூஞ்சை வளர்ச்சி, வாடுதல், தண்டு அழுகல் அல்லது பூச்சித் துளைகள், இதனால் ஒளிச்சேர்க்கை குறைகிறது.\n")
                "te" -> sb.append("\n⚖️ **ఆరోగ్యకరమైన vs తెగులు సోకిన పంట పోలిక**:\n• **ఆరోగ్యకరమైన మొక్కలు**: ప్రకాశవంతమైన ఆకుపచ్చ మరియు మచ్చలు లేని ఆకులు, బలమైన కాండం మరియు సాధారణ ఎదుగుదల.\n• **తెగులు సోకిన మొక్కలు**: ఆకులపై పసుపు/గోధుమ రంగు మచ్చలు, బూజు పెరగడం, వాడిపోవడం, కాండం కుళ్ళిపోవడం లేదా పురుగుల రంధ్రాలు ఉంటాయి.\n")
                "kn" -> sb.append("\n⚖️ **ಆರೋಗ್ಯಕರ vs ರೋಗಗ್ರಸ್ತ ಬೆಳೆ ಹೋಲಿಕೆ**:\n• **ಆರೋಗ್ಯಕರ ಸಸ್ಯಗಳು**: ಗಾಢ ಹಸಿರು ಮತ್ತು ಕಲೆಗಳಿಲ್ಲದ ಎಲೆಗಳು, ಬಲವಾದ ಕಾಂಡಗಳು ಮತ್ತು ಸಾಮಾನ್ಯ ಬೆಳವಣಿಗೆ.\n• **ರೋಗಗ್ರಸ್ತ ಸಸ್ಯಗಳು**: ಎಲೆಗಳ ಮೇಲೆ ಹಳದಿ/ಕಂದು ಕಲೆಗಳು, ಬೂಷ್ಟು ಬೆಳವಣಿಗೆ, ಒಣಗುವಿಕೆ, ಕಾಂಡ ಕೊಳೆಯುವಿಕೆ ಅಥವಾ ಕೀಟಗಳ ರಂಧ್ರಗಳು ಕಾಣಿಸಿಕೊಳ್ಳುತ್ತವೆ.\n")
                "mr" -> sb.append("\n⚖️ **निरोगी विरुद्ध रोगट पिकांची तुलना**:\n• **निरोगी पिके**: चमकदार हिरवी आणि डाग नसलेली पाने, मजबूत खोड आणि सामान्य वाढ दर्शवतात.\n• **रोगट पिके**: पानांवर पिवळे/तपकिरी डाग, बुरशीची वाढ, सुकणे, खोड कुजणे किंवा कीटकांची छिद्रे दिसतात, ज्यामुळे प्रकाशसंश्लेषण प्रक्रिया मंदावते.\n")
                "bn" -> sb.append("\n⚖️ **সুস্থ বনাম রোগাক্রান্ত উদ্ভিদের তুলনা**:\n• **সুস্থ উদ্ভিদ**: প্রাণবন্ত সবুজ এবং দাগহীন পাতা, শক্ত কাণ্ড এবং স্বাভাবিক বৃদ্ধি নির্দেশ করে।\n• **রোগাক্রান্ত উদ্ভিদ**: পাতার উপর হলুদ/বাদামী দাগ, ছত্রাক বৃদ্ধি, শুকিয়ে যাওয়া, কাণ্ড পচা বা পোকার গর্ত দেখা যায়, যার ফলে সালোকসংশ্লেষণ কমে যায়।\n")
                else -> sb.append("\n⚖️ **Healthy vs Diseased Plant Comparison**:\n• **Healthy Plants**: Vibrant green and spotless leaves, sturdy stems, upright posture, and normal growth pattern.\n• **Diseased Plants**: Yellowing/browning leaf spots, mold growth, wilting, stem rot, or insect exit holes, leading to reduced photosynthesis.\n")
            }
        }
 
        // 6. Weather-based farming suggestion
        val isWeather = q.contains("weather") || q.contains("rain") || q.contains("humid") || q.contains("temp") || q.contains("मौसम") || q.contains("बारिश") || q.contains("clima") || q.contains("lluvia") || q.contains("humedad")
        if (isWeather || recentDisease != "Healthy") {
            when (lang) {
                "hi" -> sb.append("\n🌤️ **मौसम आधारित कृषि सुझाव** (हालिया स्थिति: 29°C, आर्द्र/Humid, छिटपुट वर्षा):\n• आर्द्र और गर्म मौसम कवक (Fungal) और जीवाणु (Bacterial) रोगों के प्रसार को बढ़ावा देता है।\n• **सुझाव**: पत्तियों को गीला होने से बचाने के लिए सुबह 5 से 8 बजे के बीच सीधे जड़ों में पानी (ड्रिप सिंचाई) दें। कवक को फैलने से रोकने के लिए पौधों के बीच उचित दूरी सुनिश्चित करें।\n")
                "es" -> sb.append("\n🌤️ **Sugerencia basada en el clima** (Condición actual: 29°C, Húmedo, lluvias ocasionales):\n• El clima húmedo y cálido acelera el desarrollo de enfermedades fúngicas y bacterianas.\n• **Recomendación**: Riegue temprano por la mañana (5:00 AM - 8:00 AM) dirigiendo el agua al suelo (riego por goteo) para mantener las hojas secas. Espacie bien las plantas para reducir la humedad.\n")
                "ta" -> sb.append("\n🌤️ **வானிலை சார்ந்த விவசாய பரிந்துரை** (தற்போதைய வானிலை: 29°C, ஈரப்பதம், அவ்வப்போது மழை):\n• ஈரப்பதமான மற்றும் வெப்பமான வானிலை பூஞ்சை மற்றும் பாக்டீரியா நோய்களின் பரவலை ஊக்குவிக்கிறது.\n• **பரிந்துரை**: இலைகள் நனைவதைத் தவிர்க்க அதிகாலை 5 முதல் 8 மணிக்குள் வேர்ப்பகுதியில் நேரடியாக நீர் பாய்ச்சவும் (சொட்டு நீர் பாசனம்). காற்றோட்டத்திற்காக பயிர்களுக்கு இடையே சரியான இடைவெளி விடவும்.\n")
                "te" -> sb.append("\n🌤️ **వాతావరణ ఆధారిత వ్యవసాయ సూచన** (ప్రస్తుత వాతావరణం: 29°C, తేమ, అక్కడక్కడ జల్లులు):\n• తేమ మరియు వెచ్చని వాతావరణం శిలీంధ్ర (Fungal) మరియు బాక్టీరియల్ తెగుళ్ల వ్యాప్తిని వేగవంతం చేస్తుంది.\n• **సూచన**: ఆకులు తడిసిపోకుండా ఉదయం 5 నుండి 8 గంటల మధ్య నేరుగా వేర్ల వద్ద నీరు (డ్రిప్ నీటి పారుదల) అందించండి. గాలి ప్రసరణ కోసం మొక్కల మధ్య సరైన దూరం ఉంచండి.\n")
                "kn" -> sb.append("\n🌤️ **ಹವಾಮಾನ ಆಧಾರಿತ ಕೃಷಿ ಸಲಹೆ** (ಪ್ರಸ್ತುತ ಹವಾಮಾನ: 29°C, ಆರ್ದ್ರತೆ, ಸಾಧಾರಣ ಮಳೆ):\n• ಆರ್ದ್ರ ಮತ್ತು ಬೆಚ್ಚಗಿನ ಹವಾಮಾನವು ಬೂಷ್ಟು (Fungal) ಮತ್ತು ಬ್ಯಾಕ್ಟೀರಿಯಾ ರೋಗಗಳ ಹರಡುವಿಕೆಯನ್ನು ಹೆಚ್ಚಿಸುತ್ತದೆ.\n• **ಸಲಹೆ**: ಎಲೆಗಳು ಒದ್ದೆಯಾಗುವುದನ್ನು ತಡೆಯಲು ಬೆಳಿಗ್ಗೆ 5 ರಿಂದ 8 ರ ನಡುವೆ ನೇರವಾಗಿ ಬೇರು ವಲಯಕ್ಕೆ ನೀರುಣಿಸಿ (ಹನಿ ನೀರಾವರಿ). ಗಾಳಿಯಾಡಲು ಸಸ್ಯಗಳ ನಡುವೆ ಸೂಕ್ತ ಅಂತರವನ್ನು ಕಾಯ್ದುಕೊಳ್ಳಿ.\n")
                "mr" -> sb.append("\n🌤️ **हवामानावर आधारित शेती सल्ला** (चालू हवामान: २९°से, दमट, अधूनमधून पाऊस):\n• दमट आणि उबदार हवामान बुरशीजन्य आणि जिवाणूजन्य रोगांच्या प्रसाराला गती देते.\n• **सल्ला**: पाने ओली राहणे टाळण्यासाठी सकाळी ५ ते ८ च्या दरम्यान थेट मुळांशी पाणी (ठिबक सिंचन) द्या. पिकांमध्ये योग्य अंतर ठेवा जेणेकरून हवा खेळती राहील.\n")
                "bn" -> sb.append("\n🌤️ **আবহাওয়া ভিত্তিক কৃষি পরামর্শ** (বর্তমান আবহাওয়া: ২৯°সে, আর্দ্র, মাঝে মাঝে বৃষ্টি):\n• আর্দ্র ও উষ্ণ আবহাওয়া ছত্রাক এবং ব্যাকটেরিয়াজনিত রোগের বিস্তার ত্বরান্বিত করে।\n• **পরামর্শ**: পাতার উপরিভাগ ভেজা এড়াতে সকাল ৫টা থেকে ৮টার মধ্যে সরাসরি গোড়ায় জল (ড্রিপ সেচ) দিন। রোগ প্রতিরোধে গাছগুলোর মাঝে পর্যাপ্ত দূরত্বের ব্যবস্থা করুন।\n")
                else -> sb.append("\n🌤️ **Weather-based Farming Suggestion** (Current Weather: 29°C, Humid, occasional showers):\n• Humid and warm weather accelerates the growth of fungal and bacterial pathogens.\n• **Action Plan**: Practice drip irrigation early in the morning (5:00 AM - 8:00 AM) directly at the root zone to keep leaves dry. Space plants properly to allow airflow and decrease relative humidity.\n")
            }
        }
 
        // 7. Crop-specific analysis & recommendations
        val cropDetected = when {
            q.contains("tomato") || q.contains("टमाटर") || q.contains("tomate") || q.contains("தக்காளி") || q.contains("టమోటా") || q.contains("ಟೊಮೆಟೊ") || q.contains("टोमॅटो") || q.contains("টমেটো") -> "Tomato"
            q.contains("potato") || q.contains("आलू") || q.contains("patata") || q.contains("papa") || q.contains("உருளை") || q.contains("బంగాళాదుంప") || q.contains("ಆಲೂಗೆಡ್ಡೆ") || q.contains("बटाटा") || q.contains("আলু") -> "Potato"
            q.contains("rice") || q.contains("धान") || q.contains("चावल") || q.contains("arroz") || q.contains("நெல்") || q.contains("వరి") || q.contains("ಭತ್ತ") || q.contains("तांदूळ") || q.contains("ধান") -> "Rice"
            q.contains("wheat") || q.contains("गेहूं") || q.contains("trigo") || q.contains("கோதுமை") || q.contains("గోధుమ") || q.contains("ಗೋಧಿ") || q.contains("गहू") || q.contains("গম") -> "Wheat"
            q.contains("corn") || q.contains("maize") || q.contains("मक्का") || q.contains("maiz") || q.contains("சோளம்") || q.contains("మొక్కజొన్న") || q.contains("ಮೆಕ್ಕೆಜೋಳ") || q.contains("मका") || q.contains("ভুট্টা") -> "Corn"
            q.contains("cotton") || q.contains("कपास") || q.contains("algodon") || q.contains("பருத்தி") || q.contains("ప్రత్తి") || q.contains("ಹತ್ತಿ") || q.contains("कापूस") || q.contains("তুলা") -> "Cotton"
            q.contains("sugarcane") || q.contains("गन्ना") || q.contains("caña") || q.contains("கரும்பு") || q.contains("చెరకు") || q.contains("ಕಬ್ಬು") || q.contains("ऊस") || q.contains("আখ") -> "Sugarcane"
            q.contains("banana") || q.contains("केला") || q.contains("platano") || q.contains("banano") || q.contains("வாழை") || q.contains("అరటి") || q.contains("ಬಾಳೆಹಣ್ಣು") || q.contains("केळी") || q.contains("কলা") -> "Banana"
            q.contains("mango") || q.contains("आम") || q.contains("மாம்பழம்") || q.contains("మామిడి") || q.contains("ಮಾವಿನಹಣ್ಣು") || q.contains("आंबा") || q.contains("আম") -> "Mango"
            else -> null
        }
 
        if (cropDetected != null) {
            when (cropDetected) {
                "Tomato" -> {
                    if (lang == "hi") {
                        sb.append("\n🍅 **टमाटर रोग विश्लेषण**:\n" +
                                  "• **रोग का नाम**: लेट ब्लाइट (Late Blight)\n" +
                                  "• **तीव्रता स्तर**: 85% (उच्च जोखिम)\n" +
                                  "• **लक्षण**: पत्तियों और तनों पर काले-भूरे तैलीय धब्बे, नम मौसम में सफेद फफूंद।\n" +
                                  "• **कारण**: फाइटोफ्थोरा इन्फेस्टन्स कवक संक्रमण (Fungal infection)।\n" +
                                  "• **निवारक उपाय**: फसल चक्र अपनाएं, संक्रमित पत्तियां हटाएं।\n" +
                                  "• **उपचार और खुराक**: कॉपर ऑक्सीक्लोराइड (Copper Oxychloride) कवकनाशी का छिड़काव करें (2.5 ग्राम प्रति लीटर पानी में)।\n" +
                                  "• **सुरक्षा सावधानी**: छिड़काव करते समय मास्क और दस्ताने पहनें। सुबह जल्दी या शाम को हवा शांत होने पर छिड़काव करें।")
                    } else if (lang == "es") {
                        sb.append("\n🍅 **Análisis de Enfermedades de Tomate**:\n" +
                                  "• **Nombre de Enfermedad**: Tizón Tardío (Late Blight)\n" +
                                  "• **Nivel de Severidad**: 85% (Riesgo Alto)\n" +
                                  "• **Síntomas**: Manchas oscuras de aspecto grasiento, moho blanco debajo de las hojas.\n" +
                                  "• **Causa**: Infección fúngica por *Phytophthora infestans*.\n" +
                                  "• **Medidas Preventivas**: Rotación de cultivos, eliminar hojas infectadas, riego por goteo.\n" +
                                  "• **Tratamiento y Dosis**: Fungicida a base de cobre (Oxicloruro de cobre) a dosis de 2.5g por litro de agua.\n" +
                                  "• **Precauciones**: Use guantes y mascarilla al aplicar. Aplique temprano por la mañana.")
                    } else if (lang == "ta") {
                        sb.append("\n🍅 **தக்காளி நோய் பகுப்பாய்வு**:\n" +
                                  "• **நோய் பெயர்**: தக்காளி லேட் பிளைட் (பூஞ்சை)\n" +
                                  "• **தீவிரம்**: 85% (அதிக ஆபத்து)\n" +
                                  "• **அறிகுறிகள்**: இலைகள் மற்றும் தண்டுகளில் கரும்பழுப்பு நிற எண்ணெய் போன்ற புள்ளிகள், இலைகளின் அடியில் வெண்மையான பூஞ்சை வளர்ச்சி.\n" +
                                  "• **காரணங்கள்**: ஈரப்பதமான மற்றும் குளிர்ந்த வானிலையால் ஏற்படும் பைட்டோப்தோரா இன்ஃபெஸ்டன்ஸ் பூஞ்சை தொற்று.\n" +
                                  "• **தடுப்பு முறைகள்**: பாதிக்கப்பட்ட இலைகளை அகற்றவும்; பயிர் சுழற்சி செய்யவும்; மேல் தெளிப்பு நீர் பாசனத்தை தவிர்க்கவும்.\n" +
                                  "• **சிகிச்சை & அளவு**: ஒரு லிட்டர் தண்ணீருக்கு 2.5 கிராம் தாமிர ஆக்ஸிகுளோரைடு (Copper Oxychloride) பூஞ்சைக் கொல்லியை தெளிக்கவும்.\n" +
                                  "• **பாதுகாப்பு முன்னெச்சரிக்கைகள்**: தெளிக்கும் போது முகக்கவசம் மற்றும் கையுறைகளை அணியுங்கள்; அதிகாலை நேரத்தில் தெளிக்கவும்.")
                    } else if (lang == "te") {
                        sb.append("\n🍅 **టమోటా తెగులు విశ్లేషణ**:\n" +
                                  "• **తెగులు పేరు**: లేట్ బ్లైట్ (శిలీంధ్ర తెగులు)\n" +
                                  "• **తీవ్రత స్థాయి**: 85% (అధిక ప్రమాదం)\n" +
                                  "• **లక్షణాలు**: ఆకులు/కాండంపై ముదురు గోధుమ రంగు నూనె లాంటి మచ్చలు; ఆకుల క్రింద తెల్లటి బూజు పెరగడం.\n" +
                                  "• **కారణాలు**: తేమ, చల్లని వాతావరణం వల్ల వచ్చే ఫైటోఫ్తోరా ఇన్ఫెస్టాన్స్ శిలీంధ్ర సంక్రమణం.\n" +
                                  "• **నివారణ చర్యలు**: సోకిన ఆకులను తొలగించండి; పంట మార్పిడి చేయండి; పైనుండి నీరు పోయడం నివారించండి.\n" +
                                  "• **చికిత్స & మోతాదు**: లీటరు నీటికి 2.5 గ్రాముల కాపర్ ఆక్సిక్లోరైడ్ (Copper Oxychloride) శిలీంధ్ర నాశిని పిచికారీ చేయండి.\n" +
                                  "• **రక్షణ జాగ్రత్తలు**: పిచికారీ చేసేటప్పుడు మాస్క్ మరియు చేతి తొడుగులు ధరించండి; ఉదయం వేళల్లో మాత్రమే పిచికారీ చేయండి.")
                    } else if (lang == "kn") {
                        sb.append("\n🍅 **ಟೊಮೆಟೊ ರೋಗ ವಿಶ್ಲೇಷಣೆ**:\n" +
                                  "• **ರೋಗದ ಹೆಸರು**: ಲೇಟ್ ಬ್ಲೈಟ್ (ಬೂಷ್ಟು ರೋಗ)\n" +
                                  "• **ತೀವ್ರತೆಯ ಮಟ್ಟ**: 85% (ಹೆಚ್ಚಿನ ಅಪಾಯ)\n" +
                                  "• **ಲಕ್ಷಣಗಳು**: ಎಲೆಗಳು/ಕಾಂಡಗಳ ಮೇಲೆ ಗಾಢ ಕಂದು ಬಣ್ಣದ ಎಣ್ಣೆಯುಕ್ತ ಕಲೆಗಳು; ಎಲೆಗಳ ಕೆಳಗೆ ಬಿಳಿ ಬೂಷ್ಟು ಬೆಳವಣಿಗೆ.\n" +
                                  "• **ಕಾರಣಗಳು**: ತೇವಾಂಶ ಮತ್ತು ತಂಪಾದ ಹವಾಮಾನದಿಂದ ಉಂಟಾಗುವ ಫೈಟೊಫ್ಥೊರಾ ಇನ್ಫೆಸ್ಟನ್ಸ್ ಬೂಷ್ಟು ಸೋಂಕು.\n" +
                                  "• **ನಿವಾರಣಾ ಕ್ರಮಗಳು**: ಸೋಂಕಿತ ಎಲೆಗಳನ್ನು ತೆಗೆದುಹಾಕಿ; ಬೆಳೆ ತಿರುಗಾವಣೆ ಮಾಡಿ; ಮೇಲಿನಿಂದ ನೀರುಣಿಸುವುದನ್ನು ತಪ್ಪಿಸಿ.\n" +
                                  "• **ಚಿಕಿತ್ಸೆ ಮತ್ತು ಡೋಸೇಜ್**: ಪ್ರತಿ ಲೀಟರ್ ನೀರಿಗೆ 2.5 ಗ್ರಾಂ ತಾಮ್ರದ ಆಕ್ಸಿಕ್ಲೋರೈಡ್ (Copper Oxychloride) ಬೂಷ್ಟುನಾಶಕವನ್ನು ಸಿಂಪಡಿಸಿ.\n" +
                                  "• **ಸುರಕ್ಷತಾ ಮುನ್ನೆಚ್ಚರಿಕೆಗಳು**: ಸಿಂಪಡಿಸುವಾಗ ಮಾಸ್ಕ್ ಮತ್ತು ಕೈಗವಸುಗಳನ್ನು ಧರಿಸಿ; ಮುಂಜಾನೆ ಸಿಂಪಡಿಸಿ.")
                    } else if (lang == "mr") {
                        sb.append("\n🍅 **टोमॅटो रोग विश्लेषण**:\n" +
                                  "• **रोगाचे नाव**: लेट ब्लाइट (बुरशीजन्य)\n" +
                                  "• **तीव्रता पातळी**: ८५% (उच्च धोका)\n" +
                                  "• **लक्षणे**: पाने/खोड यांवर गडद तपकिरी तेलकट डाग; पानांच्या खाली पांढरी बुरशी वाढणे.\n" +
                                  "• **कारणे**: फायटोफ्थोरा इन्फेस्टन्स बुरशी संसर्ग.\n" +
                                  "• **प्रतिबंधात्मक उपाय**: बाधित पाने काढून टाका; पीक फिरवा; पानांवर पाणी फवारणे टाळा.\n" +
                                  "• **उपचार आणि डोस**: कॉपर ऑक्सीक्लोराइड बुरशीनाशक २.५ ग्रॅम प्रति लीटर पाण्यात मिसळून फवारणी करावी.\n" +
                                  "• **सुरक्षितता खबरदारी**: फवारणी करताना मास्क आणि हातमोजे वापरा; सकाळी शांत हवा असताना फवारणी करा.")
                    } else if (lang == "bn") {
                        sb.append("\n🍅 **টমেটো রোগ विश्लेषण**:\n" +
                                  "• **রোগের নাম**: লেট ব্লাইট (ছত্রাকজনিত)\n" +
                                  "• **তীব্রতার মাত্রা**: ৮৫% (উচ্চ ঝুঁকি)\n" +
                                  "• **লক্ষণ**: পাতা ও কাণ্ডে গাঢ় বাদামী তেলের মতো দাগ; পাতার নিচে সাদাটে ছত্রাকের আস্তরণ।\n" +
                                  "• **কারণ**: আর্দ্র ও শীতল আবহাওয়ার কারণে ফাইটোফথোরা ইনফেসট্যান্স ছত্রাক সংক্রমণ।\n" +
                                  "• **প্রতিরোধমূলক ব্যবস্থা**: আক্রান্ত পাতা কেটে ধ্বংস করুন; শস্য আবর্তন করুন; গাছের গোড়ায় জল দিন।\n" +
                                  "• **চিকিৎসা ও ডোজ**: প্রতি লিটার জলে ২.৫ গ্রাম কপার অক্সিক্লোরাইড (Copper Oxychloride) ছত্রাকনাশক স্প্রে করুন।\n" +
                                  "• **নিরাপত্তা সতর্কতা**: স্প্রে করার সময় মাস্ক এবং গ্লাভস ব্যবহার করুন; সকালে স্প্রে করা ভালো।")
                    } else {
                        sb.append("\n🍅 **Tomato Disease Analysis**:\n" +
                                  "• **Disease Name**: Tomato Late Blight (Fungal)\n" +
                                  "• **Severity Level**: 85% (High Risk)\n" +
                                  "• **Symptoms**: Dark brown, water-soaked oily spots on leaves/stems; fuzzy white fungal growth underneath.\n" +
                                  "• **Causes**: *Phytophthora infestans* fungal infection favored by humid, cool weather.\n" +
                                  "• **Preventive Measures**: Prune lower leaves to prevent soil splash; rotate crops; avoid overhead irrigation.\n" +
                                  "• **Treatment & Dosage**: Spray Copper Oxychloride fungicide at 2.5g per Liter of water.\n" +
                                  "• **Safety Precautions**: Wear protective mask and gloves; spray during calm morning hours to prevent drift.")
                    }
                }
                "Potato" -> {
                    if (lang == "hi") {
                        sb.append("\n🥔 **आलू रोग विश्लेषण**:\n" +
                                  "• **रोग का नाम**: अगेती झुलसा (Early Blight)\n" +
                                  "• **तीव्रता स्तर**: 45% (मध्यम जोखिम)\n" +
                                  "• **लक्षण**: पत्तियों पर छोटे भूरे धब्बे जिनमें संकेंद्रीय छल्ले (Concentric Rings) होते हैं।\n" +
                                  "• **कारण**: अल्टरनेरिया सोलानी कवक (Fungal infection)।\n" +
                                  "• **उपचार और खुराक**: मैनकोजेब (Mancozeb) कवकनाशी 2 ग्राम प्रति लीटर पानी की दर से छिड़काव करें।\n" +
                                  "• **सुरक्षा सावधानी**: शांत मौसम में छिड़काव करें, रसायनों के सीधे संपर्क से बचें।")
                    } else if (lang == "es") {
                        sb.append("\n🥔 **Análisis de Enfermedades de Papa**:\n" +
                                  "• **Nombre de Enfermedad**: Tizón Temprano (Early Blight)\n" +
                                  "• **Nivel de Severidad**: 45% (Riesgo Medio)\n" +
                                  "• **Síntomas**: Puntos marrones con anillos concéntricos en las hojas más viejas.\n" +
                                  "• **Causa**: Hongo *Alternaria solani*.\n" +
                                  "• **Tratamiento y Dosis**: Pulverizar Mancozeb a dosis de 2g por litro de agua.\n" +
                                  "• **Precauciones**: Use equipo de protección básica durante el rociado.")
                    } else if (lang == "ta") {
                        sb.append("\n🥔 **உருளைக்கிழங்கு நோய் பகுப்பாய்வு**:\n" +
                                  "• **நோய் பெயர்**: உருளை அலி பிளைட் (முன்கூட்டியே கருகல்)\n" +
                                  "• **தீவிரம்**: 45% (மிதமான ஆபத்து)\n" +
                                  "• **அறிகுறிகள்**: பழைய இலைகளில் வளைய வடிவிலான சிறிய கரும்பழுப்பு புள்ளிகள்.\n" +
                                  "• **சிகிச்சை & அளவு**: ஒரு லிட்டர் தண்ணீருக்கு 2 கிராம் மேன்கோசெப் (Mancozeb) தெளிக்கவும்.")
                    } else if (lang == "te") {
                        sb.append("\n🥔 **బంగాళాదుంప తెగులు విశ్లేషణ**:\n" +
                                  "• **తెగులు పేరు**: ఎర్లీ బ్లైట్ (ముందస్తు మచ్చ తెగులు)\n" +
                                  "• **తీవ్రత**: 45% (మధ్యస్థ ప్రమాదం)\n" +
                                  "• **లక్షణాలు**: పాత ఆకులపై ఏకకేంద్రక వలయాలు (concentric rings) కలిగిన చిన్న గోధుమ రంగు మచ్చలు.\n" +
                                  "• **చికిత్స**: లీటరు నీటికి 2 గ్రాముల మ్యాంకోజెబ్ (Mancozeb) పిచికారీ చేయండి.")
                    } else if (lang == "kn") {
                        sb.append("\n🥔 **ಆಲೂಗೆಡ್ಡೆ ರೋಗ ವಿಶ್ಲೇಷಣೆ**:\n" +
                                  "• **ರೋಗದ ಹೆಸರು**: ಅರ್ಲಿ ಬ್ಲೈಟ್ (ಮುಂಚಿನ ಕರುಗಲು ರೋಗ)\n" +
                                  "• **ತೀವ್ರತೆ**: 45% (ಮಧ್ಯಮ ಅಪಾಯ)\n" +
                                  "• **ಲಕ್ಷಣಗಳು**: ಹಳೆಯ ಎಲೆಗಳ ಮೇಲೆ ಏಕಕೇಂದ್ರಕ ವಲಯಗಳನ್ನು ಹೊಂದಿರುವ ಸಣ್ಣ ಕಂದು ಕಲೆಗಳು.\n" +
                                  "• **ಚಿಕಿತ್ಸೆ**: ಲೀಟರ್ ನೀರಿಗೆ 2 ಗ್ರಾಂ ಮ್ಯಾಂಕೋಜೆಬ್ (Mancozeb) ಸಿಂಪಡಿಸಿ.")
                    } else if (lang == "mr") {
                        sb.append("\n🥔 **बटाटा रोग विश्लेषण**:\n" +
                                  "• **रोगाचे नाव**: अर्ली ब्लाइट (अगेती करपा)\n" +
                                  "• **तीव्रता**: ४५% (मध्यम धोका)\n" +
                                  "• **लक्षणे**: जुन्या पानांवर गोलाकार कड्यांसारखे लहान तपकिरी डाग पडणे.\n" +
                                  "• **उपचार**: मॅनकोझेब २ ग्रॅम प्रति लीटर पाण्यात मिसळून फवारणी करावी.")
                    } else if (lang == "bn") {
                        sb.append("\n🥔 **আলু রোগ বিশ্লেষণ**:\n" +
                                  "• **রোগের নাম**: আর্লি ব্লাইট (আগে ঝলসানো রোগ)\n" +
                                  "• **তীব্রতা**: ৪৫% (মাঝারি ঝুঁকি)\n" +
                                  "• **লক্ষণ**: বয়স্ক পাতায় রিং আকৃতির ছোট বাদামী দাগ।\n" +
                                  "• **চিকিৎসা**: প্রতি লিটার জলে ২ গ্রাম ম্যানকোজেব (Mancozeb) ছত্রাকনাশক স্প্রে করুন।")
                    } else {
                        sb.append("\n🥔 **Potato Disease Analysis**:\n" +
                                  "• **Disease Name**: Early Blight (Fungal)\n" +
                                  "• **Severity Level**: 45% (Medium Risk)\n" +
                                  "• **Symptoms**: Small, dark brown spots on older leaves expanding into concentric target-board rings.\n" +
                                  "• **Causes**: *Alternaria solani* fungal pathogen triggered by wet/dry cycles.\n" +
                                  "• **Treatment & Dosage**: Spray Mancozeb fungicide at 2g per Liter of water preventively.\n" +
                                  "• **Safety Precautions**: Wear protective clothing and wash hands thoroughly after application.")
                    }
                }
                "Rice" -> {
                    if (lang == "hi") {
                        sb.append("\n🌾 **धान रोग विश्लेषण**:\n" +
                                  "• **रोग का नाम**: धान का झोंका रोग (Rice Blast)\n" +
                                  "• **तीव्रता स्तर**: 80% (उच्च जोखिम)\n" +
                                  "• **लक्षण**: पत्तियों पर आंख/धुरी के आकार के धब्बे जिनके बीच का हिस्सा धूसर और किनारे भूरे होते हैं।\n" +
                                  "• **कारण**: मैग्नापोर्थी ओराइजी कवक (Fungal infection).\n" +
                                  "• **उपचार और खुराक**: ट्राइसाइक्लाजोल (Tricyclazole) 75WP 0.6 ग्राम प्रति लीटर पानी में मिलाकर छिड़काव करें।\n" +
                                  "• **सुरक्षा सावधानी**: छिड़काव करते समय सुरक्षात्मक चश्मा और मास्क का उपयोग करें।")
                    } else if (lang == "es") {
                        sb.append("\n🌾 **Análisis de Enfermedades de Arroz**:\n" +
                                  "• **Nombre de Enfermedad**: Añublo del Arroz (Rice Blast)\n" +
                                  "• **Nivel de Severidad**: 80% (Riesgo Alto)\n" +
                                  "• **Síntomas**: Lesiones en forma de diamante con centros grises y bordes marrón rojizo.\n" +
                                  "• **Causa**: Hongo *Magnaporthe oryzae*.\n" +
                                  "• **Tratamiento y Dosis**: Aplicar Triciclazol a dosis de 0.6g por litro de agua.\n" +
                                  "• **Precauciones**: Evite la inhalación de la niebla de pulverización.")
                    } else if (lang == "ta") {
                        sb.append("\n🌾 **நெல் நோய் பகுப்பாய்வு**:\n" +
                                  "• **நோய் பெயர்**: நெல் குலை நோய் (Rice Blast)\n" +
                                  "• **தீவிரம்**: 80% (அதிக ஆபத்து)\n" +
                                  "• **அறிகுறிகள்**: இலைகளில் சாம்பல் நிற மையமும் பழுப்பு நிற ஓரமும் கொண்ட வைர வடிவிலான புள்ளிகள்.\n" +
                                  "• **சிகிச்சை**: ட்ரைசைக்ளசோல் (Tricyclazole) 0.6 கிராம் ஒரு லிட்டர் நீரில் தெளிக்கவும்.")
                    } else if (lang == "te") {
                        sb.append("\n🌾 **వరి తెగులు విశ్లేషణ**:\n" +
                                  "• **తెగులు పేరు**: అగ్గి తెగులు (Rice Blast)\n" +
                                  "• **తీవ్రత**: 80% (అధిక ప్రమాదం)\n" +
                                  "• **లక్షణాలు**: ఆకులపై మధ్యలో బూడిద రంగు, అంచులలో ఎరుపు-గోధుమ రంగు గల నూలు కండె (diamond) ఆకారపు మచ్చలు.\n" +
                                  "• **చికిత్స**: లీటరు నీటికి 0.6 గ్రాముల ట్రైసైక్లాజోల్ (Tricyclazole) పిచికారీ చేయండి.")
                    } else if (lang == "kn") {
                        sb.append("\n🌾 **ಭತ್ತದ ರೋಗ ವಿಶ್ಲೇಷಣೆ**:\n" +
                                  "• **ರೋಗದ ಹೆಸರು**: ಬೆಂಕಿ ರೋಗ (Rice Blast)\n" +
                                  "• **ತೀವ್ರತೆ**: 80% (ಹೆಚ್ಚಿನ ಅಪಾಯ)\n" +
                                  "• **ಲಕ್ಷಣಗಳು**: ಎಲೆಗಳ ಮೇಲೆ ನೂಲು ಕಲೆಯಂತಹ ಆಕಾರದ ಬೂದು ಬಣ್ಣದ ಚುಕ್ಕೆಗಳು.\n" +
                                  "• **ಚಿಕಿತ್ಸೆ**: ಲೀಟರ್ ನೀರಿಗೆ 0.6 ಗ್ರಾಂ ಟ್ರೈಸೈಕ್ಲಾಜೋಲ್ ಸಿಂಪಡಿಸಿ.")
                    } else if (lang == "mr") {
                        sb.append("\n🌾 **तांदूळ/भात रोग विश्लेषण**:\n" +
                                  "• **रोगाचे नाव**: करपा (Rice Blast)\n" +
                                  "• **तीव्रता**: ८०% (उच्च धोका)\n" +
                                  "• **लक्षणे**: पानांवर लांबट डोळ्यासारखे किंवा हिऱ्यासारखे करडे डाग पडणे.\n" +
                                  "• **उपचार**: ट्रायसायक्लॅझोल ०.६ ग्रॅम प्रति लीटर पाण्यात फवारणी करावी.")
                    } else if (lang == "bn") {
                        sb.append("\n🌾 **ধানের রোগ বিশ্লেষণ**:\n" +
                                  "• **রোগের নাম**: ব্লাস্ট রোগ (Rice Blast)\n" +
                                  "• **তীব্রতা**: ৮০% (উচ্চ ঝুঁকি)\n" +
                                  "• **লক্ষণ**: পাতায় চোখের মতো ধূসর দাগ যার কিনারা বাদামী।\n" +
                                  "• **চিকিৎসা**: প্রতি লিটার জলে ০.৬ গ্রাম ট্রাইসাইক্লাজোল স্প্রে করুন।")
                    } else {
                        sb.append("\n🌾 **Rice Disease Analysis**:\n" +
                                  "• **Disease Name**: Rice Blast (Fungal)\n" +
                                  "• **Severity Level**: 80% (High Risk)\n" +
                                  "• **Symptoms**: Diamond-shaped lesions with gray centers and reddish-brown margins.\n" +
                                  "• **Treatment**: Spray Tricyclazole at 0.6g per Liter of water.")
                    }
                }
                "Wheat" -> {
                    if (lang == "hi") {
                        sb.append("\n🌾 **गेहूं रोग विश्लेषण**:\n" +
                                  "• **रोग का नाम**: पत्ती रस्ट (Leaf Rust)\n" +
                                  "• **तीव्रता स्तर**: 50% (मध्यम जोखिम)\n" +
                                  "• **उपचार**: प्रोपिकोनाज़ोल 1 मिली प्रति लीटर पानी की दर से छिड़काव करें।")
                    } else if (lang == "ta") {
                        sb.append("\n🌾 **கோதுமை நோய் பகுப்பாய்வு**:\n" +
                                  "• **நோய் பெயர்**: இலை துரு நோய் (Leaf Rust)\n" +
                                  "• **சிகிச்சை**: புரோபிகோனசோல் 1 மி.லி ஒரு லிட்டர் நீரில் தெளிக்கவும்।")
                    } else if (lang == "te") {
                        sb.append("\n🌾 **గోధుమ తెగులు విశ్లేషణ**:\n" +
                                  "• **తెగులు పేరు**: ఆకు తుప్పు తెగులు (Leaf Rust)\n" +
                                  "• **చికిత్స**: లీటరు నీటికి 1 మి.లీ ప్రొపికోనజోల్ పిచికారీ చేయండి।")
                    } else if (lang == "kn") {
                        sb.append("\n🌾 **ಗೋಧಿ ರೋಗ ವಿಶ್ಲೇಷಣೆ**:\n" +
                                  "• **ರೋಗದ ಹೆಸರು**: ಎಲೆ ತುಕ್ಕು ರೋಗ (Leaf Rust)\n" +
                                  "• **ಚಿಕಿತ್ಸೆ**: ಲೀಟರ್ ನೀರಿಗೆ 1 ಮಿಲಿ ಪ್ರೊಪಿಕೊನಾಜೋಲ್ ಸಿಂಪಡಿಸಿ।")
                    } else if (lang == "mr") {
                        sb.append("\n🌾 **गहू रोग विश्लेषण**:\n" +
                                  "• **रोगाचे नाव**: तांबेरा (Leaf Rust)\n" +
                                  "• **उपचार**: प्रोपिकोनाझोल १ मिली प्रति लीटर पाण्यात फवारावे।")
                    } else if (lang == "bn") {
                        sb.append("\n🌾 **गमेर रोग विश्लेषण**:\n" +
                                  "• **রোগের নাম**: পাতার মরিচা রোগ (Leaf Rust)\n" +
                                  "• **চিকিৎসা**: প্রতি লিটার জলে ১ মিলি প্রোপিকোনাজোল স্প্রে করুন।")
                    } else {
                        sb.append("\n🌾 **Wheat Disease Analysis**:\n" +
                                  "• **Disease Name**: Leaf Rust (Fungal)\n" +
                                  "• **Severity Level**: 50% (Medium Risk)\n" +
                                  "• **Symptoms**: Orange-brown pustules on leaves.\n" +
                                  "• **Treatment**: Spray Propiconazole at 1ml per Liter of water.")
                    }
                }
                "Corn" -> {
                    if (lang == "hi") {
                        sb.append("\n🌽 **मक्का रोग विश्लेषण**:\n" +
                                  "• **रोग का नाम**: पत्ती झुलसा (Leaf Blight)\n" +
                                  "• **उपचार**: मेन्कोजेब 2 ग्राम प्रति लीटर पानी में मिलाकर छिड़काव करें।")
                    } else if (lang == "ta") {
                        sb.append("\n🌽 **சோளம் நோய் பகுப்பாய்வு**:\n" +
                                  "• **நோய் பெயர்**: இலை கருகல் நோய் (Leaf Blight)\n" +
                                  "• **சிகிச்சை**: மேன்கோசெப் 2 கிராம் ஒரு லிட்டர் நீரில் தெளிக்கவும்।")
                    } else if (lang == "te") {
                        sb.append("\n🌽 **మొక్కజొన్న తెగులు విశ్లేషణ**:\n" +
                                  "• **తెగులు పేరు**: ఆకు కారుడు తెగులు (Leaf Blight)\n" +
                                  "• **చికిత్స**: లీటరు నీటికి 2 గ్రాముల మ్యాంకోజెబ్ పిచికారీ చేయండి।")
                    } else if (lang == "kn") {
                        sb.append("\n🌽 **ಮೆಕ್ಕೆಜೋಳ ರೋಗ ವಿಶ್ಲೇಷಣೆ**:\n" +
                                  "• **ರೋಗದ ಹೆಸರು**: ಎಲೆ ಕರುಗಲು ರೋಗ (Leaf Blight)\n" +
                                  "• **ಚಿಕಿತ್ಸೆ**: ಲೀಟರ್ ನೀರಿಗೆ 2 ಗ್ರಾಂ ಮ್ಯಾಂಕೋಜೆಬ್ ಸಿಂಪಡಿಸಿ।")
                    } else if (lang == "mr") {
                        sb.append("\n🌽 **मका रोग विश्लेषण**:\n" +
                                  "• **रोगाचे नाव**: पानावरील करपा (Leaf Blight)\n" +
                                  "• **उपचार**: मॅनकोझेब २ ग्रॅम प्रति लीटर पाण्यात फवारावे।")
                    } else if (lang == "bn") {
                        sb.append("\n🌽 **ভুট্টার রোগ विश्लेषण**:\n" +
                                  "• **রোগের নাম**: পাতার ঝলসা রোগ (Leaf Blight)\n" +
                                  "• **চিকিৎসা**: প্রতি লিটার জলে ২ গ্রাম ম্যানকোজেব স্প্রে করুন।")
                    } else {
                        sb.append("\n🌽 **Corn Disease Analysis**:\n" +
                                  "• **Disease Name**: Northern Corn Leaf Blight (Fungal)\n" +
                                  "• **Severity Level**: 55% (Medium Risk)\n" +
                                  "• **Symptoms**: Cigar-shaped grayish-green lesions on leaves.\n" +
                                  "• **Treatment**: Spray Mancozeb at 2g per Liter of water.")
                    }
                }
                "Cotton" -> {
                    if (lang == "hi") {
                        sb.append("\n🌱 **कपास रोग विश्लेषण**:\n" +
                                  "• **रोग का नाम**: पत्ती धब्बा (Leaf Spot)\n" +
                                  "• **उपचार**: कॉपर ऑक्सीक्लोराइड 2.5 ग्राम प्रति लीटर पानी में छिड़काव करें।")
                    } else if (lang == "ta") {
                        sb.append("\n🌱 **பருத்தி நோய் பகுப்பாய்வு**:\n" +
                                  "• **நோய் பெயர்**: இலைப்புள்ளி நோய் (Leaf Spot)\n" +
                                  "• **சிகிச்சை**: தாமிர ஆக்ஸிகுளோரைடு 2.5 கிராம் ஒரு லிட்டர் நீரில் தெளிக்கவும்।")
                    } else if (lang == "te") {
                        sb.append("\n🌱 **ప్రత్తి తెగులు విశ్లేషణ**:\n" +
                                  "• **తెగులు పేరు**: ఆకుమచ్చ తెగులు (Leaf Spot)\n" +
                                  "• **చికిత్స**: లీటరు నీటికి 2.5 గ్రాముల కాపర్ ఆక్సిక్లోరైడ్ పిచికారీ చేయండి।")
                    } else if (lang == "kn") {
                        sb.append("\n🌱 **ಹತ್ತಿ ರೋಗ ವಿಶ್ಲೇಷಣೆ**:\n" +
                                  "• **ರೋಗದ ಹೆಸರು**: ಎಲೆ ಚುಕ್ಕೆ ರೋಗ (Leaf Spot)\n" +
                                  "• **ಚಿಕಿತ್ಸೆ**: ಲೀಟರ್ ನೀರಿಗೆ 2.5 ಗ್ರಾಂ ತಾಮ್ರದ ಆಕ್ಸಿಕ್ಲೋರೈಡ್ ಸಿಂಪಡಿಸಿ।")
                    } else if (lang == "mr") {
                        sb.append("\n🌱 **कापूस रोग विश्लेषण**:\n" +
                                  "• **रोगाचे नाव**: पानावरील ठिपके (Leaf Spot)\n" +
                                  "• **उपचार**: कॉपर ऑक्सीक्लोराइड २.५ ग्रॅम प्रति लीटर पाण्यात फवारावे।")
                    } else if (lang == "bn") {
                        sb.append("\n🌱 **তুলার রোগ विश्लेषण**:\n" +
                                  "• **রোগের নাম**: পাতার দাগ রোগ (Leaf Spot)\n" +
                                  "• **চিকিৎসা**: প্রতি লিটার জলে ২.৫ গ্রাম কপার অক্সিক্লোরাইড স্প্রে করুন।")
                    } else {
                        sb.append("\n🌱 **Cotton Disease Analysis**:\n" +
                                  "• **Disease Name**: Alternaria Leaf Spot (Fungal)\n" +
                                  "• **Severity Level**: 40% (Low Risk)\n" +
                                  "• **Symptoms**: Small brown lesions on leaves.\n" +
                                  "• **Treatment**: Spray Copper Oxychloride at 2.5g per Liter of water.")
                    }
                }
                "Sugarcane" -> {
                    if (lang == "hi") {
                        sb.append("\n🎋 **गन्ना रोग विश्लेषण**:\n" +
                                  "• **रोग का नाम**: लाल सड़न (Red Rot)\n" +
                                  "• **उपचार**: संक्रमित पौधों को नष्ट करें और कवकनाशी का प्रयोग करें।")
                    } else if (lang == "ta") {
                        sb.append("\n🎋 **கரும்பு நோய் பகுப்பாய்வு**:\n" +
                                  "• **நோய் பெயர்**: செவ்வழுகல் நோய் (Red Rot)\n" +
                                  "• **சிகிச்சை**: பாதிக்கப்பட்ட பயிர்களை அகற்றி, தகுந்த பூஞ்சைக் கொல்லி பயன்படுத்தவும்।")
                    } else if (lang == "te") {
                        sb.append("\n🎋 **చెరకు తెగులు విశ్లేషణ**:\n" +
                                  "• **తెగులు పేరు**: ఎర్ర కుళ్ళు తెగులు (Red Rot)\n" +
                                  "• **చికిత్స**: తెగులు సోకిన మొక్కలను తొలగించి, తగిన శిలీంధ్ర నాశిని వాడండి।")
                    } else if (lang == "kn") {
                        sb.append("\n🎋 **ಕಬ್ಬು ರೋಗ ವಿಶ್ಲೇಷಣೆ**:\n" +
                                  "• **ರೋಗದ ಹೆಸರು**: ಕೆಂಪು ಕೊಳೆ ರೋಗ (Red Rot)\n" +
                                  "• **ಚಿಕಿತ್ಸೆ**: ರೋಗಪೀಡಿತ ಸಸ್ಯಗಳನ್ನು ನಾಶಪಡಿಸಿ ಸೂಕ್ತ ಬೂಷ್ಟುನಾಶಕ ಬಳಸಿ।")
                    } else if (lang == "mr") {
                        sb.append("\n🎋 **ऊस रोग विश्लेषण**:\n" +
                                  "• **रोगाचे नाव**: लाल कुज (Red Rot)\n" +
                                  "• **उपचार**: बाधित पिके नष्ट करावीत आणि बोर्डो मिश्रण फवारावे।")
                    } else if (lang == "bn") {
                        sb.append("\n🎋 **আখের রোগ विश्लेषण**:\n" +
                                  "• **রোগের নাম**: লাল পচা রোগ (Red Rot)\n" +
                                  "• **চিকিৎসা**: আক্রান্ত গাছ ধ্বংস করুন এবং ছত্রাকনাশক ব্যবহার করুন।")
                    } else {
                        sb.append("\n🎋 **Sugarcane Disease Analysis**:\n" +
                                  "• **Disease Name**: Red Rot (Fungal)\n" +
                                  "• **Severity Level**: 75% (High Risk)\n" +
                                  "• **Symptoms**: Red split stalk internodes.\n" +
                                  "• **Treatment**: Use certified disease-free sets; spray copper fungicides.")
                    }
                }
                "Banana" -> {
                    if (lang == "hi") {
                        sb.append("\n🍌 **केला रोग विश्लेषण**:\n" +
                                  "• **रोग का नाम**: सिगाटोका पत्ती धब्बा (Sigatoka)\n" +
                                  "• **उपचार**: कार्बेन्डाजिम 1 ग्राम प्रति लीटर पानी में मिलाकर छिड़काव करें।")
                    } else if (lang == "ta") {
                        sb.append("\n🍌 **வாழை நோய் பகுப்பாய்வு**:\n" +
                                  "• **நோய் பெயர்**: சிகடோகா இலைப்புள்ளி (Sigatoka)\n" +
                                  "• **சிகிச்சை**: கார்பெண்டாசிம் 1 கிராம் ஒரு லிட்டர் நீரில் தெளிக்கவும்।")
                    } else if (lang == "te") {
                        sb.append("\n🍌 **అరటి తెగులు విశ్లేషణ**:\n" +
                                  "• **తెగులు పేరు**: సిగటోకా ఆకుమచ్చ తెగులు (Sigatoka)\n" +
                                  "• **చికిత్స**: లీటరు నీటికి 1 గ్రాము కార్బెండజిమ్ పిచికారీ చేయండి।")
                    } else if (lang == "kn") {
                        sb.append("\n🍌 **ಬಾಳೆಹಣ್ಣು ರೋಗ ವಿಶ್ಲೇಷಣೆ**:\n" +
                                  "• **ರೋಗದ ಹೆಸರು**: ಸಿಗಟೋಕಾ ಎಲೆ ಚುಕ್ಕೆ ರೋಗ (Sigatoka)\n" +
                                  "• **ಚಿಕಿತ್ಸೆ**: ಲೀಟರ್ ನೀರಿಗೆ 1 ಗ್ರಾಂ ಕಾರ್ಬೆಂಡಾಜಿಮ್ ಸಿಂಪಡಿಸಿ।")
                    } else if (lang == "mr") {
                        sb.append("\n🍌 **केळी रोग विश्लेषण**:\n" +
                                  "• **रोगाचे नाव**: सिगाटोका (Sigatoka)\n" +
                                  "• **उपचार**: कार्बेन्डाझिम १ ग्रॅम प्रति लीटर पाण्यात फवारावे।")
                    } else if (lang == "bn") {
                        sb.append("\n🍌 **কলার রোগ विश्लेषण**:\n" +
                                  "• **রোগের নাম**: সিগাতোকা পাতার দাগ রোগ (Sigatoka)\n" +
                                  "• **চিকিৎসা**: প্রতি লিটার জলে ১ গ্রাম কারবেনডাজিম স্প্রে করুন।")
                    } else {
                        sb.append("\n🍌 **Banana Disease Analysis**:\n" +
                                  "• **Disease Name**: Sigatoka Leaf Spot (Fungal)\n" +
                                  "• **Severity Level**: 60% (Medium Risk)\n" +
                                  "• **Symptoms**: Dark brown streaks on leaves.\n" +
                                  "• **Treatment**: Spray Carbendazim at 1g per Liter of water.")
                    }
                }
                "Mango" -> {
                    if (lang == "hi") {
                        sb.append("\n🥭 **आम रोग विश्लेषण**:\n" +
                                  "• **रोग का नाम**: एन्थ्रेक्नोज (Anthracnose)\n" +
                                  "• **उपचार**: कॉपर ऑक्सीक्लोराइड 2.5 ग्राम प्रति लीटर पानी में छिड़काव करें।")
                    } else if (lang == "ta") {
                        sb.append("\n🥭 **மாம்பழம் நோய் பகுப்பாய்வு**:\n" +
                                  "• **நோய் பெயர்**: ஆந்த்ராக்னஸ் (Anthracnose)\n" +
                                  "• **சிகிச்சை**: தாமிர ஆக்ஸிகுளோரைடு 2.5 கிராம் ஒரு லிட்டர் நீரில் தெளிக்கவும்।")
                    } else if (lang == "te") {
                        sb.append("\n🥭 **మామిడి తెగులు విశ్లేషణ**:\n" +
                                  "• **తెగులు పేరు**: ఆంత్రాక్నోస్ (మచ్చ తెగులు)\n" +
                                  "• **చికిత్స**: లీటరు నీటికి 2.5 గ్రాముల కాపర్ ఆక్సిక్లోరైడ్ పిచికారీ చేయండి।")
                    } else if (lang == "kn") {
                        sb.append("\n🥭 **ಮಾವಿನಹಣ್ಣು ರೋಗ ವಿಶ್ಲೇಷಣೆ**:\n" +
                                  "• **ರೋಗದ ಹೆಸರು**: ಆಂಥ್ರಾಕ್ನೋಸ್ ರೋಗ (Anthracnose)\n" +
                                  "• **ಚಿಕಿತ್ಸೆ**: ಲೀಟರ್ ನೀರಿಗೆ 2.5 ಗ್ರಾಂ ತಾಮ್ರದ ಆಕ್ಸಿಕ್ಲೋರೈಡ್ ಸಿಂಪಡಿಸಿ।")
                    } else if (lang == "mr") {
                        sb.append("\n🥭 **आंबा रोग विश्लेषण**:\n" +
                                  "• **रोगाचे नाव**: अंथ्रॅक्नोज (Anthracnose)\n" +
                                  "• **उपचार**: कॉपर ऑक्सीक्लोराइड २.५ ग्रॅम प्रति लीटर पाण्यात फवारावे।")
                    } else if (lang == "bn") {
                        sb.append("\n🥭 **আমের রোগ विश्लेषण**:\n" +
                                  "• **রোগের নাম**: অ্যানথ্রাকনোজ রোগ (Anthracnose)\n" +
                                  "• **চিকিৎসা**: প্রতি লিটার জলে ২.৫ গ্রাম কপার অক্সিক্লোরাইড স্প্রে করুন।")
                    } else {
                        sb.append("\n🥭 **Mango Disease Analysis**:\n" +
                                  "• **Disease Name**: Anthracnose (Fungal)\n" +
                                  "• **Severity Level**: 65% (Medium Risk)\n" +
                                  "• **Symptoms**: Dark brown spots on leaves and flowers.\n" +
                                  "• **Treatment**: Spray Copper Oxychloride at 2.5g per Liter of water.")
                    }
                }
            }
        } else if (isPest) {
            if (lang == "hi") {
                sb.append("\n🐛 **कीट नियंत्रण (Pest Control)**:\n" +
                          "• कीट रस चूसकर या पत्तियों को खाकर फसलों को नुकसान पहुंचाते हैं।\n" +
                          "• **समाधान**: जैविक नीम तेल (15 मिली नीम तेल + 5 मिली लिक्विड सोप प्रति लीटर पानी) का छिड़काव करें।\n" +
                          "• **सावधानी**: सुबह हवा शांत होने पर छिड़काव करें।")
            } else if (lang == "es") {
                sb.append("\n🐛 **Control de Plagas**:\n" +
                          "• Las plagas dañan las hojas succionando la savia o devorándolas.\n" +
                          "• **Solución**: Pulverice aceite de neem orgánico (15ml de aceite de neem + 5ml de jabón líquido por litro de agua). Coloque trampas adhesivas amarillas.\n" +
                          "• **Precaución**: Use guantes al mezclar jabón insecticida.")
                } else if (lang == "ta") {
                    sb.append("\n🐛 **பூச்சி மேலாண்மை**:\n" +
                              "• பூச்சிகள் இலைகளை கடித்தோ அல்லது சாற்றை உறிஞ்சியோ பயிர்களை சேதப்படுத்துகின்றன.\n" +
                              "• **தீர்வு**: சாறு உறிஞ்சும் பூச்சிகளுக்கு வேப்ப எண்ணெய் கரைசல் (15 மி.லி வேப்பெண்ணெய் + 5 மி.லி சோப்பு 1 லிட்டர் நீரில்) தெளிக்கவும். கம்பளிப்பூச்சிகளை கைகளால் அப்புறப்படுத்தலாம் அல்லது பிடி (Bt) பூச்சிக்கொல்லி தெளிக்கலாம்.\n" +
                              "• **பாதுகாப்பு**: தெளிக்கும் போது காற்றின் திசையை கவனிக்கவும்.")
                } else if (lang == "te") {
                    sb.append("\n🐛 **తెగులు మరియు కీటక నివారణ**:\n" +
                              "• పురుగులు ఆకులను తినడం లేదా రసం పీల్చడం ద్వారా పంటను దెబ్బతీస్తాయి.\n" +
                              "• **పరిష్కారం**: రసం పీల్చే పురుగుల నివారణకు సేంద్రీయ వేప నూనె (15 మి.లీ వేప నూనె + 5 మి.లీ లిక్విడ్ సోప్ 1 లీటరు నీటిలో) పిచికారీ చేయండి. లార్వాల నివారణకు బిటి (Bt) జీవ కీటకనాశిని వాడండి.\n" +
                              "• **జాగ్రత్త**: పిచికారీ చేసేటప్పుడు గాలి வீచే దిశను గమనించండి.")
                } else if (lang == "kn") {
                    sb.append("\n🐛 **ಕೀಟ ನಿರ್ವಹಣೆ ಮತ್ತು ಬೆಳೆ ರಕ್ಷಣೆ**:\n" +
                              "• ಕೀಟಗಳು ಎಲೆಗಳನ್ನು ಕಡಿಯುವ ಮೂಲಕ ಅಥವಾ ರಸ ಹೀರುವ ಮೂಲಕ ಬೆಳೆಗಳನ್ನು ಹಾನಿಗೊಳಿಸುತ್ತವೆ.\n" +
                              "• **ಪರಿಹಾರ**: ರಸ ಹೀರುವ ಕೀಟಗಳಿಗೆ ಸಾವಯವ ಬೇವಿನ ಎಣ್ಣೆ ದ್ರಾವಣ (15 ಮಿಲಿ ಬೇವಿನ ಎಣ್ಣೆ + 5 ಮಿಲಿ ಸೋಪು 1 ಲೀಟರ್ ನೀರಿನಲ್ಲಿ) ಸಿಂಪಡಿಸಿ. ಕ್ಯಾಟರ್ಪಿಲ್ಲರ್ ನಿಯಂತ್ರಣಕ್ಕೆ ಹ್ಯಾಂಡ್ ಪಿಕಿಂಗ್ ಅಥವಾ ಸಾವಯವ ಬಿಟಿ (Bt) ಸಿಂಪರಣೆ ಸೂಕ್ತ.\n" +
                              "• **ಸುರಕ್ಷತೆ**: ಗಾಳಿ ಬೀಸುವ ವಿರುದ್ಧ ದಿಕ್ಕಿನಲ್ಲಿ ಸಿಂಪಡಿಸಬೇಡಿ.")
                } else if (lang == "mr") {
                    sb.append("\n🐛 **कीड व्यवस्थापन आणि पीक संरक्षण**:\n" +
                              "• किडे पानांमधील रस शोषून किंवा पाने खाऊन पिकांचे नुकसान करतात.\n" +
                              "• **उपाय**: रस शोषणाऱ्या किडींसाठी सेंद्रिय कडुनिंबाचे तेल (१५ मिली निंबोळी तेल + ५ मिली साबण १ लीटर पाण्यात) फवारावे. अळीच्या नियंत्रणासाठी सेंद्रिय बीटी (Bt) कीटकनाशकाचा वापर करा.\n" +
                              "• **खबरदारी**: फवारणी करताना वाऱ्याचा वेग आणि दिशा तपासा.")
                } else if (lang == "bn") {
                    sb.append("\n🐛 **কীটপতঙ্গ দমন ও ফসল সুরক্ষা**:\n" +
                              "• পোকা পাতার রস চুষে বা পাতা খেয়ে ফসলের ক্ষতি করে।\n" +
                              "• **সমাধান**: চোষক পোকা দমনের জন্য নিম তেল (১৫ মিলি নিম তেল + ৫ মিলি তরল সাবান ১ লিটার জলে) স্প্রে করুন। শুঁয়োপোকা দমনে হাত দিয়ে বাছাই করা বা জৈব বিটি (Bt) কীটনাশক ব্যবহার করুন।\n" +
                              "• **নিরাপত্তা**: বাতাস যেদিকে বইছে সেদিকে মুখ করে স্প্রে করবেন না।")
                } else {
                    sb.append("\n🐛 **Pest Management & Crop Damage**:\n" +
                              "• Insects damage crops by sucking sap (whiteflies, aphids) or chewing foliage (caterpillars, beetles).\n" +
                              "• **Action Plan**: Spray organic Neem Oil solution (15ml neem oil + 5ml liquid soap in 1L of water) to combat sucking pests. For caterpillars, manual handpicking or biological Bt (Bacillus thuringiensis) spray is highly effective.\n" +
                              "• **Safety precautions**: Spray in windless conditions to avoid spray drifting into water bodies or non-target zones.")
                }
            } else if (isWatering) {
                if (lang == "hi") {
                    sb.append("\n💧 **सिंचाई प्रबंधन (Irrigation)**:\n" +
                              "• जड़ों की सड़न और फंगल रोगों से बचने के लिए ड्रिप सिंचाई (Drip Irrigation) सबसे उत्तम है।\n" +
                              "• **सुझाव**: हमेशा सुबह जल्दी पानी दें ताकि धूप निकलने तक पत्तियां सूख जाएं। जलभराव (waterlogging) से बचें।")
                } else if (lang == "es") {
                    sb.append("\n💧 **Prácticas de Riego Recomendadas**:\n" +
                              "• El riego por goteo es ideal para evitar la humedad excesiva en las hojas y prevenir pudriciones radiculares.\n" +
                              "• **Consejo**: Riegue temprano por la mañana. Evite el encharcamiento que causa estrés ambiental.")
                } else if (lang == "ta") {
                    sb.append("\n💧 **நீர் மேலாண்மை**:\n" +
                              "• வேர் அழுகல் மற்றும் பூஞ்சை நோய்களைத் தவிர்க்க சொட்டு நீர் பாசனம் (Drip Irrigation) மிகவும் சிறந்தது.\n" +
                              "• **பரிந்துரை**: இலைகள் விரைவில் உலர ஏதுவாக எப்போதும் அதிகாலையில் நீர் பாய்ச்சவும். நீர் தேங்குவதைத் தவிர்க்கவும்.")
                } else if (lang == "te") {
                    sb.append("\n💧 **నీటి యాజమాన్యం**:\n" +
                              "• వేరు కుళ్ళు తెగులు మరియు బూజు తెగుళ్లను నివారించడానికి డ్రిప్ నీటి పారుదల చాలా ఉపయోగకరం.\n" +
                              "• **సూచన**: ఉదయాన్నే నీరు పెట్టడం వల్ల ఆకులు త్వరగా ఆరిపోతాయి. నీరు నిల్వ ఉండకుండా చూసుకోండి.")
                } else if (lang == "kn") {
                    sb.append("\n💧 **ನೀರಾವರಿ ನಿರ್ವಹಣೆ**:\n" +
                              "• ಬೇರು ಕೊಳೆಯುವಿಕೆ ಮತ್ತು ಬೂಷ್ಟು ರೋಗಗಳನ್ನು ತಡೆಗಟ್ಟಲು ಹನಿ ನೀರಾವರಿ (Drip Irrigation) ಅತ್ಯುತ್ತಮ ವಿಧಾನ.\n" +
                              "• **ಸಲಹೆ**: ಎಲೆಗಳು ಬೇಗನೆ ಒಣಗಲು ಮುಂಜಾನೆ ನೀರುಣಿಸಿ. ಜೌಗು ಮಣ್ಣು ಅಥವಾ ನೀರು ನಿಲ್ಲುವುದನ್ನು ತಪ್ಪಿಸಿ.")
                } else if (lang == "mr") {
                    sb.append("\n💧 **सिंचन व्यवस्थापन**:\n" +
                              "• मूळ कुजणे आणि बुरशीजन्य रोग टाळण्यासाठी ठिबक सिंचन (Drip Irrigation) सर्वोत्तम आहे.\n" +
                              "• **सल्ला**: पाने लवकर सुकण्यासाठी सकाळी लवकर पाणी द्यावे. शेतात पाणी साचू देऊ नका (जलभराव टाळा).")
                } else if (lang == "bn") {
                    sb.append("\n💧 **সেচ ব্যবস্থাপনা**:\n" +
                              "• গোড়া পচা এবং ছত্রাকজনিত রোগ এড়াতে ড্রিপ সেচ (Drip Irrigation) সবচেয়ে উপযুক্ত।\n" +
                              "• **পরামর্শ**: সকালে জল দিন যাতে রোদে পাতা দ্রুত শুকিয়ে যায়। জল জমা বা জলাবদ্ধতা এড়িয়ে চলুন।")
                } else {
                    sb.append("\n💧 **Irrigation & Water Management**:\n" +
                              "• Proper watering avoids waterlogging (environmental stress) and dry spells.\n" +
                              "• **Action Plan**: Utilize drip irrigation to target the roots directly. Water early in the morning so that leaf surfaces dry up quickly, preventing fungal spore activation.")
                }
            } else if (isFertilizer) {
                if (lang == "hi") {
                    sb.append("\n🌱 **उर्वरक प्रबंधन (Fertilization)**:\n" +
                              "• शुरुआती विकास चरण में पत्ती वृद्धि के लिए नाइट्रोजन-युक्त खाद (जैसे यूरिया) दें।\n" +
                              "• **सुझाव**: फूल और फल आने पर पोटेशियम और फास्फोरस से भरपूर NPK खाद का छिड़काव करें। पोषक तत्वों की कमी (Nutrient Deficiency) से पत्तियां पीली पड़ जाती हैं।")
                } else if (lang == "es") {
                    sb.append("\n🌱 **Plan de Fertilización y Nutrición**:\n" +
                              "• Use abonos ricos en nitrógeno (como urea) en la etapa vegetativa. Cambie a fórmulas ricas en fósforo y potasio (NPK) durante la floración.\n" +
                              "• **Consejo**: La deficiencia de nutrientes provoca hojas amarillas y caída prematura de frutos.")
                } else if (lang == "ta") {
                    sb.append("\n🌱 **உர மேலாண்மை**:\n" +
                              "• பயிரின் ஆரம்ப வளர்ச்சி நிலையில் இலை வளர்ச்சிக்கு நைட்ரஜன் உரங்களை (யுரியா) இடவும்.\n" +
                              "• **பரிந்துரை**: பூக்கும் மற்றும் காய்க்கும் தருணத்தில் பொட்டாசியம் மற்றும் பாஸ்பரஸ் நிறைந்த NPK உரங்களை தெளிக்கவும். ஊட்டச்சத்து குறைபாட்டால் இலைகள் மஞ்சள் நிறமாக மாறும்.")
                } else if (lang == "te") {
                    sb.append("\n🌱 **ఎరువుల యాజమాన్యం**:\n" +
                              "• పంట ఎదుగుదల ప్రారంభ దశలో ఆకులు బలంగా పెరగడానికి నత్రజని (యూరియా) అందించండి.\n" +
                              "• **సూచన**: పూత మరియు పిందె దశలలో పొటాషియం, భాస్వరం అధికంగా ఉండే NPK ఎరువులు పిచికారీ చేయండి. పోషకాల లోపం వల్ల ఆకులు పసుపు రంగులోకి మారుతాయి.")
                } else if (lang == "kn") {
                    sb.append("\n🌱 **ಗೊಬ್ಬರ ಮತ್ತು ಮಣ್ಣಿನ ಪೋಷಣೆ**:\n" +
                              "• ಆರಂಭಿಕ ಬೆಳವಣಿಗೆಯ ಹಂತದಲ್ಲಿ ಎಲೆಗಳ ಬೆಳವಣಿಗೆಗೆ ಸಾರಜನಕ ಯುಕ್ತ ಗೊಬ್ಬರ (ಯೂರಿಯಾ) ನೀಡಿ.\n" +
                              "• **ಸಲಹೆ**: ಹೂವು ಮತ್ತು ಕಾಯಿ ಬಿಡುವ ಹಂತದಲ್ಲಿ ಪೊಟ್ಯಾಸಿಯಮ್ ಮತ್ತು ರಂಜಕ ಸಮೃದ್ಧವಾಗಿರುವ NPK ಗೊಬ್ಬರವನ್ನು ಬಳಸಿ. ಪೋಷಕಾಂಶಗಳ ಕೊರತೆಯಿಂದ ಎಲೆಗಳು ಹಳದಿಯಾಗುತ್ತವೆ.")
                } else if (lang == "mr") {
                    sb.append("\n🌱 **खत आणि पोषण व्यवस्थापन**:\n" +
                              "• पिकाच्या सुरुवातीच्या वाढीच्या टप्प्यात पानांच्या वाढीसाठी नत्रयुक्त खते (उदा. युरिया) द्यावीत.\n" +
                              "• **सल्ला**: फुले आणि फळे लागण्याच्या काळात पोटॅश आणि फॉस्फरसयुक्त NPK खतांचा वापर करावा. पोषक तत्वांच्या कमतरतेमुळे पाने पिवळी पडतात.")
                } else if (lang == "bn") {
                    sb.append("\n🌱 **সার ও পুষ্টি ব্যবস্থাপনা**:\n" +
                              "• গাছের প্রাথমিক বৃদ্ধির পর্যায়ে পাতার বৃদ্ধির জন্য নাইট্রোজেনযুক্ত সার (যেমন ইউরিয়া) প্রয়োগ করুন।\n" +
                              "• **পরামর্শ**: ফুল ও ফল আসার সময় পটাশিয়াম ও ফসফরাস সমৃদ্ধ NPK সার স্প্রে করুন। পুষ্টির ঘাটতি হলে পাতা হলুদ হয়ে যায়।")
                } else {
                    sb.append("\n🌱 **Fertilization & Soil Health**:\n" +
                              "• Nutrient deficiency impairs plant defense, inviting fungal/bacterial infections.\n" +
                              "• **Action Plan**: Apply nitrogen-rich fertilizers (like Urea or manure) during vegetative growth. Switch to phosphorus & potassium-rich (NPK) blends at the flowering stage.")
                }
            } else {
                if (lang == "hi") {
                    sb.append("\n🌾 **फसल सुरक्षा समाधान**:\n" +
                              "1. **कवक/जीवाणु**: पत्तियों पर पीले धब्बे दिखने पर तांबे (Copper) पर आधारित कवकनाशी का प्रयोग करें।\n" +
                              "2. **सफाई**: खेत से खरपतवार और संक्रमित फसलों को तुरंत हटाएं और जलाएं।\n" +
                              "3. **मिट्टी परीक्षण**: मिट्टी की अम्लता (pH) 6.0 से 7.0 के बीच बनाए रखें।")
                } else if (lang == "es") {
                    sb.append("\n🌾 **Guía General de Manejo de Cultivos**:\n" +
                              "1. **Enfermedades**: Elimine hojas dañadas inmediatamente para frenar hongos o bacterias.\n" +
                              "2. **Higiene**: Mantenga el campo libre de malezas que sirven de hospederas.\n" +
                              "3. **Suelo**: Asegure un buen drenaje para evitar la asfixia radicular.")
                } else if (lang == "ta") {
                    sb.append("\n🌾 **பொதுவான பயிர் பாதுகாப்பு வழிகாட்டுதல்கள்**:\n" +
                              "1. **கண்காணிப்பு**: இலைகளில் புள்ளிகள் உள்ளதா என தொடர்ந்து கண்காணிக்கவும்.\n" +
                              "2. **சுகாதாரம்**: பாதிக்கப்பட்ட பயிர் பாகங்களை அகற்றி உடனடியாக அழிக்கவும்.\n" +
                              "3. **சாகுபடி முறை**: மண்ணின் ஆரோக்கியத்தை மேம்படுத்த பயிர் சுழற்சி முறையை கடைபிடிக்கவும்.")
                } else if (lang == "te") {
                    sb.append("\n🌾 **సాధారణ పంట రక్షణ మార్గదర్శకాలు**:\n" +
                              "1. **పర్యవేక్షణ**: ఆకులపై మచ్చలను ఎప్పటికప్పుడు గమనిస్తూ ఉండండి.\n" +
                              "2. **శుభ్రత**: తెగులు సోకిన భాగాలను కత్తిరించి వెంటనే నాశనం చేయండి.\n" +
                              "3. **నేల యాజమాన్యం**: పోషకాలను పెంచడానికి లెగ్యూమ్ జాతి పంటలతో పంట మార్పిడి చేయండి.")
                } else if (lang == "kn") {
                    sb.append("\n🌾 **ಸಾಮಾನ್ಯ ಬೆಳೆ ಆರೋಗ್ಯ ಮಾರ್ಗಸೂಚಿಗಳು**:\n" +
                              "1. **ಪರಿಶೀಲನೆ**: ಎಲೆಗಳ ಮೇಲಿನ ಕಲೆಗಳು ಮತ್ತು ರೋಗಲಕ್ಷಣಗಳನ್ನು ನಿಯಮಿತವಾಗಿ ಗಮನಿಸಿ.\n" +
                              "2. **ನೈರ್ಮಲ್ಯ**: ರೋಗಪೀಡಿತ ಸಸ್ಯದ ಭಾಗಗಳನ್ನು ಕತ್ತರಿಸಿ ಸುಟ್ಟು ಹಾಕಿ.\n" +
                              "3. **ಬೆಳೆ ಪದ್ಧತಿ**: ಕೀಟಗಳ ಚಕ್ರವನ್ನು ನಿಯಂತ್ರಿಸಲು ದ್ವಿದಳ ಧಾನ್ಯಗಳೊಂದಿಗೆ ಬೆಳೆ ತಿರುಗಾವಣೆ ಮಾಡಿ.")
                } else if (lang == "mr") {
                    sb.append("\n🌾 **सामान्य पीक व्यवस्थापन मार्गदर्शक तत्त्वे**:\n" +
                              "१. **निरीक्षण**: पानांवरील डाग आणि रोगाची लक्षणे नियमित तपासा.\n" +
                              "२. **स्वच्छता**: बाधित पिकांचे भाग कापून लगेच नष्ट करा जेणेकरून रोग पसरणार नाही.\n" +
                              "३. **पीक फेरपालट**: जमिनीची सुपीकता टिकवून ठेवण्यासाठी पिकांची फेरपालट करा.")
                } else if (lang == "bn") {
                    sb.append("\n🌾 **সাধারণ শস্য ব্যবস্থাপনা নির্দেশিকা**:\n" +
                              "১. **পর্যবেক্ষণ**: পাতায় দাগ বা কোনো রোগলক্ষণ আছে কিনা তা নিয়মিত পরীক্ষা করুন।\n" +
                              "২. **পরিচ্ছন্নতা**: আক্রান্ত উদ্ভিদের অংশ কেটে পুড়িয়ে ধ্বংস করুন যাতে রোগ না ছড়ায়।\n" +
                              "৩. **শস্য আবর্তন**: মাটির উর্বরতা বাড়াতে এবং কীটপতঙ্গ দমন করতে শস্য আবর্তন করুন।")
                } else {
                    sb.append("\n🌾 **General Crop Health Guidelines**:\n" +
                              "1. **Inspection**: Regularly scout the undersides of leaves for mites and spots (indicative of fungal/bacterial/viral infections).\n" +
                              "2. **Sanitation**: Prune and destroy diseased plant sections to prevent the pathogen from spreading.\n" +
                              "3. **Loss Reduction**: Adopt rotation with leguminous crops to disrupt pest cycles and naturally enrich soil nitrogen.")
            }
        }
 
        // 8. Call to action / Farmer friendly ending
        when (lang) {
            "hi" -> sb.append("\n\nफसलों की सुरक्षा और टिकाऊ खेती को हमेशा प्राथमिकता दें। क्या आप किसी अन्य लक्षण या निवारक उपायों के बारे में जानना चाहते हैं?")
            "es" -> sb.append("\n\nPriorice siempre la seguridad y la agricultura sostenible. ¿Desea conocer detalles sobre algún otro síntoma o medida de control?")
            "ta" -> sb.append("\n\nபாதுகாப்பான மற்றும் நிலையான விவசாயத்திற்கு எப்போதும் முன்னுரிமை கொடுங்கள். மேலும் ஏதேனும் அறிகுறிகள் அல்லது தடுப்பு நடவடிக்கைகள் பற்றி அறிய விரும்புகிறீர்களா?")
            "te" -> sb.append("\n\nఎల్లప్పుడూ రక్షణ మరియు స్థిరమైన వ్యవసాయానికి ప్రాధాన్యత ఇవ్వండి. మీకు ఇతర లక్షణాలు లేదా నివారణ చర్యల గురించి సమాచారం కావాలా?")
            "kn" -> sb.append("\n\nಯಾವಾಗಲೂ ಸುರಕ್ಷತೆ ಮತ್ತು ಸುಸ್ಥಿರ ಕೃಷಿಗೆ ಆದ್ಯತೆ ನೀಡಿ. ನೀವು ಇತರ ಯಾವುದೇ ರೋಗಲಕ್ಷಣಗಳು ಅಥವಾ ನಿವಾರಣಾ ಕ್ರಮಗಳ ಬಗ್ಗೆ ತಿಳಿಯಲು ಬಯಸುತ್ತೀರಾ?")
            "mr" -> sb.append("\n\nनेहमी सुरक्षितता आणि शाश्वत शेतीला प्राधान्य द्या. आपल्याला इतर कोणत्याही लक्षणांबद्दल किंवा प्रतिबंधात्मक उपायांबद्दल जाणून घ्यायचे आहे का?")
            "bn" -> sb.append("\n\nসবসময় নিরাপত্তা এবং টেকসই কৃষিকে অগ্রাধিকার দিন। আপনি কি অন্য কোনো লক্ষণ বা প্রতিরোধমূলক ব্যবস্থা সম্পর্কে জানতে চান?")
            else -> sb.append("\n\nAlways prioritize safety and sustainable farming. Would you like details on any other symptoms or preventative measures?")
        }
 
        return sb.toString()
    }
    


    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun scrollToBottom() {
        chatScrollView.post {
            chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
}
