package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Order
import javax.inject.Inject

/**
 * UseCase for getting a customer's own orders.
 *
 * Business Rules:
 * - Only PUBLIC_USER can get their orders
 * - Must match the requesting customerId
 * - Returns only orders where customerId matches
 */
class GetMyOrdersUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(
        customerId: String,
        accountType: AccountType,
    ): Result<List<Order>> {
        // Rule 1: Only PUBLIC_USER can get their orders
        if (accountType != AccountType.PUBLIC_USER) {
            return Result.failure(
                SecurityException("Only public users can view their orders")
            )
        }

        // Get orders via repository (repository validates customerId ownership)
        return pharmaRepository.getMyOrders(customerId)
    }
}
