package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.PharmacyCustomerOrder
import javax.inject.Inject

class GetPharmacyCustomerOrdersUseCase @Inject constructor(
    private val repository: PharmaRepository,
) {
    suspend operator fun invoke(): Result<List<PharmacyCustomerOrder>> =
        repository.getPharmacyCustomerOrders()
}
