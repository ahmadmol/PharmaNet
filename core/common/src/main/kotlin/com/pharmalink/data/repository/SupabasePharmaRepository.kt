package com.pharmalink.data.repository

import android.util.Log
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.ComplianceOverview
import com.pharmalink.domain.model.DeliveryTracking
import com.pharmalink.domain.model.NotificationCategory
import com.pharmalink.domain.model.NotificationDestination
import com.pharmalink.domain.model.NotificationType
import com.pharmalink.domain.model.Order as DomainOrder
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.PharmacyProfile
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestPriority
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.model.Warehouse
import com.pharmalink.domain.model.WarehouseShipment
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TAG = "SupabasePharmaRepo"

@Singleton
class SupabasePharmaRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val realtime: SupabaseRealtimeDataSource,
) : PharmaRepository {

    override fun observeWarehouses(): Flow<List<Warehouse>> = flow {
        emit(fetchWarehouses())
    }

    override suspend fun getWarehouse(warehouseId: String): Warehouse? =
        runCatching {
            supabase.postgrest.from("warehouses").select {
                filter { eq("id", warehouseId) }
            }.decodeList<WarehouseDto>().firstOrNull()?.toDomain()
        }.getOrNull()

    private suspend fun fetchWarehouses(): List<Warehouse> = runCatching {
        supabase.postgrest.from("warehouses").select(columns = Columns.ALL).decodeList<WarehouseDto>()
            .map { it.toDomain() }
    }.getOrElse { emptyList() }

    override fun observeOrders(): Flow<List<DomainOrder>> = flow {
        emit(fetchOrders())
        try {
            realtime.tableChanges("orders").collect {
                emit(fetchOrders())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "observeOrders realtime collect failed", e)
        }
    }

    override fun observeRequests(): Flow<List<Request>> = flow {
        emit(fetchRequests())
        try {
            realtime.tableChanges("requests").collect {
                emit(fetchRequests())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "observeRequests realtime collect failed", e)
        }
    }

    override fun observeNotifications(): Flow<List<AppNotification>> = flow {
        emit(fetchNotifications())
        try {
            realtime.tableChanges("app_notifications").collect {
                emit(fetchNotifications())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "observeNotifications realtime collect failed", e)
        }
    }

    override fun observeProfile(): Flow<PharmacyProfile> = flow { emit(placeholderProfile()) }

    override fun observeCompliance(): Flow<ComplianceOverview> = flow { emit(placeholderCompliance()) }

    private suspend fun fetchOrders(): List<DomainOrder> = runCatching {
        supabase.postgrest.from("orders").select(columns = Columns.ALL).decodeList<OrderDto>()
            .map { it.toDomain() }
    }.getOrElse { emptyList() }

    private suspend fun fetchRequests(): List<Request> = runCatching {
        supabase.postgrest.from("requests").select(columns = Columns.ALL).decodeList<RequestDto>()
            .map { it.toDomain() }
    }.getOrElse { emptyList() }

    private suspend fun fetchNotifications(): List<AppNotification> = runCatching {
        supabase.postgrest.from("app_notifications").select(columns = Columns.ALL)
            .decodeList<AppNotificationDto>()
            .map { it.toDomain() }
    }.getOrElse { emptyList() }

    override suspend fun getOrder(orderId: String): DomainOrder? =
        runCatching {
            supabase.postgrest.from("orders").select {
                filter { eq("id", orderId) }
            }.decodeList<OrderDto>().firstOrNull()?.toDomain()
        }.getOrNull()

    override suspend fun getRequest(requestId: String): Request? =
        runCatching {
            supabase.postgrest.from("requests").select {
                filter { eq("id", requestId) }
            }.decodeList<RequestDto>().firstOrNull()?.toDomain()
        }.getOrNull()

    override suspend fun getWarehouseShipments(warehouseId: String): List<WarehouseShipment> = emptyList()

    override suspend fun createRequest(request: Request): Request =
        runCatching {
            val pharmacyId = resolvePharmacyId()
                ?: error("No pharmacy_id for current user. Ensure a profiles row exists for this account.")
            val createdAt = System.currentTimeMillis().toString()
            val reqInsert = RequestInsertDto(
                pharmacyId = pharmacyId,
                warehouseId = request.warehouseId,
                medicineName = request.medicineName.trim(),
                quantity = request.quantity,
                unit = request.unit,
                status = request.status.name,
                notes = request.notes,
                medicineSubtitle = request.medicineSubtitle,
                storageNotes = request.storageNotes,
                priority = request.priority.name,
                warehouseName = request.warehouseName,
                supplierName = request.supplierName,
                createdAt = createdAt,
            )
            val insertedRequest = supabase.postgrest.from("requests").insert(reqInsert) {
                select(Columns.ALL)
            }.decodeSingle<RequestDto>()
            val orderInsert = OrderInsertDto(
                requestId = insertedRequest.id,
                pharmacyId = pharmacyId,
                warehouseId = request.warehouseId,
                medicineName = request.medicineName.trim(),
                quantity = request.quantity,
                unit = request.unit,
                status = OrderStatus.PENDING.name,
                warehouseName = request.warehouseName,
                supplierName = request.supplierName,
                createdAt = createdAt,
            )
            supabase.postgrest.from("orders").insert(orderInsert) {
                select(Columns.ALL)
            }.decodeSingle<OrderDto>()
            insertedRequest.toDomain()
        }.onFailure { Log.e(TAG, "createRequest failed", it) }
            .getOrThrow()

    private suspend fun resolvePharmacyId(): String? = runCatching {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return null
        supabase.postgrest.from("profiles").select {
            filter { eq("id", uid) }
        }.decodeList<ProfilePharmacyDto>().firstOrNull()?.pharmacyId
    }.getOrNull()

    override suspend fun updateRequest(request: Request): Result<Request> =
        Result.failure(UnsupportedOperationException())

    override suspend fun deleteRequest(requestId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    override suspend fun submitRequest(requestId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    override suspend fun markNotificationRead(notificationId: String) {}

    override suspend fun markAllNotificationsRead() {}

    override suspend fun deleteNotification(notificationId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    override suspend fun deleteAllNotifications(): Result<Unit> = Result.success(Unit)

    override suspend fun updateNotificationsPreference(enabled: Boolean) {}

    override suspend fun getDeliveryTracking(orderId: String): Result<DeliveryTracking> =
        Result.failure(UnsupportedOperationException())

    override suspend fun recordDelegateCall(phoneNumber: String): Result<Unit> = Result.success(Unit)

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    override suspend fun createOrder(order: DomainOrder): Result<DomainOrder> =
        Result.failure(UnsupportedOperationException())

    override suspend fun deleteOrder(orderId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    private fun placeholderProfile(): PharmacyProfile = PharmacyProfile(
        id = "local",
        pharmacyName = "",
        city = "",
        district = "",
        managerName = "",
        addressLine = "",
        contactPhone = "",
        contactEmail = "",
        licenseStatusLabel = "",
        licenseNumber = "",
        licenseExpiryLabel = "",
        operatingHoursLabel = "",
        preferredLanguageLabel = "",
        notificationsEnabled = true,
        twoFactorEnabled = false,
        linkedDevicesCount = 0,
        totalOrders = 0,
        completedOrders = 0,
        activeOrders = 0,
    )

    private fun placeholderCompliance(): ComplianceOverview = ComplianceOverview(
        pharmacyId = "local",
        licenseStatusLabel = "",
        licenseNumber = "",
        licenseExpiryLabel = "",
        summaryLabel = "",
        alerts = emptyList(),
        documents = emptyList(),
        supplierItems = emptyList(),
    )
}

@Serializable
private data class WarehouseDto(
    val id: String,
    val name: String,
    val city: String = "",
    val district: String = "",
    @SerialName("supports_cold_chain") val supportsColdChain: Boolean = false,
    @SerialName("in_stock_percent") val inStockPercent: Int = 0,
    @SerialName("low_stock_count") val lowStockCount: Int = 0,
    @SerialName("out_of_stock_count") val outOfStockCount: Int = 0,
    @SerialName("estimated_delivery_label") val estimatedDeliveryLabel: String = "",
    @SerialName("distance_label") val distanceLabel: String = "",
    @SerialName("phone_number") val phoneNumber: String = "",
    @SerialName("last_updated_at") val lastUpdatedAt: String? = null,
) {
    fun toDomain(): Warehouse = Warehouse(
        id = id,
        name = name,
        city = city,
        district = district,
        supportsColdChain = supportsColdChain,
        inStockPercent = inStockPercent,
        lowStockCount = lowStockCount,
        outOfStockCount = outOfStockCount,
        estimatedDeliveryLabel = estimatedDeliveryLabel,
        distanceLabel = distanceLabel,
        phoneNumber = phoneNumber,
        lastUpdatedLabel = lastUpdatedAt ?: "",
    )
}

@Serializable
private data class OrderDto(
    val id: String,
    @SerialName("request_id") val requestId: String,
    @SerialName("medicine_name") val medicineName: String,
    val status: String,
    @SerialName("warehouse_id") val warehouseId: String,
    @SerialName("warehouse_name") val warehouseName: String = "",
    @SerialName("supplier_name") val supplierName: String = "",
    val quantity: Int = 0,
    val unit: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("eta_label") val etaLabel: String? = null,
    @SerialName("last_update_label") val lastUpdateLabel: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_urgent") val isUrgent: Boolean = false,
) {
    fun toDomain(): DomainOrder = DomainOrder(
        id = id,
        requestId = requestId,
        medicineName = medicineName,
        status = status.toOrderStatus(),
        warehouseId = warehouseId,
        warehouseName = warehouseName,
        supplierName = supplierName,
        quantity = quantity,
        unit = unit,
        createdAtLabel = createdAt ?: "",
        etaLabel = etaLabel,
        lastUpdateLabel = lastUpdateLabel ?: updatedAt ?: "",
        isUrgent = isUrgent,
    )
}

@Serializable
private data class RequestDto(
    val id: String,
    @SerialName("medicine_name") val medicineName: String,
    @SerialName("medicine_subtitle") val medicineSubtitle: String = "",
    val quantity: Int = 0,
    val unit: String = "",
    val notes: String = "",
    @SerialName("storage_notes") val storageNotes: String = "",
    val priority: String,
    val status: String,
    @SerialName("warehouse_id") val warehouseId: String,
    @SerialName("warehouse_name") val warehouseName: String = "",
    @SerialName("supplier_name") val supplierName: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("eta_label") val etaLabel: String = "",
    @SerialName("related_order_id") val relatedOrderId: String? = null,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    @SerialName("medicine_image_url") val medicineImageUrl: String? = null,
) {
    fun toDomain(): Request = Request(
        id = id,
        medicineName = medicineName,
        medicineSubtitle = medicineSubtitle,
        quantity = quantity,
        unit = unit,
        notes = notes,
        storageNotes = storageNotes,
        priority = priority.toRequestPriority(),
        status = status.toRequestStatus(),
        warehouseId = warehouseId,
        warehouseName = warehouseName,
        supplierName = supplierName,
        createdAtLabel = createdAt ?: "",
        updatedAtLabel = updatedAt ?: "",
        etaLabel = etaLabel,
        relatedOrderId = relatedOrderId,
        attachmentUrl = attachmentUrl,
        medicineImageUrl = medicineImageUrl,
    )
}

@Serializable
private data class AppNotificationDto(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val category: String,
    @SerialName("created_at") val createdAt: String? = null,
    val read: Boolean = false,
    @SerialName("requires_action") val requiresAction: Boolean = false,
    val destination: String? = null,
    @SerialName("destination_id") val destinationId: String? = null,
) {
    fun toDomain(): AppNotification = AppNotification(
        id = id,
        title = title,
        body = body,
        type = type.toNotificationType(),
        category = category.toNotificationCategory(),
        createdAtLabel = createdAt ?: "",
        read = read,
        requiresAction = requiresAction,
        destination = destination?.toNotificationDestination(),
        destinationId = destinationId,
    )
}

@Serializable
private data class ProfilePharmacyDto(
    @SerialName("pharmacy_id") val pharmacyId: String?,
)

@Serializable
private data class RequestInsertDto(
    @SerialName("pharmacy_id") val pharmacyId: String,
    @SerialName("warehouse_id") val warehouseId: String,
    @SerialName("medicine_name") val medicineName: String,
    val quantity: Int,
    val unit: String,
    val status: String,
    val notes: String = "",
    @SerialName("medicine_subtitle") val medicineSubtitle: String = "",
    @SerialName("storage_notes") val storageNotes: String = "",
    val priority: String,
    @SerialName("warehouse_name") val warehouseName: String = "",
    @SerialName("supplier_name") val supplierName: String = "",
    @SerialName("created_at") val createdAt: String,
)

@Serializable
private data class OrderInsertDto(
    @SerialName("request_id") val requestId: String,
    @SerialName("pharmacy_id") val pharmacyId: String,
    @SerialName("warehouse_id") val warehouseId: String,
    @SerialName("medicine_name") val medicineName: String,
    val quantity: Int,
    val unit: String = "",
    val status: String,
    @SerialName("warehouse_name") val warehouseName: String = "",
    @SerialName("supplier_name") val supplierName: String = "",
    @SerialName("created_at") val createdAt: String,
)

private fun String.toOrderStatus(): OrderStatus =
    runCatching { OrderStatus.valueOf(this) }.getOrElse { OrderStatus.PENDING }

private fun String.toRequestPriority(): RequestPriority =
    runCatching { RequestPriority.valueOf(this) }.getOrElse { RequestPriority.NORMAL }

private fun String.toRequestStatus(): RequestStatus =
    runCatching { RequestStatus.valueOf(this) }.getOrElse { RequestStatus.DRAFT }

private fun String.toNotificationType(): NotificationType =
    runCatching { NotificationType.valueOf(this) }.getOrElse { NotificationType.INFO }

private fun String.toNotificationCategory(): NotificationCategory =
    runCatching { NotificationCategory.valueOf(this) }.getOrElse { NotificationCategory.REQUESTS }

private fun String.toNotificationDestination(): NotificationDestination? =
    runCatching { NotificationDestination.valueOf(this) }.getOrNull()
