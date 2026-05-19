package com.pharmalink.domain.model

data class SystemHealth(
    val healthPercent: Int,
    val healthStatus: String,
    // Note: activeConnections removed - not available in current schema
)
