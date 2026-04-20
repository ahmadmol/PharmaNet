package com.pharmalink.core.repository

import com.pharmalink.core.common.validation.SyrianPhone
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.AuthSessionState
import com.pharmalink.domain.model.LoginRequest
import com.pharmalink.domain.model.SignUpRequest
import com.pharmalink.domain.model.User
import com.pharmalink.domain.model.UserSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val authState = MutableStateFlow<AuthSessionState>(AuthSessionState.Unauthenticated)
    private val snapshotState = MutableStateFlow<UserSnapshot?>(null)

    override suspend fun login(request: LoginRequest): Result<User> {
        delay(800)

        val normalized = SyrianPhone.normalizeToE164Digits(request.phoneNumber)
        return if (normalized == "963912345678" && request.password == "password") {
            val user = User(
                id = "user123",
                fullName = "مستخدم تجريبي",
                pharmacyName = "صيدلية التجربة",
                phoneNumber = "+$normalized",
                isActive = true,
            )
            val snapshot = fakeSnapshot(user)
            authState.value = AuthSessionState.Authenticated(user)
            snapshotState.value = snapshot
            Result.success(user)
        } else {
            Result.failure(Exception("بيانات الاعتماد غير صحيحة"))
        }
    }

    override suspend fun signUp(request: SignUpRequest): Result<User> {
        delay(1000)

        val normalized = SyrianPhone.normalizeToE164Digits(request.phoneNumber)
        return if (normalized != null && request.password.isNotEmpty()) {
            Result.success(
                User(
                    id = "user${System.currentTimeMillis()}",
                    fullName = request.fullName,
                    pharmacyName = if (request.accountType == AccountType.PHARMACY) request.pharmacyName else "",
                    phoneNumber = "+$normalized",
                    accountType = request.accountType,
                    pharmacyLocation = if (request.accountType == AccountType.PHARMACY) request.pharmacyLocation else "",
                    warehouseName = if (request.accountType == AccountType.WAREHOUSE) request.warehouseName else "",
                    warehouseLocation = if (request.accountType == AccountType.WAREHOUSE) request.warehouseLocation else "",
                    isActive = true,
                ),
            )
        } else {
            Result.failure(Exception("البيانات غير كاملة"))
        }
    }

    override suspend fun requestPasswordReset(identifier: String): Result<Unit> {
        delay(300)
        return if (identifier.isNotBlank()) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Identifier is required"))
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        delay(300)
        return if (currentPassword.isNotBlank() && newPassword.length >= 6) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Invalid password change request"))
        }
    }

    override suspend fun logout(): Result<Unit> {
        delay(300)
        authState.value = AuthSessionState.Unauthenticated
        snapshotState.value = null
        return Result.success(Unit)
    }

    override fun observeAuthState(): Flow<AuthSessionState> = authState.asStateFlow()

    override fun observeUserSnapshot(): Flow<UserSnapshot?> = snapshotState.asStateFlow()

    override suspend fun getUserSnapshot(): UserSnapshot? = snapshotState.value

    override suspend fun saveUserSnapshot(snapshot: UserSnapshot): Result<Unit> {
        snapshotState.value = snapshot
        return Result.success(Unit)
    }

    override suspend fun clearUserSnapshot(): Result<Unit> {
        snapshotState.value = null
        return Result.success(Unit)
    }

    override suspend fun ensureProfileForCurrentUser(user: User): Result<UserSnapshot> {
        val snapshot = fakeSnapshot(user)
        snapshotState.value = snapshot
        authState.value = AuthSessionState.Authenticated(user)
        return Result.success(snapshot)
    }

    private fun fakeSnapshot(user: User): UserSnapshot =
        UserSnapshot(
            userId = user.id,
            phoneNumber = user.phoneNumber,
            email = user.email,
            // Keep legacy carrier populated for compatibility during migration.
            pharmacyId = if (user.accountType == AccountType.WAREHOUSE) "warehouse123" else "pharmacy123",
            pharmacyName = if (user.accountType == AccountType.WAREHOUSE) user.warehouseName else user.pharmacyName,
            warehouseId = if (user.accountType == AccountType.WAREHOUSE) "warehouse123" else "",
            warehouseName = if (user.accountType == AccountType.WAREHOUSE) user.warehouseName else "",
            accountType = user.accountType,
            displayName = user.fullName,
        )
}
