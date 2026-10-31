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
enum class Cue { Correct, Incorrect, Tick, Toggle, Start, Complete, Whoosh, Boom, DragStart, DragEnd, Continue, Skip, Failed, XpStart, XpStop }

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
        Cue.DragStart -> listOf(Note(1500.0, 0.0, 0.03, 0.3, rise = 0.001))
        Cue.DragEnd -> listOf(Note(900.0, 0.0, 0.04, 0.35, rise = 0.001))
        Cue.Continue, Cue.Skip -> listOf(Note(1800.0, 0.0, 0.04, 0.35, rise = 0.001))
        Cue.Failed -> listOf(Note(261.6, 0.0, 0.2, 0.5), Note(220.0, 0.14, 0.24, 0.5), Note(196.0, 0.3, 0.3, 0.5))
        Cue.XpStart, Cue.XpStop -> listOf(Note(1318.5, 0.0, 0.05, 0.3, rise = 0.001))
        Cue.Whoosh, Cue.Boom -> emptyList()
    }

    /**
     * The take-off whoosh (fly-layer.js whoosh): noise through a band-pass that sweeps down (900 -> 320 Hz over 0.6 s),
     * a soft 80 ms rise and a long fall; [big] (the sound barrier) is lower, longer and adds a low thump. Quiet, never a tone.
     */
    internal fun whooshPcm(big: Boolean): ShortArray {
        val len = if (big) 1.1 else 0.6
        val out = DoubleArray((RATE * len).toInt())
        val rnd = java.util.Random(7)
        val f0 = if (big) 1400.0 else 900.0
        val f1 = if (big) 260.0 else 320.0
        val peak = if (big) 0.16 else 0.07
        var low = 0.0
        var band = 0.0
        for (i in out.indices) {
            val t = i.toDouble() / RATE
            val f = 2 * sin(PI * (f0 * Math.pow(f1 / f0, t / len)) / RATE)
            val high = (rnd.nextDouble() * 2 - 1) - low - 1.25 * band
            band += f * high
            low += f * band
            val env = if (t < 0.08) 0.0001 * Math.pow(peak / 0.0001, t / 0.08) else peak * Math.pow(0.0001 / peak, (t - 0.08) / (len - 0.08))
            out[i] = band * env
        }
        var top = out.maxOf { abs(it) }.coerceAtLeast(1e-9)
        for (i in out.indices) out[i] = out[i] / top * peak * 4
        if (big) {
            val s = (0.25 * RATE).toInt()
            var phase = 0.0
            for (i in s until minOf(out.size, (0.75 * RATE).toInt())) {
                val t = (i - s).toDouble() / RATE
                phase += 2 * PI * (90.0 * Math.pow(38.0 / 90.0, t / 0.35).coerceAtLeast(38.0 / 90.0 * 0.5)) / RATE
                val env = if (t < 0.03) t / 0.03 else exp(ln(0.001) * ((t - 0.03) / 0.47).coerceAtMost(1.0))
                out[i] += 0.22 * 4 * env * sin(phase)
            }
        }
        top = out.maxOf { abs(it) }.coerceAtLeast(1e-9)
        val loud = if (big) 0.9 else 0.5
        for (i in out.indices) out[i] = out[i] / top * loud
        // a short fade-out so the last sample is silence
        val fade = (0.02 * RATE).toInt()
        for (i in 0 until fade) out[out.size - 1 - i] *= i.toDouble() / fade
        return ShortArray(out.size) { (out[it] * 0.9 * Short.MAX_VALUE).toInt().toShort() }
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

    // Brilliant's real sounds ride in the DEBUG source set only (app/src/debug/assets/sounds, Boss's decision 2026-10-31); a cue
    // whose file is in the build plays from a preloaded SoundPool. The release build has no such files and plays our synth.
    private var pool: android.media.SoundPool? = null
    private val packIds = HashMap<Cue, Int>()
    private val packReady = HashSet<Int>()

    internal fun packName(cue: Cue): String? = when (cue) {
        Cue.Correct -> "correct"; Cue.Incorrect -> "incorrect"; Cue.Tick -> "lightweight-choice"; Cue.Toggle -> "toggle-on"
        Cue.DragStart -> "drag-started"; Cue.DragEnd -> "drag-released"; Cue.Continue -> "continue"; Cue.Skip -> "skip"
        Cue.Failed -> "lesson-failed"; Cue.XpStart -> "tutor-xp-countup-start"; Cue.XpStop -> "tutor-xp-countup-stop"
        Cue.Start, Cue.Complete, Cue.Whoosh, Cue.Boom -> null
    }

    /** Preloads whichever of those files this build holds, once at start; call it off the main thread. */
    @Synchronized
    fun loadLocalPack(context: android.content.Context) {
        if (pool != null) return
        val have = context.assets.list("sounds")?.toSet().orEmpty()
        val found = Cue.entries.mapNotNull { c -> packName(c)?.takeIf { "$it.mp3" in have }?.let { c to it } }
        if (found.isEmpty()) return
        val sp = android.media.SoundPool.Builder().setMaxStreams(4).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
        sp.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(this) { packReady.add(id) } }
        found.forEach { (c, n) -> context.assets.openFd("sounds/$n.mp3").use { packIds[c] = sp.load(it, 1) } }
        pool = sp
    }

    /** Builds the cue's track now (its PCM and AudioTrack), so the first tap in a lesson does not. */
    @Synchronized
    internal fun prepare(cue: Cue) {
        if (packIds.containsKey(cue) || tracks.containsKey(cue)) return
        runCatching { track(cue) }
    }

    private fun track(cue: Cue): AudioTrack = tracks[cue] ?: run {
        val data = if (cue == Cue.Whoosh) whooshPcm(false) else if (cue == Cue.Boom) whooshPcm(true) else pcm(cue)
        AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size * 2)
            .build()
            .also { it.write(data, 0, data.size); it.setVolume(0.4f); tracks[cue] = it }
    }

    /** A missing audio device never blocks a lesson: the picture and the haptic still answer. */
    @Synchronized
    fun play(cue: Cue) {
        if (muted) return
        packIds[cue]?.takeIf { it in packReady }?.let { id -> pool?.play(id, 1f, 1f, 1, 0, 1f); return }
        runCatching {
            val t = track(cue)
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
            t.reloadStaticData()
            t.play()
        }
    }
}
