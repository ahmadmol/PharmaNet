package com.pharmalink.core.repository

import com.pharmalink.domain.model.PharmacyProfile
import kotlinx.coroutines.flow.Flow

/**
 * Profile Repository Interface
 * Handles user profile operations
 */
interface ProfileRepository {
    suspend fun getProfile(): Flow<PharmacyProfile?>
    suspend fun updateProfile(profile: PharmacyProfile): Result<PharmacyProfile>
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}
