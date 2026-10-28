package com.mcompany.humo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.messaging.messaging
import com.mcompany.humo.crash.CrashRecorder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HumoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // First, so a crash anywhere after this line is written to files/crashes/ (copied from PAGEL).
        CrashRecorder.install(this)
        createReminderChannel()
        if (!firebaseReady(this)) return
        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory())
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

    companion object {
        private const val TOPIC_EVERYONE = "everyone"

        /**
         * A build without app/google-services.json (CI's debug APK) has no Firebase app; every Firebase call would
         * throw, so the app runs without push and analytics instead of crashing.
         */
        fun firebaseReady(app: Application): Boolean = FirebaseApp.getApps(app).isNotEmpty()
    }
}
