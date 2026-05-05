package com.pharmalink.domain.model

import java.time.Instant

data class AuditLog(
    val id: String,
    val action: String,
    val actionLabel: String,
    val adminId: String,
    val adminName: String,
    val adminEmail: String,
    val targetEntityName: String,
    val targetWarehouseName: String?,
    val targetSku: String?,
    val oldValue: String,
    val newValue: String,
    val ipAddress: String?,
    val userAgent: String?,
    val transactionId: String?,
    val createdAt: Instant,
    val isSuccess: Boolean,
)
