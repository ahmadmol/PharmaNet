package com.pharmalink.data.repository

import com.pharmalink.data.dto.NearbyOrderDto
import com.pharmalink.core.location.FacilityLocation
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.AdminUser
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.AuditLog
import com.pharmalink.domain.model.CreateFacilityRequest
import com.pharmalink.domain.model.ComplianceDocument
import com.pharmalink.domain.model.ComplianceDocumentStatus
import com.pharmalink.domain.model.ComplianceOverview
import com.pharmalink.domain.model.CustomerRequestScope
import com.pharmalink.domain.model.CustomerRequestUrgency
import com.pharmalink.domain.model.DeliveryDelegate
import com.pharmalink.domain.model.DeliveryStatus
import com.pharmalink.domain.model.DeliveryTracking
import com.pharmalink.domain.model.HomeStats
import com.pharmalink.domain.model.Medicine
import com.pharmalink.domain.model.NotificationCategory
import com.pharmalink.domain.model.NotificationDestination
import com.pharmalink.domain.model.NotificationType
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.OrderType
import com.pharmalink.domain.model.Pharmacy
import com.pharmalink.domain.model.PharmacyCustomerOrder
import com.pharmalink.domain.model.PharmacyProfile
import com.pharmalink.domain.model.PublicPharmacyForMedicine
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestItem
import com.pharmalink.domain.model.RequestPriority
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.model.SupplierComplianceItem
import com.pharmalink.domain.model.Warehouse
import com.pharmalink.domain.model.WarehouseShipment
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class InMemoryPharmaRepository @Inject constructor() : PharmaRepository {

    private val warehouses = MutableStateFlow(sampleWarehouses())
    private val orders = MutableStateFlow(sampleOrders())
    private val requests = MutableStateFlow(sampleRequests())
    private val notifications = MutableStateFlow(sampleNotifications())
    private val profile = MutableStateFlow(sampleProfile())
    private val compliance = MutableStateFlow(sampleCompliance())
    private val shipments = sampleShipments()

    override fun observeOrders(): Flow<List<Order>> = orders.asStateFlow()
    override fun observeRequests(): Flow<List<Request>> = requests.asStateFlow()

    override fun observeIncomingRequestsForWarehouse(warehouseId: String): Flow<List<Request>> =
        requests.asStateFlow().map { list -> list.filter { it.warehouseId == warehouseId } }

    override fun observeWarehouses(): Flow<List<Warehouse>> = warehouses.asStateFlow()
    override fun observeNotifications(): Flow<List<AppNotification>> = notifications.asStateFlow()
    override fun observeProfile(): Flow<PharmacyProfile> = profile.asStateFlow()
    override fun observeCompliance(): Flow<ComplianceOverview> = compliance.asStateFlow()

    override suspend fun updateProfile(profile: PharmacyProfile): Result<Unit> {
        this.profile.value = profile
        return Result.success(Unit)
    }

    override suspend fun updateMyWarehouseLocation(
        address: String,
        latitude: Double,
        longitude: Double,
    ): Result<Warehouse> = Result.failure(
        UnsupportedOperationException("updateMyWarehouseLocation requires SupabasePharmaRepository."),
    )

    override suspend fun fetchHomeStats(): Result<HomeStats> = Result.success(
        HomeStats(
            requestsTodayCount = requests.value.size,
            requestsTodayTrend = "+5%",
            totalInventoryCount = 1250,
            totalInventoryUnit = "وحدة",
            weeklySalesAmount = "12.5K",
            weeklySalesTrend = "+3.2%",
            alertMessage = if (requests.value.size > 10) "لديك عدد كبير من الطلبات المعلقة" else null,
        ),
    )

    override suspend fun fetchFeaturedWarehouses(): Result<List<Warehouse>> =
        Result.success(warehouses.value.take(10))

    override suspend fun fetchMedicines(): Result<List<Medicine>> =
        Result.success(sampleMedicines())

    override suspend fun getWarehouseProducts(warehouseId: String): Result<List<Medicine>> =
        Result.success(
            sampleMedicines().filter { medicine ->
                medicine.warehouseId == warehouseId && medicine.isActive && medicine.isVisible
            },
        )

    override suspend fun addMedicine(medicine: Medicine, warehouseId: String): Result<Unit> =
        Result.failure(
            UnsupportedOperationException("addMedicine is not supported in InMemoryPharmaRepository.")
        )

    override suspend fun updateMedicine(
        medicineId: String,
        name: String,
        brand: String?,
        strength: String?,
        description: String?,
        priceAmount: Double?,
        stockQuantity: Int,
        imageUrl: String?,
        isVisible: Boolean,
        isActive: Boolean,
    ): Result<Unit> = Result.failure(
        UnsupportedOperationException("updateMedicine is not supported in InMemoryPharmaRepository.")
    )

    override suspend fun deleteMedicine(medicineId: String): Result<Unit> = Result.failure(
        UnsupportedOperationException("deleteMedicine is not supported in InMemoryPharmaRepository.")
    )

    override suspend fun deleteMedicineImage(imageUrl: String): Result<Unit> = Result.success(Unit)

    override suspend fun getPublicPharmacies(): Result<List<PublicPharmacyForMedicine>> =
        Result.failure(
            UnsupportedOperationException("getPublicPharmacies is not supported in InMemoryPharmaRepository. Use SupabasePharmaRepository for PUBLIC_USER discovery.")
        )

    override suspend fun getPublicPharmaciesForMedicine(medicineId: String): Result<List<PublicPharmacyForMedicine>> =
        Result.failure(
            UnsupportedOperationException("getPublicPharmaciesForMedicine is not supported in InMemoryPharmaRepository. Use SupabasePharmaRepository for PUBLIC_USER discovery.")
        )

    override suspend fun getCurrentPharmacyFacilityLocation(): Result<FacilityLocation?> =
        Result.success(null)

    override suspend fun getNearbyOrders(lat: Double, lng: Double, radius: Double): Result<List<NearbyOrderDto>> =
        Result.failure(
            UnsupportedOperationException("getNearbyOrders is not supported in InMemoryPharmaRepository.")
        )

    override fun observeNearbyOrdersRealtime(
        lat: Double,
        lng: Double,
        radius: Double,
    ): kotlinx.coroutines.flow.Flow<List<NearbyOrderDto>> =
        kotlinx.coroutines.flow.flow {
            // Radar demo data is not implemented in InMemory repository.
            emit(emptyList())
        }

    override suspend fun getOrder(orderId: String): Result<Order?> =
        Result.success(orders.value.firstOrNull { it.id == orderId })

    override suspend fun getRequest(requestId: String): Result<Request?> =
        Result.success(requests.value.firstOrNull { it.id == requestId })

    override suspend fun getWarehouse(warehouseId: String): Result<Warehouse?> =
        Result.success(warehouses.value.firstOrNull { it.id == warehouseId })

    override suspend fun getWarehouseShipments(warehouseId: String): Result<List<WarehouseShipment>> =
        Result.success(shipments[warehouseId].orEmpty())

    override suspend fun createRequest(request: Request): Result<Request> {
        val warehouse = warehouses.value.firstOrNull { it.id == request.warehouseId } ?: warehouses.value.first()
        val requestId = "REQ-${1280 + requests.value.size + 1}"
        val requestItems = request.items.ifEmpty {
            listOf(request.toLegacyRequestItem(requestId = requestId, lineNo = 1))
        }.mapIndexed { index, item ->
            item.copy(
                id = item.id.ifBlank { "$requestId-ITEM-${index + 1}" },
                requestId = requestId,
                lineNo = index + 1,
            )
        }
        val firstItem = requestItems.first()

        val createdRequest = request.copy(
            id = requestId,
            warehouseId = warehouse.id,
            warehouseName = warehouse.name,
            supplierName = warehouse.name,
            medicineId = firstItem.medicineId,
            medicineName = firstItem.medicineName,
            medicineSubtitle = firstItem.medicineSubtitle,
            quantity = firstItem.quantity,
            unit = firstItem.unit,
            status = RequestStatus.DRAFT,
            createdAtLabel = "الآن",
            updatedAtLabel = "تم الإرسال الآن",
            relatedOrderId = null,
            items = requestItems,
        )

        requests.value = listOf(createdRequest) + requests.value
        notifications.value = listOf(
            AppNotification(
                id = "NOT-${notifications.value.size + 1}",
                title = "تم إنشاء طلب جديد",
                body = "تم إرسال ${createdRequest.medicineName} إلى ${warehouse.name}",
                type = NotificationType.ORDER_UPDATE,
                category = NotificationCategory.REQUESTS,
                createdAtLabel = "الآن",
                read = false,
                requiresAction = true,
                destination = NotificationDestination.REQUEST,
                destinationId = createdRequest.id,
            ),
        ) + notifications.value

        return Result.success(createdRequest)
    }

    override suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        notifications.value = notifications.value.map { notification ->
            if (notification.id == notificationId) notification.copy(read = true) else notification
        }
        return Result.success(Unit)
    }

    override suspend fun markAllNotificationsRead(): Result<Unit> {
        notifications.value = notifications.value.map { it.copy(read = true) }
        return Result.success(Unit)
    }

    override suspend fun updateNotificationsPreference(enabled: Boolean): Result<Unit> {
        profile.value = profile.value.copy(notificationsEnabled = enabled)
        return Result.success(Unit)
    }

    override suspend fun submitSupportRequest(
        subject: String,
        message: String,
        category: String?,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updateRequest(requestId: String, updates: com.pharmalink.domain.model.RequestUpdate): Result<Request> {
        val idx = requests.value.indexOfFirst { it.id == requestId }
        return if (idx >= 0) {
            val current = requests.value[idx]
            val nextItems = updates.items?.let { items ->
                require(items.isNotEmpty()) { "Request basket must contain at least one item" }
                require(current.status == RequestStatus.DRAFT) {
                    "Replacing request basket items is allowed only for DRAFT requests."
                }
                items.mapIndexed { index, item ->
                    item.copy(
                        id = item.id.ifBlank { "$requestId-ITEM-${index + 1}" },
                        requestId = requestId,
                        lineNo = index + 1,
                    )
                }
            }
            val firstItem = nextItems?.first()
            val updated = current.copy(
                status = updates.status ?: current.status,
                warehouseId = updates.warehouseId ?: current.warehouseId,
                warehouseName = updates.warehouseName ?: current.warehouseName,
                notes = updates.notes ?: current.notes,
                medicineId = firstItem?.medicineId ?: current.medicineId,
                medicineName = firstItem?.medicineName ?: current.medicineName,
                medicineSubtitle = firstItem?.medicineSubtitle ?: current.medicineSubtitle,
                quantity = firstItem?.quantity ?: current.quantity,
                unit = firstItem?.unit ?: current.unit,
                items = nextItems ?: current.items,
                updatedAtLabel = "ط§ظ„ط¢ظ†",
            )
            val mutable = requests.value.toMutableList()
            mutable[idx] = updated
            requests.value = mutable
            Result.success(updated)
        } else {
            Result.failure(IllegalArgumentException("Request not found"))
        }
    }

    override suspend fun getRequestItems(requestId: String): Result<List<RequestItem>> =
        Result.success(requests.value.firstOrNull { it.id == requestId }?.items.orEmpty())

    override suspend fun replaceRequestItems(requestId: String, items: List<RequestItem>): Result<List<RequestItem>> =
        runCatching {
            require(items.isNotEmpty()) { "Request basket must contain at least one item" }
            updateRequest(requestId, com.pharmalink.domain.model.RequestUpdate(items = items)).getOrThrow().items
        }

    override suspend fun deleteRequest(requestId: String): Result<Unit> {
        val before = requests.value.size
        requests.value = requests.value.filter { it.id != requestId }
        return if (requests.value.size < before) Result.success(Unit)
        else Result.failure(IllegalArgumentException("Request not found"))
    }

    override suspend fun submitRequest(requestId: String): Result<Unit> =
        submitPharmacyRequest(requestId).map { Unit }

    override suspend fun submitPharmacyRequest(requestId: String): Result<Request> {
        val idx = requests.value.indexOfFirst { it.id == requestId }
        return if (idx >= 0) {
            val current = requests.value[idx]
            require(current.status == RequestStatus.DRAFT) { "Can only submit DRAFT requests" }
            require(!current.medicineId.isNullOrBlank()) { "medicineId is required for B2B pharmacy requests" }
            require(orders.value.none { it.requestId == requestId && it.orderType == OrderType.PHARMACY_WAREHOUSE }) {
                "Order already exists for request"
            }
            val requestItems = current.items.ifEmpty {
                listOf(current.toLegacyRequestItem(requestId = requestId, lineNo = 1))
            }
            val firstItem = requestItems.first()
            val orderId = "ORD-${880 + orders.value.size + 1}"
            val now = Instant.now()
            val updatedRequest = current.copy(
                status = RequestStatus.PENDING,
                updatedAtLabel = "now",
                relatedOrderId = orderId,
                medicineId = firstItem.medicineId,
                medicineName = firstItem.medicineName,
                medicineSubtitle = firstItem.medicineSubtitle,
                quantity = firstItem.quantity,
                unit = firstItem.unit,
                items = requestItems,
            )
            val createdOrder = Order(
                id = orderId,
                medicineId = firstItem.medicineId,
                medicineName = firstItem.medicineName,
                quantity = firstItem.quantity,
                unit = firstItem.unit,
                status = OrderStatus.PENDING,
                orderType = OrderType.PHARMACY_WAREHOUSE,
                fulfillmentType = FulfillmentType.DELIVERY,
                pharmacyId = current.pharmacyId,
                warehouseId = current.warehouseId,
                customerId = null,
                requestId = requestId,
                totalPriceCents = null,
                currency = "SAR",
                deliveryAddress = null,
                deliveryPhone = null,
                notes = current.notes,
                createdAt = now,
                updatedAt = now,
                confirmedAt = null,
                fulfilledAt = null,
                warehouseName = current.warehouseName,
                supplierName = current.supplierName,
                etaLabel = current.etaLabel,
                isUrgent = current.priority == RequestPriority.URGENT,
            )
            val mutable = requests.value.toMutableList()
            mutable[idx] = updatedRequest
            requests.value = mutable
            orders.value = listOf(createdOrder) + orders.value
            Result.success(updatedRequest)
        } else {
            Result.failure(IllegalArgumentException("Request not found"))
        }
    }

    override suspend fun warehouseAcceptB2bRequest(
        requestId: String,
        totalPriceCents: Long,
    ): Result<Request> =
        updateB2bRequestAndOrder(
            requestId = requestId,
            expectedRequestStatus = RequestStatus.PENDING,
            expectedOrderStatus = OrderStatus.PENDING,
            nextRequestStatus = RequestStatus.QUOTE_PENDING,
            nextOrderStatus = OrderStatus.QUOTE_PENDING,
            orderTransform = { order ->
                order.copy(
                    totalPriceCents = totalPriceCents,
                )
            },
        )

    override suspend fun warehouseRejectB2bRequest(requestId: String, reason: String?): Result<Request> =
        updateB2bRequestAndOrder(
            requestId = requestId,
            expectedRequestStatus = RequestStatus.PENDING,
            expectedOrderStatus = OrderStatus.PENDING,
            nextRequestStatus = RequestStatus.REJECTED,
            nextOrderStatus = OrderStatus.REJECTED,
            requestTransform = { request -> request.copy(rejectionReason = reason) },
        )

    override suspend fun warehouseStartB2bFulfillment(requestId: String): Result<Request> =
        updateB2bRequestAndOrder(
            requestId = requestId,
            expectedRequestStatus = RequestStatus.ACCEPTED,
            expectedOrderStatus = OrderStatus.CONFIRMED,
            nextRequestStatus = RequestStatus.IN_PROGRESS,
            nextOrderStatus = OrderStatus.IN_PROGRESS,
        )

    override suspend fun warehouseMarkB2bDelivered(
        requestId: String,
        deliveryNote: String?,
    ): Result<Request> =
        updateB2bRequestAndOrder(
            requestId = requestId,
            expectedRequestStatus = RequestStatus.IN_PROGRESS,
            expectedOrderStatus = OrderStatus.IN_PROGRESS,
            nextRequestStatus = RequestStatus.FULFILLED,
            nextOrderStatus = OrderStatus.DELIVERED,
            orderTransform = { order ->
                order.copy(
                    notes = deliveryNote ?: order.notes,
                    fulfilledAt = Instant.now(),
                )
            },
        )

    override suspend fun pharmacyAcceptB2bQuote(requestId: String): Result<Request> =
        updateB2bRequestAndOrder(
            requestId = requestId,
            expectedRequestStatus = RequestStatus.QUOTE_PENDING,
            expectedOrderStatus = OrderStatus.QUOTE_PENDING,
            nextRequestStatus = RequestStatus.ACCEPTED,
            nextOrderStatus = OrderStatus.CONFIRMED,
            orderTransform = { order -> order.copy(confirmedAt = Instant.now()) },
        )

    override suspend fun pharmacyRejectB2bQuote(requestId: String, reason: String?): Result<Request> =
        updateB2bRequestAndOrder(
            requestId = requestId,
            expectedRequestStatus = RequestStatus.QUOTE_PENDING,
            expectedOrderStatus = OrderStatus.QUOTE_PENDING,
            nextRequestStatus = RequestStatus.REJECTED,
            nextOrderStatus = OrderStatus.REJECTED,
            requestTransform = { request -> request.copy(rejectionReason = reason) },
        )

    private fun updateB2bRequestAndOrder(
        requestId: String,
        expectedRequestStatus: RequestStatus,
        expectedOrderStatus: OrderStatus,
        nextRequestStatus: RequestStatus,
        nextOrderStatus: OrderStatus,
        requestTransform: (Request) -> Request = { it },
        orderTransform: (Order) -> Order = { it },
    ): Result<Request> {
        val requestIdx = requests.value.indexOfFirst { it.id == requestId }
        if (requestIdx < 0) return Result.failure(IllegalArgumentException("Request not found"))

        val currentRequest = requests.value[requestIdx]
        val orderIdx = orders.value.indexOfFirst {
            it.requestId == requestId && it.orderType == OrderType.PHARMACY_WAREHOUSE
        }
        if (orderIdx < 0) return Result.failure(IllegalArgumentException("B2B order not found"))

        val currentOrder = orders.value[orderIdx]
        return runCatching {
            require(currentRequest.status == expectedRequestStatus) {
                "Expected request status $expectedRequestStatus but was ${currentRequest.status}"
            }
            require(currentOrder.status == expectedOrderStatus) {
                "Expected order status $expectedOrderStatus but was ${currentOrder.status}"
            }

            val now = Instant.now()
            val updatedRequest = requestTransform(
                currentRequest.copy(
                    status = nextRequestStatus,
                    updatedAtLabel = "now",
                )
            )
            val updatedOrder = orderTransform(
                currentOrder.copy(
                    status = nextOrderStatus,
                    updatedAt = now,
                )
            )

            val mutableRequests = requests.value.toMutableList()
            mutableRequests[requestIdx] = updatedRequest
            requests.value = mutableRequests

            val mutableOrders = orders.value.toMutableList()
            mutableOrders[orderIdx] = updatedOrder
            orders.value = mutableOrders

            updatedRequest
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        val before = notifications.value.size
        notifications.value = notifications.value.filter { it.id != notificationId }
        return if (notifications.value.size < before) Result.success(Unit)
        else Result.failure(IllegalArgumentException("Notification not found"))
    }

    override suspend fun deleteAllNotifications(): Result<Unit> {
        notifications.value = emptyList()
        return Result.success(Unit)
    }

    override suspend fun getDeliveryTracking(orderId: String): Result<DeliveryTracking> =
        getOrder(orderId).mapCatching { order ->
            order ?: throw IllegalArgumentException("Order not found")
        }.map { buildDeliveryTrackingForDemo(it) }

    override suspend fun recordDelegateCall(phoneNumber: String): Result<Unit> {
        delay(200)
        return Result.success(Unit)
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> {
        val idx = orders.value.indexOfFirst { it.id == orderId }
        return if (idx >= 0) {
            val mutable = orders.value.toMutableList()
            mutable[idx] = mutable[idx].copy(status = status, updatedAt = java.time.Instant.now())
            orders.value = mutable
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Order not found"))
        }
    }

    override suspend fun createOrder(order: Order): Result<Order> {
        orders.value = listOf(order) + orders.value
        return Result.success(order)
    }

    override suspend fun deleteOrder(orderId: String): Result<Unit> {
        val before = orders.value.size
        orders.value = orders.value.filter { it.id != orderId }
        return if (orders.value.size < before) Result.success(Unit)
        else Result.failure(IllegalArgumentException("Order not found"))
    }

    // ==================== B2C Customer Order Methods (Phase 4.3B) ====================
    // Note: InMemory repository does not support B2C operations - use SupabasePharmaRepository

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
    ): Result<Order> = Result.failure(UnsupportedOperationException("Mock not implemented"))

    override suspend fun uploadPrescription(uri: android.net.Uri): Result<String> =
        Result.failure(UnsupportedOperationException("Mock not implemented"))

    override suspend fun uploadMedicineImage(uri: android.net.Uri): Result<String> =
        Result.failure(UnsupportedOperationException("Mock not implemented"))

    override suspend fun uploadProfileAvatar(uri: android.net.Uri): Result<String> =
        Result.failure(UnsupportedOperationException("Mock not implemented"))

    override suspend fun deleteProfileAvatar(avatarUrl: String): Result<Unit> = Result.success(Unit)

    override suspend fun cancelCustomerOrder(orderId: String): Result<Unit> = Result.success(Unit)

    override suspend fun acceptCustomerOrderPrice(orderId: String): Result<Unit> = Result.success(Unit)

    override suspend fun rejectCustomerOrderPrice(orderId: String): Result<Unit> = Result.success(Unit)

    override suspend fun confirmOrder(orderId: String, totalPriceCents: Long): Result<Order> = Result.failure(
        UnsupportedOperationException("confirmOrder is not supported in InMemoryPharmaRepository. Use SupabasePharmaRepository for B2C operations.")
    )

    override suspend fun rejectOrder(orderId: String): Result<Order> = Result.failure(
        UnsupportedOperationException("rejectOrder is not supported in InMemoryPharmaRepository. Use SupabasePharmaRepository for B2C operations.")
    )

    override suspend fun markOrderReadyForPickup(orderId: String): Result<Order> = Result.failure(
        UnsupportedOperationException("markOrderReadyForPickup is not supported in InMemoryPharmaRepository. Use SupabasePharmaRepository for B2C operations.")
    )

    override suspend fun markOrderOutForDelivery(orderId: String): Result<Order> = Result.failure(
        UnsupportedOperationException("markOrderOutForDelivery is not supported in InMemoryPharmaRepository. Use SupabasePharmaRepository for B2C operations.")
    )

    override suspend fun markOrderDelivered(orderId: String): Result<Order> = Result.failure(
        UnsupportedOperationException("markOrderDelivered is not supported in InMemoryPharmaRepository. Use SupabasePharmaRepository for B2C operations.")
    )

    override suspend fun getMyOrders(customerId: String): Result<List<Order>> = Result.failure(
        UnsupportedOperationException("getMyOrders is not supported in InMemoryPharmaRepository. Use SupabasePharmaRepository for B2C operations.")
    )

    override suspend fun getPharmacyCustomerOrders(): Result<List<PharmacyCustomerOrder>> =
        Result.success(
            orders.value
                .filter {
                    it.orderType == OrderType.CUSTOMER_PHARMACY &&
                        it.requestScope == CustomerRequestScope.SPECIFIC_PHARMACY
                }
                .map { it.toPharmacyCustomerOrder() },
        )

    override suspend fun getPharmacyCustomerOrderDetail(orderId: String): Result<PharmacyCustomerOrder> =
        getPharmacyCustomerOrders().mapCatching { list ->
            list.firstOrNull { it.id == orderId } ?: throw IllegalArgumentException("Customer order not found")
        }

    override suspend fun claimNearbyCustomerOrder(orderId: String, radiusKm: Double): Result<Unit> =
        updateCustomerOrder(orderId) { order ->
            require(order.orderType == OrderType.CUSTOMER_PHARMACY) { "Order is not a customer order" }
            require(order.requestScope == CustomerRequestScope.ALL_PHARMACIES) { "Order is not a radar order" }
            require(order.status == OrderStatus.PENDING) { "Can only claim pending radar orders" }
            order.copy(
                pharmacyId = "pharmacy_1",
                requestScope = CustomerRequestScope.SPECIFIC_PHARMACY,
                updatedAt = Instant.now(),
            )
        }

    override suspend fun confirmCustomerOrder(orderId: String, totalPriceCents: Long): Result<Unit> =
        updateCustomerOrder(orderId) { order ->
            require(order.status == OrderStatus.PENDING) { "Can only confirm pending orders" }
            order.copy(
                status = OrderStatus.CONFIRMED,
                totalPriceCents = totalPriceCents,
                confirmedAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        }

    override suspend fun rejectCustomerOrder(orderId: String): Result<Unit> =
        updateCustomerOrder(orderId) { order ->
            require(order.status == OrderStatus.PENDING) { "Can only reject pending orders" }
            order.copy(status = OrderStatus.REJECTED, updatedAt = Instant.now())
        }

    override suspend fun markCustomerOrderReadyForPickup(orderId: String): Result<Unit> =
        updateCustomerOrder(orderId) { order ->
            require(
                order.status in setOf(OrderStatus.CONFIRMED, OrderStatus.IN_PROGRESS) &&
                    order.fulfillmentType == FulfillmentType.PICKUP
            )
            order.copy(status = OrderStatus.READY_FOR_PICKUP, updatedAt = Instant.now())
        }

    override suspend fun markCustomerOrderOutForDelivery(orderId: String): Result<Unit> =
        updateCustomerOrder(orderId) { order ->
            require(
                order.status in setOf(OrderStatus.CONFIRMED, OrderStatus.IN_PROGRESS) &&
                    order.fulfillmentType == FulfillmentType.DELIVERY
            )
            order.copy(status = OrderStatus.OUT_FOR_DELIVERY, updatedAt = Instant.now())
        }

    override suspend fun markCustomerOrderDelivered(orderId: String): Result<Unit> =
        updateCustomerOrder(orderId) { order ->
            require(order.status == OrderStatus.READY_FOR_PICKUP || order.status == OrderStatus.OUT_FOR_DELIVERY)
            order.copy(status = OrderStatus.DELIVERED, fulfilledAt = Instant.now(), updatedAt = Instant.now())
        }

    override suspend fun adminGetAllUsers(): Result<List<AdminUser>> = Result.success(emptyList())

    override suspend fun adminUpdateUserProfile(
        targetUserId: String,
        fullName: String?,
        accountType: AccountType,
        pharmacyId: String?,
        warehouseId: String?,
        isActive: Boolean,
    ): Result<AdminUser> = Result.failure(
        UnsupportedOperationException("adminUpdateUserProfile requires SupabasePharmaRepository."),
    )

    override suspend fun adminGetAllPharmacies(): Result<List<Pharmacy>> = Result.success(emptyList())

    override suspend fun adminCreatePharmacy(
        name: String,
        location: String,
        contactNumber: String,
        licenseNumber: String,
    ): Result<Pharmacy> = Result.failure(
        UnsupportedOperationException("adminCreatePharmacy requires SupabasePharmaRepository."),
    )

    override suspend fun adminGetAllWarehouses(): Result<List<Warehouse>> = Result.success(warehouses.value)

    override suspend fun adminCreateWarehouse(
        name: String,
        location: String,
        contactNumber: String,
    ): Result<Warehouse> = Result.failure(
        UnsupportedOperationException("adminCreateWarehouse requires SupabasePharmaRepository."),
    )

    override suspend fun createFacility(request: CreateFacilityRequest): Result<Unit> =
        Result.failure(
            UnsupportedOperationException("createFacility requires SupabasePharmaRepository."),
        )

    override suspend fun adminGetAuditLogs(limit: Int): Result<List<AuditLog>> =
        Result.success(sampleAuditLogs().take(limit))

    override suspend fun getAuditLogById(logId: String): Result<AuditLog> {
        val log = sampleAuditLogs().firstOrNull { it.id == logId }
        return if (log != null) Result.success(log)
        else Result.failure(IllegalArgumentException("Audit log not found"))
    }

    override suspend fun getWarehouseInventory(warehouseId: String): Result<List<com.pharmalink.domain.model.InventoryItem>> =
        Result.success(emptyList()) // InMemory implementation returns empty list

    override suspend fun adminGetDashboardStats(): Result<com.pharmalink.domain.model.AdminDashboardStats> =
        Result.success(
            com.pharmalink.domain.model.AdminDashboardStats(
                totalUsers = 15,
                totalPharmacies = 8,
                totalWarehouses = 5,
                totalOrders = 42,
                b2cOrdersCount = 25,
                b2bOrdersCount = 17,
                urgentOrdersCount = 8,
                pendingOrdersCount = 12,
                confirmedOrdersCount = 18,
                deliveredOrdersCount = 10,
                activePharmacies = 7,
                activeWarehouses = 4,
            )
        )

    override suspend fun adminGetAllOrders(
        orderType: String?,
        status: String?,
        isUrgent: Boolean?,
        search: String?,
        limit: Int,
        offset: Int
    ): Result<List<com.pharmalink.domain.model.AdminOrder>> =
        Result.success(emptyList()) // InMemory implementation returns empty list

    override suspend fun adminGetOrderDetail(orderId: String): Result<com.pharmalink.domain.model.AdminOrder?> =
        Result.success(null) // InMemory implementation returns null

    override suspend fun adminGetPendingRequests(limit: Int): Result<List<com.pharmalink.domain.model.PendingRequest>> =
        Result.success(emptyList()) // InMemory implementation returns empty list

    override suspend fun adminGetRecentActivities(limit: Int): Result<List<com.pharmalink.domain.model.RecentActivity>> =
        Result.success(emptyList()) // InMemory implementation returns empty list

    override suspend fun adminGetSystemHealth(): Result<com.pharmalink.domain.model.SystemHealth> =
        Result.success(
            com.pharmalink.domain.model.SystemHealth(
                healthPercent = 95,
                healthStatus = "ممتاز",
                // activeConnections removed
            )
        ) // InMemory implementation returns default health

    // ==================== Cross-Account Completeness (Phase 2-5) — stubs ====================

    override fun observeMyCustomerOrders(): kotlinx.coroutines.flow.Flow<List<com.pharmalink.domain.model.Order>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun adminGetAllRequests(
        status: String?,
        pharmacyId: String?,
        warehouseId: String?,
        search: String?,
        limit: Int,
        offset: Int,
    ): Result<List<com.pharmalink.domain.model.AdminRequest>> = Result.success(emptyList())

    override suspend fun adminGetRequestDetail(
        requestId: String,
    ): Result<kotlinx.serialization.json.JsonElement> =
        Result.failure(UnsupportedOperationException("adminGetRequestDetail is not supported in InMemoryPharmaRepository."))

    override suspend fun adminForceCancelOrder(orderId: String, reason: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("adminForceCancelOrder is not supported in InMemoryPharmaRepository."))

    override suspend fun adminForceCancelRequest(requestId: String, reason: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("adminForceCancelRequest is not supported in InMemoryPharmaRepository."))

    override suspend fun adminProvisionPharmacyWithOwner(
        name: String,
        location: String,
        contactNumber: String,
        licenseNumber: String,
        ownerUserId: String,
    ): Result<kotlinx.serialization.json.JsonElement> =
        Result.failure(UnsupportedOperationException("adminProvisionPharmacyWithOwner is not supported in InMemoryPharmaRepository."))

    override suspend fun adminProvisionWarehouseWithOwner(
        name: String,
        location: String,
        contactNumber: String,
        ownerUserId: String,
    ): Result<kotlinx.serialization.json.JsonElement> =
        Result.failure(UnsupportedOperationException("adminProvisionWarehouseWithOwner is not supported in InMemoryPharmaRepository."))

    private fun updateCustomerOrder(
        orderId: String,
        transform: (Order) -> Order,
    ): Result<Unit> = runCatching {
        val index = orders.value.indexOfFirst {
            it.id == orderId && it.orderType == OrderType.CUSTOMER_PHARMACY
        }
        require(index >= 0) { "Customer order not found" }
        val mutable = orders.value.toMutableList()
        mutable[index] = transform(mutable[index])
        orders.value = mutable
    }

    private fun Order.toPharmacyCustomerOrder(): PharmacyCustomerOrder =
        PharmacyCustomerOrder(
            id = id,
            customerId = customerId,
            customerName = null,
            medicineId = medicineId,
            medicineName = medicineName,
            quantity = quantity,
            unit = unit,
            status = status,
            fulfillmentType = fulfillmentType,
            deliveryAddress = deliveryAddress,
            deliveryPhone = deliveryPhone,
            notes = notes,
            totalPriceCents = totalPriceCents,
            currency = currency,
            urgency = urgency,
            requestScope = requestScope,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun sampleAuditLogs(): List<AuditLog> = listOf(
        AuditLog(
            id = "550e8400-e29b-41d4-a716-446655440000",
            action = "STOCK_UPDATE",
            actionLabel = "تعديل بيانات المخزون",
            adminId = "admin-1",
            adminName = "د. أحمد خالد",
            adminEmail = "ahmed@example.com",
            targetEntityName = "أوجمنتين 1 جم",
            targetWarehouseName = "مستودع الرياض المركزي",
            targetSku = "PH-99203",
            oldValue = """{"quantity": "1000", "price": "80.00"}""",
            newValue = """{"quantity": "1200", "price": "82.50"}""",
            ipAddress = "192.168.1.104",
            userAgent = "Chrome v118 (Windows 11)",
            transactionId = "TRX-7729-AX",
            createdAt = Instant.parse("2023-10-12T07:45:00Z"),
            isSuccess = true,
        ),
    )

    private fun buildDeliveryTrackingForDemo(order: Order): DeliveryTracking {
        val oid = order.id
        return when {
            oid.contains("1") || order.status == OrderStatus.PENDING -> DeliveryTracking(
                orderId = oid,
                delegate = DeliveryDelegate(name = "أحمد محمد", phone = "+966501234567", isActive = true),
                startPoint = order.warehouseName ?: "",
                destinationPoint = "صيدلية فارمالينك",
                currentStatus = DeliveryStatus.IN_TRANSIT,
                departureTime = "2:30 م",
                lastUpdate = "10 دقائق مضت",
                orderNumber = oid,
                estimatedArrival = "25 دقيقة",
                deliveryNotes = "التسليم عند الباب الخلفي",
                startLatitude = 24.7136,
                startLongitude = 46.6753,
                destinationLatitude = 24.6877,
                destinationLongitude = 46.7212,
                driverCurrentLatitude = 24.7000,
                driverCurrentLongitude = 46.6980,
                routePolyline = null,
                lastLocationTimestamp = System.currentTimeMillis(),
            )
            oid.contains("2") || order.status == OrderStatus.CONFIRMED -> DeliveryTracking(
                orderId = oid,
                delegate = null,
                startPoint = order.warehouseName ?: "",
                destinationPoint = "صيدلية العميل",
                currentStatus = DeliveryStatus.PREPARING,
                departureTime = null,
                lastUpdate = "5 دقائق مضت",
                orderNumber = oid,
                estimatedArrival = "2-3 ساعات",
                deliveryNotes = null,
                startLatitude = null,
                startLongitude = null,
                destinationLatitude = null,
                destinationLongitude = null,
                driverCurrentLatitude = null,
                driverCurrentLongitude = null,
                routePolyline = null,
                lastLocationTimestamp = null,
            )
            else -> DeliveryTracking(
                orderId = oid,
                delegate = DeliveryDelegate(name = "محمد خالد", phone = "+966557654321", isActive = true),
                startPoint = order.warehouseName ?: "",
                destinationPoint = "صيدلية الشفاء",
                currentStatus = DeliveryStatus.DELIVERED,
                departureTime = "10:00 ص",
                lastUpdate = "ساعتان مضت",
                orderNumber = oid,
                estimatedArrival = "تم التسليم",
                deliveryNotes = null,
                startLatitude = 26.4295,
                startLongitude = 50.0878,
                destinationLatitude = 26.4200,
                destinationLongitude = 50.0900,
                driverCurrentLatitude = null,
                driverCurrentLongitude = null,
                routePolyline = null,
                lastLocationTimestamp = null,
            )
        }
    }

    private fun Request.toLegacyRequestItem(requestId: String, lineNo: Int): RequestItem =
        RequestItem(
            requestId = requestId,
            lineNo = lineNo,
            medicineId = requireNotNull(medicineId?.takeIf { it.isNotBlank() }) {
                "medicineId is required for B2B pharmacy requests"
            },
            medicineName = medicineName,
            medicineSubtitle = medicineSubtitle,
            quantity = quantity,
            unit = unit,
            createdAt = createdAtLabel,
            updatedAt = updatedAtLabel,
        )

    private fun sampleMedicines(): List<Medicine> = listOf(
        Medicine("m1", "باراسيتامول", "جنريك", "500 ملغ", 12.0, 0, null, 12.0, "w1"),
        Medicine("m2", "أموكسيسيلين", "سبيكترا", "شراب", 45.0, 0, null, 45.0, "w1"),
        Medicine("m3", "فيتامين د3", "نيوتري", "1000 وحدة", 28.5, 0, null, 28.5, "w2"),
    )

    private fun sampleWarehouses(): List<Warehouse> = listOf(
        Warehouse("w1", "مستودع الشفاء", "الرياض", "العليا", true, 92, 4, 1, "خلال ساعتين", "4 كم", "+966500001111", "قبل 12 دقيقة"),
        Warehouse("w2", "مستودع النور", "جدة", "الروضة", true, 88, 6, 2, "خلال 3 ساعات", "7 كم", "+966500001112", "قبل 35 دقيقة"),
        Warehouse("w3", "مستودع الأمل", "الدمام", "الفيصلية", false, 95, 2, 0, "خلال 5 ساعات", "12 كم", "+966500001113", "قبل ساعة"),
        Warehouse("w4", "حياة للإمداد", "مكة", "العزيزية", true, 81, 9, 3, "نفس اليوم", "18 كم", "+966500001114", "قبل ساعتين"),
    )

    private fun sampleRequests(): List<Request> = listOf(
        Request(
            id = "REQ-1284",
            pharmacyId = "pharmacy_1",
            medicineName = "باراسيتامول 500 ملغ",
            medicineSubtitle = "",
            quantity = 2,
            unit = "علبة",
            notes = "نحتاج مخزون الأطفال قبل الفترة المسائية.",
            storageNotes = "",
            priority = RequestPriority.URGENT,
            status = RequestStatus.PENDING,
            warehouseId = "w1",
            warehouseName = "مستودع الشفاء",
            supplierName = "مستودع الشفاء",
            createdAtLabel = "اليوم 09:10",
            updatedAtLabel = "تمت المراجعة قبل 10 دقائق",
            relatedOrderId = "ORD-881",
        ),
        Request(
            id = "REQ-1283",
            pharmacyId = "pharmacy_1",
            medicineName = "أموكسيسيلين شراب",
            medicineSubtitle = "",
            quantity = 1,
            unit = "زجاجة",
            notes = "إعادة تعبئة اعتيادية لنهاية الأسبوع.",
            storageNotes = "",
            priority = RequestPriority.NORMAL,
            status = RequestStatus.ACCEPTED,
            warehouseId = "w2",
            warehouseName = "مستودع النور",
            supplierName = "مستودع النور",
            createdAtLabel = "اليوم 08:30",
            updatedAtLabel = "تمت الموافقة قبل 25 دقيقة",
            relatedOrderId = "ORD-880",
        ),
        Request(
            id = "REQ-1282",
            pharmacyId = "pharmacy_1",
            medicineName = "فيتامين د3",
            medicineSubtitle = "",
            quantity = 3,
            unit = "عبوة",
            notes = "مخزون وقائي شهري.",
            storageNotes = "",
            priority = RequestPriority.NORMAL,
            status = RequestStatus.FULFILLED,
            warehouseId = "w3",
            warehouseName = "مستودع الأمل",
            supplierName = "مستودع الأمل",
            createdAtLabel = "أمس",
            updatedAtLabel = "تم التسليم",
            relatedOrderId = "ORD-879",
        ),
    )

    private fun sampleOrders(): List<Order> {
        val now = java.time.Instant.now()
        val yesterday = now.minus(java.time.Duration.ofDays(1))
        return listOf(
            Order(
                id = "ORD-881",
                medicineId = "med_001",
                medicineName = "باراسيتامول 500 ملغ",
                quantity = 2,
                unit = "علبة",
                status = OrderStatus.PENDING,
                orderType = OrderType.PHARMACY_WAREHOUSE,
                fulfillmentType = FulfillmentType.DELIVERY,
                pharmacyId = "pharmacy_1",
                warehouseId = "w1",
                customerId = null,
                requestId = "REQ-1284",
                totalPriceCents = null,
                currency = "SAR",
                deliveryAddress = null,
                deliveryPhone = null,
                notes = null,
                createdAt = now,
                updatedAt = now,
                confirmedAt = null,
                fulfilledAt = null,
                warehouseName = "مستودع الشفاء",
                supplierName = "مستودع الشفاء",
                etaLabel = "خلال ساعتين",
                isUrgent = true,
            ),
            Order(
                id = "ORD-880",
                medicineId = "med_002",
                medicineName = "أموكسيسيلين شراب",
                quantity = 1,
                unit = "زجاجة",
                status = OrderStatus.CONFIRMED,
                orderType = OrderType.PHARMACY_WAREHOUSE,
                fulfillmentType = FulfillmentType.DELIVERY,
                pharmacyId = "pharmacy_1",
                warehouseId = "w2",
                customerId = null,
                requestId = "REQ-1283",
                totalPriceCents = 4500,
                currency = "SAR",
                deliveryAddress = null,
                deliveryPhone = null,
                notes = null,
                createdAt = now,
                updatedAt = now,
                confirmedAt = now,
                fulfilledAt = null,
                warehouseName = "مستودع النور",
                supplierName = "مستودع النور",
                etaLabel = "خلال 3 ساعات",
                isUrgent = false,
            ),
            Order(
                id = "ORD-879",
                medicineId = "med_003",
                medicineName = "فيتامين د3",
                quantity = 3,
                unit = "عبوة",
                status = OrderStatus.DELIVERED,
                orderType = OrderType.PHARMACY_WAREHOUSE,
                fulfillmentType = FulfillmentType.DELIVERY,
                pharmacyId = "pharmacy_1",
                warehouseId = "w3",
                customerId = null,
                requestId = "REQ-1282",
                totalPriceCents = 2850,
                currency = "SAR",
                deliveryAddress = null,
                deliveryPhone = null,
                notes = null,
                createdAt = yesterday,
                updatedAt = yesterday,
                confirmedAt = yesterday,
                fulfilledAt = yesterday,
                warehouseName = "مستودع الأمل",
                supplierName = "مستودع الأمل",
                etaLabel = null,
                isUrgent = false,
            ),
            Order(
                id = "ORD-878",
                medicineId = "med_004",
                medicineName = "مكمل الزنك",
                quantity = 1,
                unit = "عبوة",
                status = OrderStatus.REJECTED,
                orderType = OrderType.PHARMACY_WAREHOUSE,
                fulfillmentType = FulfillmentType.DELIVERY,
                pharmacyId = "pharmacy_1",
                warehouseId = "w4",
                customerId = null,
                requestId = "REQ-1281",
                totalPriceCents = null,
                currency = "SAR",
                deliveryAddress = null,
                deliveryPhone = null,
                notes = "غير متوفر في هذه الدورة",
                createdAt = yesterday,
                updatedAt = yesterday,
                confirmedAt = null,
                fulfilledAt = null,
                warehouseName = "حياة للإمداد",
                supplierName = "حياة للإمداد",
                etaLabel = null,
                isUrgent = false,
            ),
        )
    }

    private fun sampleNotifications(): List<AppNotification> = listOf(
        AppNotification(
            id = "n1",
            title = "طلب عاجل قيد المراجعة",
            body = "الطلب REQ-1284 ينتظر تأكيد المورد.",
            type = NotificationType.ALERT,
            category = NotificationCategory.REQUESTS,
            createdAtLabel = "قبل 10 دقائق",
            read = false,
            requiresAction = true,
            destination = NotificationDestination.REQUEST,
            destinationId = "REQ-1284",
        ),
        AppNotification(
            id = "n2",
            title = "تحديث مخزون المستودع",
            body = "تم تحديث مخزون مستودع الشفاء ويمكن مراجعة التوفر الآن.",
            type = NotificationType.INFO,
            category = NotificationCategory.WAREHOUSES,
            createdAtLabel = "قبل 35 دقيقة",
            read = false,
            destination = NotificationDestination.WAREHOUSE,
            destinationId = "w1",
        ),
        AppNotification(
            id = "n3",
            title = "تم تسليم طلبية",
            body = "تم وضع الطلب ORD-879 في حالة تم التسليم.",
            type = NotificationType.ORDER_UPDATE,
            category = NotificationCategory.ORDERS,
            createdAtLabel = "أمس",
            read = true,
            destination = NotificationDestination.ORDER,
            destinationId = "ORD-879",
        ),
        AppNotification(
            id = "n4",
            title = "تنبيه امتثال",
            body = "تصريح مزاولة النشاط يحتاج مراجعة قبل نهاية الشهر.",
            type = NotificationType.COMPLIANCE,
            category = NotificationCategory.COMPLIANCE,
            createdAtLabel = "اليوم",
            read = false,
            requiresAction = true,
            destination = NotificationDestination.COMPLIANCE,
        ),
        AppNotification(
            id = "n5",
            title = "رسالة من الدعم",
            body = "تم تحديث دليل معالجة الطلبات المبردة داخل مركز المساعدة.",
            type = NotificationType.SUPPORT,
            category = NotificationCategory.SUPPORT,
            createdAtLabel = "قبل ساعتين",
            read = false,
            destination = NotificationDestination.HELP,
        ),
    )

    private fun sampleProfile(): PharmacyProfile = PharmacyProfile(
        id = "ph-1",
        pharmacyName = "صيدلية فارمالينك",
        city = "الرياض",
        district = "حي النزهة",
        managerName = "د. أحمد الزهراني",
        addressLine = "الرياض، حي النزهة، شارع الأمير سلطان",
        contactPhone = "+966501234567",
        contactEmail = "operations@pharmalink.sa",
        licenseStatusLabel = "سارية",
        licenseNumber = "PH-20458",
        licenseExpiryLabel = "31 ديسمبر 2026",
        operatingHoursLabel = "يوميًا من 8 ص إلى 12 ص",
        preferredLanguageLabel = "العربية",
        notificationsEnabled = true,
        twoFactorEnabled = true,
        linkedDevicesCount = 3,
        totalOrders = 128,
        completedOrders = 96,
        activeOrders = 12,
    )

    private fun sampleCompliance(): ComplianceOverview = ComplianceOverview(
        pharmacyId = "ph-1",
        licenseStatusLabel = "مستوفى",
        licenseNumber = "PH-20458",
        licenseExpiryLabel = "31 ديسمبر 2026",
        summaryLabel = "جميع الوثائق الأساسية محدثة مع تنبيه واحد يحتاج مراجعة.",
        alerts = listOf(
            "تنتهي صلاحية شهادة التخزين البارد خلال 18 يومًا.",
            "مراجعة اعتماد المورد حياة للإمداد مطلوبة هذا الأسبوع.",
        ),
        documents = listOf(
            ComplianceDocument(
                id = "doc-1",
                title = "رخصة مزاولة الصيدلية",
                status = ComplianceDocumentStatus.VALID,
                statusLabel = "سارية",
                expiryLabel = "31 ديسمبر 2026",
                note = "آخر تحديث تم قبل 20 يومًا.",
            ),
            ComplianceDocument(
                id = "doc-2",
                title = "شهادة التخزين البارد",
                status = ComplianceDocumentStatus.EXPIRING_SOON,
                statusLabel = "تنتهي قريبًا",
                expiryLabel = "18 أبريل 2026",
                note = "TODO(backend): ربط رفع الشهادة الجديدة من لوحة الوثائق.",
            ),
            ComplianceDocument(
                id = "doc-3",
                title = "اعتماد مورد اللقاحات",
                status = ComplianceDocumentStatus.MISSING,
                statusLabel = "بحاجة لإرفاق",
                expiryLabel = "غير مرفوع",
                note = "TODO(backend): تمكين الإرسال والمراجعة من الجهات المعتمدة.",
            ),
        ),
        supplierItems = listOf(
            SupplierComplianceItem(
                id = "supplier-1",
                supplierName = "مستودع الشفاء",
                statusLabel = "مطابق",
                nextReviewLabel = "مراجعة قادمة في يونيو 2026",
                note = "لا توجد ملاحظات حالية.",
            ),
            SupplierComplianceItem(
                id = "supplier-2",
                supplierName = "حياة للإمداد",
                statusLabel = "بحاجة متابعة",
                nextReviewLabel = "المراجعة مطلوبة هذا الأسبوع",
                note = "تأكيد التراخيص اللوجستية ما زال قيد التدقيق.",
            ),
        ),
    )

    private fun sampleShipments(): Map<String, List<WarehouseShipment>> = mapOf(
        "w1" to listOf(
            WarehouseShipment("s1", "w1", "إعادة تزويد أدوية الأمراض المزمنة", "يصل خلال 4 ساعات", "في الطريق", 24),
            WarehouseShipment("s2", "w1", "شحنة لقاحات مبردة", "تصل غدًا", "مجدولة", 8),
        ),
        "w2" to listOf(WarehouseShipment("s3", "w2", "دفعة نهاية الأسبوع", "تصل خلال 6 ساعات", "قيد التجهيز", 16)),
        "w3" to listOf(WarehouseShipment("s4", "w3", "إعادة تعبئة منتجات OTC", "تصل الليلة", "في الطريق", 12)),
        "w4" to emptyList(),
    )
}
