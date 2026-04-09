package com.pharmalink.core.repository

import com.pharmalink.domain.model.DeliveryTracking
import kotlinx.coroutines.flow.Flow

interface DeliveryRepository {
    suspend fun getDeliveryTracking(orderId: String): Result<DeliveryTracking>
    suspend fun callDelegate(phoneNumber: String): Result<Unit>
}
