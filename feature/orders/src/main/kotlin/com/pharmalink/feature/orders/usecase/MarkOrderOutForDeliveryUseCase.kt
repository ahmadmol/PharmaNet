package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.OrderType
import javax.inject.Inject

/**
 * UseCase for marking a B2C order as out for delivery (pharmacy action).
 *
 * Business Rules:
 * - Only PHARMACY can mark orders out for delivery
 * - Must own the pharmacy
 * - Only DELIVERY orders can be marked out for delivery
 * - Only CONFIRMED orders can transition to OUT_FOR_DELIVERY
 */
class MarkOrderOutForDeliveryUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(
        order: Order,
        accountType: AccountType,
        pharmacyId: String,
    ): Result<Order> {
        // Rule 1: Only PHARMACY can mark out for delivery
        if (accountType != AccountType.PHARMACY) {
            return Result.failure(
                SecurityException("Only pharmacies can mark orders out for delivery")
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

        // Rule 3: Only DELIVERY orders
        if (order.fulfillmentType != FulfillmentType.DELIVERY) {
            return Result.failure(
                IllegalStateException("Can only mark DELIVERY orders as out for delivery. Current type: ${order.fulfillmentType}")
            )
        }

        // Rule 4: Valid status transition (CONFIRMED -> OUT_FOR_DELIVERY)
        if (order.status != OrderStatus.CONFIRMED) {
            return Result.failure(
                IllegalStateException("Can only mark CONFIRMED orders as out for delivery. Current status: ${order.status}")
            )
        }

        // Mark out for delivery via repository
        return pharmaRepository.markOrderOutForDelivery(order.id)
    }
}
