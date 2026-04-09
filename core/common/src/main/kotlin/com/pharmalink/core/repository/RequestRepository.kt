package com.pharmalink.core.repository

import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestStatus
import kotlinx.coroutines.flow.Flow

/**
 * Request Repository Interface
 * Handles request management operations
 */
interface RequestRepository {
    suspend fun getRequests(): Flow<List<Request>>
    suspend fun getRequestsByStatus(status: RequestStatus): Flow<List<Request>>
    suspend fun getRequestById(requestId: String): Request?
    suspend fun createRequest(request: Request): Result<Request>
    suspend fun updateRequest(request: Request): Result<Request>
    suspend fun deleteRequest(requestId: String): Result<Unit>
    suspend fun submitRequest(requestId: String): Result<Unit>
}
