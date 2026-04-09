package com.pharmalink.core.repository

import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

/**
 * Order Repository Interface
 * Handles order management operations
 */
interface OrderRepository {
    suspend fun getOrders(): Flow<List<Order>>
    suspend fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>>
    suspend fun getOrderById(orderId: String): Order?
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit>
    suspend fun createOrder(order: Order): Result<Order>
    suspend fun deleteOrder(orderId: String): Result<Unit>
}
