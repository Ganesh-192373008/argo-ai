package com.example.agroassist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {
    private var customApiKey: String? = null

    fun setApiKey(key: String) {
        customApiKey = key.trim().ifEmpty { null }
    }

    private fun getApiKey(): String {
        return customApiKey ?: "AQ.Ab8RN6LWyLn3FNjHcBBXxSAWre6ZMMLi_nEYl7FAvXwx-iBpow"
    }

    suspend fun generateResponse(prompt: String, systemContext: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            throw IllegalStateException("API Key is empty. Falling back to local expert system.")
        }
        
        try {
            val urlConnection = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey").openConnection() as HttpURLConnection
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.doOutput = true

            // Build payload using built-in Android JSONObject
            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemContext\n\nUser Question:\n$prompt\n\nAI Response:")
                            })
                        })
                    })
                })
            }

            OutputStreamWriter(urlConnection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = urlConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = urlConnection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseString)
                val candidates = responseJson.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        parts.getJSONObject(0).getString("text")
                    } else {
                        "No response from Gemini API."
                    }
                } else {
                    "No response from Gemini API."
                }
            } else {
                val errorString = urlConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw Exception("HTTP error $responseCode: $errorString")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
