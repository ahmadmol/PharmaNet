package com.pharmalink.core.repository

import android.util.Log
import com.pharmalink.core.common.error.MissingPharmacyLinkageException
import com.pharmalink.core.common.validation.SyrianPhone
import com.pharmalink.core.datastore.UserSnapshotStore
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.AuthSessionState
import com.pharmalink.domain.model.LoginRequest
import com.pharmalink.domain.model.SignUpRequest
import com.pharmalink.domain.model.SignUpResult
import com.pharmalink.domain.model.User
import com.pharmalink.domain.model.UserSnapshot
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
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

    override suspend fun signUp(request: SignUpRequest): Result<SignUpResult> = runCatching {
        Log.d("AUTH_DEBUG", "Attempting sign up for phone: ${request.phoneNumber}")
        val internalEmail = phoneToInternalEmail(request.phoneNumber)
        val sensitiveFlow = request.accountType.requiresManualLoginAfterSignUp()

        // Hardening: prevent any stale snapshot from satisfying auth gate during transient auth emissions.
        if (sensitiveFlow) {
            userSnapshotStore.clearUserSnapshot()
        }

        val metadata = buildJsonObject {
            put("phone_number", JsonPrimitive(request.phoneNumber))
            put("phone_e164", JsonPrimitive(request.phoneNumber))
            put("full_name", JsonPrimitive(request.fullName))
            put("account_type", JsonPrimitive(request.accountType.name))
            put("pharmacy_name", JsonPrimitive(request.pharmacyName))
            put("pharmacy_location", JsonPrimitive(request.pharmacyLocation))
            put("warehouse_name", JsonPrimitive(request.warehouseName))
            put("warehouse_location", JsonPrimitive(request.warehouseLocation))
            request.latitude?.let { put("latitude", JsonPrimitive(it)) }
            request.longitude?.let { put("longitude", JsonPrimitive(it)) }
        }

        auth.signUpWith(Email) {
            email = internalEmail
            password = request.password
            data = metadata
        }

        val userInfo = if (sensitiveFlow) {
            runCatching {
                auth.currentUserOrNull() ?: auth.retrieveUserForCurrentSession(updateSession = true)
            }.getOrNull()
        } else {
            auth.currentUserOrNull() ?: run {
                auth.signInWith(Email) {
                    email = internalEmail
                    password = request.password
                }
                auth.currentUserOrNull()
            }
        }

        val user = when {
            userInfo != null -> mapUser(userInfo)
            sensitiveFlow -> buildSensitivePendingUser(request, internalEmail)
            else -> error("Account created but no active session was established.")
        }

        if (sensitiveFlow) {
            runCatching { auth.signOut() }
                .onFailure { error ->
                    Log.w("AUTH_DEBUG", "No active session to sign out after sensitive signup: ${error.message}")
                }
        }

        SignUpResult(
            user = user,
            requiresManualLogin = sensitiveFlow,
        )
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
                        runCatching { mapUser(userInfo) }
                            .onFailure { error ->
                                Log.e("AUTH_DEBUG", "Invalid authenticated user metadata for ${userInfo.id}; forcing Unauthenticated.", error)
                            }
                            .fold(
                                onSuccess = { AuthSessionState.Authenticated(it) },
                                onFailure = { AuthSessionState.Unauthenticated },
                            )
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
        val profileRow = fetchProfileRowOrNull(authUser.id).getOrThrow()
            ?: createMissingProfileForAllowedPublicUser(user).getOrThrow()

        val accountType = parsePersistedAccountType(
            value = profileRow.accountType,
            userId = authUser.id,
        )

        if (accountType == AccountType.PHARMACY && profileRow.pharmacyId == null) {
            throw MissingPharmacyLinkageException(authUser.id)
        }

        // Phase 3 role-native session adoption:
        // Prefer explicit warehouse fields first; keep pharmacy_* as temporary compatibility carrier.
        val resolvedWarehouseId = profileRow.warehouseId.orEmpty()
            .ifBlank { profileRow.pharmacyId.orEmpty() }
        val resolvedWarehouseName = profileRow.warehouseName.orEmpty()
            .ifBlank { user.warehouseName }
            .ifBlank { profileRow.pharmacyName.orEmpty() }
        val resolvedPharmacyName = profileRow.pharmacyName.orEmpty()
            .ifBlank { user.pharmacyName }

        val snapshot = UserSnapshot(
            userId = authUser.id,
            phoneNumber = user.phoneNumber,
            email = authUser.email.orEmpty(),
            // Compatibility mode:
            // - PHARMACY uses pharmacy fields as native source.
            // - WAREHOUSE dual-writes both explicit warehouse fields and legacy pharmacy carrier.
            // - PUBLIC_USER/ADMIN remain organization-empty.
            pharmacyId = when (accountType) {
                AccountType.WAREHOUSE -> resolvedWarehouseId
                AccountType.ADMIN -> ""
                AccountType.PUBLIC_USER -> ""
                else -> profileRow.pharmacyId.orEmpty()
            },
            pharmacyName = when (accountType) {
                AccountType.WAREHOUSE -> resolvedWarehouseName
                AccountType.ADMIN -> ""
                AccountType.PUBLIC_USER -> ""
                else -> resolvedPharmacyName
            },
            warehouseId = when (accountType) {
                AccountType.WAREHOUSE -> resolvedWarehouseId
                else -> ""
            },
            warehouseName = when (accountType) {
                AccountType.WAREHOUSE -> resolvedWarehouseName
                else -> ""
            },
            accountType = accountType,
            displayName = profileRow.fullName.orEmpty().ifBlank { user.fullName },
        )
        saveUserSnapshot(snapshot).getOrThrow()
        snapshot
    }.onFailure { exception ->
        Log.e("AUTH_DEBUG", "Ensuring profile failed: ${exception.message}", exception)
    }

    private suspend fun fetchProfileRow(userId: String): Result<ProfileRowDto> =
        runCatching {
            fetchProfileRowOrNull(userId).getOrThrow()
                ?: error("Profile row is missing for authenticated user $userId.")
        }

    private suspend fun fetchProfileRowOrNull(userId: String): Result<ProfileRowDto?> =
        runCatching {
            val rows = supabase.postgrest.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeList<ProfileRowDto>()

            when {
                rows.isEmpty() -> null
                rows.size > 1 -> error("Duplicate profile rows found for authenticated user $userId.")
                else -> rows.single()
            }
        }

    private suspend fun createMissingProfileForAllowedPublicUser(user: User): Result<ProfileRowDto> =
        runCatching {
            require(user.accountType == AccountType.PUBLIC_USER) {
                "Profile row is missing for ${user.accountType} user ${user.id}. Trusted admin/server provisioning is required before login can continue."
            }

            val params = CreatePublicUserProfileRpcParams(
                fullName = user.fullName.takeIf { it.isNotBlank() },
                phoneNumber = user.phoneNumber.takeIf { it.isNotBlank() },
                pharmacyName = user.pharmacyName.takeIf { it.isNotBlank() },
                pharmacyLocation = user.pharmacyLocation.takeIf { it.isNotBlank() },
                warehouseName = user.warehouseName.takeIf { it.isNotBlank() },
                warehouseLocation = user.warehouseLocation.takeIf { it.isNotBlank() },
            )

            supabase.postgrest
                .rpc("create_public_user_profile", params)
                .decodeSingle<ProfileRowDto>()
        }

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

        val rawAccountType = metaString("account_type").trim()
        val accountType = runCatching {
            require(rawAccountType.isNotBlank()) {
                "User metadata account_type is missing for user ${info.id}."
            }
            AccountType.valueOf(rawAccountType)
        }.getOrElse { cause ->
            throw IllegalStateException(
                "User ${info.id} has invalid account_type metadata '$rawAccountType'.",
                cause,
            )
        }

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

    private fun AccountType.requiresManualLoginAfterSignUp(): Boolean =
        this == AccountType.PHARMACY ||
            this == AccountType.WAREHOUSE ||
            this == AccountType.ADMIN

    private fun buildSensitivePendingUser(
        request: SignUpRequest,
        internalEmail: String,
    ): User {
        val normalizedDigits = internalEmail.substringBefore("@")
        val phoneDisplay = if (normalizedDigits.isNotBlank()) "+$normalizedDigits" else request.phoneNumber

        return User(
            id = "pending:$normalizedDigits",
            fullName = request.fullName,
            pharmacyName = request.pharmacyName,
            phoneNumber = phoneDisplay,
            email = internalEmail,
            isActive = true,
            accountType = request.accountType,
            pharmacyLocation = request.pharmacyLocation,
            warehouseName = request.warehouseName,
            warehouseLocation = request.warehouseLocation,
        )
    }
}

@Serializable
private data class CreatePublicUserProfileRpcParams(
    @SerialName("p_full_name") val fullName: String? = null,
    @SerialName("p_phone_number") val phoneNumber: String? = null,
    @SerialName("p_pharmacy_name") val pharmacyName: String? = null,
    @SerialName("p_pharmacy_location") val pharmacyLocation: String? = null,
    @SerialName("p_warehouse_name") val warehouseName: String? = null,
    @SerialName("p_warehouse_location") val warehouseLocation: String? = null,
)

@Serializable
private data class ProfileRowDto(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("account_type") val accountType: String? = null,
    @SerialName("pharmacy_id") val pharmacyId: String? = null,
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
    @SerialName("warehouse_id") val warehouseId: String? = null,
    @SerialName("warehouse_name") val warehouseName: String? = null,
)
