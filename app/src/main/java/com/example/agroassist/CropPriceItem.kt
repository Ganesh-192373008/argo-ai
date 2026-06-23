package com.example.agroassist

data class CropPriceItem(
    val id: String,
    val name: String,
    val emoji: String,
    var price: String,
    var change: String,
    var trend: String, // "up", "down", "neutral"
    val unit: String, // "kg", "q", "t"
    val bgTint: String // Hex color code for emoji background
)
