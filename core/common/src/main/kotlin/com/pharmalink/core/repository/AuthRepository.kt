package com.pharmalink.core.repository

import com.pharmalink.domain.model.AuthSessionState
import com.pharmalink.domain.model.LoginRequest
import com.pharmalink.domain.model.SignUpRequest
import com.pharmalink.domain.model.User
import com.pharmalink.domain.model.UserSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Auth Repository Interface
 * Handles authentication operations
 */
interface AuthRepository {
    suspend fun login(request: LoginRequest): Result<User>
    suspend fun signUp(request: SignUpRequest): Result<User>
    suspend fun requestPasswordReset(identifier: String): Result<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun observeAuthState(): Flow<AuthSessionState>
    fun observeUserSnapshot(): Flow<UserSnapshot?>
    suspend fun getUserSnapshot(): UserSnapshot?
    suspend fun saveUserSnapshot(snapshot: UserSnapshot): Result<Unit>
    suspend fun clearUserSnapshot(): Result<Unit>
    suspend fun ensureProfileForCurrentUser(user: User): Result<UserSnapshot>
    suspend fun bootstrapAuthenticatedUser(user: User): Result<UserSnapshot> =
        ensureProfileForCurrentUser(user)
}
