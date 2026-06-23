package com.example.agroassist

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun wrap(context: Context): Context {
        val prefs = context.getSharedPreferences("AgroAssistSettings", Context.MODE_PRIVATE)
        val lang = prefs.getString("app_language", "English") ?: "English"
        val langCode = when (lang) {
            "Hindi" -> "hi"
            "Tamil" -> "ta"
            "Telugu" -> "te"
            "Kannada" -> "kn"
            "Marathi" -> "mr"
            "Bengali" -> "bn"
            "Spanish" -> "es"
            else -> "en"
        }
        
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
