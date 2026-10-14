package com.mcompany.humo

import android.content.Intent
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
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

    private const val REGION = "asia-south1"
}
