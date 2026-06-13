package com.pharmalink

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.pharmalink.core.network.notifications.FcmTokenManager
import com.pharmalink.notifications.PharmaNotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PharmaLinkApplication : Application() {
    @Inject lateinit var fcmTokenManager: FcmTokenManager

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar"))
        PharmaNotificationChannels.ensureCreated(this)
        fcmTokenManager.refreshToken(reason = "app_start")
    }
}
