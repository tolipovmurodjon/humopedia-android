package com.mcompany.humo.ui.lesson

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Talk to Humo: Android's SpeechRecognizer, on the phone itself when it can be, listening only from the tap until the child
 * stops talking (or taps again). Nothing is recorded or stored by the app; the live words go to [onPartial], the finished
 * ones to [onFinal] (empty when nothing was understood).
 */
internal class TutorMic(private val context: Context) {
    var listening by mutableStateOf(false); private set
    /** How loud the child is right now, 0..1: the dotted waveform in the input pill moves with it. */
    var level by androidx.compose.runtime.mutableFloatStateOf(0f); private set
    private var recognizer: SpeechRecognizer? = null

    val available: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(onPartial: (String) -> Unit, onFinal: (String) -> Unit) {
        cancel()
        val r = if (Build.VERSION.SDK_INT >= 33 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        else SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            private fun best(b: Bundle?) = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onPartialResults(partialResults: Bundle?) { onPartial(best(partialResults)) }
            override fun onResults(results: Bundle?) { listening = false; release(); onFinal(best(results)) }
            override fun onError(error: Int) { listening = false; release(); onFinal("") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1),
        )
        listening = true
    }

    /** The child tapped again: stop listening and let the words so far come back as the result. */
    fun stop() { recognizer?.stopListening() }

    /** The sheet closed: drop everything. */
    fun cancel() { release(); listening = false }

    private fun release() { recognizer?.destroy(); recognizer = null }
}
