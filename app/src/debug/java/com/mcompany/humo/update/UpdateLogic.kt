package com.mcompany.humo.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/**
 * The test channel's self-update (copied from PAGEL's update/ folder). It lives in the debug source set only: Google
 * Play forbids a Play-distributed app from updating itself, so the release bundle holds none of this code and not the
 * REQUEST_INSTALL_PACKAGES permission (release.yml proves it on every release build).
 */
@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apk: String,
    val sha256: String,
    val builtAt: String = "",
    val notes: String = "",
)

/** The time source of the update decisions, so tests never read the wall clock. */
fun interface Clock {
    fun nowMillis(): Long

    companion object { val System = Clock { java.lang.System.currentTimeMillis() } }
}

object UpdateLogic {
    private val json = Json { ignoreUnknownKeys = true }

    /** The app asks the host at most this often; a new build still reaches the tester within minutes. */
    const val MIN_GAP_MILLIS = 10 * 60 * 1000L

    fun parse(text: String): UpdateManifest? = try {
        json.decodeFromString(UpdateManifest.serializer(), text)
    } catch (_: Exception) {
        null
    }

    fun encode(m: UpdateManifest): String = json.encodeToString(UpdateManifest.serializer(), m)

    /** Only ever move forward: equal or older is never installed, so a stale host cannot downgrade the app. */
    fun isNewer(currentVersionCode: Int, m: UpdateManifest): Boolean = m.versionCode > currentVersionCode

    /** The apk name comes from a remote file: refuse anything that could climb out of the download folder. */
    fun safeApkName(name: String): Boolean =
        name.isNotEmpty() && name.endsWith(".apk") && !name.contains('/') && !name.contains('\\') && !name.contains("..")

    fun shouldCheck(lastCheckMillis: Long, clock: Clock): Boolean = clock.nowMillis() - lastCheckMillis >= MIN_GAP_MILLIS

    fun sha256Hex(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun sameHash(a: String, b: String): Boolean = a.trim().equals(b.trim(), ignoreCase = true)

    /** The downloaded APK for [m] in [dir], only if its name is safe and its bytes match the published hash. */
    fun verifiedApk(dir: File, m: UpdateManifest): File? {
        if (!safeApkName(m.apk)) return null
        val f = File(dir, m.apk)
        return f.takeIf { it.isFile && sameHash(sha256Hex(it), m.sha256) }
    }
}
