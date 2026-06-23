package com.example.agroassist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VoiceListeningActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var textListeningStatus: TextView
    private lateinit var textListeningQuery: TextView
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false

    private val requestRecordAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechRecognizer()
        } else {
            Toast.makeText(this, "Microphone permission is required for voice assistant", Toast.LENGTH_LONG).show()
            showFallbackUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_listening)

        val closeButton = findViewById<ImageView>(R.id.closeButton)
        closeButton.setOnClickListener { finish() }

        textListeningStatus = findViewById(R.id.textListeningStatus)
        textListeningQuery = findViewById(R.id.textListeningQuery)

        // Initialize Text to Speech
        tts = TextToSpeech(this, this)

        // Check and request microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognizer()
        } else {
            requestRecordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showFallbackUI()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                textListeningStatus.text = "Listening..."
                textListeningQuery.text = "Speak now..."
                isListening = true
            }

            override fun onBeginningOfSpeech() {
                textListeningStatus.text = "Listening..."
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                textListeningStatus.text = "Processing..."
            }

            override fun onError(error: Int) {
                // If it fails or times out (often in emulator), fall back gracefully
                if (isListening) {
                    showFallbackUI()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val query = matches[0]
                    processVoiceQuery(query)
                } else {
                    showFallbackUI()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    textListeningQuery.text = "\"${matches[0]}\""
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun showFallbackUI() {
        textListeningStatus.text = "Listening (Simulated)"
        textListeningQuery.text = "Saying: \"How should I water my crops?\""
        
        // Wait 2.5 seconds to simulate reply
        handler.postDelayed({
            processVoiceQuery("How should I water my crops?")
        }, 2500)
    }

    private fun processVoiceQuery(query: String) {
        textListeningStatus.text = "Thinking..."
        textListeningQuery.text = "\"Heard: $query\""

        CoroutineScope(Dispatchers.Main).launch {
            val dbHelper = AgroDatabaseHelper(this@VoiceListeningActivity)
            val profile = dbHelper.getProfile()
            val location = profile["location"]?.ifEmpty { "Noida, UP" } ?: "Noida, UP"
            val crops = profile["crops"]?.ifEmpty { "Tomato, Rice" } ?: "Tomato, Rice"

            val systemContext = "System Instruction: You are AgroAI, an intelligent agricultural assistant. Keep your response very concise (maximum 2 sentences) because it will be read aloud to the user via Text-to-Speech.\n" +
                    "Farmer Context:\n" +
                    "- Location: $location\n" +
                    "- Crops: $crops\n" +
                    "- Current Weather: 29°C, Humid, sunny/occasional showers\n"

            val prefs = getSharedPreferences("AgroAssistAIKeys", android.content.Context.MODE_PRIVATE)
            var geminiKey = prefs.getString("gemini_api_key", "") ?: ""
            var openaiKey = prefs.getString("openai_api_key", "") ?: ""
            
            if (geminiKey.trim().lowercase() == "hi") geminiKey = ""
            if (openaiKey.trim().lowercase() == "hi" || openaiKey.trim().startsWith("sk-...")) openaiKey = ""
            
            GeminiClient.setApiKey(geminiKey)
            OpenAIClient.setApiKey(openaiKey)

            val responseText = try {
                GeminiClient.generateResponse(query, systemContext)
            } catch (e: Exception) {
                try {
                    OpenAIClient.generateResponse(query, systemContext)
                } catch (ex: Exception) {
                    getLocalResponseFallback(query)
                }
            }

            textListeningStatus.text = "AgroAI Response"
            textListeningQuery.text = "\"Heard: $query\"\n\nAgroAI Response:\n$responseText"
            
            tts?.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, "GeminiVoiceID")
        }
    }

    private fun getLocalResponseFallback(query: String): String {
        val normalized = query.lowercase()
        return when {
            normalized.contains("water") || normalized.contains("irrigation") -> {
                "I recommend watering your crops early in the morning to keep foliage dry and reduce fungal diseases."
            }
            normalized.contains("tomato") -> {
                "For tomato crops, watch out for late blight. Apply copper fungicides or organic neem oil."
            }
            normalized.contains("potato") -> {
                "For potato crops, watch out for early blight and Colorado beetle. Handpick beetles or use Bt spray."
            }
            normalized.contains("pest") || normalized.contains("insect") || normalized.contains("bug") -> {
                "To control pests naturally, install yellow sticky traps or apply organic neem oil spray."
            }
            normalized.contains("fertilizer") || normalized.contains("urea") -> {
                "Apply nitrogen-rich fertilizer during vegetative growth and potassium during flowering stages."
            }
            else -> {
                "I heard your query. Please check soil moisture, monitor for pests, and ensure balanced nutrients."
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

