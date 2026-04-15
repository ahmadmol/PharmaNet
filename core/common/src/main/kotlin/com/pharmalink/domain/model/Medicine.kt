package com.pharmalink.domain.model

data class Medicine(
    val id: String,
    val name: String,
    val brand: String,
    val strength: String,
    val price: Double,
    val imageUrl: String? = null
)
