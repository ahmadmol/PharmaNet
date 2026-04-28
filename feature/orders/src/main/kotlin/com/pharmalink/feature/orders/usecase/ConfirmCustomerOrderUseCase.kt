package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import javax.inject.Inject

/**
 * UseCase for confirming a B2C customer order (pharmacy action).
 *
 * Business Rules:
 * - Only PHARMACY can confirm orders
 * - Must own the pharmacy that received the order
 * - Only PENDING orders can be confirmed
 * - totalPriceCents must be >= 0
 */
class ConfirmCustomerOrderUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(
        order: Order,
        totalPriceCents: Long,
        accountType: AccountType,
        pharmacyId: String,
    ): Result<Order> {
        // Rule 1: Only PHARMACY can confirm
        if (accountType != AccountType.PHARMACY) {
            return Result.failure(
                SecurityException("Only pharmacies can confirm orders")
            )
        }

        // Rule 2: Must own the pharmacy
        if (order.pharmacyId != pharmacyId) {
            return Result.failure(
                SecurityException("Cannot confirm order for another pharmacy")
            )
        }

        // Rule 3: Only PENDING orders can be confirmed
        if (order.status != OrderStatus.PENDING) {
            return Result.failure(
                IllegalStateException("Can only confirm pending orders. Current status: ${order.status}")
            )
        }

        // Rule 4: Price must be valid
        if (totalPriceCents < 0) {
            return Result.failure(
                IllegalArgumentException("Total price must be >= 0")
            )
        }

        // Confirm via repository
        return pharmaRepository.confirmOrder(order.id, totalPriceCents)
    }
}
