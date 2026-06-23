package com.example.agroassist

data class RecommendedProduct(
    val id: String,
    val name: String,
    val brand: String,
    val rating: String,
    val price: Int,
    val imageResId: Int,
    val description: String
)
