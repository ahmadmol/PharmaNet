package com.pharmalink.feature.orders.usecase

import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.CustomerRequestScope
import com.pharmalink.domain.model.CustomerRequestUrgency
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.domain.model.Order
import javax.inject.Inject

/**
 * UseCase for creating a B2C customer order.
 *
 * Business Rules:
 * - Only PUBLIC_USER can create customer orders
 * - DELIVERY requires deliveryAddress and deliveryPhone
 * - Order starts with PENDING status and null totalPriceCents
 * - medicineId is catalog-based (no inventory validation in this phase)
 */
class CreateCustomerOrderUseCase @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) {
    suspend operator fun invoke(
        medicineId: String,
        medicineName: String,
        quantity: Int,
        unit: String,
        pharmacyId: String?,
        urgency: CustomerRequestUrgency,
        requestScope: CustomerRequestScope,
        fulfillmentType: FulfillmentType,
        deliveryAddress: String?,
        deliveryPhone: String?,
        notes: String?,
        accountType: AccountType,
    ): Result<Order> {
        // Rule 1: Only PUBLIC_USER can create customer orders
        if (accountType != AccountType.PUBLIC_USER) {
            return Result.failure(
                SecurityException("Only public users can create customer orders")
            )
        }

        // Rule 2: Validate quantity
        if (quantity <= 0) {
            return Result.failure(
                IllegalArgumentException("Quantity must be greater than 0")
            )
        }

        if (medicineId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("medicineId must not be blank")
            )
        }

        if (requestScope == CustomerRequestScope.SPECIFIC_PHARMACY && pharmacyId.isNullOrBlank()) {
            return Result.failure(
                IllegalArgumentException("pharmacyId must not be blank for specific pharmacy orders")
            )
        }

        // Rule 3: DELIVERY requires address and phone
        if (fulfillmentType == FulfillmentType.DELIVERY) {
            if (deliveryAddress.isNullOrBlank()) {
                return Result.failure(
                    IllegalArgumentException("Delivery address is required for delivery orders")
                )
            }
            if (deliveryPhone.isNullOrBlank()) {
                return Result.failure(
                    IllegalArgumentException("Delivery phone is required for delivery orders")
                )
            }
        }

        // Rule 4: PICKUP should not have delivery fields
        if (fulfillmentType == FulfillmentType.PICKUP) {
            if (!deliveryAddress.isNullOrBlank() || !deliveryPhone.isNullOrBlank()) {
                // This is a warning but not a failure - we'll let the repository handle it
            }
        }

        // TODO: replace with catalog-backed medicine existence validation.

        // Create order via repository
        return pharmaRepository.createCustomerOrder(
            medicineId = medicineId,
            medicineName = medicineName,
            quantity = quantity,
            unit = unit,
            pharmacyId = pharmacyId,
            urgency = urgency,
            requestScope = requestScope,
            fulfillmentType = fulfillmentType,
            deliveryAddress = deliveryAddress,
            deliveryPhone = deliveryPhone,
            notes = notes,
        )
    }
}
