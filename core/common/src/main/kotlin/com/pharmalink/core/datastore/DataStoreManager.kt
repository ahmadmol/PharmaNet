package com.pharmalink.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ds: DataStore<Preferences> by preferencesDataStore("user_prefs")

@Singleton
class DataStoreManager @Inject constructor(@ApplicationContext private val c: Context) {

    private val ACC = stringPreferencesKey("account_type")
    private val UID = stringPreferencesKey("user_id")

    val accountType: Flow<String?> = c.ds.data.map { it[ACC] }
    val userId: Flow<String?> = c.ds.data.map { it[UID] }

    suspend fun saveRole(t: String, id: String) {
        c.ds.edit { it[ACC] = t; it[UID] = id }
    }

    suspend fun clear() {
        c.ds.edit { it.clear() }
    }
}