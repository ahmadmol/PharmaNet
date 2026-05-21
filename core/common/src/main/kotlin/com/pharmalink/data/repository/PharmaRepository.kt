package com.pharmalink.data.repository

import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.AdminUser
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.AuditLog
import com.pharmalink.domain.model.CreateFacilityRequest
import com.pharmalink.domain.model.ComplianceOverview
import com.pharmalink.domain.model.DeliveryTracking
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.domain.model.HomeStats
import com.pharmalink.domain.model.Medicine
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.Pharmacy
import com.pharmalink.domain.model.PharmacyCustomerOrder
import com.pharmalink.domain.model.PharmacyProfile
import com.pharmalink.domain.model.PublicPharmacyForMedicine
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.CustomerRequestScope
import com.pharmalink.domain.model.CustomerRequestUrgency
import com.pharmalink.domain.model.RequestUpdate
import com.pharmalink.domain.model.Warehouse
import com.pharmalink.domain.model.WarehouseShipment
import com.pharmalink.data.dto.NearbyOrderDto
import com.pharmalink.core.location.FacilityLocation
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

    suspend fun updateMyWarehouseLocation(
        address: String,
        latitude: Double,
        longitude: Double,
    ): Result<Warehouse>

    fun observeCompliance(): Flow<ComplianceOverview>

    suspend fun fetchHomeStats(): Result<HomeStats>

    suspend fun fetchFeaturedWarehouses(): Result<List<Warehouse>>

    suspend fun fetchMedicines(): Result<List<Medicine>>

    suspend fun getPublicPharmacies(): Result<List<PublicPharmacyForMedicine>>

    suspend fun getPublicPharmaciesForMedicine(medicineId: String): Result<List<PublicPharmacyForMedicine>>

    suspend fun getCurrentPharmacyFacilityLocation(): Result<FacilityLocation?>

    suspend fun getNearbyOrders(lat: Double, lng: Double, radius: Double): Result<List<NearbyOrderDto>>

    suspend fun addMedicine(medicine: Medicine, warehouseId: String): Result<Unit>

    suspend fun getOrder(orderId: String): Result<Order?>

    suspend fun getRequest(requestId: String): Result<Request?>

    suspend fun getWarehouse(warehouseId: String): Result<Warehouse?>

    suspend fun getWarehouseShipments(warehouseId: String): Result<List<WarehouseShipment>>

    suspend fun createRequest(request: Request): Result<Request>

    suspend fun updateRequest(requestId: String, updates: RequestUpdate): Result<Request>

    suspend fun deleteRequest(requestId: String): Result<Unit>

    suspend fun submitRequest(requestId: String): Result<Unit>

    suspend fun submitPharmacyRequest(requestId: String): Result<Request>

    suspend fun warehouseAcceptB2bRequest(
        requestId: String,
        totalPriceCents: Long,
        note: String? = null
    ): Result<Request>

    suspend fun warehouseRejectB2bRequest(
        requestId: String,
        reason: String? = null
    ): Result<Request>

    suspend fun warehouseStartB2bFulfillment(requestId: String): Result<Request>

    suspend fun warehouseMarkB2bDelivered(
        requestId: String,
        deliveryNote: String? = null
    ): Result<Request>

    suspend fun markNotificationRead(notificationId: String): Result<Unit>

    suspend fun markAllNotificationsRead(): Result<Unit>

    suspend fun deleteNotification(notificationId: String): Result<Unit>

    suspend fun deleteAllNotifications(): Result<Unit>

    suspend fun updateNotificationsPreference(enabled: Boolean): Result<Unit>

    suspend fun submitSupportRequest(
        subject: String,
        message: String,
        category: String? = null,
    ): Result<Unit>

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
        pharmacyId: String?,
        urgency: CustomerRequestUrgency,
        requestScope: CustomerRequestScope,
        fulfillmentType: FulfillmentType,
        deliveryAddress: String?,
        deliveryLatitude: Double?,
        deliveryLongitude: Double?,
        deliveryPhone: String?,
        notes: String?,
        prescriptionUrl: String? = null,
    ): Result<Order>

    suspend fun uploadPrescription(uri: android.net.Uri): Result<String>

    suspend fun uploadMedicineImage(uri: android.net.Uri): Result<String>

    suspend fun uploadProfileAvatar(uri: android.net.Uri): Result<String>

    suspend fun deleteProfileAvatar(avatarUrl: String): Result<Unit>

    suspend fun cancelCustomerOrder(orderId: String): Result<Unit>

    suspend fun acceptCustomerOrderPrice(orderId: String): Result<Unit>

    suspend fun rejectCustomerOrderPrice(orderId: String): Result<Unit>

    suspend fun confirmOrder(orderId: String, totalPriceCents: Long): Result<Order>

    suspend fun rejectOrder(orderId: String): Result<Order>

    suspend fun markOrderReadyForPickup(orderId: String): Result<Order>

    suspend fun markOrderOutForDelivery(orderId: String): Result<Order>

    suspend fun markOrderDelivered(orderId: String): Result<Order>

    suspend fun getMyOrders(customerId: String): Result<List<Order>>

    // ==================== PHARMACY Customer Inbox Methods ====================

    suspend fun getPharmacyCustomerOrders(): Result<List<PharmacyCustomerOrder>>

    suspend fun getPharmacyCustomerOrderDetail(orderId: String): Result<PharmacyCustomerOrder>

    suspend fun claimNearbyCustomerOrder(orderId: String, radiusKm: Double = 10.0): Result<Unit>

    suspend fun confirmCustomerOrder(orderId: String, totalPriceCents: Long): Result<Unit>

    suspend fun rejectCustomerOrder(orderId: String): Result<Unit>

    suspend fun markCustomerOrderReadyForPickup(orderId: String): Result<Unit>

    suspend fun markCustomerOrderOutForDelivery(orderId: String): Result<Unit>

    suspend fun markCustomerOrderDelivered(orderId: String): Result<Unit>

    // ==================== Admin Management Methods (Phase 4.5.6) ====================

    // Admin: User Management
    suspend fun adminGetAllUsers(): Result<List<AdminUser>>

    suspend fun adminUpdateUserProfile(
        targetUserId: String,
        fullName: String?,
        accountType: AccountType,
        pharmacyId: String?,
        warehouseId: String?,
        isActive: Boolean
    ): Result<AdminUser>

    // Admin: Pharmacy Management
    suspend fun adminGetAllPharmacies(): Result<List<Pharmacy>>

    suspend fun adminCreatePharmacy(
        name: String,
        location: String,
        contactNumber: String,
        licenseNumber: String
    ): Result<Pharmacy>

    // Admin: Warehouse Management
    suspend fun adminGetAllWarehouses(): Result<List<Warehouse>>

    suspend fun adminCreateWarehouse(
        name: String,
        location: String,
        contactNumber: String
    ): Result<Warehouse>

    suspend fun createFacility(request: CreateFacilityRequest): Result<Unit>

    // Admin: Audit Logs
    suspend fun adminGetAuditLogs(limit: Int = 100): Result<List<AuditLog>>

    suspend fun getAuditLogById(logId: String): Result<AuditLog>

    // Admin: Warehouse Inventory
    suspend fun getWarehouseInventory(warehouseId: String): Result<List<com.pharmalink.domain.model.InventoryItem>>

    // Admin: Dashboard Statistics
    suspend fun adminGetDashboardStats(): Result<com.pharmalink.domain.model.AdminDashboardStats>

    // Admin: Dashboard Additional Data
    suspend fun adminGetPendingRequests(limit: Int = 5): Result<List<com.pharmalink.domain.model.PendingRequest>>
    
    suspend fun adminGetRecentActivities(limit: Int = 5): Result<List<com.pharmalink.domain.model.RecentActivity>>
    
    suspend fun adminGetSystemHealth(): Result<com.pharmalink.domain.model.SystemHealth>

    // Admin: Orders Management
    suspend fun adminGetAllOrders(
        orderType: String? = null,
        status: String? = null,
        isUrgent: Boolean? = null,
        search: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<com.pharmalink.domain.model.AdminOrder>>

    suspend fun adminGetOrderDetail(orderId: String): Result<com.pharmalink.domain.model.AdminOrder?>
}
