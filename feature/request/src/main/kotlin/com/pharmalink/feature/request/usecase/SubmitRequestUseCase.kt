package com.pharmalink.feature.request.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.model.RequestTransitions
import javax.inject.Inject

/**
 * UseCase for submitting a request (DRAFT -> PENDING).
 *
 * Business Rules:
 * - Only PHARMACY can submit requests
 * - Only the owning pharmacy can submit their request
 * - Status must transition from DRAFT to PENDING
 */
class SubmitRequestUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(
        request: Request,
        accountType: AccountType,
    ): Result<Unit> {
        // Rule 1: Only PHARMACY can submit
        if (accountType != AccountType.PHARMACY) {
            return Result.failure(
                SecurityException("Only pharmacies can submit requests")
            )
        }

        // Rule 2: Validate status transition DRAFT -> PENDING
        if (!RequestTransitions.canTransition(
                request.status,
                RequestStatus.PENDING,
                AccountType.PHARMACY,
            )
        ) {
            return Result.failure(
                IllegalStateException(
                    "Cannot transition from ${request.status} to ${RequestStatus.PENDING}"
                )
            )
        }

        // Rule 3: Submit via repository (repository handles ownership check)
        return pharmaRepository.submitRequest(request.id)
    }
}
