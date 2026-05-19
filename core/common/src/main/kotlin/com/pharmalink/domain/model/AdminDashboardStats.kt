package com.pharmalink.domain.model

data class AdminDashboardStats(
    // Basic Counts
    val totalUsers: Int,
    val totalPharmacies: Int,
    val totalWarehouses: Int,
    val totalOrders: Int,
    
    // Orders Breakdown
    val b2cOrdersCount: Int,
    val b2bOrdersCount: Int,
    val urgentOrdersCount: Int,
    
    // Orders by Status
    val pendingOrdersCount: Int,
    val confirmedOrdersCount: Int,
    val deliveredOrdersCount: Int,
    
    // Active Facilities
    val activePharmacies: Int,
    val activeWarehouses: Int,
)
