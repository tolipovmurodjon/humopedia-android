package com.mcompany.humo

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig

/** Values Firebase Remote Config can change without a release, and A/B tests can split. */
object RemoteFlags {
    const val LAUNCH_PATH = "launch_path"
    const val DEFAULT_LAUNCH_PATH = "/uz/app"

    private val safePath = Regex("""^/[A-Za-z0-9/_-]*(\?[A-Za-z0-9=&_-]*)?$""")

    /** The path the app opens on its own site; anything that is not a plain path falls back to the default. */
    fun launchPath(): String =
        Firebase.remoteConfig.getString(LAUNCH_PATH).takeIf { safePath.matches(it) } ?: DEFAULT_LAUNCH_PATH
}
