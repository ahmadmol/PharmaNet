package com.pharmalink.domain.model

enum class NotificationType {
    INFO,
    ALERT,
    ORDER_UPDATE,
    COMPLIANCE,
    SUPPORT,
}

enum class NotificationCategory {
    REQUESTS,
    ORDERS,
    WAREHOUSES,
    COMPLIANCE,
    SUPPORT,
}

enum class NotificationDestination {
    ORDER,
    REQUEST,
    WAREHOUSE,
    COMPLIANCE,
    HELP,
    PHARMACY_CUSTOMER_ORDER,
}

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val category: NotificationCategory,
    val createdAtLabel: String,
    val read: Boolean,
    val requiresAction: Boolean = false,
    val destination: NotificationDestination? = null,
    val destinationId: String? = null,
)
