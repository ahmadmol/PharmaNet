package com.pharmalink.core.repository

import android.util.Log
import com.pharmalink.core.common.error.MissingPharmacyLinkageException
import com.pharmalink.core.common.validation.SyrianPhone
import com.pharmalink.core.datastore.UserSnapshotStore
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.AuthSessionState
import com.pharmalink.domain.model.LoginRequest
import com.pharmalink.domain.model.SignUpRequest
import com.pharmalink.domain.model.User
import com.pharmalink.domain.model.UserSnapshot
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userSnapshotStore: UserSnapshotStore,
) : AuthRepository {

    private val auth: Auth get() = supabase.auth

    override suspend fun login(request: LoginRequest): Result<User> = runCatching {
        Log.d("AUTH_DEBUG", "=== SUPABASE LOGIN DEBUG ===")
        val internalEmail = phoneToInternalEmail(request.phoneNumber)

        auth.signInWith(Email) {
            email = internalEmail
            password = request.password
        }

        val userInfo = auth.currentUserOrNull()
            ?: error("Failed to sign in. No active session was created.")
        mapUser(userInfo)
    }.onFailure { exception ->
        Log.e("AUTH_DEBUG", "Login failed: ${exception.message}", exception)
    }

    override suspend fun signUp(request: SignUpRequest): Result<User> = runCatching {
        Log.d("AUTH_DEBUG", "Attempting sign up for phone: ${request.phoneNumber}")
        val internalEmail = phoneToInternalEmail(request.phoneNumber)
        val metadata = buildJsonObject {
            put("phone_number", JsonPrimitive(request.phoneNumber))
            put("phone_e164", JsonPrimitive(request.phoneNumber))
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

        val userInfo = auth.currentUserOrNull() ?: run {
            auth.signInWith(Email) {
                email = internalEmail
                password = request.password
            }
            auth.currentUserOrNull()
        } ?: error("Account created but no active session was established.")

        mapUser(userInfo)
    }.onFailure { exception ->
        Log.e("AUTH_DEBUG", "Sign up failed: ${exception.message}", exception)
    }

    override suspend fun requestPasswordReset(identifier: String): Result<Unit> = runCatching {
        val recoveryEmail = identifierToRecoveryEmail(identifier)
        auth.resetPasswordForEmail(recoveryEmail)
    }.onFailure { exception ->
        Log.e("AUTH_DEBUG", "Password reset request failed: ${exception.message}", exception)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        require(currentPassword.isNotBlank()) { "Current password is required." }
        require(newPassword.length >= 8) { "New password must be at least 8 characters." }

        val currentUser = auth.currentUserOrNull()
            ?: error("No active session. Please sign in again.")
        val currentEmail = currentUser.email
            ?: error("Current account has no email credential. Please sign in again.")

        auth.signInWith(Email) {
            email = currentEmail
            password = currentPassword
        }
        auth.updateUser {
            password = newPassword
        }
        Unit
    }.onFailure { exception ->
        Log.e("AUTH_DEBUG", "Password change failed: ${exception.message}", exception)
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        var firstFailure: Throwable? = null

        runCatching { auth.signOut() }
            .onFailure { error ->
                firstFailure = error
                Log.e("AUTH_DEBUG", "Supabase sign out failed: ${error.message}", error)
            }

        runCatching { userSnapshotStore.clearUserSnapshot() }
            .onFailure { error ->
                if (firstFailure == null) {
                    firstFailure = error
                }
                Log.e("AUTH_DEBUG", "User snapshot clear failed: ${error.message}", error)
            }

        firstFailure?.let { throw it }
        Unit
    }.onFailure { exception ->
        Log.e("AUTH_DEBUG", "Logout failed: ${exception.message}", exception)
    }

    override fun observeAuthState(): Flow<AuthSessionState> =
        auth.sessionStatus
            .map { status ->
                when (status) {
                    SessionStatus.Initializing -> AuthSessionState.Loading
                    is SessionStatus.Authenticated -> {
                        val userInfo = status.session.user
                            ?: auth.currentUserOrNull()
                            ?: return@map AuthSessionState.Unauthenticated
                        AuthSessionState.Authenticated(mapUser(userInfo))
                    }
                    is SessionStatus.NotAuthenticated -> AuthSessionState.Unauthenticated
                    is SessionStatus.RefreshFailure -> AuthSessionState.Unauthenticated
                }
            }
            .distinctUntilChanged()

    override fun observeUserSnapshot(): Flow<UserSnapshot?> = userSnapshotStore.observeUserSnapshot()

    override suspend fun getUserSnapshot(): UserSnapshot? = userSnapshotStore.getUserSnapshot()

    override suspend fun saveUserSnapshot(snapshot: UserSnapshot): Result<Unit> = runCatching {
        userSnapshotStore.saveUserSnapshot(snapshot)
    }

    override suspend fun clearUserSnapshot(): Result<Unit> = runCatching {
        userSnapshotStore.clearUserSnapshot()
    }

    override suspend fun ensureProfileForCurrentUser(user: User): Result<UserSnapshot> = runCatching {
        val authUser = auth.currentUserOrNull() ?: error("No active session. Please sign in again.")
        val profilePayload = buildProfilePayload(user, authUser)
        supabase.postgrest["profiles"].upsert(profilePayload) { ignoreDuplicates = false }

        val profileRow = fetchProfileRow(authUser.id).getOrThrow()
        if (user.accountType == AccountType.PHARMACY && profileRow.pharmacyId == null) {
            throw MissingPharmacyLinkageException(authUser.id)
        }

        val snapshot = UserSnapshot(
            userId = authUser.id,
            phoneNumber = user.phoneNumber,
            email = authUser.email.orEmpty(),
            pharmacyId = profileRow.pharmacyId.orEmpty(),
            pharmacyName = profileRow.pharmacyName.orEmpty().ifBlank { user.pharmacyName },
            accountType = parsePersistedAccountType(
                value = profileRow.accountType,
                userId = authUser.id,
            ),
            displayName = profileRow.fullName.orEmpty().ifBlank { user.fullName },
        )
        saveUserSnapshot(snapshot).getOrThrow()
        snapshot
    }.onFailure { exception ->
        Log.e("AUTH_DEBUG", "Ensuring profile failed: ${exception.message}", exception)
    }

    private suspend fun fetchProfileRow(userId: String): Result<ProfileRowDto> =
        runCatching {
            val rows = supabase.postgrest.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeList<ProfileRowDto>()

            when {
                rows.isEmpty() -> error("Profile row is missing for authenticated user $userId.")
                rows.size > 1 -> error("Duplicate profile rows found for authenticated user $userId.")
                else -> rows.single()
            }
        }

    private fun buildProfilePayload(user: User, authUser: UserInfo): ProfileUpsertDto =
        ProfileUpsertDto(
            id = authUser.id,
            phoneNumber = user.phoneNumber,
            fullName = user.fullName,
            accountType = user.accountType.name,
            pharmacyName = user.pharmacyName.takeIf { it.isNotBlank() },
            pharmacyLocation = user.pharmacyLocation.takeIf { it.isNotBlank() },
            warehouseName = user.warehouseName.takeIf { it.isNotBlank() },
            warehouseLocation = user.warehouseLocation.takeIf { it.isNotBlank() },
            isActive = user.isActive,
        )

    private fun parsePersistedAccountType(
        value: String?,
        userId: String,
    ): AccountType {
        val rawValue = value?.trim().orEmpty()
        require(rawValue.isNotBlank()) {
            "Profile row for user $userId is missing account_type."
        }
        return runCatching { AccountType.valueOf(rawValue) }
            .getOrElse { cause ->
                throw IllegalStateException(
                    "Profile row for user $userId has invalid account_type '$rawValue'.",
                    cause,
                )
            }
    }

    private fun phoneToInternalEmail(phone: String): String {
        val digitsOnly = phone.filter { it.isDigit() }
        val normalizedDigits = when {
            digitsOnly.startsWith("963") && digitsOnly.length >= 12 -> digitsOnly.take(12)
            digitsOnly.startsWith("09") && digitsOnly.length >= 10 -> "963${digitsOnly.drop(1)}"
            digitsOnly.startsWith("9") && digitsOnly.length >= 9 -> "963$digitsOnly"
            else -> digitsOnly
        }
        return "${normalizedDigits}@pharmalink.app"
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
    private fun identifierToRecoveryEmail(identifier: String): String {
        val trimmed = identifier.trim()
        require(trimmed.isNotBlank()) { "Email or phone number is required." }

        if ("@" in trimmed) {
            return trimmed
        }

        val normalizedDigits = SyrianPhone.normalizeToE164Digits(trimmed)
        if (normalizedDigits != null) {
            throw UnsupportedOperationException(
                "Password reset by phone is not available with the current synthetic-email auth model. Use the account email or contact support.",
            )
        }

        throw IllegalArgumentException("Enter a valid email address or Syrian phone number.")
    }
}

@Serializable
private data class ProfileUpsertDto(
    val id: String,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
    @SerialName("pharmacy_location") val pharmacyLocation: String? = null,
    @SerialName("warehouse_name") val warehouseName: String? = null,
    @SerialName("warehouse_location") val warehouseLocation: String? = null,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
private data class ProfileRowDto(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("account_type") val accountType: String? = null,
    @SerialName("pharmacy_id") val pharmacyId: String? = null,
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
)
