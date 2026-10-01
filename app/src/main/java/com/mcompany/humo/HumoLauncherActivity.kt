package com.mcompany.humo

import android.content.pm.PackageManager
import com.google.androidbrowserhelper.trusted.LauncherActivity
import com.google.androidbrowserhelper.trusted.SessionStore
import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore
import com.google.androidbrowserhelper.trusted.TwaLauncher

/**
 * Opens the web app in Chrome whenever Chrome is installed. The library otherwise uses the phone's default browser, and
 * on Samsung phones that is Samsung Internet, which renders the app poorly (Boss on the A05s, 2026-10-01). Without
 * Chrome it falls back to the library's own choice.
 */
class HumoLauncherActivity : LauncherActivity() {

    override fun createTwaLauncher(): TwaLauncher =
        TwaLauncher(this, preferredBrowser(), SessionStore.makeSessionId(taskId), SharedPreferencesTokenStore(this))

    private fun preferredBrowser(): String? = CHROME.takeIf { isEnabled(it) }

    private fun isEnabled(packageName: String): Boolean = try {
        packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        const val CHROME = "com.android.chrome"
    }
}
