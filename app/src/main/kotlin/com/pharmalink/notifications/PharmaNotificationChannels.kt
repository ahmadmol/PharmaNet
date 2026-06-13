package com.pharmalink.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object PharmaNotificationChannels {
    const val DEFAULT_CHANNEL_ID = "pharmalink_default_notifications"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            "PharmaLink notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Operational updates from PharmaLink"
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
