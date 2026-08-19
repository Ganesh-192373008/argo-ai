package com.example.agroassist

import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ShopMapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_map)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        // Read user location from database
        val dbHelper = AgroDatabaseHelper(this)
        val profile = dbHelper.getProfile()
        val userLocation = profile["location"]?.ifEmpty { "Pune, Maharashtra" } ?: "Pune, Maharashtra"

        // Initialize Embedded Google Maps WebView
        val googleMapView = findViewById<WebView>(R.id.googleMapView)
        googleMapView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        googleMapView.webViewClient = WebViewClient()

        renderGoogleMapIframe(googleMapView, "fertilizer pesticide agro shops near $userLocation")
    }

    private fun renderGoogleMapIframe(webView: WebView?, query: String) {
        val iframeHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    html, body { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background: #e8e8e8; }
                    iframe { width: 100%; height: 100%; border: 0; }
                </style>
            </head>
            <body>
                <iframe 
                    src="https://maps.google.com/maps?q=${Uri.encode(query)}&t=m&z=13&output=embed" 
                    allowfullscreen>
                </iframe>
            </body>
            </html>
        """.trimIndent()

        webView?.loadDataWithBaseURL("https://maps.google.com", iframeHtml, "text/html", "utf-8", null)
    }
}
