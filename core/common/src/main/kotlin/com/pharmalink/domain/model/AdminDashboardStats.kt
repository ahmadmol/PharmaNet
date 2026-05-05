package com.pharmalink.domain.model

data class AdminDashboardStats(
    val totalUsers: Int,
    val totalPharmacies: Int,
    val totalWarehouses: Int,
    val totalOrders: Int,
    val pendingOrdersCount: Int,
    val activePharmacies: Int,
    val activeWarehouses: Int,
)
