package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.PublicPharmacyForMedicine
import javax.inject.Inject

class GetPublicPharmaciesForMedicineUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(medicineId: String): Result<List<PublicPharmacyForMedicine>> {
        if (medicineId.isBlank()) {
            return Result.failure(IllegalArgumentException("medicineId must not be blank"))
        }
        return pharmaRepository.getPublicPharmaciesForMedicine(medicineId)
    }
}
