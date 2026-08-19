package com.example.agroassist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GroqClient {
    private var customApiKey: String = "zjwbiuhkljcwwgz"

    // Supported Groq models in order of preference
    private val candidateModels = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x7b-32768",
        "gemma2-9b-it"
    )

    fun setApiKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isNotEmpty()) {
            customApiKey = trimmed
        }
    }

    fun getApiKey(): String {
        return customApiKey
    }

    suspend fun generateResponse(prompt: String, systemContext: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        
        if (apiKey.isEmpty()) {
            return@withContext getSmartLocalAgroResponse(prompt, systemContext)
        }

        for (modelName in candidateModels) {
            try {
                val result = callGroqModel(modelName, apiKey, prompt, systemContext)
                if (!result.isNullOrBlank()) {
                    return@withContext result
                }
            } catch (e: Exception) {
                // Continue to next fallback model
            }
        }

        // Return rich local expert fallback if all cloud models fail
        getSmartLocalAgroResponse(prompt, systemContext)
    }

    private fun callGroqModel(modelName: String, apiKey: String, prompt: String, systemContext: String): String? {
        val url = URL("https://api.groq.com/openai/v1/chat/completions")
        val urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.requestMethod = "POST"
        urlConnection.setRequestProperty("Content-Type", "application/json")
        urlConnection.setRequestProperty("Authorization", "Bearer $apiKey")
        urlConnection.connectTimeout = 6000
        urlConnection.readTimeout = 6000
        urlConnection.doOutput = true

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemContext)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val requestBody = JSONObject().apply {
            put("model", modelName)
            put("messages", messagesArray)
            put("temperature", 0.7)
            put("max_tokens", 800)
        }

        OutputStreamWriter(urlConnection.outputStream).use { writer ->
            writer.write(requestBody.toString())
            writer.flush()
        }

        val responseCode = urlConnection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseString = urlConnection.inputStream.bufferedReader().use { it.readText() }
            val responseJson = JSONObject(responseString)
            val choices = responseJson.getJSONArray("choices")
            if (choices.length() > 0) {
                val messageObj = choices.getJSONObject(0).getJSONObject("message")
                return messageObj.getString("content")
            }
        }
        return null
    }

    private fun getSmartLocalAgroResponse(prompt: String, systemContext: String): String {
        val lower = prompt.toLowerCase()
        return when {
            lower.contains("disease") || lower.contains("prevention") || lower.contains("blight") -> {
                "🌿 **AgroAI Crop Disease & Prevention Advisory**:\n\n" +
                "1. **Scouting & Inspection**: Regularly check undersides of lower leaves for spots or yellowing.\n" +
                "2. **Sanitation & Pruning**: Remove infected leaves immediately to prevent fungal spore spread.\n" +
                "3. **Crop Rotation**: Rotate with leguminous pulse crops to break pathogen cycles and replenish soil nitrogen.\n" +
                "4. **Organic Spraying**: Spray Neem oil (5ml/L) or copper oxychloride solution as a preventive shield."
            }
            lower.contains("fertilizer") || lower.contains("nutrient") || lower.contains("soil") -> {
                "🌱 **AgroAI Soil Nutrient & Fertilizer Guide**:\n\n" +
                "1. **Basal Application**: Apply well-decomposed FYM (Farm Yard Manure) or vermicompost before sowing.\n" +
                "2. **Balanced NPK**: Use NPK 19-19-19 for early foliage growth and NPK 10-26-26 during flowering.\n" +
                "3. **Micronutrient Care**: Spray Zinc Sulphate (0.5%) and Calcium Nitrate to prevent leaf curl and blossom end rot."
            }
            lower.contains("water") || lower.contains("irrigate") || lower.contains("drip") -> {
                "💧 **AgroAI Irrigation & Water Management**:\n\n" +
                "1. **Drip Efficiency**: Drip irrigation reduces water wastage by 40% and prevents leaf wetness diseases.\n" +
                "2. **Timing**: Irrigate early in the morning to reduce evaporation and fungal growth.\n" +
                "3. **Mulching**: Apply paddy straw mulching to conserve soil moisture."
            }
            else -> {
                "🌾 **AgroAI Smart Farming Assistant**:\n\n" +
                "Based on your farm profile and crop selection:\n\n" +
                "• **Crop Inspection**: Inspect foliage twice weekly for early signs of pests.\n" +
                "• **Soil Moisture**: Maintain optimal moisture; avoid waterlogging around roots.\n" +
                "• **Government Support**: Check active subsidies in our Government Schemes section for drip irrigation and seeds."
            }
        }
    }
}
