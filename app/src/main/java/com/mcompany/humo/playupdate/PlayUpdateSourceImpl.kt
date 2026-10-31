package com.mcompany.humo.playupdate

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/** Google Play's In-App Updates, flexible flow. Only the Play build uses it (the test build updates itself). */
class PlayUpdateSourceImpl(private val activity: Activity) : PlayUpdateSource {
    private val manager = AppUpdateManagerFactory.create(activity)
    private var info: AppUpdateInfo? = null

    override fun query(done: (PlayOffer) -> Unit) {
        manager.appUpdateInfo.addOnSuccessListener {
            info = it
            done(
                when {
                    it.installStatus() == InstallStatus.DOWNLOADED -> PlayOffer.Downloaded
                    it.installStatus() == InstallStatus.DOWNLOADING -> PlayOffer.Downloading
                    it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && it.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> PlayOffer.Available
                    else -> PlayOffer.None
                },
            )
        }
    }

    override fun startDownload(progress: (PlayOffer) -> Unit) {
        val current = info ?: return
        manager.registerListener(object : InstallStateUpdatedListener {
            override fun onStateUpdate(state: com.google.android.play.core.install.InstallState) {
                when (state.installStatus()) {
                    InstallStatus.DOWNLOADING -> progress(PlayOffer.Downloading)
                    InstallStatus.DOWNLOADED -> { progress(PlayOffer.Downloaded); manager.unregisterListener(this) }
                    InstallStatus.FAILED, InstallStatus.CANCELED -> { progress(PlayOffer.Available); manager.unregisterListener(this) }
                    else -> Unit
                }
            }
        })
        manager.startUpdateFlow(current, activity, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
    }

    override fun installNow() { manager.completeUpdate() }
}
