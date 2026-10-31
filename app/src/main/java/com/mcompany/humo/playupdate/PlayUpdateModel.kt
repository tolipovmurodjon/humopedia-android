package com.mcompany.humo.playupdate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** What Google Play knows about a newer version, as the pill needs it. */
enum class PlayOffer { None, Available, Downloading, Downloaded }

/** Play In-App Updates, cut to what the model needs, so a fake can stand in for Play in tests. */
interface PlayUpdateSource {
    /** Asks Play what is on offer right now. */
    fun query(done: (PlayOffer) -> Unit)

    /** Starts Play's flexible flow (a background download); [progress] hears Downloading, then Downloaded. */
    fun startDownload(progress: (PlayOffer) -> Unit)

    /** Installs the downloaded update and restarts the app. */
    fun installNow()
}

/**
 * The quiet "Yangilanish tayyor" pill. Nothing downloads and nothing installs until the learner taps: the first tap
 * starts the flexible download, the second tap (once it is Downloaded) installs. Play's own dialog stays the first
 * confirmation; the app never calls [PlayUpdateSource.installNow] by itself.
 */
class PlayUpdateModel(private val source: PlayUpdateSource) {
    var offer by mutableStateOf(PlayOffer.None)
        private set

    val pillText: String? get() = when (offer) {
        PlayOffer.Available -> "Yangilanish tayyor"
        PlayOffer.Downloaded -> "Oʻrnatish uchun bosing"
        PlayOffer.None, PlayOffer.Downloading -> null
    }

    fun refresh() = source.query { offer = it }

    fun onPillTapped() {
        when (offer) {
            PlayOffer.Available -> source.startDownload { offer = it }
            PlayOffer.Downloaded -> source.installNow()
            PlayOffer.None, PlayOffer.Downloading -> Unit
        }
    }
}
