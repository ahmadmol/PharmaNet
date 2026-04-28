package com.pharmalink.data.repository

import android.util.Log
import com.pharmalink.core.common.error.MissingPharmacyLinkageException
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.mapper.toUserIdentity
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.ComplianceOverview
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
import com.pharmalink.domain.model.PharmacyProfile
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestPriority
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.model.Warehouse
import com.pharmalink.domain.model.WarehouseShipment
import io.github.jan.supabase.SupabaseClient
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
        val pharmacyId = resolvePharmacyIdOrNull()
        if (pharmacyId == null) {
            Log.w(TAG, "fetchHomeStats fallback: non-PHARMACY or missing pharmacy linkage, returning defaults.")
            return@runCatching defaultHomeStats()
        }
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
                    // Logout/session-race safe fallback: never crash collectors on null snapshot.
                    Log.w(TAG, "observeProfile fallback: snapshot unavailable, emitting placeholder profile.")
                    placeholderProfile()
                } else {
                    runCatching {
                        fetchProfile(snapshot.userId)
                            .getOrThrow()
                            .toDomain(snapshot.email)
                            .getOrThrow()
                    }.onFailure { error ->
                        Log.e(TAG, "observeProfile fallback: failed to fetch/decode profile for ${snapshot.userId}", error)
                    }.getOrElse {
                        placeholderProfile()
                    }
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
        val pharmacyId = resolvePharmacyIdOrNull()
        if (pharmacyId == null) {
            Log.w(TAG, "updateNotificationsReadState no-op: non-PHARMACY or missing pharmacy linkage.")
            return@runCatching Unit
        }
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
            }
            AccountType.WAREHOUSE -> {
                val orgId = identity.organizationId
                    ?: error("WAREHOUSE user missing organizationId")
                supabase.postgrest.from("requests").select(columns = Columns.ALL) {
                    filter { eq("warehouse_id", orgId) }
                }.decodeList<RequestDto>()
                    .map { it.toDomain().getOrThrow() }
            }
            AccountType.ADMIN -> {
                // ADMIN sees all requests (no filter)
                supabase.postgrest.from("requests").select(columns = Columns.ALL)
                    .decodeList<RequestDto>()
                    .map { it.toDomain().getOrThrow() }
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
    }

    private suspend fun fetchNotifications(): Result<List<AppNotification>> = runCatching {
        val pharmacyId = resolvePharmacyIdOrNull()
        if (pharmacyId == null) {
            Log.w(TAG, "fetchNotifications fallback: non-PHARMACY or missing pharmacy linkage, returning empty list.")
            return@runCatching emptyList()
        }
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
                    }.decodeList<RequestDto>().firstOrNull()?.toDomain()?.getOrThrow()
                }
                AccountType.WAREHOUSE -> {
                    val orgId = identity.organizationId
                        ?: error("WAREHOUSE user missing organizationId")
                    supabase.postgrest.from("requests").select {
                        filter {
                            eq("id", requestId)
                            eq("warehouse_id", orgId)  // Ownership check
                        }
                    }.decodeList<RequestDto>().firstOrNull()?.toDomain()?.getOrThrow()
                }
                AccountType.ADMIN -> {
                    // ADMIN can view any request by ID
                    supabase.postgrest.from("requests").select {
                        filter { eq("id", requestId) }
                    }.decodeList<RequestDto>().firstOrNull()?.toDomain()?.getOrThrow()
                }
                AccountType.PUBLIC_USER -> {
                    null // PUBLIC_USER cannot view requests
                }
            }
        }


    override suspend fun getWarehouseShipments(warehouseId: String): Result<List<WarehouseShipment>> = Result.success(emptyList())

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
                status = initialStatus,
            )

            Log.d(TAG, "Attempting to insert request into Supabase...")
            val insertedRequest = supabase.postgrest.from("requests").insert(reqInsert) {
                select(Columns.ALL)
            }.decodeSingle<RequestDto>()

            Log.d(TAG, "✅ REQUEST INSERT SUCCESS!")
            Log.d(TAG, "Returned request ID: ${insertedRequest.id}")
            Log.d(TAG, "Returned request status: ${insertedRequest.status}")
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

            // Build update DTO
            val updateDto = RequestUpdateDto(
                status = updates.status?.name,
                warehouseId = updates.warehouseId,
                warehouseName = updates.warehouseName,
                notes = updates.notes,
            )

            // Apply update via Supabase
            val updatedRequest = supabase.postgrest.from("requests").update(updateDto) {
                select(Columns.ALL)
                filter { eq("id", requestId) }
            }.decodeSingle<RequestDto>()

            Log.d(TAG, "✅ REQUEST UPDATE SUCCESS")
            updatedRequest.toDomain().getOrThrow()
        }.onFailure { exception ->
            Log.e(TAG, "❌ UPDATE REQUEST FAILED", exception)
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
        runCatching {
            val ownedRequest = getOwnedRequestForPharmacy(
                requestId = requestId,
                actionName = "Submitting request",
            )

            require(
                com.pharmalink.domain.model.RequestTransitions.canTransition(
                    ownedRequest.status,
                    RequestStatus.PENDING,
                    AccountType.PHARMACY,
                )
            ) {
                "Invalid status transition: ${ownedRequest.status} -> ${RequestStatus.PENDING} for role ${AccountType.PHARMACY}"
            }

            supabase.postgrest.from("requests").update(
                RequestUpdateDto(status = RequestStatus.PENDING.name),
            ) {
                select(Columns.ALL)
                filter {
                    eq("id", requestId)
                    eq("pharmacy_id", ownedRequest.pharmacyId)
                }
            }.decodeSingle<RequestDto>()
        }.map { Unit }

    override suspend fun markNotificationRead(notificationId: String): Result<Unit> =
        updateNotificationsReadState(
            read = true,
            notificationId = notificationId,
        )

    override suspend fun markAllNotificationsRead(): Result<Unit> =
        updateNotificationsReadState(read = true)

    override suspend fun deleteNotification(notificationId: String): Result<Unit> =
        runCatching {
            val pharmacyId = resolvePharmacyIdOrNull()
            if (pharmacyId == null) {
                Log.w(TAG, "deleteNotification no-op: non-PHARMACY or missing pharmacy linkage.")
                return@runCatching Unit
            }
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
            val pharmacyId = resolvePharmacyIdOrNull()
            if (pharmacyId == null) {
                Log.w(TAG, "deleteAllNotifications no-op: non-PHARMACY or missing pharmacy linkage.")
                return@runCatching Unit
            }
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

    // ==================== B2C Customer Order Methods (Phase 4.3B) ====================

    override suspend fun createCustomerOrder(
        medicineId: String,
        medicineName: String,
        quantity: Int,
        unit: String,
        pharmacyId: String,
        fulfillmentType: FulfillmentType,
        deliveryAddress: String?,
        deliveryPhone: String?,
        notes: String?,
    ): Result<DomainOrder> = runCatching {
        // 1. Role validation: PUBLIC_USER only
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PUBLIC_USER) {
            "Only PUBLIC_USER can create customer orders"
        }
        
        // 2. Get customer ID from identity
        val customerId = identity.userId
            ?: throw IllegalStateException("PUBLIC_USER missing userId")
        
        // 3. Validate fulfillment requirements
        if (fulfillmentType == FulfillmentType.DELIVERY) {
            require(!deliveryAddress.isNullOrBlank()) {
                "Delivery address required for DELIVERY fulfillment"
            }
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
            fulfillmentType = fulfillmentType.name,
            deliveryAddress = deliveryAddress,
            deliveryPhone = deliveryPhone,
            notes = notes,
        )
        
        // 5. Insert order (starts PENDING, totalPriceCents = null)
        val result = supabase.postgrest.from("orders").insert(createOrderDto) {
            select(Columns.ALL)
        }.decodeSingle<OrderDto>()
        
        result.toOrderDomain().getOrThrow()
    }

    override suspend fun cancelCustomerOrder(orderId: String): Result<Unit> = runCatching {
        // 1. Get current user
        val identity = resolveAccessContext()
        require(identity.role == AccountType.PUBLIC_USER) {
            "Only PUBLIC_USER can cancel their orders"
        }
        
        val customerId = identity.userId
            ?: throw IllegalStateException("PUBLIC_USER missing userId")
        
        // 2. Get order and validate ownership
        val order = getOrder(orderId).getOrThrow()
            ?: throw IllegalStateException("Order not found: $orderId")
        
        require(order.customerId == customerId) {
            "Cannot cancel order that doesn't belong to you"
        }
        
        require(order.status == OrderStatus.PENDING) {
            "Can only cancel PENDING orders. Current status: ${order.status}"
        }
        
        // 3. Update status to CANCELLED
        supabase.postgrest.from("orders").update(
            { "status" to OrderStatus.CANCELLED.name }
        ) {
            filter {
                eq("id", orderId)
                eq("customer_id", customerId)
            }
        }
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
        
        // 2. Validate status transition
        require(order.status == OrderStatus.PENDING) {
            "Can only confirm PENDING orders. Current status: ${order.status}"
        }
        
        // 3. Validate price
        require(totalPriceCents >= 0) {
            "Total price must be >= 0"
        }
        
        // 4. Update order
        val updates = mapOf(
            "status" to OrderStatus.CONFIRMED.name,
            "total_price_cents" to totalPriceCents,
            "confirmed_at" to Instant.now().toString(),
            "updated_at" to Instant.now().toString(),
        )
        
        supabase.postgrest.from("orders").update(updates) {
            filter { eq("id", orderId) }
        }
        
        // 5. Return updated order
        getOrder(orderId).getOrThrow()
            ?: throw IllegalStateException("Order not found after update")
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
        
        // 2. Validate status transition
        require(order.status == OrderStatus.PENDING) {
            "Can only reject PENDING orders. Current status: ${order.status}"
        }
        
        // 3. Update order
        val updates = mapOf(
            "status" to OrderStatus.REJECTED.name,
            "updated_at" to Instant.now().toString(),
        )
        
        supabase.postgrest.from("orders").update(updates) {
            filter { eq("id", orderId) }
        }
        
        // 4. Return updated order
        getOrder(orderId).getOrThrow()
            ?: throw IllegalStateException("Order not found after update")
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
        
        // 2. Validate fulfillment type
        require(order.fulfillmentType == FulfillmentType.PICKUP) {
            "Can only mark PICKUP orders as ready. Current type: ${order.fulfillmentType}"
        }
        
        // 3. Validate status transition (CONFIRMED or IN_PROGRESS -> READY_FOR_PICKUP)
        require(order.status == OrderStatus.CONFIRMED || order.status == OrderStatus.IN_PROGRESS) {
            "Can only mark CONFIRMED or IN_PROGRESS orders as ready. Current status: ${order.status}"
        }
        
        // 4. Update order
        val updates = mapOf(
            "status" to OrderStatus.READY_FOR_PICKUP.name,
            "updated_at" to Instant.now().toString(),
        )
        
        supabase.postgrest.from("orders").update(updates) {
            filter { eq("id", orderId) }
        }
        
        // 5. Return updated order
        getOrder(orderId).getOrThrow()
            ?: throw IllegalStateException("Order not found after update")
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
        
        // 2. Validate fulfillment type
        require(order.fulfillmentType == FulfillmentType.DELIVERY) {
            "Can only mark DELIVERY orders as out for delivery. Current type: ${order.fulfillmentType}"
        }
        
        // 3. Validate status transition
        require(order.status == OrderStatus.CONFIRMED || order.status == OrderStatus.IN_PROGRESS) {
            "Can only mark CONFIRMED or IN_PROGRESS orders as out for delivery. Current status: ${order.status}"
        }
        
        // 4. Update order
        val updates = mapOf(
            "status" to OrderStatus.OUT_FOR_DELIVERY.name,
            "updated_at" to Instant.now().toString(),
        )
        
        supabase.postgrest.from("orders").update(updates) {
            filter { eq("id", orderId) }
        }
        
        // 5. Return updated order
        getOrder(orderId).getOrThrow()
            ?: throw IllegalStateException("Order not found after update")
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
        
        // 2. Validate status transition (must be READY_FOR_PICKUP or OUT_FOR_DELIVERY)
        require(order.status == OrderStatus.READY_FOR_PICKUP || order.status == OrderStatus.OUT_FOR_DELIVERY) {
            "Can only mark READY_FOR_PICKUP or OUT_FOR_DELIVERY orders as delivered. Current status: ${order.status}"
        }
        
        // 3. Update order
        val updates = mapOf(
            "status" to OrderStatus.DELIVERED.name,
            "fulfilled_at" to Instant.now().toString(),
            "updated_at" to Instant.now().toString(),
        )
        
        supabase.postgrest.from("orders").update(updates) {
            filter { eq("id", orderId) }
        }
        
        // 4. Return updated order
        getOrder(orderId).getOrThrow()
            ?: throw IllegalStateException("Order not found after update")
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
        
        // 2. Fetch orders by customer_id
        supabase.postgrest.from("orders").select(columns = Columns.ALL) {
            filter { eq("customer_id", customerId) }
        }.decodeList<OrderDto>().map { it.toOrderDomain().getOrThrow() }
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

    private fun defaultHomeStats(): HomeStats = HomeStats(
        requestsTodayCount = 0,
        requestsTodayTrend = "",
        totalInventoryCount = null,
        totalInventoryUnit = null,
        weeklySalesAmount = null,
        weeklySalesTrend = null,
        alertMessage = null,
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
    @SerialName("medicine_id") val medicineId: String,
    @SerialName("medicine_name") val medicineName: String,
    val quantity: Int,
    val unit: String,
    val status: String,
    @SerialName("order_type") val orderType: String,
    @SerialName("fulfillment_type") val fulfillmentType: String,
    @SerialName("pharmacy_id") val pharmacyId: String,
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
        )
    }
}

@Serializable
private data class CreateOrderDto(
    @SerialName("medicine_id") val medicineId: String,
    @SerialName("medicine_name") val medicineName: String,
    val quantity: Int,
    val unit: String,
    @SerialName("pharmacy_id") val pharmacyId: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("order_type") val orderType: String,
    @SerialName("fulfillment_type") val fulfillmentType: String,
    @SerialName("delivery_address") val deliveryAddress: String? = null,
    @SerialName("delivery_phone") val deliveryPhone: String? = null,
    val notes: String? = null,
    val status: String = "PENDING",
    val currency: String = "SAR",
)

@Serializable
private data class RequestDto(
    val id: String,
    @SerialName("pharmacy_id") val pharmacyId: String,
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
        val priorityEnum = priority.toRequestPriority()
        val statusEnum = status.toRequestStatus()

        val createdAtLabel = isoStringToDisplayLabel(createdAt)
        val updatedAtLabel = isoStringToDisplayLabel(updatedAt).ifBlank { createdAtLabel }

        Request(
            id = id,
            pharmacyId = pharmacyId,
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

@Serializable
private data class RequestUpdateDto(
    val status: String? = null,
    @SerialName("warehouse_id") val warehouseId: String? = null,
    @SerialName("warehouse_name") val warehouseName: String? = null,
    val notes: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private val isoDisplayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

private class UnauthorizedException(
    message: String,
) : IllegalStateException(message)

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

private fun String.toNotificationType(): NotificationType = NotificationType.valueOf(this)

private fun String.toNotificationCategory(): NotificationCategory = NotificationCategory.valueOf(this)

private fun String.toNotificationDestination(): NotificationDestination? =
    runCatching { NotificationDestination.valueOf(this) }.getOrNull()

private fun isoStringToInstant(iso: String?): java.time.Instant {
    return iso?.let {
        runCatching { java.time.Instant.parse(it) }.getOrNull()
    } ?: java.time.Instant.now()
}

