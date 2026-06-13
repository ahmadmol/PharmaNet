package com.pharmalink.core.network.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class FcmTokenManager @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val tokenSyncRepository: FcmTokenSyncRepository,
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refreshToken(reason: String) {
        firebaseMessaging.token
            .addOnSuccessListener { token ->
                logToken(reason = reason, token = token)
                syncToken(reason = reason, token = token)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "FCM token refresh failed. reason=$reason", error)
            }
    }

    fun logToken(reason: String, token: String) {
        Log.d(TAG, "FCM token available. reason=$reason token=$token")
    }

    fun syncToken(reason: String, token: String) {
        syncScope.launch {
            tokenSyncRepository.saveCurrentUserToken(token = token, source = reason)
        }
    }

    companion object {
        private const val TAG = "PharmaFcmToken"
    }
}
