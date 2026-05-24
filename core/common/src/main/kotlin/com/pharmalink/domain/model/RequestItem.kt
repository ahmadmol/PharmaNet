package com.pharmalink.domain.model

data class RequestItem(
    val id: String = "",
    val requestId: String = "",
    val lineNo: Int,
    val medicineId: String,
    val medicineName: String,
    val medicineSubtitle: String = "",
    val quantity: Int,
    val unit: String,
    val createdAt: String = "",
    val updatedAt: String = "",
)
