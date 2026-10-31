package com.mcompany.humo

import android.app.Application
import android.content.Intent
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.functions.functions
import com.google.firebase.installations.installations
import com.google.firebase.messaging.messaging

/** The push a child tapped to open the app, read from the data the server put in it (functions: src/push/push.js). */
data class PushTap(val sendId: String, val arm: String, val slot: String) {
    companion object {
        fun from(sendId: String?, arm: String?, slot: String?): PushTap? =
            if (sendId.isNullOrBlank() || arm.isNullOrBlank() || slot.isNullOrBlank()) null else PushTap(sendId, arm, slot)

        fun from(intent: Intent?): PushTap? =
            from(intent?.getStringExtra("push_send_id"), intent?.getStringExtra("push_arm"), intent?.getStringExtra("push_slot"))
    }
}

/**
 * Tells the server this phone opened the app: the daily push times itself from the last open (Duolingo style), stops
 * for the day once the child is in, and counts a tapped push as opened. Fire and forget: a launch never waits on it.
 */
object DailyPush {
    fun onLaunch(intent: Intent?, appVersion: String) {
        val tap = PushTap.from(intent)
        tap?.let {
            Firebase.analytics.logEvent("push_open") {
                param("arm", it.arm)
                param("slot", it.slot)
            }
        }
        val fid = Firebase.installations.id
        val token = Firebase.messaging.token
        Tasks.whenAllSuccess<String>(fid, token).addOnSuccessListener { (id, fcmToken) ->
            Firebase.functions(REGION).getHttpsCallable("registerPushDevice")
                .call(mapOf("fid" to id, "token" to fcmToken, "sendId" to tap?.sendId, "appVersion" to appVersion))
        }
    }

    /**
     * CI smoke only (BuildConfig.SMOKE_BACKEND = "host:port"): the same callable, sent to the fake server on the runner
     * through a second Firebase app named "smoke". The real default app, analytics and push are never started, so the
     * walkthrough can prove that this one request is all the app sends.
     */
    fun onLaunchAgainstFakeServer(app: Application, hostAndPort: String, appVersion: String) {
        val options = FirebaseOptions.Builder().setApplicationId("1:1234567890:android:abcdef1234567890").setApiKey("AIzaSyCiFakeKeyCiFakeKeyCiFakeKeyCiFak0").setProjectId("humo-ci").build()
        val smokeApp = FirebaseApp.getApps(app).firstOrNull { it.name == "smoke" } ?: FirebaseApp.initializeApp(app, options, "smoke")
        val functions = Firebase.functions(smokeApp, REGION)
        functions.useEmulator(hostAndPort.substringBefore(':'), hostAndPort.substringAfter(':').toInt())
        functions.getHttpsCallable("registerPushDevice")
            .call(mapOf("fid" to "ci-fid", "token" to "ci-token", "sendId" to null, "appVersion" to appVersion))
            .addOnSuccessListener { android.util.Log.i("HumoSmoke", "registerPushDevice answered") }
            .addOnFailureListener { android.util.Log.w("HumoSmoke", "registerPushDevice failed", it) }
    }

    private const val REGION = "asia-south1"
}
