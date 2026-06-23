package com.example.agroassist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object OpenAIClient {
    private var customApiKey: String? = null

    fun setApiKey(key: String) {
        customApiKey = key.trim().ifEmpty { null }
    }

    private fun getApiKey(): String {
        return customApiKey ?: "sk-proj-YourOpenAIApiKeyHere"
    }

    suspend fun generateResponse(prompt: String, systemContext: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey == "sk-proj-YourOpenAIApiKeyHere" || apiKey.isEmpty()) {
            throw IllegalStateException("OpenAI API Key is empty or placeholder. Falling back.")
        }
        
        try {
            val urlConnection = URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.setRequestProperty("Authorization", "Bearer $apiKey")
            urlConnection.doOutput = true

            // Build Chat Completions payload
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
                put("model", "gpt-4o-mini")
                put("messages", messagesArray)
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
                    messageObj.getString("content")
                } else {
                    "No response from OpenAI API."
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
