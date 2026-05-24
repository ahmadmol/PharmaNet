package com.pharmalink.domain.model

import kotlinx.serialization.json.JsonElement

data class Medicine(
    val id: String,
    val name: String,
    val brand: String,
    val strength: String,
    val price: Double,
    val stockQuantity: Int = 0,
    val imageUrl: String? = null,
    val priceAmount: Double? = null,
    val warehouseId: String? = null,
    val description: String? = null,
    val specs: JsonElement? = null,
    val isVisible: Boolean = true,
    val isActive: Boolean = true,
    val currency: String = "SYP",
)
