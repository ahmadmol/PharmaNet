package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.OrderType
import javax.inject.Inject

/**
 * UseCase for marking a B2C order as ready for pickup (pharmacy action).
 *
 * Business Rules:
 * - Only PHARMACY can mark orders ready
 * - Must own the pharmacy
 * - Only PICKUP orders can be marked ready
 * - Only CONFIRMED orders can transition to READY_FOR_PICKUP
 */
class MarkOrderReadyUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(
        order: Order,
        accountType: AccountType,
        pharmacyId: String,
    ): Result<Order> {
        // Rule 1: Only PHARMACY can mark ready
        if (accountType != AccountType.PHARMACY) {
            return Result.failure(
                SecurityException("Only pharmacies can mark orders ready")
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

        // Rule 3: Only PICKUP orders
        if (order.fulfillmentType != FulfillmentType.PICKUP) {
            return Result.failure(
                IllegalStateException("Can only mark PICKUP orders as ready. Current type: ${order.fulfillmentType}")
            )
        }

        // Rule 4: Valid status transition (CONFIRMED -> READY_FOR_PICKUP)
        if (order.status != OrderStatus.CONFIRMED) {
            return Result.failure(
                IllegalStateException("Can only mark CONFIRMED orders as ready. Current status: ${order.status}")
            )
        }

        // Mark ready via repository
        return pharmaRepository.markOrderReadyForPickup(order.id)
    }
}
