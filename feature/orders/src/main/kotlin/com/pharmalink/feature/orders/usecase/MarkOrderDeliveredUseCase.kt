package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.OrderType
import javax.inject.Inject

/**
 * UseCase for marking a B2C order as delivered (pharmacy action).
 *
 * Business Rules:
 * - Only PHARMACY can mark orders delivered
 * - Must own the pharmacy
 * - Only READY_FOR_PICKUP or OUT_FOR_DELIVERY orders can be marked DELIVERED
 */
class MarkOrderDeliveredUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(
        order: Order,
        accountType: AccountType,
        pharmacyId: String,
    ): Result<Order> {
        // Rule 1: Only PHARMACY can mark delivered
        if (accountType != AccountType.PHARMACY) {
            return Result.failure(
                SecurityException("Only pharmacies can mark orders as delivered")
            )
        }

        // Rule 2: Must own the pharmacy
        if (order.pharmacyId != pharmacyId) {
            return Result.failure(
                SecurityException("Cannot modify order for another pharmacy")
            )
        }

        if (order.orderType != OrderType.CUSTOMER_PHARMACY) {
            return Result.failure(
                IllegalStateException("Can only modify CUSTOMER_PHARMACY orders")
            )
        }

        // Rule 3: Valid status transition (READY_FOR_PICKUP or OUT_FOR_DELIVERY -> DELIVERED)
        if (order.status != OrderStatus.READY_FOR_PICKUP && order.status != OrderStatus.OUT_FOR_DELIVERY) {
            return Result.failure(
                IllegalStateException("Can only mark READY_FOR_PICKUP or OUT_FOR_DELIVERY orders as delivered. Current status: ${order.status}")
            )
        }

        // Mark delivered via repository
        return pharmaRepository.markOrderDelivered(order.id)
    }
}
