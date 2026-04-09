package com.pharmalink.core.repository

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.model.Warehouse
import com.pharmalink.domain.model.WarehouseFilter
import com.pharmalink.domain.model.WarehouseSort
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PharmaBackedOrderRepository @Inject constructor(
    private val pharma: PharmaRepository,
) : OrderRepository {
    override suspend fun getOrders(): Flow<List<Order>> = pharma.observeOrders()

    override suspend fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>> =
        pharma.observeOrders().map { list -> list.filter { it.status == status } }

    override suspend fun getOrderById(orderId: String): Order? = pharma.getOrder(orderId)

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> =
        pharma.updateOrderStatus(orderId, status)

    override suspend fun createOrder(order: Order): Result<Order> = pharma.createOrder(order)

    override suspend fun deleteOrder(orderId: String): Result<Unit> = pharma.deleteOrder(orderId)
}

@Singleton
class PharmaBackedRequestRepository @Inject constructor(
    private val pharma: PharmaRepository,
) : RequestRepository {
    override suspend fun getRequests(): Flow<List<Request>> = pharma.observeRequests()

    override suspend fun getRequestsByStatus(status: RequestStatus): Flow<List<Request>> =
        pharma.observeRequests().map { list -> list.filter { it.status == status } }

    override suspend fun getRequestById(requestId: String): Request? = pharma.getRequest(requestId)

    override suspend fun createRequest(request: Request): Result<Request> =
        runCatching { pharma.createRequest(request) }

    override suspend fun updateRequest(request: Request): Result<Request> = pharma.updateRequest(request)

    override suspend fun deleteRequest(requestId: String): Result<Unit> = pharma.deleteRequest(requestId)

    override suspend fun submitRequest(requestId: String): Result<Unit> = pharma.submitRequest(requestId)
}

@Singleton
class PharmaBackedNotificationRepository @Inject constructor(
    private val pharma: PharmaRepository,
) : NotificationRepository {
    override suspend fun getNotifications(): Flow<List<AppNotification>> = pharma.observeNotifications()

    override suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        pharma.markNotificationRead(notificationId)
    }

    override suspend fun markAllAsRead(): Result<Unit> = runCatching {
        pharma.markAllNotificationsRead()
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> =
        pharma.deleteNotification(notificationId)

    override suspend fun deleteAllNotifications(): Result<Unit> = pharma.deleteAllNotifications()

    override fun getUnreadCount(): Flow<Int> =
        pharma.observeNotifications().map { list -> list.count { !it.read } }
}

@Singleton
class PharmaBackedWarehouseRepository @Inject constructor(
    private val pharma: PharmaRepository,
) : WarehouseRepository {
    override suspend fun getWarehouses(): Flow<List<Warehouse>> = pharma.observeWarehouses()

    override suspend fun getWarehousesByFilter(filter: WarehouseFilter): Flow<List<Warehouse>> =
        pharma.observeWarehouses().map { applyWarehouseFilter(it, filter) }

    override suspend fun searchWarehouses(query: String): Flow<List<Warehouse>> =
        pharma.observeWarehouses().map { list ->
            if (query.isBlank()) list
            else list.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.city.contains(query, ignoreCase = true) ||
                    it.district.contains(query, ignoreCase = true)
            }
        }

    override suspend fun getWarehouseById(warehouseId: String): Warehouse? = pharma.getWarehouse(warehouseId)

    override suspend fun updateWarehouse(warehouse: Warehouse): Result<Warehouse> =
        Result.success(warehouse)

    override suspend fun getNearbyWarehouses(location: String): Flow<List<Warehouse>> =
        pharma.observeWarehouses().map { it.take(3) }
}

@Suppress("UNUSED_PARAMETER")
private fun applyWarehouseFilter(warehouses: List<Warehouse>, filter: WarehouseFilter): List<Warehouse> =
    when (filter) {
        WarehouseFilter.ALL -> warehouses
        WarehouseFilter.NEARBY -> warehouses.take(2)
        WarehouseFilter.COLD_CHAIN -> warehouses.filter { it.supportsColdChain }
        WarehouseFilter.AVAILABLE_NOW -> warehouses.filter { it.inStockPercent > 50 }
        WarehouseFilter.SUPPLY_CHAIN -> warehouses.filter { it.inStockPercent > 80 }
        WarehouseFilter.FAST_DELIVERY -> warehouses.filter { w ->
            w.distanceLabel.replace(" كم", "").toDoubleOrNull()?.let { it < 10.0 } == true
        }
    }

@Singleton
class PharmaBackedDeliveryRepository @Inject constructor(
    private val pharma: PharmaRepository,
) : DeliveryRepository {
    override suspend fun getDeliveryTracking(orderId: String): Result<com.pharmalink.domain.model.DeliveryTracking> =
        pharma.getDeliveryTracking(orderId)

    override suspend fun callDelegate(phoneNumber: String): Result<Unit> =
        pharma.recordDelegateCall(phoneNumber)
}
