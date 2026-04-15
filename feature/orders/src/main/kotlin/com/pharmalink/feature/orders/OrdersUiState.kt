package com.pharmalink.feature.orders

import com.pharmalink.domain.model.OrderStatus

data class OrdersUiState(
    val orders: List<OrderItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class OrderItem(
    val id: String,
    val requestId: String,
    val orderNumber: String,
    val date: String,
    val status: String,
    val statusType: OrderStatus,
    val totalAmount: String,
    val deliveryDate: String? = null,
    val warehouseName: String? = null,
    val warehouseId: String? = null,
    val supplierName: String? = null,
    val medicineName: String,
    val quantity: Int,
    val unit: String,
    val isUrgent: Boolean = false,
    val lastUpdate: String? = null,
    val items: List<OrderItemDetail> = emptyList()
)

data class OrderItemDetail(
    val medicineName: String,
    val quantity: Int,
    val price: String
)
