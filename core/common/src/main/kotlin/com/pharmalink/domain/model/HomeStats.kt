package com.pharmalink.domain.model

data class HomeStats(
    val requestsTodayCount: Int,
    val requestsTodayTrend: String,
    val totalInventoryCount: Int?,
    val totalInventoryUnit: String?,
    val weeklySalesAmount: String?,
    val weeklySalesTrend: String?,
    val alertMessage: String? = null
)
