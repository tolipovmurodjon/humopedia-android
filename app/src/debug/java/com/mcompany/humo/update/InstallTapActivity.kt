package com.mcompany.humo.update

import android.app.Activity
import android.os.Bundle

/** The "Yangilanish tayyor" notification opens this invisible screen: it starts the install and closes at once. */
class InstallTapActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val updater = Updater(this)
        val ready = updater.readyManifest()
        when {
            ready == null -> Unit
            !updater.canInstall() -> updater.openInstallPermission()
            else -> updater.installDownloaded(ready)
        }
        finish()
    }
}
