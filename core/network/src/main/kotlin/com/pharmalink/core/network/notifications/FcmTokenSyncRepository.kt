package com.pharmalink.core.network.notifications

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Singleton
class FcmTokenSyncRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun saveCurrentUserToken(token: String, source: String) {
        val trimmedToken = token.trim()
        if (trimmedToken.isBlank()) {
            Log.d(TAG, "Skipping blank FCM token. source=$source")
            return
        }

        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId.isNullOrBlank()) {
            Log.d(TAG, "Skipping FCM token sync; no authenticated Supabase user. source=$source")
            return
        }

        runCatching {
            supabase.postgrest.rpc(
                function = "sync_user_fcm_token",
                parameters = buildJsonObject {
                    put("p_token", trimmedToken)
                    put("p_platform", PLATFORM_ANDROID)
                },
            )
        }.onSuccess {
            Log.d(TAG, "FCM token synced. source=$source userId=$userId")
        }.onFailure { error ->
            Log.e(TAG, "FCM token sync failed. source=$source userId=$userId", error)
        }
    }

    companion object {
        private const val TAG = "FcmTokenSyncRepo"
        private const val PLATFORM_ANDROID = "android"
    }
}
