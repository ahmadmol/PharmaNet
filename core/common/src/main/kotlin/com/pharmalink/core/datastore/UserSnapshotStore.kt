package com.pharmalink.core.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.UserSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userSnapshotDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_snapshot")
private const val TAG = "UserSnapshotStore"

@Singleton
class UserSnapshotStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private object Keys {
        val userId = stringPreferencesKey("user_id")
        val phoneNumber = stringPreferencesKey("phone_number")
        val email = stringPreferencesKey("email")
        // Legacy compatibility carrier keys (kept intentionally for migration safety).
        val pharmacyId = stringPreferencesKey("pharmacy_id")
        val pharmacyName = stringPreferencesKey("pharmacy_name")
        // Role-native warehouse keys (Phase 2 additive).
        val warehouseId = stringPreferencesKey("warehouse_id")
        val warehouseName = stringPreferencesKey("warehouse_name")
        val accountType = stringPreferencesKey("account_type")
        val displayName = stringPreferencesKey("display_name")
    }

    fun observeUserSnapshot(): Flow<UserSnapshot?> =
        context.userSnapshotDataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map(::preferencesToSnapshot)

    suspend fun getUserSnapshot(): UserSnapshot? = observeUserSnapshot().first()

    suspend fun saveUserSnapshot(snapshot: UserSnapshot) {
        context.userSnapshotDataStore.edit { preferences ->
            preferences[Keys.userId] = snapshot.userId
            preferences[Keys.phoneNumber] = snapshot.phoneNumber
            preferences[Keys.email] = snapshot.email
            preferences[Keys.pharmacyId] = snapshot.pharmacyId
            preferences[Keys.pharmacyName] = snapshot.pharmacyName
            preferences[Keys.warehouseId] = snapshot.warehouseId
            preferences[Keys.warehouseName] = snapshot.warehouseName
            preferences[Keys.accountType] = snapshot.accountType.name
            preferences[Keys.displayName] = snapshot.displayName
        }
    }

    suspend fun clearUserSnapshot() {
        context.userSnapshotDataStore.edit { preferences -> preferences.clear() }
    }

    private fun preferencesToSnapshot(preferences: Preferences): UserSnapshot? {
        val userId = preferences[Keys.userId].orEmpty()
        if (userId.isBlank()) {
            return null
        }

        val rawAccountType = preferences[Keys.accountType].orEmpty().trim()
        if (rawAccountType.isBlank()) {
            Log.w(TAG, "Discarding persisted snapshot for user=$userId because account_type is missing.")
            return null
        }
        val accountType = runCatching {
            AccountType.valueOf(rawAccountType)
        }.getOrElse { error ->
            Log.w(TAG, "Discarding persisted snapshot for user=$userId because account_type='$rawAccountType' is invalid.", error)
            return null
        }

        val persistedPharmacyId = preferences[Keys.pharmacyId].orEmpty()
        val persistedPharmacyName = preferences[Keys.pharmacyName].orEmpty()
        val persistedWarehouseId = preferences[Keys.warehouseId].orEmpty()
        val persistedWarehouseName = preferences[Keys.warehouseName].orEmpty()
        // Phase 3 compatibility hydration:
        // old snapshots may only have legacy carrier fields for WAREHOUSE.
        val resolvedWarehouseId = if (accountType == AccountType.WAREHOUSE) {
            persistedWarehouseId.ifBlank { persistedPharmacyId }
        } else {
            persistedWarehouseId
        }
        val resolvedWarehouseName = if (accountType == AccountType.WAREHOUSE) {
            persistedWarehouseName.ifBlank { persistedPharmacyName }
        } else {
            persistedWarehouseName
        }

        return UserSnapshot(
            userId = userId,
            phoneNumber = preferences[Keys.phoneNumber].orEmpty(),
            email = preferences[Keys.email].orEmpty(),
            pharmacyId = persistedPharmacyId,
            pharmacyName = persistedPharmacyName,
            // Old snapshots do not have warehouse_* keys; hydrate from carrier for WAREHOUSE only.
            warehouseId = resolvedWarehouseId,
            warehouseName = resolvedWarehouseName,
            accountType = accountType,
            displayName = preferences[Keys.displayName].orEmpty(),
        )
    }
}

