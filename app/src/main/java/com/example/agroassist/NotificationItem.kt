package com.example.agroassist

data class NotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val category: String, // "market", "scheme", "weather", "operation"
    val time: String // "Today", "Yesterday", etc.
)
