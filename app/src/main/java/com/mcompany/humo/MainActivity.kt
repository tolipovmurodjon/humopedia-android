package com.mcompany.humo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mcompany.humo.ui.glass.GlassStage
import com.mcompany.humo.ui.glass.GlassTheme
import com.mcompany.humo.ui.shell.AppShell
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The native app (NATIVE-BRIEF N0): the glass tabs, the Path first. No WebView, no TWA. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The glass bar draws under the navigation bar; Android must not paint its own scrim over it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        GlassStage.coldStart()
        setContent { GlassTheme { AppShell() } }
        if (savedInstanceState == null) afterFirstFrame()
        // Debug builds only: the CI smoke forces a crash to prove CrashRecorder writes it down.
        if (BuildConfig.DEBUG && intent?.getBooleanExtra(EXTRA_TEST_CRASH, false) == true) {
            lifecycleScope.launch { delay(500); throw IllegalStateException("Humo test crash") }
        }
    }

    /** Push registration and the one-time notification question, never on the first frame. */
    private fun afterFirstFrame() {
        lifecycleScope.launch {
            if (HumoApp.firebaseReady(application)) {
                DailyPush.onLaunch(intent, packageManager.getPackageInfo(packageName, 0).versionName.orEmpty())
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@launch
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return@launch
            // The same flag the 2.x launcher wrote: a child who already answered is not asked again.
            val prefs = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            val asked = withContext(Dispatchers.IO) { prefs.getBoolean(ASKED_NOTIFICATIONS, false) }
            if (asked) return@launch
            prefs.edit().putBoolean(ASKED_NOTIFICATIONS, true).apply()
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private companion object {
        const val EXTRA_TEST_CRASH = "humo.test_crash"
        const val PREFERENCES = "launcher"
        const val ASKED_NOTIFICATIONS = "asked_notifications"
    }
}
