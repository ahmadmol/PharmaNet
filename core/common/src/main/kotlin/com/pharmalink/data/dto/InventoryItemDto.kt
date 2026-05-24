package com.pharmalink.data.dto

import com.pharmalink.domain.model.InventoryItem
import com.pharmalink.domain.model.StockStatus
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InventoryItemDto(
    val id: String,
    @SerialName("medicine_id") val medicineId: String,
    @SerialName("medicine_name") val medicineName: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("warehouse_id") val warehouseId: String,
    val quantity: Int,
    val unit: String,
    @SerialName("stock_status") val stockStatus: String,
    @SerialName("last_updated") val lastUpdated: String,
    val description: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    @SerialName("is_visible") val isVisible: Boolean? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
)

fun InventoryItemDto.toDomain(): InventoryItem = InventoryItem(
    id = id,
    medicineId = medicineId,
    medicineName = medicineName,
    medicineImageUrl = imageUrl,
    warehouseId = warehouseId,
    quantity = quantity,
    unit = unit,
    stockStatus = when (stockStatus.uppercase()) {
        "LOW_STOCK" -> StockStatus.LOW_STOCK
        "OUT_OF_STOCK" -> StockStatus.OUT_OF_STOCK
        else -> StockStatus.IN_STOCK
    },
    lastUpdated = Instant.parse(lastUpdated),
    description = description,
    priceAmount = price,
    currency = currency ?: "SYP",
    isVisible = isVisible ?: true,
    isActive = isActive ?: true,
)
