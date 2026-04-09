package com.pharmalink.data.repository

import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.ComplianceOverview
import com.pharmalink.domain.model.DeliveryTracking
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.PharmacyProfile
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.Warehouse
import com.pharmalink.domain.model.WarehouseShipment
import kotlinx.coroutines.flow.Flow

/**
 * Single application data contract. Replace [InMemoryPharmaRepository] with a network-backed
 * implementation when the API is ready.
 */
interface PharmaRepository {
    fun observeOrders(): Flow<List<Order>>

    fun observeRequests(): Flow<List<Request>>

    fun observeWarehouses(): Flow<List<Warehouse>>

    fun observeNotifications(): Flow<List<AppNotification>>

    fun observeProfile(): Flow<PharmacyProfile>

    fun observeCompliance(): Flow<ComplianceOverview>

    suspend fun getOrder(orderId: String): Order?

    suspend fun getRequest(requestId: String): Request?

    suspend fun getWarehouse(warehouseId: String): Warehouse?

    suspend fun getWarehouseShipments(warehouseId: String): List<WarehouseShipment>

    suspend fun createRequest(request: Request): Request

    suspend fun updateRequest(request: Request): Result<Request>

    suspend fun deleteRequest(requestId: String): Result<Unit>

    suspend fun submitRequest(requestId: String): Result<Unit>

    suspend fun markNotificationRead(notificationId: String)

    suspend fun markAllNotificationsRead()

    suspend fun deleteNotification(notificationId: String): Result<Unit>

    suspend fun deleteAllNotifications(): Result<Unit>

    suspend fun updateNotificationsPreference(enabled: Boolean)

    suspend fun getDeliveryTracking(orderId: String): Result<DeliveryTracking>

    suspend fun recordDelegateCall(phoneNumber: String): Result<Unit>

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit>

    suspend fun createOrder(order: Order): Result<Order>

    suspend fun deleteOrder(orderId: String): Result<Unit>
}
