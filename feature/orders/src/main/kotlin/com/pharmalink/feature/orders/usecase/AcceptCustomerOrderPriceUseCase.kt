package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.OrderType
import javax.inject.Inject

/**
 * UseCase for accepting the pharmacy's confirmed price for a B2C customer order.
 *
 * Business Rules:
 * - Only PUBLIC_USER can accept prices for their own orders
 * - Only CONFIRMED orders can be accepted (pharmacy has set price)
 * - Must be the order owner (customerId matches)
 * - After acceptance, order moves to next fulfillment stage
 */
class AcceptCustomerOrderPriceUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(
        order: Order,
        accountType: AccountType,
        customerId: String,
    ): Result<Unit> {
        // Rule 1: Only PUBLIC_USER can accept
        if (accountType != AccountType.PUBLIC_USER) {
            return Result.failure(
                SecurityException("Only public users can accept order prices")
            )
        }

        // Rule 2: Must be order owner
        if (order.customerId != customerId) {
            return Result.failure(
                SecurityException("Cannot accept price for order that doesn't belong to you")
            )
        }

        if (order.orderType != OrderType.CUSTOMER_PHARMACY) {
            return Result.failure(
                IllegalStateException("Can only accept prices for CUSTOMER_PHARMACY orders")
            )
        }

        // Rule 3: Only CONFIRMED orders can be accepted
        if (order.status != OrderStatus.CONFIRMED) {
            return Result.failure(
                IllegalStateException("Can only accept price for confirmed orders. Current status: ${order.status}")
            )
        }

        // Rule 4: Price must be set
        if (order.totalPriceCents == null) {
            return Result.failure(
                IllegalStateException("Cannot accept order without confirmed price")
            )
        }

        // Accept via repository
        return pharmaRepository.acceptCustomerOrderPrice(order.id)
    }
}
