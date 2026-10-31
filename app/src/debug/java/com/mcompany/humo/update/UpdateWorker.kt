package com.mcompany.humo.update

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mcompany.humo.BuildConfig
import java.util.concurrent.TimeUnit

/** Asks the host; if a newer build exists, downloads and verifies it, then posts "Yangilanish tayyor". It never installs. */
class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val updater = Updater(applicationContext)
        val state = updater.check()
        if (state is UpdateState.Available && updater.download(state.manifest) == null) {
            updater.rememberReady(state.manifest)
            updater.notifyReady(state.manifest)
        }
        return Result.success()
    }

    companion object {
        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<UpdateWorker>(30, TimeUnit.MINUTES).setConstraints(online).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("humo-self-update", ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        /** One check now, at each app start, unless the last one was under 10 minutes ago. */
        fun checkNow(ctx: Context) {
            if (!Updater(ctx).dueForCheck()) return
            val req = OneTimeWorkRequestBuilder<UpdateWorker>().setConstraints(online).build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("humo-self-update-now", ExistingWorkPolicy.KEEP, req)
        }
    }
}

/**
 * Starts the updater without touching the shared code: a debug-only provider hooks the first activity, so the release
 * build, which has no such provider, never knows an updater exists. The CI smoke build (fake backend) stays offline.
 */
class UpdateStartup : ContentProvider() {
    override fun onCreate(): Boolean {
        val app = context?.applicationContext as? Application ?: return true
        if (BuildConfig.SMOKE_BACKEND.isNotEmpty()) return true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var started = false
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (started) return
                started = true
                UpdateWorker.schedule(app)
                UpdateWorker.checkNow(app)
            }
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
