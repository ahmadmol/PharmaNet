package com.pharmalink.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pharmalink.MainActivity
import com.pharmalink.R
import com.pharmalink.core.network.notifications.FcmTokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.absoluteValue

@AndroidEntryPoint
class PharmaFirebaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var fcmTokenManager: FcmTokenManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        fcmTokenManager.logToken(reason = "on_new_token", token = token)
        fcmTokenManager.syncToken(reason = "on_new_token", token = token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        Log.d(TAG, "FCM data message received. keys=${data.keys}")
        if (data.isEmpty()) return

        showDataNotification(
            title = data["title"].orEmpty().ifBlank { getString(R.string.app_name) },
            body = data["body"].orEmpty().ifBlank { data["message"].orEmpty() },
            notificationId = data["notification_id"] ?: data["id"] ?: message.messageId,
        )
    }

    private fun showDataNotification(
        title: String,
        body: String,
        notificationId: String?,
    ) {
        if (!canPostNotifications()) {
            Log.d(TAG, "Skipping local notification because POST_NOTIFICATIONS is not granted")
            return
        }

        PharmaNotificationChannels.ensureCreated(this)

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(
            this,
            PharmaNotificationChannels.DEFAULT_CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(
            stableNotificationId(notificationId),
            notification,
        )
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun stableNotificationId(value: String?): Int =
        value?.hashCode()?.absoluteValue?.takeIf { it != 0 } ?: System.currentTimeMillis().toInt()

    companion object {
        private const val TAG = "PharmaFcmService"
    }
}
