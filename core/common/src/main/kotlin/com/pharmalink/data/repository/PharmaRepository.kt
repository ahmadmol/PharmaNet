package com.pharmalink.data.repository

import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.ComplianceOverview
import com.pharmalink.domain.model.DeliveryTracking
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.domain.model.HomeStats
import com.pharmalink.domain.model.Medicine
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.PharmacyProfile
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestUpdate
import com.pharmalink.domain.model.Warehouse
import com.pharmalink.domain.model.WarehouseShipment
import kotlinx.coroutines.flow.Flow

/**
 * Single application data contract. Replace [InMemoryPharmaRepository] with a network-backed
 * implementation when the API is ready.
 *
 * Convention: [Flow] APIs emit plain data; [suspend] APIs return [Result].
 */
interface PharmaRepository {
    fun observeOrders(): Flow<List<Order>>

    fun observeRequests(): Flow<List<Request>>

    fun observeIncomingRequestsForWarehouse(warehouseId: String): Flow<List<Request>>

    fun observeWarehouses(): Flow<List<Warehouse>>

    fun observeNotifications(): Flow<List<AppNotification>>

    fun observeProfile(): Flow<PharmacyProfile>

    suspend fun updateProfile(profile: PharmacyProfile): Result<Unit>

    fun observeCompliance(): Flow<ComplianceOverview>

    suspend fun fetchHomeStats(): Result<HomeStats>

    suspend fun fetchFeaturedWarehouses(): Result<List<Warehouse>>

    suspend fun fetchMedicines(): Result<List<Medicine>>

    suspend fun getOrder(orderId: String): Result<Order?>

    suspend fun getRequest(requestId: String): Result<Request?>

    suspend fun getWarehouse(warehouseId: String): Result<Warehouse?>

    suspend fun getWarehouseShipments(warehouseId: String): Result<List<WarehouseShipment>>

    suspend fun createRequest(request: Request): Result<Request>

    suspend fun updateRequest(requestId: String, updates: RequestUpdate): Result<Request>

    suspend fun deleteRequest(requestId: String): Result<Unit>

    suspend fun submitRequest(requestId: String): Result<Unit>

    suspend fun markNotificationRead(notificationId: String): Result<Unit>

    suspend fun markAllNotificationsRead(): Result<Unit>

    suspend fun deleteNotification(notificationId: String): Result<Unit>

    suspend fun deleteAllNotifications(): Result<Unit>

    suspend fun updateNotificationsPreference(enabled: Boolean): Result<Unit>

    suspend fun getDeliveryTracking(orderId: String): Result<DeliveryTracking>

    suspend fun recordDelegateCall(phoneNumber: String): Result<Unit>

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit>

    suspend fun createOrder(order: Order): Result<Order>

    suspend fun deleteOrder(orderId: String): Result<Unit>

    // ==================== B2C Customer Order Methods (Phase 4.3B) ====================

    suspend fun createCustomerOrder(
        medicineId: String,
        medicineName: String,
        quantity: Int,
        unit: String,
        pharmacyId: String,
        fulfillmentType: FulfillmentType,
        deliveryAddress: String?,
        deliveryPhone: String?,
        notes: String?,
    ): Result<Order>

    suspend fun cancelCustomerOrder(orderId: String): Result<Unit>

    suspend fun confirmOrder(orderId: String, totalPriceCents: Long): Result<Order>

    suspend fun rejectOrder(orderId: String): Result<Order>

    suspend fun markOrderReadyForPickup(orderId: String): Result<Order>

    suspend fun markOrderOutForDelivery(orderId: String): Result<Order>

    suspend fun markOrderDelivered(orderId: String): Result<Order>

    suspend fun getMyOrders(customerId: String): Result<List<Order>>
}
