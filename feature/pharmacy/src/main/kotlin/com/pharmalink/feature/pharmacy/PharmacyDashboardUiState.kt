package com.pharmalink.feature.pharmacy

/**
 * UI state for the pharmacy workspace dashboard.
 *
 * Holds lightweight situational-awareness counters loaded from the repository so the
 * dashboard can surface real activity (today's warehouse requests + pending customer
 * orders) instead of static placeholder cards. Navigation remains available regardless
 * of load/error state, so [errorMessage] is surfaced inline for the stats area only.
 */
data class PharmacyDashboardUiState(
    val isLoading: Boolean = false,
    val requestsTodayCount: Int? = null,
    val pendingCustomerOrdersCount: Int? = null,
    val totalCustomerOrdersCount: Int? = null,
    val errorMessage: String? = null,
    val isPharmacyLinked: Boolean = false,
) {
    val hasStats: Boolean
        get() = requestsTodayCount != null ||
            pendingCustomerOrdersCount != null ||
            totalCustomerOrdersCount != null
}