package com.pharmalink.core.repository

import com.pharmalink.core.datastore.DataStoreManager
import android.util.Log
import com.pharmalink.core.common.validation.SyrianPhone
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.LoginRequest
import com.pharmalink.domain.model.SignUpRequest
import com.pharmalink.domain.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val ds: DataStoreManager
) : AuthRepository {

    private val auth: Auth get() = supabase.auth

    /**
     * Converts phone number to internal email format for Supabase Auth
     * Example: 963912345678 -> 963912345678@pharmalink.internal
     */
    private fun phoneToInternalEmail(phone: String): String {
        val digitsOnly = phone.filter { it.isDigit() }
        val normalizedDigits = when {
            digitsOnly.startsWith("963") && digitsOnly.length >= 12 -> digitsOnly.take(12)
            digitsOnly.startsWith("09") && digitsOnly.length >= 10 -> "963${digitsOnly.drop(1)}"
            digitsOnly.startsWith("9") && digitsOnly.length >= 9 -> "963$digitsOnly"
            else -> digitsOnly
        }
        return "${normalizedDigits}@pharmalink.internal"
    }

    override suspend fun login(request: LoginRequest): Result<User> = runCatching {
        Log.d("Auth", "Attempting login for phone: ${request.phoneNumber}")
        val internalEmail = phoneToInternalEmail(request.phoneNumber)
        Log.d("Auth", "Converted to internal email: $internalEmail")
        
        auth.signInWith(Email) {
            email = internalEmail
            password = request.password
        }
        
        val user = auth.currentUserOrNull()
        if (user == null) {
            Log.e("Auth", "Login failed: No session after sign in")
            error("Failed to sign in. Check phone number and password.")
        }
        
        Log.d("Auth", "Login successful for user: ${user.id}")
        val mappedUser = mapUser(user)
        ds.saveRole(mappedUser.accountType.name, mappedUser.id)
        mappedUser
    }

    override suspend fun signUp(request: SignUpRequest): Result<User> = runCatching {
        Log.d("Auth", "Attempting sign up for phone: ${request.phoneNumber}")
        val internalEmail = phoneToInternalEmail(request.phoneNumber)
        Log.d("Auth", "Converted to internal email: $internalEmail")
        
        // Prepare user metadata
        val metadata = buildJsonObject {
            put("phone_number", JsonPrimitive(request.phoneNumber))
            put("full_name", JsonPrimitive(request.fullName))
            put("account_type", JsonPrimitive(request.accountType.name))
            put("pharmacy_name", JsonPrimitive(request.pharmacyName))
            put("pharmacy_location", JsonPrimitive(request.pharmacyLocation))
            put("warehouse_name", JsonPrimitive(request.warehouseName))
            put("warehouse_location", JsonPrimitive(request.warehouseLocation))
        }
        
        auth.signUpWith(Email) {
            email = internalEmail
            password = request.password
            data = metadata
        }
        
        // Handle email confirmation scenario
        val user = auth.currentUserOrNull()
        if (user == null) {
            Log.w("Auth", "Sign up completed but no session - likely due to email confirmation")
            
            // Try to retrieve user by email for profile creation
            try {
                // Note: This requires admin privileges or different approach
                // For now, we'll return an error indicating email confirmation is needed
                error("Please confirm your account or disable email confirmation in Supabase settings")
            } catch (e: Exception) {
                Log.e("Auth", "Failed to retrieve user after sign up", e)
                error("Failed to create account: please try again")
            }
        }
        
        // Try to create profile record (non-critical)
        try {
            val profileData = mapOf(
                "id" to user.id,
                "phone_number" to request.phoneNumber,
                "full_name" to request.fullName,
                "account_type" to request.accountType.name,
                "pharmacy_name" to request.pharmacyName.takeIf { it.isNotBlank() },
                "pharmacy_location" to request.pharmacyLocation.takeIf { it.isNotBlank() },
                "warehouse_name" to request.warehouseName.takeIf { it.isNotBlank() },
                "warehouse_location" to request.warehouseLocation.takeIf { it.isNotBlank() },
                "is_active" to true
            )
            
            supabase.postgrest["profiles"].upsert(profileData) { ignoreDuplicates = false }
            Log.d("Auth", "Profile created/updated for user: ${user.id}")
        } catch (e: Exception) {
            Log.w("Auth", "Failed to create profile record", e)
            // Don't fail sign up process if profile creation fails
        }
        
        Log.d("Auth", "Sign up successful for user: ${user.id}")
        mapUser(user)
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        auth.signOut()
        ds.clear()
    }

    override suspend fun getCurrentUser(): Flow<User?> =
        auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> status.session.user?.let { mapUser(it) }
                else -> null
            }
        }.distinctUntilChanged()

    override fun isLoggedIn(): Flow<Boolean> =
        auth.sessionStatus.map { it is SessionStatus.Authenticated }.distinctUntilChanged()

    private fun phoneDigitsToSyntheticEmail(digits: String): String = "$digits@pharmalink.phone"

    private fun signUpMetadata(request: SignUpRequest, phoneDigits: String): JsonObject = buildJsonObject {
        put("full_name", JsonPrimitive(request.fullName))
        put("phone_e164", JsonPrimitive("+$phoneDigits"))
        put("account_type", JsonPrimitive(request.accountType.name))
        put("pharmacy_name", JsonPrimitive(request.pharmacyName))
        put("pharmacy_location", JsonPrimitive(request.pharmacyLocation))
        put("warehouse_name", JsonPrimitive(request.warehouseName))
        put("warehouse_location", JsonPrimitive(request.warehouseLocation))
    }

    private fun mapUser(info: UserInfo): User {
        val meta = info.userMetadata ?: JsonObject(emptyMap())
        fun metaString(key: String): String =
            (meta[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

        val phoneFromMeta = metaString("phone_e164").ifBlank {
            info.phone?.let { "+$it" } ?: ""
        }
        val digits = SyrianPhone.normalizeToE164Digits(phoneFromMeta)
        val phoneDisplay = if (digits != null) {
            "+$digits"
        } else {
            info.email?.substringBefore("@")?.let { "+$it" } ?: ""
        }

        val accountType = runCatching {
            AccountType.valueOf(metaString("account_type"))
        }.getOrDefault(AccountType.PHARMACY)

        return User(
            id = info.id,
            fullName = metaString("full_name").ifBlank { info.email ?: "" },
            pharmacyName = metaString("pharmacy_name"),
            phoneNumber = phoneDisplay,
            email = info.email ?: "",
            isActive = true,
            accountType = accountType,
            pharmacyLocation = metaString("pharmacy_location"),
            warehouseName = metaString("warehouse_name"),
            warehouseLocation = metaString("warehouse_location"),
        )
    }
}
