package com.pharmalink.core.repository

import com.pharmalink.domain.model.LoginRequest
import com.pharmalink.domain.model.SignUpRequest
import com.pharmalink.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Auth Repository Interface
 * Handles authentication operations
 */
interface AuthRepository {
    suspend fun login(request: LoginRequest): Result<User>
    suspend fun signUp(request: SignUpRequest): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): Flow<User?>
    fun isLoggedIn(): Flow<Boolean>
}
