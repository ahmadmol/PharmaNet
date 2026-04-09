package com.pharmalink.core.repository

import com.pharmalink.core.common.validation.SyrianPhone
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.LoginRequest
import com.pharmalink.domain.model.SignUpRequest
import com.pharmalink.domain.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake Auth Repository Implementation
 * Temporary implementation for testing before backend integration
 */
@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {
    
    override suspend fun login(request: LoginRequest): Result<User> {
        delay(800) // Simulate network delay
        
        val normalized = SyrianPhone.normalizeToE164Digits(request.phoneNumber)
        return if (normalized == "963912345678" && request.password == "password") {
            Result.success(
                User(
                    id = "user123",
                    fullName = "مستخدم تجريبي",
                    pharmacyName = "صيدلية التجربة",
                    phoneNumber = "+${normalized}",
                    isActive = true
                )
            )
        } else {
            Result.failure(Exception("بيانات الاعتماد غير صحيحة"))
        }
    }
    
    override suspend fun signUp(request: SignUpRequest): Result<User> {
        delay(1000) // Simulate network delay
        
        val normalized = SyrianPhone.normalizeToE164Digits(request.phoneNumber)
        return if (normalized != null && request.password.isNotEmpty()) {
            Result.success(
                User(
                    id = "user${System.currentTimeMillis()}",
                    fullName = request.fullName,
                    pharmacyName = when (request.accountType) {
                        AccountType.PHARMACY -> request.pharmacyName
                        else -> ""
                    },
                    phoneNumber = "+${normalized}",
                    accountType = request.accountType,
                    pharmacyLocation = if (request.accountType == AccountType.PHARMACY) {
                        request.pharmacyLocation
                    } else {
                        ""
                    },
                    warehouseName = if (request.accountType == AccountType.WAREHOUSE) {
                        request.warehouseName
                    } else {
                        ""
                    },
                    warehouseLocation = if (request.accountType == AccountType.WAREHOUSE) {
                        request.warehouseLocation
                    } else {
                        ""
                    },
                    isActive = true,
                )
            )
        } else {
            Result.failure(Exception("البيانات غير كاملة"))
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        delay(300)
        return Result.success(Unit)
    }
    
    override suspend fun getCurrentUser(): Flow<User?> {
        return flowOf(null) // Not implemented in fake version
    }
    
    override fun isLoggedIn(): Flow<Boolean> {
        return flowOf(false) // Not implemented in fake version
    }
}
