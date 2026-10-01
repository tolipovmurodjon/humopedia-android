package com.mcompany.humo

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.google.androidbrowserhelper.trusted.LauncherActivity
import com.google.androidbrowserhelper.trusted.SessionStore
import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore
import com.google.androidbrowserhelper.trusted.TwaLauncher
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

/**
 * Opens the web app in Chrome whenever Chrome is installed. The library otherwise uses the phone's default browser, and
 * on Samsung phones that is Samsung Internet, which renders the app poorly (Boss on the A05s, 2026-10-01). Without
 * Chrome it falls back to the library's own choice.
 *
 * On the first launch on Android 13+ it asks for notifications before opening the site, because once the browser is on
 * top this activity can no longer show the system dialog.
 */
class HumoLauncherActivity : LauncherActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.analytics.logEvent("twa_launch") {
            param("browser", preferredBrowser() ?: "default")
            param("launch_path", RemoteFlags.launchPath())
        }
        if (shouldAskForNotifications()) {
            preferences().edit().putBoolean(ASKED_NOTIFICATIONS, true).apply()
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATIONS_REQUEST)
        }
    }

    override fun shouldLaunchImmediately(): Boolean = !shouldAskForNotifications()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATIONS_REQUEST) {
            val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            Firebase.analytics.logEvent("notifications_permission") { param("granted", granted.toString()) }
            launchTwa()
        }
    }

    override fun createTwaLauncher(): TwaLauncher =
        TwaLauncher(this, preferredBrowser(), SessionStore.makeSessionId(taskId), SharedPreferencesTokenStore(this))

    // A link into the site keeps its own address; only a plain launch takes the Remote Config path, so it can be A/B tested.
    override fun getLaunchingUrl(): Uri =
        if (intent?.data != null) super.getLaunchingUrl()
        else Uri.parse("https://${getString(R.string.web_host)}${RemoteFlags.launchPath()}")

    private fun shouldAskForNotifications(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !preferences().getBoolean(ASKED_NOTIFICATIONS, false)

    private fun preferences() = getSharedPreferences(PREFERENCES, MODE_PRIVATE)

    private fun preferredBrowser(): String? = CHROME.takeIf { isEnabled(it) }

    private fun isEnabled(packageName: String): Boolean = try {
        packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        const val CHROME = "com.android.chrome"
        const val PREFERENCES = "launcher"
        const val ASKED_NOTIFICATIONS = "asked_notifications"
        const val NOTIFICATIONS_REQUEST = 1
    }
}
