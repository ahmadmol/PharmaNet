package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.OrderType
import javax.inject.Inject

/**
 * UseCase for canceling a B2C customer order.
 *
 * Business Rules:
 * - Only PUBLIC_USER can cancel their own orders
 * - Only PENDING orders can be cancelled
 * - Must be the order owner (customerId matches)
 */
class CancelCustomerOrderUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(
        order: Order,
        accountType: AccountType,
        customerId: String,
    ): Result<Unit> {
        // Rule 1: Only PUBLIC_USER can cancel
        if (accountType != AccountType.PUBLIC_USER) {
            return Result.failure(
                SecurityException("Only public users can cancel their orders")
            )
        }

        // Rule 2: Must be order owner
        if (order.customerId != customerId) {
            return Result.failure(
                SecurityException("Cannot cancel order that doesn't belong to you")
            )
        }

        if (order.orderType != OrderType.CUSTOMER_PHARMACY) {
            return Result.failure(
                IllegalStateException("Can only cancel CUSTOMER_PHARMACY orders")
            )
        }

        // Rule 3: Only PENDING orders can be cancelled
        if (order.status != OrderStatus.PENDING) {
            return Result.failure(
                IllegalStateException("Can only cancel pending orders. Current status: ${order.status}")
            )
        }

        // Cancel via repository
        return pharmaRepository.cancelCustomerOrder(order.id)
    }
}
