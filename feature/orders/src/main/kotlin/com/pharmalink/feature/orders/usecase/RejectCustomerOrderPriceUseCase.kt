package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.OrderType
import javax.inject.Inject

/**
 * UseCase for rejecting the pharmacy's confirmed price for a B2C customer order.
 *
 * Business Rules:
 * - Only PUBLIC_USER can reject prices for their own orders
 * - Only CONFIRMED orders can be rejected (pharmacy has set price)
 * - Must be the order owner (customerId matches)
 * - After rejection, order moves to REJECTED status
 */
class RejectCustomerOrderPriceUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(
        order: Order,
        accountType: AccountType,
        customerId: String,
    ): Result<Unit> {
        // Rule 1: Only PUBLIC_USER can reject
        if (accountType != AccountType.PUBLIC_USER) {
            return Result.failure(
                SecurityException("Only public users can reject order prices")
            )
        }

        // Rule 2: Must be order owner
        if (order.customerId != customerId) {
            return Result.failure(
                SecurityException("Cannot reject price for order that doesn't belong to you")
            )
        }

        if (order.orderType != OrderType.CUSTOMER_PHARMACY) {
            return Result.failure(
                IllegalStateException("Can only reject prices for CUSTOMER_PHARMACY orders")
            )
        }

        // Rule 3: Only CONFIRMED orders can be rejected
        if (order.status != OrderStatus.CONFIRMED) {
            return Result.failure(
                IllegalStateException("Can only reject price for confirmed orders. Current status: ${order.status}")
            )
        }

        // Reject via repository
        return pharmaRepository.rejectCustomerOrderPrice(order.id)
    }
}
