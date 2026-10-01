package com.mcompany.humo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.messaging.messaging
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

class HumoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory())
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = REMOTE_CONFIG_FETCH_INTERVAL_SECONDS })
            setDefaultsAsync(mapOf(RemoteFlags.LAUNCH_PATH to RemoteFlags.DEFAULT_LAUNCH_PATH))
            // Fetched now, used on the next launch: a launch never waits on the network for a flag.
            fetchAndActivate()
        }
        createReminderChannel()
        // One topic for everyone, so a push can be sent from the Firebase console before per-user targeting exists.
        Firebase.messaging.subscribeToTopic(TOPIC_EVERYONE)
    }

    private fun createReminderChannel() {
        val channel = NotificationChannel(
            getString(R.string.notification_channel_id),
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val TOPIC_EVERYONE = "everyone"
    }
}
