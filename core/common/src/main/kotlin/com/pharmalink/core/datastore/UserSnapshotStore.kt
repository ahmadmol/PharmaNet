package com.pharmalink.core.datastore

import android.content.Context
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

@Singleton
class UserSnapshotStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private object Keys {
        val userId = stringPreferencesKey("user_id")
        val phoneNumber = stringPreferencesKey("phone_number")
        val email = stringPreferencesKey("email")
        val pharmacyId = stringPreferencesKey("pharmacy_id")
        val pharmacyName = stringPreferencesKey("pharmacy_name")
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

        val accountType = runCatching {
            AccountType.valueOf(preferences[Keys.accountType].orEmpty())
        }.getOrDefault(AccountType.PHARMACY)

        return UserSnapshot(
            userId = userId,
            phoneNumber = preferences[Keys.phoneNumber].orEmpty(),
            email = preferences[Keys.email].orEmpty(),
            pharmacyId = preferences[Keys.pharmacyId].orEmpty(),
            pharmacyName = preferences[Keys.pharmacyName].orEmpty(),
            accountType = accountType,
            displayName = preferences[Keys.displayName].orEmpty(),
        )
    }
}
