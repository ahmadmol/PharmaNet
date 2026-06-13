package com.pharmalink.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.pharmalink.core.common.BuildConfig
import com.pharmalink.core.common.error.MissingPharmacyLinkageException
import com.pharmalink.core.location.FacilityLocation
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.data.dto.AdminUserDto
import com.pharmalink.data.dto.AuditLogDto
import com.pharmalink.data.dto.CreatePharmacyRpcParams
import com.pharmalink.data.dto.CreatePharmacyWithCoordinatesRpcParams
import com.pharmalink.data.dto.CreateWarehouseRpcParams
import com.pharmalink.data.dto.CreateWarehouseWithCoordinatesRpcParams
import com.pharmalink.data.dto.FacilityDto
import com.pharmalink.data.dto.GetAuditLogsRpcParams
import com.pharmalink.data.dto.GetNearbyOrdersRpcParams
import com.pharmalink.data.dto.InventoryItemDto
import com.pharmalink.data.dto.NearbyOrderDto
import com.pharmalink.data.dto.UpdateUserProfileRpcParams
import com.pharmalink.data.dto.toDomain
import com.pharmalink.domain.mapper.toUserIdentity
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.AdminUser
import com.pharmalink.domain.model.AuditLog
import com.pharmalink.domain.model.CreateFacilityRequest
import com.pharmalink.domain.model.FacilityType
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.ComplianceOverview
import com.pharmalink.domain.model.CustomerRequestScope
import com.pharmalink.domain.model.CustomerRequestUrgency
import com.pharmalink.domain.model.DeliveryTracking
import com.pharmalink.domain.model.HomeStats
import com.pharmalink.domain.model.Medicine
import com.pharmalink.domain.model.NotificationCategory
import com.pharmalink.domain.model.NotificationDestination
import com.pharmalink.domain.model.NotificationType
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.domain.model.Order as DomainOrder
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.OrderType
import com.pharmalink.domain.model.Pharmacy
import com.pharmalink.domain.model.PharmacyCustomerOrder
import com.pharmalink.domain.model.PharmacyProfile
import com.pharmalink.domain.model.PublicPharmacyAvailabilityStatus
import com.pharmalink.domain.model.PublicPharmacyForMedicine
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestItem
import com.pharmalink.domain.model.RequestPriority
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.model.UserIdentity
import com.pharmalink.domain.model.Warehouse
import com.pharmalink.domain.model.WarehouseShipment
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.upload
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "SupabasePharmaRepo"

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SupabasePharmaRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : PharmaRepository {
    private val realtime = SupabaseRealtimeDataSource(supabase)
    private val auth: Auth get() = supabase.auth
    private val profileRefreshRequests = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
    )

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

    private suspend fun fetchPharmacies(): Result<List<Pharmacy>> = runCatching {
        supabase.postgrest.from("pharmacies").select(columns = Columns.ALL)
            .decodeList<FacilityDto>()
            .map { it.toPharmacy() }
    }

    private suspend fun fetchWarehouses(): Result<List<Warehouse>> = runCatching {
        supabase.postgrest.from("warehouses").select(columns = Columns.ALL)
            .decodeList<FacilityDto>()
            .map { it.toWarehouse() }
    }

    override suspend fun fetchFeaturedWarehouses(): Result<List<Warehouse>> = runCatching {
        supabase.postgrest.from("warehouses").select(columns = Columns.ALL) {
            limit(10)
        }.decodeList<WarehouseDto>()
            .map { it.toDomain().getOrThrow() }
    }

    override suspend fun fetchHomeStats(): Result<HomeStats> = runCatching {
        val pharmacyId = resolvePharmacyIdOrNull()
            ?: throw UnsupportedOperationException("Backend not confirmed")

        val requestRows = supabase.postgrest.from("requests").select(columns = Columns.raw("id,created_at")) {
            filter { eq("pharmacy_id", pharmacyId) }
        }.decodeList<HomeStatsRequestRow>()

        val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
        val requestsTodayCount = requestRows.count { row ->
            row.createdAt
                ?.let { isoStringToInstant(it).atZone(ZoneId.systemDefault()).toLocalDate() == today }
                ?: false
        }

        HomeStats(
            requestsTodayCount = requestsTodayCount,
            requestsTodayTrend = "",
            totalInventoryCount = null,
            totalInventoryUnit = null,
            weeklySalesAmount = null,
            weeklySalesTrend = null,
            alertMessage = null,
        )
    }.onFailure { error ->
        Log.e(TAG, "fetchHomeStats failed", error)
    }

    override suspend fun fetchMedicines(): Result<List<Medicine>> = runCatching {
        supabase.postgrest.from("medicines").select(columns = Columns.ALL)
            .decodeList<MedicineDto>()
            .mapNotNull { it.toDomain().getOrNull() }
    }

    override suspend fun getWarehouseProducts(warehouseId: String): Result<List<Medicine>> = runCatching {
        require(warehouseId.isNotBlank()) { "warehouseId must not be blank" }
        supabase.postgrest.rpc(
            "get_warehouse_visible_products",
            WarehouseProductsRpcParams(warehouseId = warehouseId),
        ).decodeList<MedicineDto>()
            .mapNotNull { it.toDomain().getOrNull() }
    }

    override suspend fun getPublicPharmacies(): Result<List<PublicPharmacyForMedicine>> = runCatching {
        supabase.postgrest.rpc("get_public_pharmacies")
            .decodeList<PublicPharmacyForMedicineDto>()
            .map { it.toDomain() }
    }

    override suspend fun getPublicPharmaciesForMedicine(medicineId: String): Result<List<PublicPharmacyForMedicine>> = runCatching {
        require(medicineId.isNotBlank()) { "medicineId must not be blank" }
        supabase.postgrest.rpc(
            "get_public_pharmacies_for_medicine",
            PublicPharmaciesForMedicineRpcParams(medicineId = medicineId),
        ).decodeList<PublicPharmacyForMedicineDto>()
            .map { it.toDomain() }
    }

    override suspend fun getCurrentPharmacyFacilityLocation(): Result<FacilityLocation?> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PHARMACY) {
            "Only PHARMACY can read pharmacy facility location"
        }
        val pharmacyId = identity.organizationId
            ?: throw IllegalStateException("PHARMACY missing organizationId")

        val pharmacy = supabase.postgrest.from("pharmacies").select(columns = Columns.ALL) {
            filter { eq("id", pharmacyId) }
        }.decodeList<FacilityDto>().singleOrNull()
            ?: throw IllegalStateException("Linked pharmacy not found")

        val latitude = pharmacy.latitude ?: return@runCatching null
        val longitude = pharmacy.longitude ?: return@runCatching null

        FacilityLocation(
            latitude = latitude,
            longitude = longitude,
            areaName = pharmacy.formattedAddress
                ?: pharmacy.location
                ?: pharmacy.name,
        )
    }

    override suspend fun getNearbyOrders(lat: Double, lng: Double, radius: Double): Result<List<NearbyOrderDto>> = runCatching {
        require(radius > 0.0) { "radius must be greater than zero" }
        supabase.postgrest.rpc(
            "get_nearby_orders",
            GetNearbyOrdersRpcParams(
                latitude = lat,
                longitude = lng,
                radiusKm = radius,
            ),
        ).decodeList<NearbyOrderDto>().map { dto ->
            NearbyOrderDto(
                id = dto.id?.takeIf { it.isNotBlank() },
                userName = dto.userName?.takeIf { it.isNotBlank() } ?: "Unknown",
                medicineName = dto.medicineName?.takeIf { it.isNotBlank() } ?: "Unknown medicine",
                distanceKm = dto.distanceKm ?: 0.0,
            )
        }
    }

    override fun observeNearbyOrdersRealtime(
        lat: Double,
        lng: Double,
        radius: Double,
    ): Flow<List<NearbyOrderDto>> = flow {
        // Initial load so the UI never shows "failed" due to a missing manual refresh.
        emit(getNearbyOrders(lat = lat, lng = lng, radius = radius).getOrDefault(emptyList()))
        // Re-query the RPC whenever any B2C order row changes; RLS/RPC filtering scopes results.
        try {
            realtime.tableChanges("orders").collect {
                emit(getNearbyOrders(lat = lat, lng = lng, radius = radius).getOrDefault(emptyList()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "observeNearbyOrdersRealtime failed", e)
            emit(emptyList())
        }
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

    override fun observeIncomingRequestsForWarehouse(warehouseId: String): Flow<List<Request>> = flow {
        Log.d(TAG, "=== OBSERVE INCOMING REQUESTS FOR WAREHOUSE INITIALIZED ===")
        val initialRequests = fetchIncomingRequestsForWarehouse(warehouseId).getOrThrow()
        Log.d(TAG, "Initial incoming requests count for warehouse $warehouseId: ${initialRequests.size}")
        emit(initialRequests)
        try {
            Log.d(TAG, "Starting realtime subscription to 'requests' table for warehouse...")
            realtime.tableChanges("requests").collect { event ->
                Log.d(TAG, "=== WAREHOUSE REQUESTS REALTIME EVENT ===")
                Log.d(TAG, "Event type: ${event::class.simpleName}")

                val updatedRequests = fetchIncomingRequestsForWarehouse(warehouseId).getOrThrow()
                Log.d(TAG, "Updated incoming requests count: ${updatedRequests.size}")
                updatedRequests.forEach { request ->
                    Log.d(TAG, "Request: ID=${request.id}, Status=${request.status}, WarehouseID=${request.warehouseId}")
                }
                emit(updatedRequests)
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Warehouse requests realtime subscription cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "observeIncomingRequestsForWarehouse realtime collect failed", e)
            Log.e(TAG, "Realtime error type: ${e::class.simpleName}")
            Log.e(TAG, "Realtime error message: ${e.message}")
            throw e
        }
    }

    override fun observeNotifications(): Flow<List<AppNotification>> = flow {
        // Fetch initial state
        val initial = fetchNotifications().getOrThrow()
        emit(initial)

        realtime.tableChanges("app_notifications").collect {
            val latest = fetchNotifications().getOrThrow()
            emit(latest)
        }
    }.flowOn(Dispatchers.IO)

    override fun observeProfile(): Flow<PharmacyProfile> =
        combine(
            authRepository.observeUserSnapshot(),
            profileRefreshRequests.onStart { emit(Unit) },
        ) { snapshot, _ -> snapshot }
            .mapLatest { snapshot ->
                if (snapshot == null) {
                    // Logout/session-race safe fallback: never crash collectors on null snapshot.
                    Log.w(TAG, "observeProfile fallback: snapshot unavailable, emitting placeholder profile.")
                    placeholderProfile()
                } else {
                    // Security fix: Only fetch the current user's profile, no realtime subscription
                    // to avoid receiving ALL users' profile changes. Profile updates are rare enough
                    // that polling on user snapshot changes is acceptable.
                    runCatching {
                        fetchProfileDomain(snapshot.userId, snapshot.email)
                    }.onFailure { error ->
                        Log.e(TAG, "observeProfile fallback: failed to fetch/decode profile for ${snapshot.userId}", error)
                    }.getOrElse {
                        placeholderProfile()
                    }
                }
            }

    override fun observeCompliance(): Flow<ComplianceOverview> = flow {
        Log.w(TAG, "observeCompliance disabled: Backend not confirmed")
    }


    override suspend fun updateProfile(profile: PharmacyProfile): Result<Unit> = runCatching {
        val identity = resolveAccessContext()

        if (identity.role == AccountType.WAREHOUSE) {
            val updateDto = WarehouseProfileUpdateDto(
                fullName = profile.managerName,
                phoneNumber = profile.contactPhone,
                avatarUrl = profile.avatarUrl,
            )

            val updatedRows = supabase.postgrest.from("profiles").update(updateDto) {
                select(Columns.ALL)
                filter { eq("id", profile.id) }
            }.decodeList<ProfileDetailsDto>()

            when {
                updatedRows.isEmpty() -> error("Profile update did not affect any row for profile ${profile.id}.")
                updatedRows.size > 1 -> error("Profile update affected duplicate rows for profile ${profile.id}.")
            }

            val updatedRow = updatedRows.single()
            require(updatedRow.id == profile.id) {
                "Profile update returned row ${updatedRow.id} instead of profile ${profile.id}."
            }
            require(updatedRow.fullName == updateDto.fullName) {
                "Profile update verification failed for full_name on profile ${profile.id}."
            }
            require(updatedRow.pharmacyName == updateDto.pharmacyName) {
                "Profile update verification failed for pharmacy_name on profile ${profile.id}."
            }
            require(updatedRow.phoneNumber == updateDto.phoneNumber) {
                "Profile update verification failed for phone_number on profile ${profile.id}."
            }
            require(updatedRow.avatarUrl == updateDto.avatarUrl) {
                "Profile update verification failed for avatar_url on profile ${profile.id}."
            }
            profileRefreshRequests.tryEmit(Unit)
            return@runCatching
        }

        val updateDto = ProfileUpdateDto(
            fullName = profile.managerName,
            pharmacyName = profile.pharmacyName.takeIf { it.isNotBlank() },
            pharmacyLocation = profile.addressLine.takeIf { profile.pharmacyName.isNotBlank() },
            defaultAddress = profile.addressLine.takeIf { it.isNotBlank() },
            phoneNumber = profile.contactPhone,
            avatarUrl = profile.avatarUrl,
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
        profileRefreshRequests.tryEmit(Unit)
    }.onFailure { e ->
        Log.e(TAG, "updateProfile failed", e)
    }.map { Unit }

    override suspend fun updateMyWarehouseLocation(
        address: String,
        latitude: Double,
        longitude: Double,
    ): Result<Warehouse> = runCatching {
        require(address.isNotBlank()) { "Warehouse address is required" }
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Invalid warehouse latitude" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Invalid warehouse longitude" }

        supabase.postgrest.rpc(
            "update_my_warehouse_location",
            UpdateMyWarehouseLocationRpcParams(
                address = address.trim(),
                latitude = latitude,
                longitude = longitude,
            ),
        ).decodeSingle<FacilityDto>().toWarehouse()
    }.onFailure { e ->
        Log.e(TAG, "updateMyWarehouseLocation failed", e)
    }

    private suspend fun updateNotificationsReadState(
        read: Boolean,
        notificationId: String? = null,
    ): Result<Unit> = runCatching {
        val identity = resolveAccessContext()
        
        when (identity.role) {
            AccountType.ADMIN -> {
                val adminUserId = resolveAdminNotificationUserId(identity.userId)
                // ADMIN updates their own notifications
                if (notificationId != null) {
                    // Verify ownership before updating
                    val rows = supabase.postgrest.from("app_notifications").select(columns = Columns.raw("id")) {
                        filter {
                            eq("user_id", adminUserId)
                            eq("id", notificationId)
                        }
                    }.decodeList<NotificationRowIdDto>()
                    
                    when {
                        rows.isEmpty() -> error("Marking notification as read failed because notification $notificationId was not found for admin user $adminUserId.")
                        rows.size > 1 -> error("Marking notification as read failed because notification $notificationId matched duplicate rows for admin user $adminUserId.")
                    }
                }
                
                supabase.postgrest.from("app_notifications").update(NotificationReadUpdateDto(read = read)) {
                    filter {
                        eq("user_id", adminUserId)
                        notificationId?.let { eq("id", it) }
                    }
                }
            }
            AccountType.PHARMACY -> {
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
            }
            AccountType.WAREHOUSE -> {
                if (notificationId != null) {
                    val rows = supabase.postgrest.from("app_notifications").select(columns = Columns.raw("id")) {
                        filter {
                            eq("user_id", identity.userId)
                            eq("id", notificationId)
                        }
                    }.decodeList<NotificationRowIdDto>()
                    when {
                        rows.isEmpty() -> error("Marking notification as read failed because notification $notificationId was not found for warehouse user ${identity.userId}.")
                        rows.size > 1 -> error("Marking notification as read failed because notification $notificationId matched duplicate rows for warehouse user ${identity.userId}.")
                    }
                }
                supabase.postgrest.from("app_notifications").update(NotificationReadUpdateDto(read = read)) {
                    filter {
                        eq("user_id", identity.userId)
                        notificationId?.let { eq("id", it) }
                    }
                }
            }
            AccountType.PUBLIC_USER -> {
                if (notificationId != null) {
                    val rows = supabase.postgrest.from("app_notifications").select(columns = Columns.raw("id")) {
                        filter {
                            eq("user_id", identity.userId)
                            eq("id", notificationId)
                        }
                    }.decodeList<NotificationRowIdDto>()
                    when {
                        rows.isEmpty() -> error("Marking notification as read failed because notification $notificationId was not found for public user ${identity.userId}.")
                        rows.size > 1 -> error("Marking notification as read failed because notification $notificationId matched duplicate rows for public user ${identity.userId}.")
                    }
                }
                supabase.postgrest.from("app_notifications").update(NotificationReadUpdateDto(read = read)) {
                    filter {
                        eq("user_id", identity.userId)
                        notificationId?.let { eq("id", it) }
                    }
                }
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
        val identity = resolveAccessContext()
        fetchOrdersForIdentity(identity)
    }

    private suspend fun fetchOrdersForIdentity(
        identity: com.pharmalink.domain.model.UserIdentity,
    ): List<DomainOrder> =
        when (identity.role) {
            AccountType.ADMIN -> {
                supabase.postgrest.from("orders").select(columns = Columns.ALL)
                    .decodeList<OrderDto>()
                    .map { it.toOrderDomain().getOrThrow() }
            }
            AccountType.WAREHOUSE -> {
                val orgId = identity.organizationId
                    ?: throw UnauthorizedException("unauthorized: WAREHOUSE user missing organizationId")
                supabase.postgrest.from("orders").select(columns = Columns.ALL) {
                    filter { eq("warehouse_id", orgId) }
                }.decodeList<OrderDto>()
                    .map { it.toOrderDomain().getOrThrow() }
            }
            AccountType.PHARMACY -> {
                val orgId = identity.organizationId
                    ?: throw UnauthorizedException("unauthorized: PHARMACY user missing organizationId")
                val allowedRequestIds = supabase.postgrest.from("requests").select(columns = Columns.raw("id")) {
                    filter { eq("pharmacy_id", orgId) }
                }.decodeList<RequestIdRow>()
                    .map { it.id }
                    .filter { it.isNotBlank() }

                if (allowedRequestIds.isEmpty()) {
                    emptyList()
                } else {
                    supabase.postgrest.from("orders").select(columns = Columns.ALL) {
                        filter { isIn("request_id", allowedRequestIds) }
                    }.decodeList<OrderDto>()
                        .map { it.toOrderDomain().getOrThrow() }
                }
            }
            AccountType.PUBLIC_USER -> {
                throw UnauthorizedException(
                    "unauthorized: PUBLIC_USER order access is blocked because the current schema does not expose a provable order ownership field.",
                )
            }
        }

    private suspend fun fetchRequests(): Result<List<Request>> = runCatching {
        val identity = resolveAccessContext()

        when (identity.role) {
            AccountType.PHARMACY -> {
                val orgId = identity.organizationId
                    ?: error("PHARMACY user missing organizationId")
                supabase.postgrest.from("requests").select(columns = Columns.ALL) {
                    filter { eq("pharmacy_id", orgId) }
                }.decodeList<RequestDto>()
                    .map { it.toDomain().getOrThrow() }
                    .let { hydrateRequestsWithItems(it) }
            }
            AccountType.WAREHOUSE -> {
                val orgId = identity.organizationId
                    ?: error("WAREHOUSE user missing organizationId")
                supabase.postgrest.from("requests").select(columns = Columns.ALL) {
                    filter { eq("warehouse_id", orgId) }
                }.decodeList<RequestDto>()
                    .map { it.toDomain().getOrThrow() }
                    .let { hydrateRequestsWithItems(it) }
            }
            AccountType.ADMIN -> {
                // ADMIN sees all requests (no filter)
                supabase.postgrest.from("requests").select(columns = Columns.ALL)
                    .decodeList<RequestDto>()
                    .map { it.toDomain().getOrThrow() }
                    .let { hydrateRequestsWithItems(it) }
            }
            AccountType.PUBLIC_USER -> {
                // PUBLIC_USER has no request access
                emptyList()
            }
        }
    }

    private suspend fun fetchIncomingRequestsForWarehouse(warehouseId: String): Result<List<Request>> = runCatching {
        supabase.postgrest.from("requests").select(columns = Columns.ALL) {
            filter { eq("warehouse_id", warehouseId) }
        }.decodeList<RequestDto>()
            .map { it.toDomain().getOrThrow() }
            .let { hydrateRequestsWithItems(it) }
    }

    private suspend fun fetchNotifications(): Result<List<AppNotification>> = runCatching {
        val identity = resolveAccessContext()
        
        when (identity.role) {
            AccountType.ADMIN -> {
                val adminUserId = resolveAdminNotificationUserId(identity.userId)
                Log.d(TAG, "fetchNotifications ADMIN: snapshotUserId=${identity.userId}, sessionUserId=$adminUserId")
                supabase.postgrest.from("app_notifications").select(columns = Columns.ALL) {
                    filter { eq("user_id", adminUserId) }
                }
                    .decodeList<AppNotificationDto>()
                    .mapNotNull { dto ->
                        dto.toDomain().getOrElse { error ->
                            Log.w(TAG, "Skipping notification ${dto.id} due to decode failure: ${error.message}")
                            null
                        }
                    }
            }
            AccountType.PHARMACY -> {
                val pharmacyId = resolvePharmacyId()
                supabase.postgrest.from("app_notifications").select(columns = Columns.ALL) {
                    filter { eq("pharmacy_id", pharmacyId) }
                }
                    .decodeList<AppNotificationDto>()
                    .mapNotNull { dto ->
                        dto.toDomain().getOrElse { error ->
                            Log.w(TAG, "Skipping notification ${dto.id} due to decode failure: ${error.message}")
                            null
                        }
                    }
            }
            AccountType.WAREHOUSE -> {
                Log.d(TAG, "fetchNotifications WAREHOUSE: userId=${identity.userId}")
                supabase.postgrest.from("app_notifications").select(columns = Columns.ALL) {
                    filter { eq("user_id", identity.userId) }
                }
                    .decodeList<AppNotificationDto>()
                    .mapNotNull { dto ->
                        dto.toDomain().getOrElse { error ->
                            Log.w(TAG, "Skipping notification ${dto.id} due to decode failure: ${error.message}")
                            null
                        }
                    }
            }
            AccountType.PUBLIC_USER -> {
                Log.d(TAG, "fetchNotifications PUBLIC_USER: userId=${identity.userId}")
                supabase.postgrest.from("app_notifications").select(columns = Columns.ALL) {
                    filter { eq("user_id", identity.userId) }
                }
                    .decodeList<AppNotificationDto>()
                    .mapNotNull { dto ->
                        dto.toDomain().getOrElse { error ->
                            Log.w(TAG, "Skipping notification ${dto.id} due to decode failure: ${error.message}")
                            null
                        }
                    }
            }
        }
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

    private suspend fun fetchProfileDomain(userId: String, contactEmail: String): PharmacyProfile {
        val profile = fetchProfile(userId).getOrThrow()
        val warehouseLocation = if (
            profile.accountType == AccountType.WAREHOUSE.name &&
            !profile.warehouseId.isNullOrBlank()
        ) {
            fetchWarehouseLocation(profile.warehouseId).getOrThrow()
        } else {
            null
        }
        return profile.toDomain(contactEmail, warehouseLocation).getOrThrow()
    }

    private suspend fun fetchWarehouseLocation(warehouseId: String): Result<WarehouseLocationDto> = runCatching {
        val rows = supabase.postgrest.from("warehouses").select(
            columns = Columns.raw("id,name,formatted_address,latitude,longitude"),
        ) {
            filter { eq("id", warehouseId) }
        }.decodeList<WarehouseLocationDto>()

        when {
            rows.isEmpty() -> error("Warehouse location not found for warehouse $warehouseId.")
            rows.size > 1 -> error("Duplicate warehouse location rows for warehouse $warehouseId.")
        }
        rows.single()
    }

    override suspend fun getOrder(orderId: String): Result<DomainOrder?> =
        runCatching {
            val identity = resolveAccessContext()

            when (identity.role) {
                AccountType.ADMIN -> {
                    supabase.postgrest.from("orders").select {
                        filter { eq("id", orderId) }
                    }.decodeList<OrderDto>().firstOrNull()?.toOrderDomain()?.getOrThrow()
                }
                AccountType.WAREHOUSE -> {
                    val orgId = identity.organizationId
                        ?: throw UnauthorizedException("unauthorized: WAREHOUSE user missing organizationId")
                    supabase.postgrest.from("orders").select {
                        filter {
                            eq("id", orderId)
                            eq("warehouse_id", orgId)
                        }
                    }.decodeList<OrderDto>().firstOrNull()?.toOrderDomain()?.getOrThrow()
                }
                AccountType.PHARMACY -> {
                    val orgId = identity.organizationId
                        ?: throw UnauthorizedException("unauthorized: PHARMACY user missing organizationId")
                    val order = supabase.postgrest.from("orders").select {
                        filter { eq("id", orderId) }
                    }.decodeList<OrderDto>().firstOrNull() ?: return@runCatching null

                    val requestId = order.requestId
                        ?: throw IllegalStateException("B2B order is missing requestId")
                    
                    val matchingRequestRows = supabase.postgrest.from("requests").select(columns = Columns.raw("id")) {
                        filter {
                            eq("id", requestId)
                            eq("pharmacy_id", orgId)
                        }
                    }.decodeList<RequestIdRow>()

                    if (matchingRequestRows.isEmpty()) {
                        null
                    } else {
                        order.toOrderDomain().getOrThrow()
                    }
                }
                AccountType.PUBLIC_USER -> {
                    throw UnauthorizedException(
                        "unauthorized: PUBLIC_USER order access is blocked because the current schema does not expose a provable order ownership field.",
                    )
                }
            }
        }

    override suspend fun getRequest(requestId: String): Result<Request?> =
        runCatching {
            val identity = resolveAccessContext()

            when (identity.role) {
                AccountType.PHARMACY -> {
                    val orgId = identity.organizationId
                        ?: error("PHARMACY user missing organizationId")
                    supabase.postgrest.from("requests").select {
                        filter {
                            eq("id", requestId)
                            eq("pharmacy_id", orgId)  // Ownership check
                        }
                    }.decodeList<RequestDto>().firstOrNull()
                        ?.toDomain(fetchRequestItems(requestId).getOrThrow())
                        ?.getOrThrow()
                }
                AccountType.WAREHOUSE -> {
                    val orgId = identity.organizationId
                        ?: error("WAREHOUSE user missing organizationId")
                    supabase.postgrest.from("requests").select {
                        filter {
                            eq("id", requestId)
                            eq("warehouse_id", orgId)  // Ownership check
                        }
                    }.decodeList<RequestDto>().firstOrNull()
                        ?.toDomain(fetchRequestItems(requestId).getOrThrow())
                        ?.getOrThrow()
                }
                AccountType.ADMIN -> {
                    // ADMIN can view any request by ID
                    supabase.postgrest.from("requests").select {
                        filter { eq("id", requestId) }
                    }.decodeList<RequestDto>().firstOrNull()
                        ?.toDomain(fetchRequestItems(requestId).getOrThrow())
                        ?.getOrThrow()
                }
                AccountType.PUBLIC_USER -> {
                    null // PUBLIC_USER cannot view requests
                }
            }
        }


    override suspend fun getWarehouseShipments(warehouseId: String): Result<List<WarehouseShipment>> =
        Result.failure(UnsupportedOperationException("Backend not confirmed"))

    override suspend fun createRequest(request: Request): Result<Request> =
        runCatching {
            Log.d(TAG, "=== CREATE REQUEST DEBUG ===")
            Log.d(TAG, "Input request ID: ${request.id}")
            Log.d(TAG, "Medicine: ${request.medicineName}")
            Log.d(TAG, "Quantity: ${request.quantity} ${request.unit}")
            Log.d(TAG, "Priority: ${request.priority.name}")
            Log.d(TAG, "Warehouse ID: ${request.warehouseId}")
            Log.d(TAG, "Warehouse Name: ${request.warehouseName}")

            // Role check: Only PHARMACY can create requests
            val identity = resolveAccessContext()
            Log.d(TAG, "User role: ${identity.role}, orgId: ${identity.organizationId}")

            require(identity.role == AccountType.PHARMACY) {
                "Only PHARMACY users can create requests"
            }

            val pharmacyId = identity.organizationId
                ?: throw IllegalStateException("PHARMACY user missing organizationId")
            Log.d(TAG, "Resolved pharmacy ID: $pharmacyId")

            val createdAt = System.currentTimeMillis().toString()
            Log.d(TAG, "Created at timestamp: $createdAt")

            // Force status to DRAFT - initial state for all new requests
            val initialStatus = RequestStatus.DRAFT.name
            Log.d(TAG, "Initial status: $initialStatus (forced to DRAFT)")
            val requestItems = normalizeRequestItems(
                requestId = "",
                items = request.items.ifEmpty {
                    listOf(request.toLegacyRequestItem(requestId = "", lineNo = 1))
                },
            )
            val firstItem = requestItems.first()

            val reqInsert = RequestInsertDto(
                pharmacyId = pharmacyId,
                warehouseId = request.warehouseId,
                medicineId = firstItem.medicineId,
                medicineName = firstItem.medicineName.trim(),
                quantity = firstItem.quantity,
                unit = firstItem.unit,
                totalPrice = request.totalPrice,
                medicineSubtitle = firstItem.medicineSubtitle,
                priority = request.priority.name,
                warehouseName = request.warehouseName,
                supplierName = request.supplierName,
                status = initialStatus,
            )

            Log.d(TAG, "Attempting to insert request into Supabase...")
            val insertedRequest = supabase.postgrest.from("requests").insert(reqInsert) {
                select(Columns.ALL)
            }.decodeSingle<RequestDto>()

            Log.d(TAG, "أ¢إ“â€¦ REQUEST INSERT SUCCESS!")
            Log.d(TAG, "Returned request ID: ${insertedRequest.id}")
            Log.d(TAG, "Returned request status: ${insertedRequest.status}")
            Log.d(TAG, "Returned request warehouse ID: ${insertedRequest.warehouseId}")
            Log.d(TAG, "Returned request warehouse: ${insertedRequest.warehouseName}")
            Log.d(TAG, "Returned request supplier: ${insertedRequest.supplierName}")

            val insertedItems = runCatching {
                insertRequestItems(
                    requestId = insertedRequest.id,
                    items = requestItems,
                )
            }.getOrElse { itemInsertError ->
                runCatching {
                    supabase.postgrest.from("requests").delete {
                        filter {
                            eq("id", insertedRequest.id)
                            eq("pharmacy_id", pharmacyId)
                            eq("status", RequestStatus.DRAFT.name)
                        }
                    }
                }.onFailure { cleanupError ->
                    Log.e(TAG, "Failed to cleanup DRAFT request ${insertedRequest.id} after item insert failure", cleanupError)
                }
                throw itemInsertError
            }

            val domainRequest = insertedRequest.toDomain(insertedItems).getOrThrow()
            Log.d(TAG, "أ¢إ“â€¦ CREATE REQUEST FLOW COMPLETED")
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

    /**
     * Returns pharmacy organization id only for PHARMACY users.
     * Non-PHARMACY roles intentionally receive null and should use safe defaults.
     */
    private suspend fun resolvePharmacyIdOrNull(): String? {
        val snapshot = authRepository.getUserSnapshot() ?: run {
            Log.w(TAG, "resolvePharmacyIdOrNull: no snapshot available.")
            return null
        }
        if (snapshot.accountType != AccountType.PHARMACY) {
            return null
        }
        return snapshot.toUserIdentity().organizationId?.takeIf { it.isNotBlank() }
    }

    /**
     * Strict PHARMACY linkage resolver used only when pharmacy linkage is mandatory.
     * Throws MissingPharmacyLinkageException for PHARMACY paths with absent linkage.
     */
    private suspend fun resolvePharmacyId(): String {
        val snapshot = authRepository.getUserSnapshot()
        if (snapshot?.accountType != AccountType.PHARMACY) {
            throw IllegalStateException("resolvePharmacyId called for non-PHARMACY context.")
        }
        return resolvePharmacyIdOrNull()
            ?: throw MissingPharmacyLinkageException(snapshot.userId)
    }

    /**
     * Resolves the current user's access context including role and organization ID.
     * This is the role-aware replacement for resolvePharmacyId().
     */
    private suspend fun resolveAccessContext(): com.pharmalink.domain.model.UserIdentity = runCatching {
        val userSnapshot = authRepository.getUserSnapshot()
            ?: error("No user snapshot available")
        userSnapshot.toUserIdentity()
    }.getOrThrow()

    private fun resolveAdminNotificationUserId(snapshotUserId: String): String {
        val sessionUserId = auth.currentUserOrNull()?.id
        if (sessionUserId != null && sessionUserId != snapshotUserId) {
            Log.w(
                TAG,
                "Admin notification userId mismatch: snapshot=$snapshotUserId, session=$sessionUserId. Using session user id.",
            )
        }
        return sessionUserId ?: snapshotUserId
    }

    private fun readUploadFile(
        uri: Uri,
        allowedMimeTypes: Map<String, String>,
    ): UploadFile {
        val mimeType = context.contentResolver.getType(uri)
            ?: inferMimeTypeFromUri(uri)
            ?: throw IllegalArgumentException("Could not determine file type for URI: $uri")
        val extension = allowedMimeTypes[mimeType]
            ?: throw IllegalArgumentException("Unsupported upload file type: $mimeType")
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Could not read URI: $uri")
        return UploadFile(
            bytes = bytes,
            extension = extension,
            contentType = ContentType.parse(mimeType),
        )
    }

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
        require(updatedRow.avatarUrl == expected.avatarUrl) {
            "Profile update verification failed for avatar_url on profile $profileId."
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

    override suspend fun updateRequest(requestId: String, updates: com.pharmalink.domain.model.RequestUpdate): Result<Request> =
        runCatching {
            Log.d(TAG, "=== UPDATE REQUEST ===")
            Log.d(TAG, "Request ID: $requestId")
            Log.d(TAG, "Updates: status=${updates.status}, warehouseId=${updates.warehouseId}")

            val identity = resolveAccessContext()
            Log.d(TAG, "User role: ${identity.role}, orgId: ${identity.organizationId}")

            // Fetch current request
            val currentRequest = getRequest(requestId).getOrThrow()
                ?: throw IllegalArgumentException("Request not found: $requestId")
            Log.d(TAG, "Current request status: ${currentRequest.status}")

            // Ownership check
            when (identity.role) {
                AccountType.PHARMACY -> {
                    // Explicit ownership check: PHARMACY can only update their own requests
                    require(currentRequest.pharmacyId == identity.organizationId) {
                        "Not authorized to update this request"
                    }
                    require(currentRequest.status != RequestStatus.FULFILLED && 
                            currentRequest.status != RequestStatus.REJECTED) {
                        "Cannot update request in ${currentRequest.status} status"
                    }
                }
                AccountType.WAREHOUSE -> {
                    require(currentRequest.warehouseId == identity.organizationId) {
                        "Not authorized to update this request"
                    }
                }
                AccountType.ADMIN -> {
                    // No lifecycle status transition permission in Phase 0D.1
                    if (updates.status != null && updates.status != currentRequest.status) {
                        throw IllegalStateException("ADMIN cannot change request lifecycle status in Phase 0D.1")
                    }
                }
                AccountType.PUBLIC_USER -> {
                    throw IllegalStateException("PUBLIC_USER cannot update requests")
                }
            }

            // Transition validation
            if (updates.status != null && updates.status != currentRequest.status) {
                require(com.pharmalink.domain.model.RequestTransitions.canTransition(
                    currentRequest.status,
                    updates.status,
                    identity.role
                )) {
                    "Invalid status transition: ${currentRequest.status} -> ${updates.status} for role ${identity.role}"
                }
                Log.d(TAG, "Status transition valid: ${currentRequest.status} -> ${updates.status}")
            }

            val normalizedUpdateItems = updates.items?.let { items ->
                require(items.isNotEmpty()) { "Request basket must contain at least one item" }
                require(currentRequest.status == RequestStatus.DRAFT) {
                    "Replacing request basket items is allowed only for DRAFT requests."
                }
                normalizeRequestItems(requestId, items)
            }
            val firstUpdateItem = normalizedUpdateItems?.firstOrNull()
            val replacedItems = normalizedUpdateItems?.let { items ->
                replaceRequestItems(requestId, items).getOrThrow()
            }

            // Build update DTO
            val updateDto = RequestUpdateDto(
                status = updates.status?.name,
                warehouseId = updates.warehouseId,
                warehouseName = updates.warehouseName,
                notes = updates.notes,
                medicineId = firstUpdateItem?.medicineId,
                medicineName = firstUpdateItem?.medicineName,
                medicineSubtitle = firstUpdateItem?.medicineSubtitle,
                quantity = firstUpdateItem?.quantity,
                unit = firstUpdateItem?.unit,
            )

            // Apply update via Supabase
            val updatedRequest = supabase.postgrest.from("requests").update(updateDto) {
                select(Columns.ALL)
                filter { eq("id", requestId) }
            }.decodeSingle<RequestDto>()

            Log.d(TAG, "أ¢إ“â€¦ REQUEST UPDATE SUCCESS")
            updatedRequest.toDomain(replacedItems ?: fetchRequestItems(requestId).getOrThrow()).getOrThrow()
        }.onFailure { exception ->
            Log.e(TAG, "أ¢â€Œإ’ UPDATE REQUEST FAILED", exception)
        }

    override suspend fun getRequestItems(requestId: String): Result<List<RequestItem>> =
        fetchRequestItems(requestId)

    override suspend fun replaceRequestItems(requestId: String, items: List<RequestItem>): Result<List<RequestItem>> =
        runCatching {
            require(items.isNotEmpty()) { "Request basket must contain at least one item" }

            val ownedRequest = getOwnedRequestForPharmacy(
                requestId = requestId,
                actionName = "Replacing request basket items",
            )
            require(ownedRequest.status == RequestStatus.DRAFT) {
                "Replacing request basket items is allowed only for DRAFT requests."
            }

            val normalizedItems = normalizeRequestItems(requestId, items)
            val firstItem = normalizedItems.first()

            supabase.postgrest.from("request_items").delete {
                filter { eq("request_id", requestId) }
            }

            val insertedItems = insertRequestItems(
                requestId = requestId,
                items = normalizedItems,
            )

            supabase.postgrest.from("requests").update(
                RequestUpdateDto(
                    medicineId = firstItem.medicineId,
                    medicineName = firstItem.medicineName,
                    medicineSubtitle = firstItem.medicineSubtitle,
                    quantity = firstItem.quantity,
                    unit = firstItem.unit,
                    updatedAt = Instant.now().toString(),
                )
            ) {
                filter {
                    eq("id", requestId)
                    eq("pharmacy_id", ownedRequest.pharmacyId)
                    eq("status", RequestStatus.DRAFT.name)
                }
            }

            insertedItems
        }

    override suspend fun deleteRequest(requestId: String): Result<Unit> =
        runCatching {
            val ownedRequest = getOwnedRequestForPharmacy(
                requestId = requestId,
                actionName = "Deleting request",
            )

            require(ownedRequest.status == RequestStatus.DRAFT) {
                "Deleting requests is allowed only for DRAFT requests."
            }

            supabase.postgrest.from("requests").delete {
                filter {
                    eq("id", requestId)
                    eq("pharmacy_id", ownedRequest.pharmacyId)
                }
            }
        }.map { Unit }

    override suspend fun submitRequest(requestId: String): Result<Unit> =
        submitPharmacyRequest(requestId).map { Unit }

    override suspend fun submitPharmacyRequest(requestId: String): Result<Request> =
        callB2bRequestRpc(
            functionName = "submit_pharmacy_request",
            params = B2bRequestIdRpcParams(requestId = requestId),
        ).onSuccess { submittedRequest ->
            sendWarehouseNotificationForSubmittedRequest(submittedRequest)
        }

    override suspend fun warehouseAcceptB2bRequest(
        requestId: String,
        totalPriceCents: Long,
    ): Result<Request> =
        callB2bRequestRpc(
            functionName = "warehouse_accept_b2b_request",
            params = WarehouseAcceptB2bRpcParams(
                requestId = requestId,
                totalPriceCents = totalPriceCents,
            ),
        ).onSuccess { quotedRequest ->
            sendPharmacyNotificationForB2bRequest(
                request = quotedRequest,
                title = "عرض سعر جديد",
                body = "تم إرسال عرض سعر جديد لطلب المستودع. يرجى مراجعة السعر قبل المتابعة.",
            )
        }

    override suspend fun warehouseRejectB2bRequest(
        requestId: String,
        reason: String?,
    ): Result<Request> =
        callB2bRequestRpc(
            functionName = "warehouse_reject_b2b_request",
            params = WarehouseRejectB2bRpcParams(
                requestId = requestId,
                rejectionReason = reason,
            ),
        )

    override suspend fun warehouseStartB2bFulfillment(requestId: String): Result<Request> =
        callB2bRequestRpc(
            functionName = "warehouse_start_b2b_fulfillment",
            params = B2bRequestIdRpcParams(requestId = requestId),
        )

    override suspend fun warehouseMarkB2bDelivered(
        requestId: String,
        deliveryNote: String?, // deliveryNote is no longer used by the RPC, but keeping for interface compatibility
    ): Result<Request> =
        callB2bRequestRpc(
            functionName = "warehouse_mark_b2b_delivered",
            params = WarehouseMarkB2bDeliveredRpcParams(
                requestId = requestId,
                deliveryNote = deliveryNote,
            ),
        ).onSuccess { deliveredRequest ->
            // After successful delivery, send a notification to the pharmacy
            sendAppNotification(
                recipientPharmacyId = deliveredRequest.pharmacyId,
                title = "طھظ… طھط³ظ„ظٹظ… ط§ظ„ط·ظ„ط¨",
                content = "ظ‚ط§ظ… ط§ظ„ظ…ط³طھظˆط¯ط¹ ط¨طھط³ظ„ظٹظ… ط·ظ„ط¨ظƒ ط¨ظ†ط¬ط§ط­. طھظ… طھط­ط¯ظٹط« ط³ط¬ظ„ط§طھ ط§ظ„طھظˆط±ظٹط¯.",
                type = NotificationType.ORDER_UPDATE,
                destination = NotificationDestination.REQUEST,
                destinationArgs = mapOf("requestId" to deliveredRequest.id)
            )
        }

    override suspend fun pharmacyAcceptB2bQuote(requestId: String): Result<Request> =
        callB2bRequestRpc(
            functionName = "pharmacy_accept_b2b_quote",
            params = B2bRequestIdRpcParams(requestId = requestId),
        ).onSuccess { acceptedRequest ->
            sendWarehouseNotificationForB2bQuoteDecision(
                request = acceptedRequest,
                title = "تمت الموافقة على عرض السعر",
                body = "وافقت الصيدلية على عرض السعر. يمكن بدء تجهيز الطلب.",
            )
        }

    override suspend fun pharmacyRejectB2bQuote(
        requestId: String,
        reason: String?,
    ): Result<Request> =
        callB2bRequestRpc(
            functionName = "pharmacy_reject_b2b_quote",
            params = WarehouseRejectB2bRpcParams(
                requestId = requestId,
                rejectionReason = reason,
            ),
        ).onSuccess { rejectedRequest ->
            sendWarehouseNotificationForB2bQuoteDecision(
                request = rejectedRequest,
                title = "تم رفض عرض السعر",
                body = "رفضت الصيدلية عرض السعر. تم إغلاق الطلب.",
            )
        }

    private suspend fun sendAppNotification(
        recipientPharmacyId: String,
        title: String,
        content: String,
        type: NotificationType,
        destination: NotificationDestination,
        destinationArgs: Map<String, String> = emptyMap()
    ): Result<Unit> = runCatching<Unit> {
        // Find the user ID associated with the pharmacy
        val pharmacyUserId = supabase.postgrest.from("profiles").select(columns = Columns.raw("id")) {
            filter { eq("pharmacy_id", recipientPharmacyId) }
        }.decodeSingle<UserIdDto>().id

        supabase.postgrest.from("app_notifications").insert(
            AppNotificationInsertDto(
                userId = pharmacyUserId,
                pharmacyId = recipientPharmacyId,
                title = title,
                body = content,
                type = type.name,
                category = NotificationCategory.ORDERS.name,
                read = false,
                destination = destination.name,
                destinationId = destinationArgs.notificationDestinationId()
            )
        )
        Unit
        Log.d(TAG, "Notification sent for request $recipientPharmacyId: $title")
    }.onFailure { e ->
        Log.e(TAG, "Failed to send notification to pharmacy $recipientPharmacyId: ${e.message}", e)
    }

    private suspend fun sendWarehouseNotificationForSubmittedRequest(
        submittedRequest: Request,
    ) {
        val warehouseId = submittedRequest.warehouseId?.takeIf { it.isNotBlank() } ?: run {
            Log.w(TAG, "Skipping warehouse notification for request ${submittedRequest.id}: warehouseId is missing.")
            return
        }

        runCatching {
            val warehouseUserRows = supabase.postgrest.from("profiles").select(columns = Columns.raw("id")) {
                filter {
                    eq("warehouse_id", warehouseId)
                    eq("account_type", AccountType.WAREHOUSE.name)
                    eq("is_active", true)
                }
            }.decodeList<UserIdDto>()

            val warehouseUserId = requireSingleProfileRow(
                rows = warehouseUserRows,
                userId = warehouseId,
                rowLabel = "Active warehouse profile",
            ).id

            supabase.postgrest.from("app_notifications").insert(
                AppNotificationInsertDto(
                    userId = warehouseUserId,
                    pharmacyId = submittedRequest.pharmacyId,
                    title = "ط·ظ„ط¨ ط¬ط¯ظٹط¯ ظ…ظ† طµظٹط¯ظ„ظٹط©",
                    body = "طھظ… ط§ط³طھظ„ط§ظ… ط·ظ„ط¨ ط¬ط¯ظٹط¯ ظˆظٹط­طھط§ط¬ ظ…ط±ط§ط¬ط¹طھظƒ.",
                    type = NotificationType.ORDER_UPDATE.name,
                    category = NotificationCategory.REQUESTS.name,
                    read = false,
                    destination = NotificationDestination.REQUEST.name,
                    destinationId = submittedRequest.id,
                ),
            )

            Log.d(TAG, "Warehouse notification created for request ${submittedRequest.id} -> user $warehouseUserId")
        }.onFailure { error ->
            Log.e(
                TAG,
                "Failed to create warehouse notification for request ${submittedRequest.id} (warehouseId=$warehouseId): ${error.message}",
                error,
            )
        }
    }

    private suspend fun sendPharmacyNotificationForB2bRequest(
        request: Request,
        title: String,
        body: String,
    ) {
        val pharmacyId = request.pharmacyId.takeIf { it.isNotBlank() } ?: run {
            Log.w(TAG, "Skipping B2B pharmacy notification: target pharmacy is missing.")
            return
        }

        runCatching {
            val pharmacyUserRows = supabase.postgrest.from("profiles").select(columns = Columns.raw("id")) {
                filter {
                    eq("pharmacy_id", pharmacyId)
                    eq("account_type", AccountType.PHARMACY.name)
                    eq("is_active", true)
                }
            }.decodeList<UserIdDto>()

            val pharmacyUserId = requireSingleProfileRow(
                rows = pharmacyUserRows,
                userId = pharmacyId,
                rowLabel = "Active pharmacy profile",
            ).id

            supabase.postgrest.from("app_notifications").insert(
                AppNotificationInsertDto(
                    userId = pharmacyUserId,
                    pharmacyId = pharmacyId,
                    title = title,
                    body = body,
                    type = NotificationType.ORDER_UPDATE.name,
                    category = NotificationCategory.REQUESTS.name,
                    read = false,
                    destination = NotificationDestination.REQUEST.name,
                    destinationId = request.id,
                ),
            )

            Log.d(TAG, "B2B pharmacy notification created.")
        }.onFailure {
            Log.w(TAG, "B2B pharmacy notification delivery failed.")
        }
    }

    private suspend fun sendWarehouseNotificationForB2bQuoteDecision(
        request: Request,
        title: String,
        body: String,
    ) {
        val warehouseId = request.warehouseId?.takeIf { it.isNotBlank() } ?: run {
            Log.w(TAG, "Skipping B2B warehouse notification: target warehouse is missing.")
            return
        }

        runCatching {
            val warehouseUserRows = supabase.postgrest.from("profiles").select(columns = Columns.raw("id")) {
                filter {
                    eq("warehouse_id", warehouseId)
                    eq("account_type", AccountType.WAREHOUSE.name)
                    eq("is_active", true)
                }
            }.decodeList<UserIdDto>()

            val warehouseUserId = requireSingleProfileRow(
                rows = warehouseUserRows,
                userId = warehouseId,
                rowLabel = "Active warehouse profile",
            ).id

            supabase.postgrest.from("app_notifications").insert(
                AppNotificationInsertDto(
                    userId = warehouseUserId,
                    pharmacyId = request.pharmacyId,
                    title = title,
                    body = body,
                    type = NotificationType.ORDER_UPDATE.name,
                    category = NotificationCategory.REQUESTS.name,
                    read = false,
                    destination = NotificationDestination.REQUEST.name,
                    destinationId = request.id,
                ),
            )
        }.onFailure {
            Log.w(TAG, "B2B warehouse notification delivery failed.")
        }
    }


    private suspend fun fetchRequestItems(requestId: String): Result<List<RequestItem>> =
        runCatching {
            supabase.postgrest.from("request_items").select(columns = Columns.ALL) {
                filter { eq("request_id", requestId) }
            }.decodeList<RequestItemDto>()
                .sortedWith(compareBy<RequestItemDto> { it.lineNo }.thenBy { it.createdAt.orEmpty() }.thenBy { it.id })
                .map { it.toDomain() }
        }

    private suspend fun fetchRequestItemsByRequestIds(requestIds: List<String>): Map<String, List<RequestItem>> {
        val ids = requestIds.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return emptyMap()

        return supabase.postgrest.from("request_items").select(columns = Columns.ALL) {
            filter { isIn("request_id", ids) }
        }.decodeList<RequestItemDto>()
            .sortedWith(compareBy<RequestItemDto> { it.requestId }.thenBy { it.lineNo }.thenBy { it.createdAt.orEmpty() }.thenBy { it.id })
            .map { it.toDomain() }
            .groupBy { it.requestId }
    }

    private suspend fun hydrateRequestsWithItems(requests: List<Request>): List<Request> {
        if (requests.isEmpty()) return requests
        val itemsByRequestId = fetchRequestItemsByRequestIds(requests.map { it.id })
        return requests.map { request ->
            val items = itemsByRequestId[request.id].orEmpty()
            if (items.isEmpty()) {
                request
            } else {
                val firstItem = items.first()
                request.copy(
                    medicineId = firstItem.medicineId,
                    medicineName = firstItem.medicineName,
                    medicineSubtitle = firstItem.medicineSubtitle,
                    quantity = firstItem.quantity,
                    unit = firstItem.unit,
                    items = items,
                )
            }
        }
    }

    private fun normalizeRequestItems(requestId: String, items: List<RequestItem>): List<RequestItem> {
        require(items.isNotEmpty()) { "Request basket must contain at least one item" }
        return items.mapIndexed { index, item ->
            require(item.medicineId.isNotBlank()) { "medicineId is required for request item ${index + 1}" }
            require(item.medicineName.isNotBlank()) { "medicineName is required for request item ${index + 1}" }
            require(item.quantity > 0) { "quantity must be greater than zero for request item ${index + 1}" }
            require(item.unit.isNotBlank()) { "unit is required for request item ${index + 1}" }
            item.copy(
                requestId = requestId,
                lineNo = index + 1,
                medicineName = item.medicineName.trim(),
                medicineSubtitle = item.medicineSubtitle.trim(),
                unit = item.unit.trim(),
            )
        }
    }

    private suspend fun insertRequestItems(requestId: String, items: List<RequestItem>): List<RequestItem> {
        val insertDtos = normalizeRequestItems(requestId, items).map { item ->
            RequestItemInsertDto(
                requestId = requestId,
                lineNo = item.lineNo,
                medicineId = item.medicineId,
                medicineName = item.medicineName,
                medicineSubtitle = item.medicineSubtitle,
                quantity = item.quantity,
                unit = item.unit,
            )
        }

        return supabase.postgrest.from("request_items").insert(insertDtos) {
            select(Columns.ALL)
        }.decodeList<RequestItemDto>()
            .sortedWith(compareBy<RequestItemDto> { it.lineNo }.thenBy { it.createdAt.orEmpty() }.thenBy { it.id })
            .map { it.toDomain() }
    }

    private fun Request.toLegacyRequestItem(requestId: String, lineNo: Int): RequestItem =
        RequestItem(
            requestId = requestId,
            lineNo = lineNo,
            medicineId = requireNotNull(medicineId?.takeIf { it.isNotBlank() }) {
                "medicineId is required for B2B pharmacy requests."
            },
            medicineName = medicineName,
            medicineSubtitle = medicineSubtitle,
            quantity = quantity,
            unit = unit,
            createdAt = createdAtLabel,
            updatedAt = updatedAtLabel,
        )

    private suspend inline fun <reified T : Any> callB2bRequestRpc(
        functionName: String,
        params: T,
    ): Result<Request> =
        runCatching {
            val response = supabase.postgrest.rpc(functionName, params)
                .decodeAs<B2bRequestRpcResponse>()
            response.request
                .toDomain(response.items.map { it.toDomain() })
                .getOrThrow()
        }.onFailure { error ->
            Log.e(TAG, "B2B RPC $functionName failed", error)
        }

    override suspend fun markNotificationRead(notificationId: String): Result<Unit> =
        updateNotificationsReadState(
            read = true,
            notificationId = notificationId,
        )

    override suspend fun markAllNotificationsRead(): Result<Unit> =
        updateNotificationsReadState(read = true)

    override suspend fun deleteNotification(notificationId: String): Result<Unit> =
        runCatching {
            val identity = resolveAccessContext()
            
            when (identity.role) {
                AccountType.PHARMACY -> {
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
                }
                AccountType.ADMIN -> {
                    val adminUserId = resolveAdminNotificationUserId(identity.userId)
                    // ADMIN users: delete by user_id
                    supabase.postgrest.from("app_notifications").delete {
                        filter {
                            eq("id", notificationId)
                            eq("user_id", adminUserId)
                        }
                    }
                }
                AccountType.WAREHOUSE -> {
                    supabase.postgrest.from("app_notifications").delete {
                        filter {
                            eq("id", notificationId)
                            eq("user_id", identity.userId)
                        }
                    }
                }
                AccountType.PUBLIC_USER -> {
                    supabase.postgrest.from("app_notifications").delete {
                        filter {
                            eq("id", notificationId)
                            eq("user_id", identity.userId)
                        }
                    }
                }
                else -> {
                    Log.w(TAG, "deleteNotification no-op: unsupported account type ${identity.role}.")
                    return@runCatching Unit
                }
            }
        }.map { Unit }

    override suspend fun deleteAllNotifications(): Result<Unit> =
        runCatching {
            val identity = resolveAccessContext()
            
            when (identity.role) {
                AccountType.PHARMACY -> {
                    val pharmacyId = resolvePharmacyId()
                    supabase.postgrest.from("app_notifications").delete {
                        filter { eq("pharmacy_id", pharmacyId) }
                    }
                }
                AccountType.ADMIN -> {
                    val adminUserId = resolveAdminNotificationUserId(identity.userId)
                    // ADMIN users: delete all by user_id
                    supabase.postgrest.from("app_notifications").delete {
                        filter { eq("user_id", adminUserId) }
                    }
                }
                AccountType.WAREHOUSE -> {
                    supabase.postgrest.from("app_notifications").delete {
                        filter { eq("user_id", identity.userId) }
                    }
                }
                AccountType.PUBLIC_USER -> {
                    supabase.postgrest.from("app_notifications").delete {
                        filter { eq("user_id", identity.userId) }
                    }
                }
                else -> {
                    Log.w(TAG, "deleteAllNotifications no-op: unsupported account type ${identity.role}.")
                    return@runCatching Unit
                }
            }
        }.map { Unit }

    override suspend fun updateNotificationsPreference(enabled: Boolean): Result<Unit> =
        runCatching {
            val identity = resolveAccessContext()
            val userId = identity.userId
            supabase.postgrest.from("profiles").update(
                ProfileNotificationsPreferenceUpdateDto(notificationsEnabled = enabled),
            ) {
                filter { eq("id", userId) }
            }
        }.map { Unit }

    override suspend fun submitSupportRequest(
        subject: String,
        message: String,
        category: String?,
    ): Result<Unit> = runCatching {
        val identity = authRepository.observeUserSnapshot().firstOrNull()?.toUserIdentity()
            ?: error("طھط¹ط°ط± طھط­ط¯ظٹط¯ ظ‡ظˆظٹط© ط§ظ„ظ…ط³طھط®ط¯ظ… ط§ظ„ط­ط§ظ„ظٹط©.")
        check(identity.role != AccountType.ADMIN) {
            "Support requests are intended for non-admin accounts."
        }

        supabase.postgrest.rpc(
            "submit_support_request",
            SubmitSupportRequestRpcParams(
                subject = subject,
                message = message,
                category = category,
            ),
        ).decodeAs<String>()
    }.map { Unit }

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

    // ==================== B2C Customer Order Methods (Phase 4.3B) ====================

    override suspend fun createCustomerOrder(
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
        prescriptionUrl: String?,
    ): Result<DomainOrder> = runCatching {
        // 1. Role validation: PUBLIC_USER only
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PUBLIC_USER) {
            "Only PUBLIC_USER can create customer orders"
        }

        require(medicineId.isNotBlank()) {
            "medicineId must not be blank"
        }

        require(quantity > 0) {
            "Quantity must be greater than 0"
        }

        require(requestScope != CustomerRequestScope.SPECIFIC_PHARMACY || !pharmacyId.isNullOrBlank()) {
            "pharmacyId must not be blank for SPECIFIC_PHARMACY"
        }

        // TODO: replace with catalog-backed medicine existence validation.
        
        // 2. Get customer ID from identity
        val customerId = identity.userId
            ?: throw IllegalStateException("PUBLIC_USER missing userId")
        
        // 3. Validate fulfillment requirements
        val requiresLocation = fulfillmentType == FulfillmentType.DELIVERY ||
            requestScope == CustomerRequestScope.ALL_PHARMACIES
        if (requiresLocation) {
            require(!deliveryAddress.isNullOrBlank()) {
                "Delivery address required for DELIVERY fulfillment or ALL_PHARMACIES requests"
            }
            require(deliveryLatitude != null && deliveryLongitude != null) {
                "Delivery latitude and longitude required for DELIVERY fulfillment or ALL_PHARMACIES requests"
            }
        }
        if (fulfillmentType == FulfillmentType.DELIVERY) {
            require(!deliveryPhone.isNullOrBlank()) {
                "Delivery phone required for DELIVERY fulfillment"
            }
        }
        
        // 4. Create order DTO
        val createOrderDto = CreateOrderDto(
            medicineId = medicineId,
            medicineName = medicineName,
            quantity = quantity,
            unit = unit,
            pharmacyId = pharmacyId,
            customerId = customerId,
            orderType = OrderType.CUSTOMER_PHARMACY.name,
            urgency = urgency.name,
            requestScope = requestScope.name,
            fulfillmentType = fulfillmentType.name,
            deliveryAddress = deliveryAddress,
            deliveryLatitude = deliveryLatitude,
            deliveryLongitude = deliveryLongitude,
            deliveryPhone = deliveryPhone,
            notes = notes,
            prescriptionUrl = prescriptionUrl,
        )
        
        // 5. Insert order (starts PENDING, totalPriceCents = null)
        val result = try {
            supabase.postgrest.from("orders").insert(createOrderDto) {
                select(Columns.ALL)
            }.decodeSingle<OrderDto>()
        } catch (error: Throwable) {
            deleteUploadedPrescriptionIfOwned(
                prescriptionUrl = prescriptionUrl,
                userId = customerId,
                cause = error,
            )
            throw error
        }

        val order = result.toOrderDomain().getOrThrow()

        // 6. Pharmacy notification is handled by the DB trigger notify_pharmacy_on_new_order.
        //    No Kotlin-side insert to avoid duplicate notifications.

        order
    }

    override suspend fun uploadPrescription(uri: android.net.Uri): Result<String> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PUBLIC_USER) {
            "Only PUBLIC_USER can upload prescriptions"
        }
        val sessionUserId = auth.currentUserOrNull()?.id
            ?: throw UnauthorizedException("unauthorized: no authenticated Supabase user")
        require(sessionUserId == identity.userId) {
            "Authenticated user does not match access context"
        }

        val bucket = supabase.storage.from("prescriptions")
        val uploadFile = readUploadFile(
            uri = uri,
            allowedMimeTypes = prescriptionUploadMimeTypes,
        )
        val fileName = "${sessionUserId}_${System.currentTimeMillis()}_${UUID.randomUUID()}.${uploadFile.extension}"

        bucket.upload(fileName, uploadFile.bytes) {
            contentType = uploadFile.contentType
            upsert = false
        }

        bucket.publicUrl(fileName)
    }

    override suspend fun uploadMedicineImage(uri: android.net.Uri): Result<String> = runCatching {
        val identity = resolveAccessContext()
        val ownerPath = when (identity.role) {
            AccountType.WAREHOUSE -> "warehouse/${resolveRealWarehouseIdForMedicineWrite(identity)}"
            AccountType.ADMIN -> "admin/${identity.userId}"
            else -> throw UnauthorizedException("Only ADMIN or WAREHOUSE can upload medicine images")
        }
        val sessionUserId = auth.currentUserOrNull()?.id
            ?: throw UnauthorizedException("unauthorized: no authenticated Supabase user")
        require(sessionUserId == identity.userId) {
            "Authenticated user does not match access context"
        }

        val bucket = supabase.storage.from("medicines")
        val uploadFile = readUploadFile(
            uri = uri,
            allowedMimeTypes = medicineUploadMimeTypes,
        )
        val fileName = "$ownerPath/medicine_${UUID.randomUUID()}.${uploadFile.extension}"

        bucket.upload(fileName, uploadFile.bytes) {
            contentType = uploadFile.contentType
            upsert = false
        }

        bucket.publicUrl(fileName)
    }

    override suspend fun uploadProfileAvatar(uri: android.net.Uri): Result<String> = runCatching {
        val identity = resolveAccessContext()
        val sessionUserId = auth.currentUserOrNull()?.id
            ?: throw UnauthorizedException("unauthorized: no authenticated Supabase user")
        require(sessionUserId == identity.userId) {
            "Authenticated user does not match access context"
        }

        val bucket = supabase.storage.from("profile-avatars")
        val uploadFile = readUploadFile(
            uri = uri,
            allowedMimeTypes = avatarUploadMimeTypes,
        )
        val fileName = "$sessionUserId/avatar_${UUID.randomUUID()}.${uploadFile.extension}"

        bucket.upload(fileName, uploadFile.bytes) {
            contentType = uploadFile.contentType
            upsert = false
        }

        bucket.publicUrl(fileName)
    }

    override suspend fun deleteProfileAvatar(avatarUrl: String): Result<Unit> = runCatching {
        val identity = resolveAccessContext()
        val sessionUserId = identity.userId

        val path = extractOwnedProfileAvatarStoragePath(
            avatarUrl = avatarUrl,
            userId = sessionUserId,
        ) ?: return@runCatching

        // Best-effort safety proof:
        // - only delete objects under "{auth.uid()}/..." inside profile-avatars bucket
        // - ignore malformed/unowned URLs (extractOwnedProfileAvatarStoragePath returns null)
        supabase.storage.from("profile-avatars").delete(path)
    }

    override suspend fun addMedicine(medicine: Medicine, warehouseId: String): Result<Unit> = runCatching {
        val identity = resolveAccessContext()
        val targetWarehouseId = when (identity.role) {
            AccountType.ADMIN -> warehouseId.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("warehouseId is required for medicine creation")
            AccountType.WAREHOUSE -> resolveRealWarehouseIdForMedicineWrite(
                identity = identity,
                requestedWarehouseId = warehouseId,
            )
            else -> throw UnauthorizedException("Only ADMIN or WAREHOUSE can add medicines")
        }

        val dto = MedicineInsertDto(
            name = medicine.name,
            brand = medicine.brand,
            strength = medicine.strength,
            price = medicine.priceAmount,
            description = medicine.description,
            specs = medicine.specs,
            stockQuantity = medicine.stockQuantity,
            imageUrl = medicine.imageUrl,
            warehouseId = targetWarehouseId,
            isVisible = medicine.isVisible,
            isActive = medicine.isActive,
            currency = medicine.currency,
        )
        
        try {
            supabase.postgrest.from("medicines").insert(dto)
        } catch (error: Throwable) {
            Log.e(TAG, "Medicine insert failed for warehouseId=$targetWarehouseId: ${error.message}", error)
            deleteUploadedMedicineImageIfOwned(
                imageUrl = medicine.imageUrl,
                identity = identity,
                cause = error,
            )
            throw error
        }
    }.map { Unit }

    private suspend fun resolveRealWarehouseIdForMedicineWrite(
        identity: UserIdentity,
        requestedWarehouseId: String? = null,
    ): String {
        require(identity.role == AccountType.WAREHOUSE) {
            "Real warehouse ownership proof is only available for WAREHOUSE users"
        }

        val rows = supabase.postgrest.from("profiles").select(
            columns = Columns.raw("account_type,is_active,warehouse_id"),
        ) {
            filter { eq("id", identity.userId) }
        }.decodeList<WarehouseMedicineWriteProfileDto>()
        val profile = requireSingleProfileRow(
            rows = rows,
            userId = identity.userId,
            rowLabel = "Warehouse medicine write profile",
        )
        require(profile.accountType == AccountType.WAREHOUSE.name) {
            "Authenticated profile is not a WAREHOUSE account"
        }
        require(profile.isActive == true) {
            "WAREHOUSE profile is not active"
        }
        val realWarehouseId = profile.warehouseId?.takeIf { it.isNotBlank() }
            ?: throw UnauthorizedException("unauthorized: WAREHOUSE user missing real profiles.warehouse_id")

        requestedWarehouseId?.takeIf { it.isNotBlank() }?.let { requested ->
            require(requested == realWarehouseId) {
                "WAREHOUSE users can only add medicines to their own warehouse"
            }
        }

        return realWarehouseId
    }

    override suspend fun cancelCustomerOrder(orderId: String): Result<Unit> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PUBLIC_USER) {
            "Only PUBLIC_USER can cancel their orders"
        }
        callOrderRpc(
            functionName = "cancel_customer_order",
            params = OrderIdRpcParams(orderId),
        )
    }.map { }

    override suspend fun acceptCustomerOrderPrice(orderId: String): Result<Unit> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PUBLIC_USER) {
            "Only PUBLIC_USER can accept order prices"
        }
        callOrderRpc(
            functionName = "customer_accept_order_price",
            params = OrderIdRpcParams(orderId),
        )
    }.map { }

    override suspend fun rejectCustomerOrderPrice(orderId: String): Result<Unit> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PUBLIC_USER) {
            "Only PUBLIC_USER can reject order prices"
        }
        callOrderRpc(
            functionName = "customer_reject_order_price",
            params = OrderIdRpcParams(orderId),
        )
    }.map { }

    override suspend fun confirmOrder(orderId: String, totalPriceCents: Long): Result<DomainOrder> = runCatching {
        // 1. Validate ownership: pharmacy only
        val order = getOrder(orderId).getOrThrow()
            ?: throw IllegalStateException("Order not found: $orderId")
        
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PHARMACY) {
            "Only PHARMACY can confirm orders"
        }
        
        val pharmacyId = identity.organizationId
            ?: throw IllegalStateException("PHARMACY missing organizationId")
        
        require(order.pharmacyId == pharmacyId) {
            "Cannot confirm order that doesn't belong to your pharmacy"
        }

        require(order.orderType == OrderType.CUSTOMER_PHARMACY) {
            "Only CUSTOMER_PHARMACY orders can be confirmed via confirmOrder"
        }
        
        // 2. Validate status transition
        require(order.status == OrderStatus.PENDING) {
            "Can only confirm PENDING orders. Current status: ${order.status}"
        }
        
        // 3. Validate price
        require(totalPriceCents >= 0) {
            "Total price must be >= 0"
        }
        
        // 4. Confirm through validated RPC. RLS no longer permits broad direct B2C updates.
        callOrderRpc(
            functionName = "confirm_customer_order",
            params = ConfirmCustomerOrderRpcParams(
                orderId = orderId,
                totalPriceCents = totalPriceCents,
            ),
        )
    }

    override suspend fun rejectOrder(orderId: String): Result<DomainOrder> = runCatching {
        // 1. Validate ownership: pharmacy only
        val order = getOrder(orderId).getOrThrow()
            ?: throw IllegalStateException("Order not found: $orderId")
        
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PHARMACY) {
            "Only PHARMACY can reject orders"
        }
        
        val pharmacyId = identity.organizationId
            ?: throw IllegalStateException("PHARMACY missing organizationId")
        
        require(order.pharmacyId == pharmacyId) {
            "Cannot reject order that doesn't belong to your pharmacy"
        }

        require(order.orderType == OrderType.CUSTOMER_PHARMACY) {
            "Only CUSTOMER_PHARMACY orders can be rejected via rejectOrder"
        }
        
        // 2. Validate status transition
        require(order.status == OrderStatus.PENDING) {
            "Can only reject PENDING orders. Current status: ${order.status}"
        }
        
        // 3. Reject through validated RPC. No rejection reason or notes are written.
        callOrderRpc(
            functionName = "reject_customer_order",
            params = OrderIdRpcParams(orderId),
        )
    }

    override suspend fun markOrderReadyForPickup(orderId: String): Result<DomainOrder> = runCatching {
        // 1. Validate ownership: pharmacy only
        val order = getOrder(orderId).getOrThrow()
            ?: throw IllegalStateException("Order not found: $orderId")
        
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PHARMACY) {
            "Only PHARMACY can mark orders ready"
        }
        
        val pharmacyId = identity.organizationId
            ?: throw IllegalStateException("PHARMACY missing organizationId")
        
        require(order.pharmacyId == pharmacyId) {
            "Cannot modify order that doesn't belong to your pharmacy"
        }

        require(order.orderType == OrderType.CUSTOMER_PHARMACY) {
            "Only CUSTOMER_PHARMACY orders can transition to READY_FOR_PICKUP"
        }
        
        // 2. Validate fulfillment type
        require(order.fulfillmentType == FulfillmentType.PICKUP) {
            "Can only mark PICKUP orders as ready. Current type: ${order.fulfillmentType}"
        }
        
        // 3. Validate status transition (CONFIRMED/IN_PROGRESS -> READY_FOR_PICKUP)
        require(order.status == OrderStatus.CONFIRMED || order.status == OrderStatus.IN_PROGRESS) {
            "Can only mark CONFIRMED or IN_PROGRESS orders as ready. Current status: ${order.status}"
        }
        
        // 4. Transition through validated RPC.
        callOrderRpc(
            functionName = "mark_customer_order_ready_for_pickup",
            params = OrderIdRpcParams(orderId),
        )
    }

    override suspend fun markOrderOutForDelivery(orderId: String): Result<DomainOrder> = runCatching {
        // 1. Validate ownership: pharmacy only
        val order = getOrder(orderId).getOrThrow()
            ?: throw IllegalStateException("Order not found: $orderId")
        
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PHARMACY) {
            "Only PHARMACY can mark orders out for delivery"
        }
        
        val pharmacyId = identity.organizationId
            ?: throw IllegalStateException("PHARMACY missing organizationId")
        
        require(order.pharmacyId == pharmacyId) {
            "Cannot modify order that doesn't belong to your pharmacy"
        }

        require(order.orderType == OrderType.CUSTOMER_PHARMACY) {
            "Only CUSTOMER_PHARMACY orders can transition to OUT_FOR_DELIVERY"
        }
        
        // 2. Validate fulfillment type
        require(order.fulfillmentType == FulfillmentType.DELIVERY) {
            "Can only mark DELIVERY orders as out for delivery. Current type: ${order.fulfillmentType}"
        }
        
        // 3. Validate status transition (CONFIRMED/IN_PROGRESS -> OUT_FOR_DELIVERY)
        require(order.status == OrderStatus.CONFIRMED || order.status == OrderStatus.IN_PROGRESS) {
            "Can only mark CONFIRMED or IN_PROGRESS orders as out for delivery. Current status: ${order.status}"
        }
        
        // 4. Transition through validated RPC.
        callOrderRpc(
            functionName = "mark_customer_order_out_for_delivery",
            params = OrderIdRpcParams(orderId),
        )
    }

    override suspend fun markOrderDelivered(orderId: String): Result<DomainOrder> = runCatching {
        // 1. Validate ownership: pharmacy only
        val order = getOrder(orderId).getOrThrow()
            ?: throw IllegalStateException("Order not found: $orderId")
        
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PHARMACY) {
            "Only PHARMACY can mark orders as delivered"
        }
        
        val pharmacyId = identity.organizationId
            ?: throw IllegalStateException("PHARMACY missing organizationId")
        
        require(order.pharmacyId == pharmacyId) {
            "Cannot modify order that doesn't belong to your pharmacy"
        }

        require(order.orderType == OrderType.CUSTOMER_PHARMACY) {
            "Only CUSTOMER_PHARMACY orders can transition to DELIVERED"
        }
        
        // 2. Validate status transition (must be READY_FOR_PICKUP or OUT_FOR_DELIVERY)
        require(order.status == OrderStatus.READY_FOR_PICKUP || order.status == OrderStatus.OUT_FOR_DELIVERY) {
            "Can only mark READY_FOR_PICKUP or OUT_FOR_DELIVERY orders as delivered. Current status: ${order.status}"
        }
        
        // 3. Transition through validated RPC.
        callOrderRpc(
            functionName = "mark_customer_order_delivered",
            params = OrderIdRpcParams(orderId),
        )
    }

    private suspend inline fun <reified T : Any> callOrderRpc(
        functionName: String,
        params: T,
    ): DomainOrder {
        return supabase.postgrest.rpc(functionName, params)
            .decodeSingle<OrderDto>()
            .toOrderDomain()
            .getOrThrow()
    }

    override suspend fun getMyOrders(customerId: String): Result<List<DomainOrder>> = runCatching {
        // 1. Validate ownership
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PUBLIC_USER) {
            "Only PUBLIC_USER can view their orders"
        }
        
        val currentCustomerId = identity.userId
            ?: throw IllegalStateException("PUBLIC_USER missing userId")
        
        require(currentCustomerId == customerId) {
            "Can only view your own orders"
        }
        
        val rows = runCatching {
            supabase.postgrest.rpc("get_my_customer_orders").decodeList<OrderDto>()
        }.getOrElse {
            supabase.postgrest.from("orders").select(columns = Columns.ALL) {
                filter { eq("customer_id", customerId) }
            }.decodeList<OrderDto>()
        }

        rows.map { it.toOrderDomain().getOrThrow() }
    }

    override suspend fun getPharmacyCustomerOrders(): Result<List<PharmacyCustomerOrder>> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PHARMACY) {
            "Only PHARMACY can view pharmacy customer orders"
        }
        require(!identity.organizationId.isNullOrBlank()) {
            "PHARMACY missing organizationId"
        }

        supabase.postgrest.rpc("get_pharmacy_customer_orders")
            .decodeList<PharmacyCustomerOrderDto>()
            .map { it.toDomain().getOrThrow() }
    }

    override suspend fun getPharmacyCustomerOrderDetail(orderId: String): Result<PharmacyCustomerOrder> = runCatching {
        require(orderId.isNotBlank()) { "orderId must not be blank" }
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PHARMACY) {
            "Only PHARMACY can view pharmacy customer order details"
        }
        require(!identity.organizationId.isNullOrBlank()) {
            "PHARMACY missing organizationId"
        }

        supabase.postgrest.rpc(
            "get_pharmacy_customer_order_detail",
            PharmacyCustomerOrderDetailRpcParams(orderId),
        ).decodeSingle<PharmacyCustomerOrderDto>()
            .toDomain()
            .getOrThrow()
    }

    override suspend fun claimNearbyCustomerOrder(orderId: String, radiusKm: Double): Result<Unit> = runCatching {
        require(orderId.isNotBlank()) { "orderId must not be blank" }
        require(radiusKm > 0.0) { "radiusKm must be greater than zero" }
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PHARMACY) {
            "Only PHARMACY can claim nearby customer orders"
        }
        require(!identity.organizationId.isNullOrBlank()) {
            "PHARMACY missing organizationId"
        }

        callOrderRpc(
            functionName = "claim_nearby_customer_order",
            params = ClaimNearbyCustomerOrderRpcParams(
                orderId = orderId,
                radiusKm = radiusKm,
            ),
        )
    }.map { Unit }

    override suspend fun confirmCustomerOrder(orderId: String, totalPriceCents: Long): Result<Unit> = runCatching {
        require(totalPriceCents >= 0) { "Total price must be >= 0" }
        callOrderRpc(
            functionName = "confirm_customer_order",
            params = ConfirmCustomerOrderRpcParams(
                orderId = orderId,
                totalPriceCents = totalPriceCents,
            ),
        )
    }.map { Unit }

    override suspend fun rejectCustomerOrder(orderId: String): Result<Unit> =
        callPharmacyCustomerOrderAction(
            functionName = "reject_customer_order",
            orderId = orderId,
        )

    override suspend fun markCustomerOrderReadyForPickup(orderId: String): Result<Unit> =
        callPharmacyCustomerOrderAction(
            functionName = "mark_customer_order_ready_for_pickup",
            orderId = orderId,
        )

    override suspend fun markCustomerOrderOutForDelivery(orderId: String): Result<Unit> =
        callPharmacyCustomerOrderAction(
            functionName = "mark_customer_order_out_for_delivery",
            orderId = orderId,
        )

    override suspend fun markCustomerOrderDelivered(orderId: String): Result<Unit> =
        callPharmacyCustomerOrderAction(
            functionName = "mark_customer_order_delivered",
            orderId = orderId,
        )

    private suspend fun callPharmacyCustomerOrderAction(
        functionName: String,
        orderId: String,
    ): Result<Unit> = runCatching {
        require(orderId.isNotBlank()) { "orderId must not be blank" }
        callOrderRpc(
            functionName = functionName,
            params = OrderIdRpcParams(orderId),
        )
    }.map { Unit }

    private suspend fun deleteUploadedPrescriptionIfOwned(
        prescriptionUrl: String?,
        userId: String,
        cause: Throwable,
    ) {
        val path = extractOwnedPrescriptionStoragePath(
            prescriptionUrl = prescriptionUrl,
            userId = userId,
        ) ?: return

        runCatching {
            supabase.storage.from("prescriptions").delete(path)
        }.onFailure { cleanupError ->
            Log.w(
                TAG,
                "Failed to clean up uploaded prescription after customer order creation failed: ${cleanupError.message}. Original failure: ${cause.message}",
                cleanupError,
            )
        }
    }

    private fun extractOwnedPrescriptionStoragePath(
        prescriptionUrl: String?,
        userId: String,
    ): String? {
        if (prescriptionUrl.isNullOrBlank() || userId.isBlank()) return null
        val marker = "/storage/v1/object/public/prescriptions/"
        val rawPath = prescriptionUrl.substringAfter(marker, missingDelimiterValue = "")
            .substringBefore('?')
            .takeIf { it.isNotBlank() }
            ?: return null
        val decodedPath = runCatching {
            java.net.URLDecoder.decode(rawPath, Charsets.UTF_8.name())
        }.getOrDefault(rawPath)

        return decodedPath.takeIf { path ->
            path.startsWith("${userId}_") &&
                !path.contains("..") &&
                !path.startsWith("/") &&
                !path.startsWith("\\")
        }
    }

    private suspend fun deleteUploadedMedicineImageIfOwned(
        imageUrl: String?,
        identity: UserIdentity,
        cause: Throwable,
    ) {
        val path = extractOwnedMedicineImageStoragePath(
            imageUrl = imageUrl,
            identity = identity,
        ) ?: return

        runCatching {
            supabase.storage.from("medicines").delete(path)
        }.onFailure { cleanupError ->
            Log.w(
                TAG,
                "Failed to clean up uploaded medicine image after addMedicine failed: ${cleanupError.message}. Original failure: ${cause.message}",
                cleanupError,
            )
        }
    }

    private fun extractOwnedMedicineImageStoragePath(
        imageUrl: String?,
        identity: UserIdentity,
    ): String? {
        if (imageUrl.isNullOrBlank() || identity.role != AccountType.WAREHOUSE) return null
        val warehouseId = identity.organizationId?.takeIf { it.isNotBlank() } ?: return null
        val marker = "/storage/v1/object/public/medicines/"
        val rawPath = imageUrl.substringAfter(marker, missingDelimiterValue = "")
            .substringBefore('?')
            .takeIf { it.isNotBlank() }
            ?: return null
        val decodedPath = runCatching {
            java.net.URLDecoder.decode(rawPath, Charsets.UTF_8.name())
        }.getOrDefault(rawPath)

        return decodedPath.takeIf { path ->
            path.startsWith("warehouse/$warehouseId/") &&
                !path.contains("..") &&
                !path.startsWith("/") &&
                !path.startsWith("\\")
        }
    }

    private fun Map<String, String>.notificationDestinationId(): String? =
        this["requestId"] ?: this["orderId"] ?: values.firstOrNull()

    override suspend fun adminGetAllUsers(): Result<List<AdminUser>> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }
        supabase.postgrest.rpc("admin_get_all_users").decodeList<AdminUserDto>().map { it.toDomain() }
    }

    override suspend fun adminUpdateUserProfile(
        targetUserId: String,
        fullName: String?,
        accountType: AccountType,
        pharmacyId: String?,
        warehouseId: String?,
        isActive: Boolean,
    ): Result<AdminUser> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }
        supabase.postgrest.rpc(
            "admin_update_user_profile",
            UpdateUserProfileRpcParams(
                targetUserId = targetUserId,
                fullName = fullName,
                accountType = accountType.name,
                pharmacyId = pharmacyId,
                warehouseId = warehouseId,
                isActive = isActive,
            ),
        ).decodeSingle<JsonElement>()
        adminGetAllUsers().getOrThrow().firstOrNull { it.id == targetUserId }
            ?: throw IllegalStateException("User not found after update")
    }

    override suspend fun adminGetAllPharmacies(): Result<List<Pharmacy>> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }
        fetchPharmacies().getOrThrow()
    }

    override suspend fun adminCreatePharmacy(
        name: String,
        location: String,
        contactNumber: String,
        licenseNumber: String,
    ): Result<Pharmacy> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }
        supabase.postgrest.rpc(
            "admin_create_pharmacy",
            CreatePharmacyRpcParams(
                name = name,
                location = location,
                contactNumber = contactNumber,
                licenseNumber = licenseNumber,
            ),
        ).decodeAs<FacilityDto>().toPharmacy()
    }

    override suspend fun adminGetAllWarehouses(): Result<List<Warehouse>> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }
        fetchWarehouses().getOrThrow()
    }

    override suspend fun adminCreateWarehouse(
        name: String,
        location: String,
        contactNumber: String,
    ): Result<Warehouse> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }
        supabase.postgrest.rpc(
            "admin_create_warehouse",
            CreateWarehouseRpcParams(
                name = name,
                location = location,
                contactNumber = contactNumber,
            ),
        ).decodeAs<FacilityDto>().toWarehouse()
    }

    override suspend fun createFacility(request: CreateFacilityRequest): Result<Unit> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }
        val lat = request.latitude ?: error("coordinates required")
        val lng = request.longitude ?: error("coordinates required")
        val locationLabel = request.address.trim()
        when (request.type) {
            FacilityType.PHARMACY -> {
                val pharmacy = supabase.postgrest.rpc(
                    "admin_create_pharmacy",
                    CreatePharmacyWithCoordinatesRpcParams(
                        name = request.name.trim(),
                        location = locationLabel,
                        contactNumber = request.phone.trim(),
                        licenseNumber = request.licenseNumber.trim(),
                        latitude = lat,
                        longitude = lng,
                    ),
                ).decodeAs<FacilityDto>().toPharmacy()
                if (!request.isActive) {
                    supabase.postgrest.from("pharmacies").update(FacilityActiveUpdateDto(isActive = false)) {
                        filter { eq("id", pharmacy.id) }
                    }
                }
            }
            FacilityType.WAREHOUSE -> {
                val warehouse = supabase.postgrest.rpc(
                    "admin_create_warehouse",
                    CreateWarehouseWithCoordinatesRpcParams(
                        name = request.name.trim(),
                        location = locationLabel,
                        contactNumber = request.phone.trim(),
                        latitude = lat,
                        longitude = lng,
                    ),
                ).decodeAs<FacilityDto>().toWarehouse()
                if (!request.isActive) {
                    supabase.postgrest.from("warehouses").update(FacilityActiveUpdateDto(isActive = false)) {
                        filter { eq("id", warehouse.id) }
                    }
                }
            }
        }
    }

    override suspend fun adminGetAuditLogs(limit: Int): Result<List<AuditLog>> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }
        supabase.postgrest.rpc(
            "admin_get_audit_logs",
            GetAuditLogsRpcParams(limit = limit),
        ).decodeList<AuditLogDto>().map { it.toDomain() }
    }

    override suspend fun getAuditLogById(logId: String): Result<AuditLog> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }
        supabase.postgrest.rpc(
            "admin_get_audit_log_detail",
            buildJsonObject { put("p_log_id", logId) },
        ).decodeSingle<AuditLogDto>().toDomain()
    }

    private suspend fun getOwnedRequestForPharmacy(
        requestId: String,
        actionName: String,
    ): Request {
        val identity = resolveAccessContext()
        if (identity.role != AccountType.PHARMACY) {
            throw UnauthorizedException("unauthorized: $actionName is allowed only for PHARMACY users")
        }

        val pharmacyId = identity.organizationId
            ?: throw UnauthorizedException("unauthorized: PHARMACY user missing organizationId")

        val rows = supabase.postgrest.from("requests").select(columns = Columns.ALL) {
            filter {
                eq("id", requestId)
                eq("pharmacy_id", pharmacyId)
            }
        }.decodeList<RequestDto>()

        return when {
            rows.isEmpty() -> throw IllegalArgumentException(
                "$actionName failed because request $requestId was not found for pharmacy $pharmacyId.",
            )
            rows.size > 1 -> throw IllegalStateException(
                "$actionName failed because request $requestId matched duplicate rows for pharmacy $pharmacyId.",
            )
            else -> rows.single().toDomain().getOrThrow()
        }
    }

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

    override suspend fun getWarehouseInventory(warehouseId: String): Result<List<com.pharmalink.domain.model.InventoryItem>> = runCatching {
        val identity = resolveAccessContext()
        val targetWarehouseId = when (identity.role) {
            AccountType.ADMIN -> warehouseId
            AccountType.WAREHOUSE -> {
                val ownWarehouseId = identity.organizationId
                    ?: throw UnauthorizedException("unauthorized: WAREHOUSE user missing organizationId")
                require(warehouseId == ownWarehouseId) {
                    "WAREHOUSE users can only read their own warehouse inventory"
                }
                ownWarehouseId
            }
            else -> throw UnauthorizedException("Only ADMIN or WAREHOUSE can read warehouse inventory")
        }

        supabase.postgrest.from("warehouse_inventory").select(Columns.ALL) {
            filter { eq("warehouse_id", targetWarehouseId) }
        }.decodeList<com.pharmalink.data.dto.InventoryItemDto>()
            .map { dto -> dto.toDomain() }
    }

    override suspend fun adminGetDashboardStats(): Result<com.pharmalink.domain.model.AdminDashboardStats> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }

        // Call the new RPC
        val response = supabase.postgrest.rpc("admin_get_dashboard_stats")
            .decodeAs<AdminDashboardStatsDto>()

        com.pharmalink.domain.model.AdminDashboardStats(
            totalUsers = response.totalUsers ?: 0,
            totalPharmacies = response.totalPharmacies ?: 0,
            totalWarehouses = response.totalWarehouses ?: 0,
            totalOrders = response.totalOrders ?: 0,
            b2cOrdersCount = response.b2cOrdersCount ?: 0,
            b2bOrdersCount = response.b2bOrdersCount ?: 0,
            urgentOrdersCount = response.urgentOrdersCount ?: 0,
            pendingOrdersCount = response.pendingOrdersCount ?: 0,
            confirmedOrdersCount = response.confirmedOrdersCount ?: 0,
            deliveredOrdersCount = response.deliveredOrdersCount ?: 0,
            activePharmacies = response.activePharmacies ?: 0,
            activeWarehouses = response.activeWarehouses ?: 0,
        )
    }

    override suspend fun adminGetAllOrders(
        orderType: String?,
        status: String?,
        isUrgent: Boolean?,
        search: String?,
        limit: Int,
        offset: Int
    ): Result<List<com.pharmalink.domain.model.AdminOrder>> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }

        // Build parameters
        val params = buildJsonObject {
            orderType?.let { put("p_order_type", it) }
            status?.let { put("p_status", it) }
            isUrgent?.let { put("p_is_urgent", it) }
            search?.takeIf { it.isNotBlank() }?.let { put("p_search", it) }
            put("p_limit", limit)
            put("p_offset", offset)
        }

        // Call RPC
        val response = supabase.postgrest.rpc("admin_get_all_orders", params)
            .decodeList<AdminOrderDto>()

        response.map { it.toDomain() }
    }

    override suspend fun adminGetOrderDetail(orderId: String): Result<com.pharmalink.domain.model.AdminOrder?> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }

        // Build parameters
        val params = buildJsonObject {
            put("p_order_id", orderId)
        }

        // Call RPC
        val response = supabase.postgrest.rpc("admin_get_order_detail", params)
            .decodeList<AdminOrderDto>()

        response.firstOrNull()?.toDomain()
    }

    override suspend fun adminGetPendingRequests(limit: Int): Result<List<com.pharmalink.domain.model.PendingRequest>> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }

        val params = buildJsonObject {
            put("p_limit", limit)
        }

        val response = supabase.postgrest.rpc("admin_get_pending_requests", params)
            .decodeList<PendingRequestDto>()

        response.map { it.toDomain() }
    }

    override suspend fun adminGetRecentActivities(limit: Int): Result<List<com.pharmalink.domain.model.RecentActivity>> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }

        val params = buildJsonObject {
            put("p_limit", limit)
        }

        val response = supabase.postgrest.rpc("admin_get_recent_activities", params)
            .decodeList<RecentActivityDto>()

        response.map { it.toDomain() }
    }

    override suspend fun adminGetSystemHealth(): Result<com.pharmalink.domain.model.SystemHealth> = runCatching {
        val identity = resolveAccessContext()
        require(identity.role == AccountType.ADMIN) { "Admin access required" }

        val response = supabase.postgrest.rpc("admin_get_system_health")
            .decodeAs<SystemHealthDto>()

        response.toDomain()
    }

}

@Serializable
private data class UserIdDto(
    val id: String
)

@Serializable
private data class AppNotificationInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("pharmacy_id") val pharmacyId: String?,
    val title: String,
    val body: String,
    val type: String,
    val category: String,
    val read: Boolean,
    val destination: String? = null,
    @SerialName("destination_id") val destinationId: String? = null,
)

@Serializable
private data class FacilityActiveUpdateDto(
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
private data class RequestIdRow(val id: String)

@Serializable
private data class HomeStatsRequestRow(
    val id: String,
    @SerialName("created_at") val createdAt: String? = null,
)

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
private data class MedicineInsertDto(
    val name: String,
    val brand: String? = null,
    val strength: String? = null,
    val price: Double? = null,
    val description: String? = null,
    val specs: JsonElement? = null,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("warehouse_id") val warehouseId: String? = null,
    @SerialName("is_visible") val isVisible: Boolean? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    val currency: String? = null,
)

@Serializable
private data class MedicineDto(
    val id: String,
    val name: String,
    val brand: String? = null,
    val strength: String? = null,
    val price: Double? = null,
    val description: String? = null,
    val specs: JsonElement? = null,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("warehouse_id") val warehouseId: String? = null,
    @SerialName("is_visible") val isVisible: Boolean? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    val currency: String? = null,
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
            brand = brand.orEmpty(),
            strength = strength.orEmpty(),
            price = price ?: 0.0,
            stockQuantity = stockQuantity ?: 0,
            imageUrl = imageUrl,
            priceAmount = price,
            warehouseId = warehouseId,
            description = description,
            specs = specs,
            isVisible = isVisible ?: true,
            isActive = isActive ?: true,
            currency = currency ?: "SYP",
        )
    }
}

@Serializable
private data class WarehouseMedicineWriteProfileDto(
    @SerialName("account_type") val accountType: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("warehouse_id") val warehouseId: String? = null,
)

@Serializable
private data class WarehouseProductsRpcParams(
    @SerialName("p_warehouse_id") val warehouseId: String,
)

@Serializable
private data class PublicPharmaciesForMedicineRpcParams(
    @SerialName("p_medicine_id") val medicineId: String,
)

@Serializable
private data class PublicPharmacyForMedicineDto(
    @SerialName("pharmacy_id") val pharmacyId: String,
    @SerialName("pharmacy_name") val pharmacyName: String,
    val location: String? = null,
    val area: String? = null,
    val city: String? = null,
    val district: String? = null,
    @SerialName("supports_delivery") val supportsDelivery: Boolean = false,
    @SerialName("supports_pickup") val supportsPickup: Boolean = true,
    @SerialName("is_on_duty") val isOnDuty: Boolean = false,
    @SerialName("distance_label") val distanceLabel: String? = null,
    @SerialName("availability_status") val availabilityStatus: String = "UNKNOWN",
    @SerialName("estimated_time_label") val estimatedTimeLabel: String? = null,
) {
    fun toDomain(): PublicPharmacyForMedicine =
        PublicPharmacyForMedicine(
            pharmacyId = pharmacyId,
            pharmacyName = pharmacyName,
            location = location.orEmpty(),
            area = area,
            city = city,
            district = district,
            supportsDelivery = supportsDelivery,
            supportsPickup = supportsPickup,
            isOnDuty = isOnDuty,
            distanceLabel = distanceLabel,
            availabilityStatus = runCatching {
                PublicPharmacyAvailabilityStatus.valueOf(availabilityStatus)
            }.getOrDefault(PublicPharmacyAvailabilityStatus.UNKNOWN),
            estimatedTimeLabel = estimatedTimeLabel,
        )
}

@Serializable
private data class OrderDto(
    val id: String,
    @SerialName("medicine_id") val medicineId: String,
    @SerialName("medicine_name") val medicineName: String,
    val quantity: Int,
    val unit: String,
    val status: String,
    @SerialName("order_type") val orderType: String,
    @SerialName("fulfillment_type") val fulfillmentType: String,
    @SerialName("pharmacy_id") val pharmacyId: String? = null,
    @SerialName("warehouse_id") val warehouseId: String? = null,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("request_id") val requestId: String? = null,
    @SerialName("total_price_cents") val totalPriceCents: Long? = null,
    val currency: String = "SAR",
    @SerialName("delivery_address") val deliveryAddress: String? = null,
    @SerialName("delivery_phone") val deliveryPhone: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
    @SerialName("fulfilled_at") val fulfilledAt: String? = null,
    @SerialName("warehouse_name") val warehouseName: String? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    @SerialName("eta_label") val etaLabel: String? = null,
    @SerialName("is_urgent") val isUrgent: Boolean = false,
    val urgency: String = "URGENT",
    @SerialName("request_scope") val requestScope: String = "SPECIFIC_PHARMACY",
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
    @SerialName("pharmacy_location") val pharmacyLocation: String? = null,
) {
    fun toOrderDomain(): Result<DomainOrder> = runCatching {
        val statusEnum = status.toOrderStatus()
        val orderTypeEnum = orderType.toOrderType()
        val fulfillmentTypeEnum = fulfillmentType.toFulfillmentType()

        DomainOrder(
            id = id,
            medicineId = medicineId,
            medicineName = medicineName,
            quantity = quantity,
            unit = unit,
            status = statusEnum,
            orderType = orderTypeEnum,
            fulfillmentType = fulfillmentTypeEnum,
            pharmacyId = pharmacyId,
            warehouseId = warehouseId,
            customerId = customerId,
            requestId = requestId,
            totalPriceCents = totalPriceCents,
            currency = currency,
            deliveryAddress = deliveryAddress,
            deliveryPhone = deliveryPhone,
            notes = notes,
            createdAt = isoStringToInstant(createdAt),
            updatedAt = isoStringToInstant(updatedAt),
            confirmedAt = confirmedAt?.let { isoStringToInstant(it) },
            fulfilledAt = fulfilledAt?.let { isoStringToInstant(it) },
            warehouseName = warehouseName,
            supplierName = supplierName,
            etaLabel = etaLabel,
            isUrgent = isUrgent,
            urgency = runCatching { CustomerRequestUrgency.valueOf(urgency) }.getOrDefault(CustomerRequestUrgency.URGENT),
            requestScope = runCatching { CustomerRequestScope.valueOf(requestScope) }.getOrDefault(CustomerRequestScope.SPECIFIC_PHARMACY),
            pharmacyName = pharmacyName,
            pharmacyLocation = pharmacyLocation,
        )
    }
}

@Serializable
private data class CreateOrderDto(
    @SerialName("medicine_id") val medicineId: String,
    @SerialName("medicine_name") val medicineName: String,
    val quantity: Int,
    val unit: String,
    @SerialName("pharmacy_id") val pharmacyId: String? = null,
    @SerialName("customer_id") val customerId: String,
    @SerialName("order_type") val orderType: String,
    val urgency: String,
    @SerialName("request_scope") val requestScope: String,
    @SerialName("fulfillment_type") val fulfillmentType: String,
    @SerialName("delivery_address") val deliveryAddress: String? = null,
    @SerialName("delivery_latitude") val deliveryLatitude: Double? = null,
    @SerialName("delivery_longitude") val deliveryLongitude: Double? = null,
    @SerialName("delivery_phone") val deliveryPhone: String? = null,
    val notes: String? = null,
    @SerialName("prescription_url") val prescriptionUrl: String? = null,
    val status: String = "PENDING",
    val currency: String = "SAR",
)

@Serializable
private data class OrderIdRpcParams(
    @SerialName("p_order_id") val orderId: String,
)

@Serializable
private data class ConfirmCustomerOrderRpcParams(
    @SerialName("p_order_id") val orderId: String,
    @SerialName("p_total_price_cents") val totalPriceCents: Long,
)

@Serializable
private data class PharmacyCustomerOrderDetailRpcParams(
    @SerialName("p_order_id") val orderId: String,
)

@Serializable
private data class ClaimNearbyCustomerOrderRpcParams(
    @SerialName("p_order_id") val orderId: String,
    @SerialName("p_radius_km") val radiusKm: Double,
)

@Serializable
private data class PharmacyCustomerOrderDto(
    val id: String,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("medicine_id") val medicineId: String,
    @SerialName("medicine_name") val medicineName: String,
    val quantity: Int,
    val unit: String,
    val status: String,
    @SerialName("fulfillment_type") val fulfillmentType: String,
    @SerialName("delivery_address") val deliveryAddress: String? = null,
    @SerialName("delivery_phone") val deliveryPhone: String? = null,
    @SerialName("prescription_url") val prescriptionUrl: String? = null,
    val notes: String? = null,
    @SerialName("total_price_cents") val totalPriceCents: Long? = null,
    val currency: String = "SAR",
    val urgency: String = "URGENT",
    @SerialName("request_scope") val requestScope: String = "SPECIFIC_PHARMACY",
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
) {
    fun toDomain(): Result<PharmacyCustomerOrder> = runCatching {
        PharmacyCustomerOrder(
            id = id,
            customerId = customerId,
            customerName = customerName?.takeIf { it.isNotBlank() },
            medicineId = medicineId,
            medicineName = medicineName,
            quantity = quantity,
            unit = unit,
            status = status.toOrderStatus(),
            fulfillmentType = fulfillmentType.toFulfillmentType(),
            deliveryAddress = deliveryAddress,
            deliveryPhone = deliveryPhone,
            prescriptionUrl = prescriptionUrl,
            notes = notes,
            totalPriceCents = totalPriceCents,
            currency = currency,
            urgency = runCatching { CustomerRequestUrgency.valueOf(urgency) }.getOrDefault(CustomerRequestUrgency.URGENT),
            requestScope = runCatching { CustomerRequestScope.valueOf(requestScope) }.getOrDefault(CustomerRequestScope.SPECIFIC_PHARMACY),
            createdAt = isoStringToInstant(createdAt),
            updatedAt = isoStringToInstant(updatedAt),
        )
    }
}

@Serializable
private data class RequestDto(
    val id: String,
    @SerialName("pharmacy_id") val pharmacyId: String,
    @SerialName("medicine_id") val medicineId: String? = null,
    @SerialName("medicine_name") val medicineName: String,
    @SerialName("medicine_subtitle") val medicineSubtitle: String = "",
    val quantity: Int = 0,
    val unit: String = "",
    val notes: String = "",
    @SerialName("total_price") val totalPrice: Double = 0.0,
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
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("attachment_url") val attachmentUrl: String? = null,
    @SerialName("medicine_image_url") val medicineImageUrl: String? = null,
) {
    fun toDomain(items: List<RequestItem> = emptyList()): Result<Request> = runCatching {
        val priorityEnum = priority.toRequestPriority()
        val statusEnum = status.toRequestStatus()

        val createdAtLabel = isoStringToDisplayLabel(createdAt)
        val updatedAtLabel = isoStringToDisplayLabel(updatedAt).ifBlank { createdAtLabel }
        val firstItem = items.firstOrNull()

        Request(
            id = id,
            pharmacyId = pharmacyId,
            medicineId = firstItem?.medicineId ?: medicineId,
            medicineName = firstItem?.medicineName ?: medicineName,
            medicineSubtitle = firstItem?.medicineSubtitle ?: medicineSubtitle,
            quantity = firstItem?.quantity ?: quantity,
            unit = firstItem?.unit ?: unit,
            notes = notes,
            storageNotes = storageNotes,
            totalPrice = totalPrice,
            priority = priorityEnum,
            status = statusEnum,
            warehouseId = warehouseId,
            warehouseName = warehouseName,
            supplierName = supplierName,
            createdAtLabel = createdAtLabel,
            updatedAtLabel = updatedAtLabel,
            etaLabel = etaLabel,
            relatedOrderId = relatedOrderId,
            rejectionReason = rejectionReason,
            attachmentUrl = attachmentUrl,
            medicineImageUrl = medicineImageUrl,
            items = items,
        )
    }
}

@Serializable
private data class RequestItemDto(
    val id: String,
    @SerialName("request_id") val requestId: String,
    @SerialName("line_no") val lineNo: Int,
    @SerialName("medicine_id") val medicineId: String,
    @SerialName("medicine_name") val medicineName: String,
    @SerialName("medicine_subtitle") val medicineSubtitle: String = "",
    val quantity: Int,
    val unit: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun toDomain(): RequestItem =
        RequestItem(
            id = id,
            requestId = requestId,
            lineNo = lineNo,
            medicineId = medicineId,
            medicineName = medicineName,
            medicineSubtitle = medicineSubtitle,
            quantity = quantity,
            unit = unit,
            createdAt = createdAt.orEmpty(),
            updatedAt = updatedAt.orEmpty(),
        )
}

@Serializable
private data class AppNotificationDto(
    val id: String,
    val title: String,
    val body: String,
    // Fields below do not exist in Supabase schema - using safe defaults
    val type: String = "INFO",  // Default to INFO (most generic NotificationType)
    val category: String = "ORDERS",  // Default to ORDERS (most common NotificationCategory)
    @SerialName("created_at") val createdAt: String? = null,
    val read: Boolean = false,
    @SerialName("requires_action") val requiresAction: Boolean = false,  // Does not exist in Supabase
    val destination: String? = null,  // Does not exist in Supabase
    @SerialName("destination_id") val destinationId: String? = null,  // Does not exist in Supabase
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
private data class ProfileIdDto(
    val id: String,
)

@Serializable
private data class SubmitSupportRequestRpcParams(
    @SerialName("p_subject") val subject: String,
    @SerialName("p_message") val message: String,
    @SerialName("p_category") val category: String? = null,
)

@Serializable
private data class SupportNotificationInsertDto(
    @SerialName("user_id") val userId: String,
    val title: String,
    val body: String,
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
private data class ProfileDetailsDto(
    val id: String,
    @SerialName("pharmacy_id") val pharmacyId: String? = null,
    @SerialName("warehouse_id") val warehouseId: String? = null,
    @SerialName("account_type") val accountType: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
    @SerialName("warehouse_name") val warehouseName: String? = null,
    @SerialName("pharmacy_location") val pharmacyLocation: String? = null,
    @SerialName("default_address") val defaultAddress: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
) {
    fun toDomain(
        contactEmail: String,
        warehouseLocation: WarehouseLocationDto? = null,
    ): Result<PharmacyProfile> = runCatching {
        val isWarehouse = accountType == AccountType.WAREHOUSE.name
        PharmacyProfile(
            id = id,
            pharmacyName = if (isWarehouse) {
                warehouseLocation?.name ?: warehouseName.orEmpty()
            } else {
                pharmacyName.orEmpty()
            },
            city = "",
            district = "",
            managerName = fullName.orEmpty(),
            addressLine = if (isWarehouse) {
                warehouseLocation?.formattedAddress.orEmpty()
            } else {
                defaultAddress ?: pharmacyLocation.orEmpty()
            },
            contactPhone = phoneNumber.orEmpty(),
            contactEmail = contactEmail,
            licenseStatusLabel = "",
            licenseNumber = "",
            licenseExpiryLabel = "",
            operatingHoursLabel = "",
            preferredLanguageLabel = "",
            notificationsEnabled = notificationsEnabled ?: true,
            twoFactorEnabled = false,
            linkedDevicesCount = 0,
            totalOrders = 0,
            completedOrders = 0,
            activeOrders = 0,
            latitude = if (isWarehouse) warehouseLocation?.latitude else null,
            longitude = if (isWarehouse) warehouseLocation?.longitude else null,
            avatarUrl = avatarUrl,
        )
    }
}

@Serializable
private data class WarehouseLocationDto(
    val id: String,
    val name: String? = null,
    @SerialName("formatted_address") val formattedAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
private data class ProfileUpdateDto(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
    @SerialName("pharmacy_location") val pharmacyLocation: String? = null,
    @SerialName("default_address") val defaultAddress: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
private data class WarehouseProfileUpdateDto(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
private data class UpdateMyWarehouseLocationRpcParams(
    @SerialName("p_address") val address: String,
    @SerialName("p_lat") val latitude: Double,
    @SerialName("p_lng") val longitude: Double,
)

@Serializable
private data class ProfileNotificationsPreferenceUpdateDto(
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean,
)

@Serializable
private data class RequestInsertDto(
    @SerialName("pharmacy_id") val pharmacyId: String,
    @SerialName("warehouse_id") val warehouseId: String,
    @SerialName("medicine_id") val medicineId: String,
    @SerialName("medicine_name") val medicineName: String,
    val quantity: Int,
    val unit: String,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("medicine_subtitle") val medicineSubtitle: String = "",
    val priority: String,
    @SerialName("warehouse_name") val warehouseName: String = "",
    @SerialName("supplier_name") val supplierName: String = "",
    val status: String,
)

@Serializable
private data class RequestItemInsertDto(
    @SerialName("request_id") val requestId: String,
    @SerialName("line_no") val lineNo: Int,
    @SerialName("medicine_id") val medicineId: String,
    @SerialName("medicine_name") val medicineName: String,
    @SerialName("medicine_subtitle") val medicineSubtitle: String = "",
    val quantity: Int,
    val unit: String,
)

@Serializable
private data class RequestUpdateDto(
    val status: String? = null,
    @SerialName("warehouse_id") val warehouseId: String? = null,
    @SerialName("warehouse_name") val warehouseName: String? = null,
    val notes: String? = null,
    @SerialName("medicine_id") val medicineId: String? = null,
    @SerialName("medicine_name") val medicineName: String? = null,
    @SerialName("medicine_subtitle") val medicineSubtitle: String? = null,
    val quantity: Int? = null,
    val unit: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class B2bRequestRpcResponse(
    val request: RequestDto,
    val order: OrderDto? = null,
    val items: List<RequestItemDto> = emptyList(),
)

@Serializable
private data class B2bRequestIdRpcParams(
    @SerialName("p_request_id") val requestId: String,
)

@Serializable
private data class WarehouseAcceptB2bRpcParams(
    @SerialName("p_request_id") val requestId: String,
    @SerialName("p_total_price_cents") val totalPriceCents: Long,
)

@Serializable
private data class WarehouseRejectB2bRpcParams(
    @SerialName("p_request_id") val requestId: String,
    @SerialName("p_rejection_reason") val rejectionReason: String? = null,
)

@Serializable
private data class WarehouseMarkB2bDeliveredRpcParams(
    @SerialName("p_request_id") val requestId: String,
    @SerialName("p_delivery_note") val deliveryNote: String? = null,
)

private val isoDisplayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

private val prescriptionUploadMimeTypes = mapOf(
    "image/jpeg" to "jpg",
    "image/png" to "png",
    "image/webp" to "webp",
    "application/pdf" to "pdf",
)

private val medicineUploadMimeTypes = mapOf(
    "image/jpeg" to "jpg",
    "image/png" to "png",
    "image/webp" to "webp",
)

private val avatarUploadMimeTypes = mapOf(
    "image/jpeg" to "jpg",
    "image/png" to "png",
    "image/webp" to "webp",
)

private data class UploadFile(
    val bytes: ByteArray,
    val extension: String,
    val contentType: ContentType,
)

private class UnauthorizedException(
    message: String,
) : IllegalStateException(message)

private fun inferMimeTypeFromUri(uri: Uri): String? {
    val extension = uri.lastPathSegment
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
    return when (extension) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        else -> null
    }
}

private fun extractOwnedProfileAvatarStoragePath(
    avatarUrl: String,
    userId: String,
): String? {
    if (avatarUrl.isBlank() || userId.isBlank()) return null

    val uri = runCatching { Uri.parse(avatarUrl) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "https" && scheme != "http") return null

    val expectedHost = runCatching {
        Uri.parse(BuildConfig.SUPABASE_URL.trim()).host
    }.getOrNull()?.lowercase() ?: return null
    val actualHost = uri.host?.lowercase() ?: return null
    if (actualHost != expectedHost) return null

    val segments = uri.pathSegments
    val bucketIndex = segments.indexOf("profile-avatars")
    val publicIndex = bucketIndex - 1
    if (
        bucketIndex < 4 ||
        publicIndex < 3 ||
        segments.getOrNull(publicIndex) != "public" ||
        segments.getOrNull(publicIndex - 1) != "object" ||
        segments.getOrNull(publicIndex - 2) != "v1" ||
        segments.getOrNull(publicIndex - 3) != "storage"
    ) {
        return null
    }

    val objectSegments = segments.drop(bucketIndex + 1)
    if (objectSegments.size != 2) return null
    if (objectSegments[0] != userId) return null

    val fileName = objectSegments[1]
    if (
        fileName.isBlank() ||
        fileName.contains('/') ||
        fileName.contains('\\') ||
        fileName.contains("..")
    ) {
        return null
    }

    val allowedAvatarName = Regex(
        pattern = """avatar_[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\.(jpg|png|webp)""",
    )
    if (!allowedAvatarName.matches(fileName)) return null

    return "$userId/$fileName"
}

private fun isoStringToDisplayLabel(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        val instant = Instant.parse(iso)
        isoDisplayFormatter.format(instant)
    }.getOrElse { iso }
}

private fun String.toOrderStatus(): OrderStatus = OrderStatus.valueOf(this)

private fun String.toOrderType(): OrderType = OrderType.valueOf(this)

private fun String.toFulfillmentType(): FulfillmentType = FulfillmentType.valueOf(this)

private fun String.toRequestPriority(): RequestPriority = RequestPriority.valueOf(this)

private fun String.toRequestStatus(): RequestStatus = RequestStatus.valueOf(this)

private fun String.toNotificationType(): NotificationType =
    runCatching { NotificationType.valueOf(trim().uppercase()) }
        .getOrDefault(NotificationType.INFO)

private fun String.toNotificationCategory(): NotificationCategory =
    runCatching { NotificationCategory.valueOf(trim().uppercase()) }
        .getOrDefault(NotificationCategory.ORDERS)

private fun String.toNotificationDestination(): NotificationDestination? =
    runCatching { NotificationDestination.valueOf(trim().uppercase()) }.getOrNull()

private fun isoStringToInstant(iso: String?): java.time.Instant {
    return iso?.let {
        runCatching { java.time.Instant.parse(it) }.getOrNull()
    } ?: java.time.Instant.now()
}


@Serializable
private data class AdminDashboardStatsDto(
    @SerialName("totalUsers") val totalUsers: Int?,
    @SerialName("totalPharmacies") val totalPharmacies: Int?,
    @SerialName("totalWarehouses") val totalWarehouses: Int?,
    @SerialName("totalOrders") val totalOrders: Int?,
    @SerialName("b2cOrdersCount") val b2cOrdersCount: Int?,
    @SerialName("b2bOrdersCount") val b2bOrdersCount: Int?,
    @SerialName("urgentOrdersCount") val urgentOrdersCount: Int?,
    @SerialName("pendingOrdersCount") val pendingOrdersCount: Int?,
    @SerialName("confirmedOrdersCount") val confirmedOrdersCount: Int?,
    @SerialName("deliveredOrdersCount") val deliveredOrdersCount: Int?,
    @SerialName("activePharmacies") val activePharmacies: Int?,
    @SerialName("activeWarehouses") val activeWarehouses: Int?,
)

@Serializable
private data class AdminOrderDto(
    val id: String,
    @SerialName("order_type") val orderType: String,
    val status: String,
    @SerialName("medicine_name") val medicineName: String,
    val quantity: Int,
    val unit: String,
    @SerialName("pharmacy_id") val pharmacyId: String?,
    @SerialName("pharmacy_name") val pharmacyName: String?,
    @SerialName("warehouse_id") val warehouseId: String?,
    @SerialName("warehouse_name") val warehouseName: String?,
    @SerialName("customer_id") val customerId: String?,
    @SerialName("customer_name") val customerName: String?,
    @SerialName("is_urgent") val isUrgent: Boolean,
    @SerialName("total_price_cents") val totalPriceCents: Long?,
    val currency: String,
    @SerialName("fulfillment_type") val fulfillmentType: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("confirmed_at") val confirmedAt: String?,
    @SerialName("fulfilled_at") val fulfilledAt: String?,
) {
    fun toDomain(): com.pharmalink.domain.model.AdminOrder {
        return com.pharmalink.domain.model.AdminOrder(
            id = id,
            orderType = orderType,
            status = status.toOrderStatus(),
            medicineName = medicineName,
            quantity = quantity,
            unit = unit,
            pharmacyId = pharmacyId,
            pharmacyName = pharmacyName,
            warehouseId = warehouseId,
            warehouseName = warehouseName,
            customerId = customerId,
            customerName = customerName,
            isUrgent = isUrgent,
            totalPriceCents = totalPriceCents,
            currency = currency,
            fulfillmentType = fulfillmentType.toFulfillmentType(),
            createdAt = createdAt,
            updatedAt = updatedAt,
            confirmedAt = confirmedAt,
            fulfilledAt = fulfilledAt,
        )
    }
}

@Serializable
private data class PendingRequestDto(
    val id: String,
    val title: String,
    val subtitle: String,
    val timestamp: String,
    @SerialName("request_type") val requestType: String,
) {
    fun toDomain(): com.pharmalink.domain.model.PendingRequest {
        return com.pharmalink.domain.model.PendingRequest(
            id = id,
            title = title,
            subtitle = subtitle,
            timestamp = timestamp,
            requestType = requestType,
        )
    }
}

@Serializable
private data class RecentActivityDto(
    val id: String,
    val action: String,
    @SerialName("user_name") val userName: String,
    val timestamp: String,
    val status: String,
) {
    fun toDomain(): com.pharmalink.domain.model.RecentActivity {
        return com.pharmalink.domain.model.RecentActivity(
            id = id,
            action = action,
            userName = userName,
            timestamp = timestamp,
            status = status,
        )
    }
}

@Serializable
private data class SystemHealthDto(
    @SerialName("healthPercent") val healthPercent: Int,
    @SerialName("healthStatus") val healthStatus: String,
    // Note: activeConnections removed - not available in schema
) {
    fun toDomain(): com.pharmalink.domain.model.SystemHealth {
        return com.pharmalink.domain.model.SystemHealth(
            healthPercent = healthPercent,
            healthStatus = healthStatus,
        )
    }
}

