package com.mcompany.humo.crash

import android.content.Context
import android.os.Build
import com.mcompany.humo.BuildConfig
import java.io.File

/**
 * Catches every uncaught crash, writes its stack to `files/crashes/`, then lets Android close the app as before.
 * Copied from PAGEL. The file stays on the phone for now: Humopedia has no crash endpoint yet.
 * TODO(native N1+): send pending files on the next start once the server has an endpoint (PAGEL's CrashUploader).
 */
object CrashRecorder {
    fun dir(ctx: Context) = File(ctx.filesDir, "crashes")

    fun install(ctx: Context) {
        val dir = dir(ctx)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching {
                CrashLog.write(
                    dir,
                    CrashLog.report(thread, e, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, "${Build.MANUFACTURER} ${Build.MODEL}", Build.VERSION.SDK_INT, System.currentTimeMillis()),
                )
            }
            previous?.uncaughtException(thread, e)
        }
    }
}
