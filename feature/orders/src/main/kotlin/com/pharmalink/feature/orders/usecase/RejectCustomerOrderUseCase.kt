package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import javax.inject.Inject

/**
 * UseCase for rejecting a B2C customer order (pharmacy action).
 *
 * Business Rules:
 * - Only PHARMACY can reject orders
 * - Must own the pharmacy that received the order
 * - Only PENDING orders can be rejected
 */
class RejectCustomerOrderUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(
        order: Order,
        accountType: AccountType,
        pharmacyId: String,
    ): Result<Order> {
        // Rule 1: Only PHARMACY can reject
        if (accountType != AccountType.PHARMACY) {
            return Result.failure(
                SecurityException("Only pharmacies can reject orders")
            )
        }

        // Rule 2: Must own the pharmacy
        if (order.pharmacyId != pharmacyId) {
            return Result.failure(
                SecurityException("Cannot reject order for another pharmacy")
            )
        }

        // Rule 3: Only PENDING orders can be rejected
        if (order.status != OrderStatus.PENDING) {
            return Result.failure(
                IllegalStateException("Can only reject pending orders. Current status: ${order.status}")
            )
        }

        // Reject via repository
        return pharmaRepository.rejectOrder(order.id)
    }
}
