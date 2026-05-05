package com.pharmalink.domain.model

data class AdminUser(
    val id: String,
    val email: String,
    val accountType: AccountType,
    val pharmacyId: String?,
    val warehouseId: String?,
    val isActive: Boolean,
    val fullName: String?,
    val phoneNumber: String?,
    val pharmacyName: String?,
    val warehouseName: String?,
    val createdAt: String
)
