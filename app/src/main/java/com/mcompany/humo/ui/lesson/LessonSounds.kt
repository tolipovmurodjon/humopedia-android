package com.mcompany.humo.ui.lesson

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/**
 * The lesson's own short sounds, made as PCM (PAGEL's two-note ding method): Brilliant's mp3s never ship (their asset,
 * a public repo). Each matches the character of Brilliant's cue as measured (docs/qa/l11-spec.md §9): length, pitch
 * direction and a soft envelope. Clean tones only, no noise, no buzz (Boss: "I hate buzzy sounds").
 */
enum class Cue { Correct, Incorrect, Tick, Toggle, Start, Complete }

object LessonSounds {
    private const val RATE = 44_100
    private val tracks = HashMap<Cue, AudioTrack>()
    @Volatile var muted = false

    /** One note: sine with a soft 2nd harmonic, a [rise] s attack and an exponential fall to silence by [fall] s. */
    internal class Note(val f: Double, val at: Double, val fall: Double, val gain: Double = 1.0, val rise: Double = 0.006, val to: Double? = null)

    internal fun notes(cue: Cue): List<Note> = when (cue) {
        // Brilliant: 410 ms, 3-4 soft rising hits. Ours: C6 E6 G6, 90 ms apart.
        Cue.Correct -> listOf(Note(1046.5, 0.0, 0.22), Note(1318.5, 0.09, 0.22), Note(1568.0, 0.18, 0.26))
        // Brilliant: 365 ms of dry, quiet clicks. Ours: two soft low notes falling, half volume.
        Cue.Incorrect -> listOf(Note(329.6, 0.0, 0.16, 0.5), Note(261.6, 0.12, 0.18, 0.5))
        // Brilliant: a 5 ms click near 2 kHz.
        Cue.Tick -> listOf(Note(2100.0, 0.0, 0.03, 0.35, rise = 0.001))
        // Brilliant: a 30 ms low thump on each node crossed.
        Cue.Toggle -> listOf(Note(180.0, 0.0, 0.05, 0.6, rise = 0.002, to = 120.0))
        Cue.Start -> listOf(523.3, 659.3, 784.0, 1046.5).mapIndexed { i, f -> Note(f, i * 0.1, 0.3, 0.8) }
        Cue.Complete -> listOf(523.3, 659.3, 784.0, 1046.5, 1318.5).mapIndexed { i, f -> Note(f, i * 0.11, if (i == 4) 0.9 else 0.35, 0.8) }
    }

    /** The cue as 16-bit mono PCM, normalised to 0.9 of full scale times the cue's loudest note. */
    internal fun pcm(cue: Cue): ShortArray {
        val ns = notes(cue)
        val len = ns.maxOf { it.at + it.fall } + 0.02
        val out = DoubleArray((RATE * len).toInt())
        for (n in ns) {
            val start = (n.at * RATE).toInt()
            val end = minOf(out.size, start + (n.fall * RATE).toInt())
            var phase = 0.0
            for (i in start until end) {
                val t = (i - start).toDouble() / RATE
                val env = if (t < n.rise) t / n.rise else exp(ln(0.001) * ((t - n.rise) / (n.fall - n.rise)).coerceAtMost(1.0))
                val f = if (n.to == null) n.f else n.f * Math.pow(n.to / n.f, t / n.fall)
                phase += 2 * PI * f / RATE
                out[i] += n.gain * env * (sin(phase) + 0.18 * sin(2 * phase))
            }
        }
        val peak = out.maxOf { abs(it) }.coerceAtLeast(1e-9)
        val loud = ns.maxOf { it.gain }
        return ShortArray(out.size) { (out[it] / peak * 0.9 * loud * Short.MAX_VALUE).toInt().toShort() }
    }

    /** A missing audio device never blocks a lesson: the picture and the haptic still answer. */
    @Synchronized
    fun play(cue: Cue) {
        if (muted) return
        runCatching {
            val t = tracks[cue] ?: run {
                val data = pcm(cue)
                AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(data.size * 2)
                    .build()
                    .also { it.write(data, 0, data.size); it.setVolume(0.4f); tracks[cue] = it }
            }
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
            t.reloadStaticData()
            t.play()
        }
    }
}
