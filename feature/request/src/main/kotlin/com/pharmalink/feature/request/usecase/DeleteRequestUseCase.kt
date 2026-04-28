package com.pharmalink.feature.request.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestStatus
import javax.inject.Inject

/**
 * UseCase for deleting a request.
 *
 * Business Rules:
 * - Only PHARMACY can delete requests
 * - Only the owning pharmacy can delete their request
 * - Only DRAFT requests can be deleted
 */
class DeleteRequestUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(
        request: Request,
        accountType: AccountType,
    ): Result<Unit> {
        // Rule 1: Only PHARMACY can delete
        if (accountType != AccountType.PHARMACY) {
            return Result.failure(
                SecurityException("Only pharmacies can delete requests")
            )
        }

        // Rule 2: Only DRAFT requests can be deleted
        if (request.status != RequestStatus.DRAFT) {
            return Result.failure(
                IllegalStateException("Deleting requests is allowed only for DRAFT requests")
            )
        }

        // Rule 3: Delete via repository (repository handles ownership check)
        return pharmaRepository.deleteRequest(request.id)
    }
}
