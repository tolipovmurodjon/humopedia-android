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
        val t0 = android.os.SystemClock.elapsedRealtime()
        super.onCreate()
        // First, so a crash anywhere after this line is written to files/crashes/ (copied from PAGEL).
        CrashRecorder.install(this)
        android.util.Log.i("HumoPerf", "app-oncreate ms=${android.os.SystemClock.elapsedRealtime() - t0} (process start to here ms=${android.os.SystemClock.elapsedRealtime() - android.os.Process.getStartElapsedRealtime()})")
    }

    /** Everything that is not needed to draw the first frame: the push channel, App Check and the one topic. MainActivity calls it after the first frame. */
    fun startAfterFirstFrame() {
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
