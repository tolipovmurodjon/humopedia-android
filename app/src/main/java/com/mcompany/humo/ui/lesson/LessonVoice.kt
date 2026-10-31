package com.mcompany.humo.ui.lesson

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Humo's voice (player.js `speak`): the scripted lines are Jessa's recordings bundled in assets/voice (our own generated audio,
 * no key in the app). A line without a clip stays silent here and the mouth falls back to a letter-count estimate. One line
 * at a time: a new line, a new page or the speaker switch stops the one in the air. The speaker choice is saved.
 */
internal class HumoVoice(private val context: Context) {
    private val prefs = context.getSharedPreferences("humo-voice", Context.MODE_PRIVATE)
    var on by mutableStateOf(prefs.getBoolean("on", true)); private set
    private var player: MediaPlayer? = null

    fun toggle() {
        on = !on
        prefs.edit().putBoolean("on", on).apply()
        if (!on) stop()
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }

    /** Plays the clip that has [text], returns true when it ran to its end; false when there is no clip or it failed. */
    suspend fun say(text: String): Boolean {
        val file = L11.voice[text] ?: return false
        stop()
        return suspendCancellableCoroutine { done ->
            val mp = MediaPlayer()
            player = mp
            fun end(ok: Boolean) { if (player === mp) player = null; runCatching { mp.release() }; if (done.isActive) done.resume(ok) }
            try {
                context.assets.openFd("voice/$file").use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                mp.setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_MEDIA).build())
                mp.setOnPreparedListener { it.start() }
                mp.setOnCompletionListener { end(true) }
                mp.setOnErrorListener { _, _, _ -> end(false); true }
                mp.prepareAsync()
            } catch (e: Exception) { end(false) }
            done.invokeOnCancellation { if (player === mp) player = null; runCatching { mp.stop() }; runCatching { mp.release() } }
        }
    }
}

@Composable
internal fun rememberHumoVoice(): HumoVoice {
    val context = LocalContext.current.applicationContext
    val voice = remember { HumoVoice(context) }
    DisposableEffect(voice) { onDispose { voice.stop() } }
    return voice
}
