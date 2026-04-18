package com.pharmalink.data.repository

import android.util.Log
import com.pharmalink.core.common.error.MissingPharmacyLinkageException
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.ComplianceOverview
import com.pharmalink.domain.model.DeliveryTracking
import com.pharmalink.domain.model.HomeStats
import com.pharmalink.domain.model.Medicine
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

private const val TAG = "SupabasePharmaRepo"

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SupabasePharmaRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val realtime: SupabaseRealtimeDataSource,
    private val authRepository: AuthRepository,
) : PharmaRepository {

    override fun observeWarehouses(): Flow<List<Warehouse>> = flow {
        Log.d(TAG, "=== OBSERVE WAREHOUSES INITIALIZED ===")
        val initialWarehouses = fetchWarehouses().getOrThrow()
        Log.d(TAG, "Initial warehouses count: ${initialWarehouses.size}")
        initialWarehouses.forEach { warehouse ->
            Log.d(TAG, "Warehouse: ID=${warehouse.id}, Name=${warehouse.name}, Stock=${warehouse.inStockPercent}%")
        }
        emit(initialWarehouses)
    }

    override suspend fun getWarehouse(warehouseId: String): Result<Warehouse?> =
        runCatching {
            supabase.postgrest.from("warehouses").select {
                filter { eq("id", warehouseId) }
            }.decodeList<WarehouseDto>().firstOrNull()?.toDomain()?.getOrThrow()
        }

    private suspend fun fetchWarehouses(): Result<List<Warehouse>> = runCatching {
        supabase.postgrest.from("warehouses").select(columns = Columns.ALL).decodeList<WarehouseDto>()
            .map { it.toDomain().getOrThrow() }
    }

    override suspend fun fetchFeaturedWarehouses(): Result<List<Warehouse>> = runCatching {
        supabase.postgrest.from("warehouses").select(columns = Columns.ALL) {
            limit(10)
        }.decodeList<WarehouseDto>()
            .map { it.toDomain().getOrThrow() }
    }

    override suspend fun fetchHomeStats(): Result<HomeStats> = runCatching {
        val pharmacyId = resolvePharmacyId()
        val requestsCount = supabase.postgrest.from("requests").select(columns = Columns.raw("id")) {
            filter { eq("pharmacy_id", pharmacyId) }
        }.decodeList<RequestIdRow>().size

        HomeStats(
            requestsTodayCount = requestsCount,
            requestsTodayTrend = "", // Empty trend until calculated
            totalInventoryCount = null, // Unsupported until a verified backend contract exists
            totalInventoryUnit = null,
            weeklySalesAmount = null, // Unsupported until a verified backend contract exists
            weeklySalesTrend = null,
            alertMessage = if (requestsCount > 10) "لديك عدد كبير من الطلبات المعلقة" else null
        )
    }

    override suspend fun fetchMedicines(): Result<List<Medicine>> = runCatching {
        supabase.postgrest.from("medicines").select(columns = Columns.ALL)
            .decodeList<MedicineDto>()
            .map { it.toDomain().getOrThrow() }
    }



    override fun observeOrders(): Flow<List<DomainOrder>> = flow {
        Log.d(TAG, "=== OBSERVE ORDERS INITIALIZED ===")
        val initialOrders = fetchOrders().getOrThrow()
        Log.d(TAG, "Initial orders count: ${initialOrders.size}")
        emit(initialOrders)

        try {
            Log.d(TAG, "Starting realtime subscription to 'orders' table...")
            realtime.tableChanges("orders").collect { event ->
                Log.d(TAG, "=== ORDERS REALTIME EVENT ===")
                Log.d(TAG, "Event type: ${event::class.simpleName}")

                val updatedOrders = fetchOrders().getOrThrow()
                Log.d(TAG, "Updated orders count: ${updatedOrders.size}")
                updatedOrders.forEach { order ->
                    Log.d(TAG, "Order: ID=${order.id}, RequestID=${order.requestId}, Status=${order.status}")
                }
                emit(updatedOrders)
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Orders realtime subscription cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "observeOrders realtime collect failed", e)
            Log.e(TAG, "Realtime error type: ${e::class.simpleName}")
            Log.e(TAG, "Realtime error message: ${e.message}")
            throw e
        }
    }

    override fun observeRequests(): Flow<List<Request>> = flow {
        Log.d(TAG, "=== OBSERVE REQUESTS INITIALIZED ===")
        val initialRequests = fetchRequests().getOrThrow()
        Log.d(TAG, "Initial requests count: ${initialRequests.size}")
        emit(initialRequests)
        try {
            Log.d(TAG, "Starting realtime subscription to 'requests' table...")
            realtime.tableChanges("requests").collect { event ->
                Log.d(TAG, "=== REQUESTS REALTIME EVENT ===")
                Log.d(TAG, "Event type: ${event::class.simpleName}")

                val updatedRequests = fetchRequests().getOrThrow()
                Log.d(TAG, "Updated requests count: ${updatedRequests.size}")
                updatedRequests.forEach { request ->
                    Log.d(TAG, "Request: ID=${request.id}, Status=${request.status}, RelatedOrderID=${request.relatedOrderId}")
                }
                emit(updatedRequests)
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Requests realtime subscription cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "observeRequests realtime collect failed", e)
            Log.e(TAG, "Realtime error type: ${e::class.simpleName}")
            Log.e(TAG, "Realtime error message: ${e.message}")
            throw e
        }
    }

    override fun observeNotifications(): Flow<List<AppNotification>> = flow {
        val initialNotifications = fetchNotifications().getOrThrow()
        emit(initialNotifications)
        try {
            realtime.tableChanges("app_notifications").collect {
                emit(fetchNotifications().getOrThrow())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "observeNotifications realtime collect failed", e)
            throw e
        }
    }

    override fun observeProfile(): Flow<PharmacyProfile> =
        combine(
            authRepository.observeUserSnapshot(),
            realtime.tableChanges("profiles").onStart { emit(Unit) }
        ) { snapshot, _ -> snapshot }
            .mapLatest { snapshot ->
                if (snapshot == null) {
                    error("Authenticated profile snapshot is unavailable.")
                } else {
                    fetchProfile(snapshot.userId)
                        .getOrThrow()
                        .toDomain(snapshot.email)
                        .getOrThrow()
                }
            }

    override fun observeCompliance(): Flow<ComplianceOverview> =
        authRepository.observeUserSnapshot()
            .mapLatest { snapshot ->
                ComplianceOverview(
                    pharmacyId = snapshot?.pharmacyId.orEmpty(),
                    licenseStatusLabel = if (snapshot?.pharmacyId.isNullOrBlank()) "غير متاح" else "متاح",
                    licenseNumber = snapshot?.userId.orEmpty(),
                    licenseExpiryLabel = "غير متاح",
                    summaryLabel = "بيانات الامتثال التفصيلية غير متاحة حاليًا. يتم عرض ملخص آمن فقط لتجنب تعطل الشاشة.",
                    alerts = emptyList(),
                    documents = emptyList(),
                    supplierItems = emptyList(),
                )
            }

    override suspend fun updateProfile(profile: PharmacyProfile): Result<Unit> = runCatching {
        val updateDto = ProfileUpdateDto(
            fullName = profile.managerName,
            pharmacyName = profile.pharmacyName,
            pharmacyLocation = profile.addressLine,
            phoneNumber = profile.contactPhone
        )

        val updatedRows = supabase.postgrest.from("profiles").update(updateDto) {
            select(Columns.ALL)
            filter { eq("id", profile.id) }
        }.decodeList<ProfileDetailsDto>()

        when {
            updatedRows.isEmpty() -> error("Profile update did not affect any row for profile ${profile.id}.")
            updatedRows.size > 1 -> error("Profile update affected duplicate rows for profile ${profile.id}.")
        }

        verifyUpdatedProfileRow(
            updatedRow = updatedRows.single(),
            expected = updateDto,
            profileId = profile.id,
        )
    }.onFailure { e ->
        Log.e(TAG, "updateProfile failed", e)
    }.map { Unit }

    private suspend fun updateNotificationsReadState(
        read: Boolean,
        notificationId: String? = null,
    ): Result<Unit> = runCatching {
        val pharmacyId = resolvePharmacyId()
        if (notificationId != null) {
            requireOwnedNotification(
                pharmacyId = pharmacyId,
                notificationId = notificationId,
                actionName = "Marking notification as read",
            )
        }
        supabase.postgrest.from("app_notifications").update(NotificationReadUpdateDto(read = read)) {
            filter {
                eq("pharmacy_id", pharmacyId)
                notificationId?.let { eq("id", it) }
            }
        }
    }.map { Unit }

    private suspend fun requireOwnedNotification(
        pharmacyId: String,
        notificationId: String,
        actionName: String,
    ) {
        val rows = supabase.postgrest.from("app_notifications").select(columns = Columns.raw("id")) {
            filter {
                eq("pharmacy_id", pharmacyId)
                eq("id", notificationId)
            }
        }.decodeList<NotificationRowIdDto>()

        when {
            rows.isEmpty() -> error("$actionName failed because notification $notificationId was not found for pharmacy $pharmacyId.")
            rows.size > 1 -> error("$actionName failed because notification $notificationId matched duplicate rows for pharmacy $pharmacyId.")
        }
    }

    private fun <T> unsupportedFeatureFailure(
        featureName: String,
        reason: String,
    ): Result<T> =
        Result.failure(
            NotImplementedError("$featureName is intentionally disabled for the current Supabase implementation. $reason"),
        )

    private suspend fun fetchOrders(): Result<List<DomainOrder>> = runCatching {
        supabase.postgrest.from("orders").select(columns = Columns.ALL).decodeList<OrderDto>()
            .map { it.toDomain().getOrThrow() }
    }

    private suspend fun fetchRequests(): Result<List<Request>> = runCatching {
        val pharmacyId = resolvePharmacyId()
        supabase.postgrest.from("requests").select(columns = Columns.ALL) {
            filter { eq("pharmacy_id", pharmacyId) }
        }.decodeList<RequestDto>()
            .map { it.toDomain().getOrThrow() }
    }


    private suspend fun fetchNotifications(): Result<List<AppNotification>> = runCatching {
        val pharmacyId = resolvePharmacyId()
        supabase.postgrest.from("app_notifications").select(columns = Columns.ALL) {
            filter { eq("pharmacy_id", pharmacyId) }
        }
            .decodeList<AppNotificationDto>()
            .map { it.toDomain().getOrThrow() }
    }

    private suspend fun fetchProfilePharmacyRow(userId: String): Result<ProfilePharmacyDto> = runCatching {
        val rows = supabase.postgrest.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeList<ProfilePharmacyDto>()

        requireSingleProfileRow(
            rows = rows,
            userId = userId,
            rowLabel = "Profile pharmacy linkage",
        )
    }

    private suspend fun fetchProfile(userId: String): Result<ProfileDetailsDto> = runCatching {
        val rows = supabase.postgrest.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeList<ProfileDetailsDto>()

        requireSingleProfileRow(
            rows = rows,
            userId = userId,
            rowLabel = "Profile",
        )
    }

    override suspend fun getOrder(orderId: String): Result<DomainOrder?> =
        runCatching {
            supabase.postgrest.from("orders").select {
                filter { eq("id", orderId) }
            }.decodeList<OrderDto>().firstOrNull()?.toDomain()?.getOrThrow()
        }

    override suspend fun getRequest(requestId: String): Result<Request?> =
        runCatching {
            val pharmacyId = resolvePharmacyId()
            supabase.postgrest.from("requests").select {
                filter {
                    eq("id", requestId)
                    eq("pharmacy_id", pharmacyId)
                }
            }.decodeList<RequestDto>().firstOrNull()?.toDomain()?.getOrThrow()
        }


    override suspend fun getWarehouseShipments(warehouseId: String): Result<List<WarehouseShipment>> = Result.success(emptyList())

    override suspend fun createRequest(request: Request): Result<Request> =
        runCatching {
            Log.d(TAG, "=== CREATE REQUEST DEBUG ===")
            Log.d(TAG, "Input request ID: ${request.id}")
            Log.d(TAG, "Medicine: ${request.medicineName}")
            Log.d(TAG, "Quantity: ${request.quantity} ${request.unit}")
            Log.d(TAG, "Priority: ${request.priority.name}")
            Log.d(TAG, "Status: ${request.status.name}")
            Log.d(TAG, "Warehouse ID: ${request.warehouseId}")
            Log.d(TAG, "Warehouse Name: ${request.warehouseName}")
            
            val pharmacyId = resolvePharmacyId()
            Log.d(TAG, "Resolved pharmacy ID: $pharmacyId")
            
            val createdAt = System.currentTimeMillis().toString()
            Log.d(TAG, "Created at timestamp: $createdAt")
            
            val reqInsert = RequestInsertDto(
                pharmacyId = pharmacyId,
                warehouseId = request.warehouseId,
                medicineName = request.medicineName.trim(),
                quantity = request.quantity,
                unit = request.unit,
                medicineSubtitle = request.medicineSubtitle,
                priority = request.priority.name,
                warehouseName = request.warehouseName,
                supplierName = request.supplierName,
                status = request.status.name,
            )
            
            Log.d(TAG, "Attempting to insert request into Supabase...")
            val insertedRequest = supabase.postgrest.from("requests").insert(reqInsert) {
                select(Columns.ALL)
            }.decodeSingle<RequestDto>()
            
            Log.d(TAG, "✅ REQUEST INSERT SUCCESS!")
            Log.d(TAG, "Returned request ID: ${insertedRequest.id}")
            Log.d(TAG, "Returned request warehouse ID: ${insertedRequest.warehouseId}")
            Log.d(TAG, "Returned request warehouse: ${insertedRequest.warehouseName}")
            Log.d(TAG, "Returned request supplier: ${insertedRequest.supplierName}")
            
            val domainRequest = insertedRequest.toDomain().getOrThrow()
            Log.d(TAG, "✅ CREATE REQUEST FLOW COMPLETED")
            Log.d(TAG, "Final domain request ID: ${domainRequest.id}")
            Log.d(TAG, "Final domain request status: ${domainRequest.status}")
            
            domainRequest
        }.onFailure { exception ->
            Log.e(TAG, "=== CREATE REQUEST FAILURE ===")
            Log.e(TAG, "Exception type: ${exception::class.simpleName}")
            Log.e(TAG, "Exception message: ${exception.message}")
            Log.e(TAG, "Exception cause: ${exception.cause?.message}")
            Log.e(TAG, "Exception stack: ${exception.stackTraceToString()}")
            Log.e(TAG, "createRequest failed", exception)
        }

    private suspend fun resolvePharmacyId(): String = runCatching {
        Log.d(TAG, "=== RESOLVE PHARMACY ID DEBUG ===")
        val currentUser = supabase.auth.currentUserOrNull()
        Log.d(TAG, "Current auth user: ${currentUser?.id ?: "NULL"}")
        Log.d(TAG, "Current auth email: ${currentUser?.email ?: "NULL"}")

        val uid = currentUser?.id
            ?: error("No authenticated user found while resolving pharmacy linkage.")
        Log.d(TAG, "Looking up profile for user ID: $uid")

        val profile = fetchProfilePharmacyRow(uid).getOrThrow()
        Log.d(TAG, "Profile found: PharmacyID=${profile.pharmacyId}")

        val pharmacyId = profile.pharmacyId?.takeIf { it.isNotBlank() }
            ?: throw MissingPharmacyLinkageException(uid)
        Log.d(TAG, "✅ PHARMACY ID RESOLVED: $pharmacyId")
        pharmacyId
    }.onFailure { exception ->
        Log.e(TAG, "❌ PHARMACY ID RESOLUTION FAILED", exception)
        Log.e(TAG, "Exception type: ${exception::class.simpleName}")
        Log.e(TAG, "Exception message: ${exception.message}")
    }.getOrThrow()

    private fun verifyUpdatedProfileRow(
        updatedRow: ProfileDetailsDto,
        expected: ProfileUpdateDto,
        profileId: String,
    ) {
        require(updatedRow.id == profileId) {
            "Profile update returned row ${updatedRow.id} instead of profile $profileId."
        }
        require(updatedRow.fullName == expected.fullName) {
            "Profile update verification failed for full_name on profile $profileId."
        }
        require(updatedRow.pharmacyName == expected.pharmacyName) {
            "Profile update verification failed for pharmacy_name on profile $profileId."
        }
        require(updatedRow.pharmacyLocation == expected.pharmacyLocation) {
            "Profile update verification failed for pharmacy_location on profile $profileId."
        }
        require(updatedRow.phoneNumber == expected.phoneNumber) {
            "Profile update verification failed for phone_number on profile $profileId."
        }
    }

    private fun <T> requireSingleProfileRow(
        rows: List<T>,
        userId: String,
        rowLabel: String,
    ): T =
        when {
            rows.isEmpty() -> error("$rowLabel row is missing for user $userId.")
            rows.size > 1 -> error("Duplicate $rowLabel rows found for user $userId.")
            else -> rows.single()
        }

    override suspend fun updateRequest(request: Request): Result<Request> =
        unsupportedFeatureFailure(
            featureName = "Updating requests",
            reason = "The app can read requests, but the writable request update contract is not provable from the visible schema or DTOs.",
        )

    override suspend fun deleteRequest(requestId: String): Result<Unit> =
        unsupportedFeatureFailure(
            featureName = "Deleting requests",
            reason = "Delete semantics and tenant/RLS guarantees for requests are not provable from the current codebase.",
        )

    override suspend fun submitRequest(requestId: String): Result<Unit> =
        unsupportedFeatureFailure(
            featureName = "Submitting requests",
            reason = "No visible backend contract defines how a request is submitted or which state transition must occur.",
        )

    override suspend fun markNotificationRead(notificationId: String): Result<Unit> =
        updateNotificationsReadState(
            read = true,
            notificationId = notificationId,
        )

    override suspend fun markAllNotificationsRead(): Result<Unit> =
        updateNotificationsReadState(read = true)

    override suspend fun deleteNotification(notificationId: String): Result<Unit> =
        runCatching {
            val pharmacyId = resolvePharmacyId()
            requireOwnedNotification(
                pharmacyId = pharmacyId,
                notificationId = notificationId,
                actionName = "Deleting notification",
            )
            supabase.postgrest.from("app_notifications").delete {
                filter {
                    eq("id", notificationId)
                    eq("pharmacy_id", pharmacyId)
                }
            }
        }.map { Unit }

    override suspend fun deleteAllNotifications(): Result<Unit> =
        runCatching {
            val pharmacyId = resolvePharmacyId()
            supabase.postgrest.from("app_notifications").delete {
                filter { eq("pharmacy_id", pharmacyId) }
            }
        }.map { Unit }

    override suspend fun updateNotificationsPreference(enabled: Boolean): Result<Unit> =
        unsupportedFeatureFailure(
            featureName = "Updating notification preferences",
            reason = "No visible profile column or dedicated table proves where notification preference changes should be persisted.",
        )

    override suspend fun getDeliveryTracking(orderId: String): Result<DeliveryTracking> =
        unsupportedFeatureFailure(
            featureName = "Delivery tracking",
            reason = "No delivery-tracking table, DTO, or RPC contract is visible in the current Supabase implementation.",
        )

    override suspend fun recordDelegateCall(phoneNumber: String): Result<Unit> =
        unsupportedFeatureFailure(
            featureName = "Delegate call logging",
            reason = "No visible backend table or RPC contract proves how delegate call events should be recorded.",
        )

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> =
        unsupportedFeatureFailure(
            featureName = "Updating order status",
            reason = "The app can read order status values, but write rules and allowed backend transitions are not provable from the visible schema.",
        )

    override suspend fun createOrder(order: DomainOrder): Result<DomainOrder> =
        unsupportedFeatureFailure(
            featureName = "Creating orders",
            reason = "No insert DTO or verified backend contract proves which order fields are required for a valid Supabase write.",
        )

    override suspend fun deleteOrder(orderId: String): Result<Unit> =
        unsupportedFeatureFailure(
            featureName = "Deleting orders",
            reason = "Delete semantics and tenant/RLS guarantees for orders are not provable from the current codebase.",
        )

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

}

@Serializable
private data class RequestIdRow(val id: String)

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
    fun toDomain(): Result<Warehouse> = runCatching {
        Log.d(TAG, "=== WAREHOUSE DTO TO DOMAIN MAPPING ===")
        Log.d(TAG, "Warehouse DTO ID: $id")
        Log.d(TAG, "Warehouse DTO Name: $name")
        Log.d(TAG, "Warehouse DTO Stock: $inStockPercent%")
        Log.d(TAG, "Warehouse DTO Cold Chain: $supportsColdChain")
        Log.d(TAG, "Warehouse DTO Last Updated: $lastUpdatedAt")
        
        // Validate critical fields
        if (id.isBlank()) {
            throw IllegalArgumentException("Warehouse ID cannot be blank")
        }
        if (name.isBlank()) {
            throw IllegalArgumentException("Warehouse name cannot be blank")
        }
        if (inStockPercent < 0 || inStockPercent > 100) {
            throw IllegalArgumentException("Invalid stock percentage: $inStockPercent")
        }
        
        Warehouse(
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
}

@Serializable
private data class MedicineDto(
    val id: String,
    val name: String,
    val brand: String,
    val strength: String,
    val price: Double,
    @SerialName("image_url") val imageUrl: String? = null
) {
    fun toDomain(): Result<Medicine> = runCatching {
        if (id.isBlank()) {
            throw IllegalArgumentException("Medicine ID cannot be blank")
        }
        if (name.isBlank()) {
            throw IllegalArgumentException("Medicine name cannot be blank")
        }
        Medicine(
            id = id,
            name = name,
            brand = brand,
            strength = strength,
            price = price,
            imageUrl = imageUrl
        )
    }
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
    fun toDomain(): Result<DomainOrder> = runCatching {
        Log.d(TAG, "=== ORDER DTO TO DOMAIN MAPPING ===")
        Log.d(TAG, "Order DTO ID: $id")
        Log.d(TAG, "Order DTO RequestID: $requestId")
        Log.d(TAG, "Order DTO Medicine: $medicineName")
        Log.d(TAG, "Order DTO Status: $status")
        Log.d(TAG, "Order DTO Warehouse: $warehouseName")
        Log.d(TAG, "Order DTO Quantity: $quantity $unit")
        
        // Validate critical fields
        if (id.isBlank()) {
            Log.e(TAG, "❌ ORDER MAPPING: Blank ID detected!")
        }
        if (requestId.isBlank()) {
            Log.e(TAG, "❌ ORDER MAPPING: Blank requestId detected!")
        }
        if (medicineName.isBlank()) {
            Log.e(TAG, "❌ ORDER MAPPING: Blank medicineName detected!")
        }
        if (quantity <= 0) {
            Log.e(TAG, "❌ ORDER MAPPING: Invalid quantity: $quantity")
        }
        
        val statusEnum = status.toOrderStatus()

        val createdAtLabel = isoStringToDisplayLabel(createdAt)
        val lastLabel = lastUpdateLabel?.takeIf { it.isNotBlank() }
            ?: isoStringToDisplayLabel(updatedAt)

        DomainOrder(
            id = id,
            requestId = requestId,
            medicineName = medicineName,
            status = statusEnum,
            warehouseId = warehouseId,
            warehouseName = warehouseName,
            supplierName = supplierName,
            quantity = quantity,
            unit = unit,
            createdAtLabel = createdAtLabel,
            etaLabel = etaLabel,
            lastUpdateLabel = lastLabel.ifBlank { createdAtLabel },
            isUrgent = isUrgent,
        )
    }
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
    fun toDomain(): Result<Request> = runCatching {
        Log.d(TAG, "=== REQUEST DTO TO DOMAIN MAPPING ===")
        Log.d(TAG, "Request DTO ID: $id")
        Log.d(TAG, "Request DTO Medicine: $medicineName")
        Log.d(TAG, "Request DTO Status: $status")
        Log.d(TAG, "Request DTO Priority: $priority")
        Log.d(TAG, "Request DTO Warehouse: $warehouseName")
        Log.d(TAG, "Request DTO RelatedOrderID: $relatedOrderId")
        
        // Validate critical fields
        if (id.isBlank()) {
            Log.e(TAG, "❌ REQUEST MAPPING: Blank ID detected!")
        }
        if (medicineName.isBlank()) {
            Log.e(TAG, "❌ REQUEST MAPPING: Blank medicineName detected!")
        }
        if (quantity <= 0) {
            Log.e(TAG, "❌ REQUEST MAPPING: Invalid quantity: $quantity")
        }
        
        val priorityEnum = priority.toRequestPriority()
        val statusEnum = status.toRequestStatus()

        val createdAtLabel = isoStringToDisplayLabel(createdAt)
        val updatedAtLabel = isoStringToDisplayLabel(updatedAt).ifBlank { createdAtLabel }

        Request(
            id = id,
            medicineName = medicineName,
            medicineSubtitle = medicineSubtitle,
            quantity = quantity,
            unit = unit,
            notes = notes,
            storageNotes = storageNotes,
            priority = priorityEnum,
            status = statusEnum,
            warehouseId = warehouseId,
            warehouseName = warehouseName,
            supplierName = supplierName,
            createdAtLabel = createdAtLabel,
            updatedAtLabel = updatedAtLabel,
            etaLabel = etaLabel,
            relatedOrderId = relatedOrderId,
            attachmentUrl = attachmentUrl,
            medicineImageUrl = medicineImageUrl,
        )
    }
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
    fun toDomain(): Result<AppNotification> = runCatching {
        AppNotification(
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
}

@Serializable
private data class NotificationReadUpdateDto(
    val read: Boolean,
)

@Serializable
private data class NotificationRowIdDto(
    val id: String,
)

@Serializable
private data class ProfilePharmacyDto(
    @SerialName("pharmacy_id") val pharmacyId: String?,
)

@Serializable
private data class ProfileDetailsDto(
    val id: String,
    @SerialName("pharmacy_id") val pharmacyId: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
    @SerialName("pharmacy_location") val pharmacyLocation: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
) {
    fun toDomain(contactEmail: String): Result<PharmacyProfile> = runCatching {
        PharmacyProfile(
            id = id,
            pharmacyName = pharmacyName.orEmpty(),
            city = "",
            district = "",
            managerName = fullName.orEmpty(),
            addressLine = pharmacyLocation.orEmpty(),
            contactPhone = phoneNumber.orEmpty(),
            contactEmail = contactEmail,
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
    }
}

@Serializable
private data class ProfileUpdateDto(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
    @SerialName("pharmacy_location") val pharmacyLocation: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
)

@Serializable
private data class RequestInsertDto(
    @SerialName("pharmacy_id") val pharmacyId: String,
    @SerialName("warehouse_id") val warehouseId: String,
    @SerialName("medicine_name") val medicineName: String,
    val quantity: Int,
    val unit: String,
    @SerialName("medicine_subtitle") val medicineSubtitle: String = "",
    val priority: String,
    @SerialName("warehouse_name") val warehouseName: String = "",
    @SerialName("supplier_name") val supplierName: String = "",
    val status: String,
)

private val isoDisplayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

private fun isoStringToDisplayLabel(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        val instant = Instant.parse(iso)
        isoDisplayFormatter.format(instant)
    }.getOrElse { iso }
}

private fun String.toOrderStatus(): OrderStatus = OrderStatus.valueOf(this)

private fun String.toRequestPriority(): RequestPriority = RequestPriority.valueOf(this)

private fun String.toRequestStatus(): RequestStatus = RequestStatus.valueOf(this)

private fun String.toNotificationType(): NotificationType = NotificationType.valueOf(this)

private fun String.toNotificationCategory(): NotificationCategory = NotificationCategory.valueOf(this)

private fun String.toNotificationDestination(): NotificationDestination? =
    runCatching { NotificationDestination.valueOf(this) }.getOrNull()
