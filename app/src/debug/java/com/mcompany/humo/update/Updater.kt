package com.mcompany.humo.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.mcompany.humo.R
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateState {
    data object UpToDate : UpdateState
    data class Available(val manifest: UpdateManifest) : UpdateState
    data class Failed(val reason: String) : UpdateState
}

/**
 * Checks the GitHub Release host, downloads the APK, verifies its SHA-256, tells the tester "Yangilanish tayyor" and
 * installs only after a tap, behind Android's own confirm screen. Android also refuses an APK signed with another key
 * than the installed app, so a tampered file cannot replace it even if the hash check were bypassed.
 */
class Updater(private val ctx: Context, private val clock: Clock = Clock.System) {

    fun check(): UpdateState = try {
        val text = get(BASE + "latest.json") { it.readBytes().decodeToString() }
        val m = UpdateLogic.parse(text)
        when {
            m == null -> UpdateState.Failed("latest.json is not valid")
            !UpdateLogic.safeApkName(m.apk) -> UpdateState.Failed("unsafe apk name in latest.json")
            UpdateLogic.isNewer(installedVersionCode(), m) -> UpdateState.Available(m)
            else -> UpdateState.UpToDate
        }
    } catch (e: Exception) {
        UpdateState.Failed(e.message ?: e.javaClass.simpleName)
    } finally {
        prefs().edit().putLong(LAST_CHECK, clock.nowMillis()).apply()
    }

    fun dueForCheck(): Boolean = UpdateLogic.shouldCheck(prefs().getLong(LAST_CHECK, 0L), clock)

    /** Downloads the APK into the cache and checks its SHA-256. Null = a verified file is ready, else the reason. */
    fun download(m: UpdateManifest): String? {
        if (UpdateLogic.verifiedApk(dir(), m) != null) return null
        dir().listFiles()?.forEach { it.delete() }
        val file = File(dir(), m.apk)
        try {
            get(BASE + m.apk) { input -> file.outputStream().use { input.copyTo(it) } }
        } catch (e: Exception) {
            file.delete()
            return "download failed: ${e.message}"
        }
        if (UpdateLogic.verifiedApk(dir(), m) == null) {
            file.delete()
            return "checksum mismatch, file discarded"
        }
        return null
    }

    fun rememberReady(m: UpdateManifest) { File(dir(), READY).writeText(UpdateLogic.encode(m)) }

    /** The newest manifest that was downloaded and verified, so a tap can install without asking the host again. */
    fun readyManifest(): UpdateManifest? {
        val m = runCatching { UpdateLogic.parse(File(dir(), READY).readText()) }.getOrNull() ?: return null
        return m.takeIf { UpdateLogic.isNewer(installedVersionCode(), it) && UpdateLogic.verifiedApk(dir(), it) != null }
    }

    fun canInstall(): Boolean = ctx.packageManager.canRequestPackageInstalls()

    /** Only called after the tester's tap. Returns null when handed to Android, else the reason. */
    fun installDownloaded(m: UpdateManifest): String? {
        if (!canInstall()) return "install unknown apps is off"
        val file = UpdateLogic.verifiedApk(dir(), m) ?: return "no verified download"
        return try { install(file); null } catch (e: Exception) { "installer refused: ${e.message}" }
    }

    /** The notification: its tap opens [InstallTapActivity], which starts the install. */
    fun notifyReady(m: UpdateManifest) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Yangilanishlar", NotificationManager.IMPORTANCE_DEFAULT))
        val tap = PendingIntent.getActivity(
            ctx, NOTIF_READY, Intent(ctx, InstallTapActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Yangilanish tayyor")
            .setContentText("Humopedia ${m.versionName}: oʻrnatish uchun bosing")
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(NOTIF_READY, n) }
    }

    /** Sends the tester to the one switch that lets this app install updates. */
    fun openInstallPermission() {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun installedVersionCode(): Int = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(ctx.packageManager.getPackageInfo(ctx.packageName, 0)).toInt()

    private fun prefs() = ctx.getSharedPreferences("update", Context.MODE_PRIVATE)

    private fun dir() = File(ctx.cacheDir, "updates").apply { mkdirs() }

    /** GET [url] (GitHub redirects release assets to its storage host) and hand the body to [use]. */
    private fun <T> get(url: String, use: (java.io.InputStream) -> T): T {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8_000
        c.readTimeout = 60_000
        try {
            if (c.responseCode != 200) error("update host answered ${c.responseCode}")
            return c.inputStream.use(use)
        } finally {
            c.disconnect()
        }
    }

    private fun install(file: File) {
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        // Always Android's own confirm screen: an update never replaces the app without a yes.
        if (Build.VERSION.SDK_INT >= 31) params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            file.inputStream().use { input ->
                session.openWrite("humopedia.apk", 0, file.length()).use { out ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }
            // MUTABLE: the installer fills in the status extras. The intent is explicit.
            val pending = PendingIntent.getBroadcast(
                ctx, id, Intent(ctx, InstallResultReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pending.intentSender)
        }
    }

    companion object {
        /** The newest GitHub Release of the public repo; release.yml attaches latest.json and the signed test APK to it. */
        const val BASE = "https://github.com/tolipovmurodjon/humopedia-android/releases/latest/download/"
        const val CHANNEL = "humo_updates"
        private const val READY = "ready.json"
        private const val LAST_CHECK = "last_check"
        private const val NOTIF_READY = 8
    }
}

/** Android's installer answers here; when it needs the tester's yes, its confirm screen opens. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE) != PackageInstaller.STATUS_PENDING_USER_ACTION) return
        @Suppress("DEPRECATION")
        val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
        // The tester just tapped, so the app is in front and may open Android's confirm screen itself.
        runCatching { context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
