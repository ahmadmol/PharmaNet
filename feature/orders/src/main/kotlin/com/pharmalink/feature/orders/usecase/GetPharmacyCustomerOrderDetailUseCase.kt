package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.PharmacyCustomerOrder
import javax.inject.Inject

class GetPharmacyCustomerOrderDetailUseCase @Inject constructor(
    private val repository: PharmaRepository,
) {
    suspend operator fun invoke(orderId: String): Result<PharmacyCustomerOrder> =
        repository.getPharmacyCustomerOrderDetail(orderId)
}
