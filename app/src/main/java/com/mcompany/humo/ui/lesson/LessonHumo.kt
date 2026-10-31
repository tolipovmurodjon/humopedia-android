package com.mcompany.humo.ui.lesson

import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import kotlinx.coroutines.delay
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** What Humo's face shows (humo-live.js FACES: the ones the lesson uses). */
enum class HumoFace { Rest, Happy, Thinking, Cheer, Laugh, Proud, Talk, Sad, Cheeky, Surprised, Wink, Sleepy, Oops }

/** What Humo does with his arms and a prop while he is not talking (humo-live.js POSES). */
enum class HumoPose { None, Wave, Point, Read, Laptop, Science }

/** The one-shot moves (humo-live.js MOVES). */
enum class HumoMove { Jump, Celebrate, Nod, Shake, Surprise }

/**
 * Humo's animation state, springs and all (humo-live.js ported): the screen sets the [face] and calls [play]; the
 * drawing reads [frame] so only the Canvas redraws. [look] is where the machine is, -1..1 each way (null = ahead).
 * When [alive] is false nothing runs by itself: a still pose per face, for the screenshot tests and reduced motion.
 */
@Stable
class HumoState(val alive: Boolean) {
    var face by mutableStateOf(HumoFace.Rest)
    var pose by mutableStateOf(HumoPose.None)
    /** When the confetti burst began (uptime ms), or -1. */
    internal var burstAt = -1L
    var look: Offset? = null
    /** Read every frame while [tracking]; the lesson points it at the flow's machine. */
    var lookSource: (() -> Offset?)? = null
    /** True while the machine flies: Humo's eyes follow [look] every frame instead of settling. */
    var tracking by mutableStateOf(false)
    /** Humo is saying a line: his mouth moves for about as long as the line takes (player.js `speak`). */
    var talking by mutableStateOf(false)
    val frame = mutableLongStateOf(0L)
    internal var moveSerial by mutableStateOf(0)
    internal var move: HumoMove? = null
    internal var playedSerial = 0

    internal val x = FloatArray(Ch.values().size)
    internal val v = FloatArray(Ch.values().size)
    internal var blink = 0f
    internal var mouth = "smile"

    fun play(m: HumoMove) { move = m; moveSerial++ }

    internal enum class Ch { Squash, Hop, Tilt, Sway, LookX, LookY, Happy, Brow, BrowL, TiltR, Sad, HappyR, BrowR, TiltL, ArmL, ArmR, Front, LeanX, OpenL, OpenR, EyeScale }

    init { x[Ch.OpenL.ordinal] = 1f; x[Ch.OpenR.ordinal] = 1f; x[Ch.EyeScale.ordinal] = 1f }

    /** The pose's arms and prop, then the face on top of it (humo-live.js: T = BASE + POSES + FACES). */
    internal fun targets(f: HumoFace): FloatArray {
        val t = FloatArray(Ch.values().size)
        fun set(c: Ch, value: Float) { t[c.ordinal] = value }
        set(Ch.OpenL, 1f); set(Ch.OpenR, 1f); set(Ch.EyeScale, 1f)
        when (pose) {
            HumoPose.None -> Unit
            HumoPose.Wave -> { set(Ch.ArmR, -126f); set(Ch.Tilt, -3f) }
            HumoPose.Point -> { set(Ch.ArmR, -125f); set(Ch.LookX, 0.8f); set(Ch.LookY, -0.6f); set(Ch.Tilt, 3f) }
            HumoPose.Read -> { set(Ch.ArmL, -34f); set(Ch.ArmR, 34f); set(Ch.Front, 1f); set(Ch.LookY, 0.75f) }
            HumoPose.Laptop -> { set(Ch.ArmL, -28f); set(Ch.ArmR, 28f); set(Ch.Front, 1f); set(Ch.LookY, 0.85f) }
            HumoPose.Science -> { set(Ch.ArmR, -118f); set(Ch.LookX, 0.75f); set(Ch.LookY, -0.55f) }
        }
        when (f) {
            HumoFace.Happy -> { set(Ch.Happy, 1f); set(Ch.HappyR, 1f); set(Ch.Squash, 0.02f) }
            HumoFace.Laugh -> { set(Ch.Happy, 1f); set(Ch.HappyR, 1f); set(Ch.Squash, 0.03f) }
            HumoFace.Cheer -> { set(Ch.Brow, 1f); set(Ch.BrowL, -0.25f); set(Ch.BrowR, -0.25f); set(Ch.Tilt, 3f); set(Ch.ArmL, 112f); set(Ch.ArmR, -112f) }
            HumoFace.Proud -> { set(Ch.Brow, 1f); set(Ch.BrowL, -0.22f); set(Ch.BrowR, -0.22f); set(Ch.Tilt, -4f) }
            HumoFace.Thinking -> { set(Ch.Brow, 1f); set(Ch.BrowL, -0.32f); set(Ch.TiltR, 6f); set(Ch.LookX, -0.7f); set(Ch.LookY, -0.9f); set(Ch.Tilt, -5f); set(Ch.Sway, -4f) }
            // A child whose parent is leaving: big eyes looking up, brows tilted up in the middle, a small frown, a slight droop. No tears.
            HumoFace.Sad -> { set(Ch.Brow, 1f); set(Ch.Sad, 1f); set(Ch.LookY, -1f); set(Ch.Tilt, -3f); set(Ch.Squash, 0.03f) }
            HumoFace.Cheeky -> { set(Ch.Brow, 1f); set(Ch.BrowL, -0.3f); set(Ch.BrowR, 0.08f); set(Ch.TiltR, -10f); set(Ch.LookX, 0.9f); set(Ch.OpenR, 0.8f); set(Ch.Tilt, 4f) }
            HumoFace.Surprised -> { set(Ch.Brow, 1f); set(Ch.BrowL, -0.45f); set(Ch.BrowR, -0.45f); set(Ch.EyeScale, 1.12f); set(Ch.Squash, -0.04f) }
            HumoFace.Wink -> { set(Ch.HappyR, 1f); set(Ch.Tilt, -4f) }
            HumoFace.Sleepy -> { set(Ch.OpenL, 0f); set(Ch.OpenR, 0f); set(Ch.Squash, 0.04f); set(Ch.Tilt, 3f) }
            HumoFace.Oops -> { set(Ch.Brow, 1f); set(Ch.BrowL, -0.12f); set(Ch.BrowR, -0.12f); set(Ch.TiltL, -16f); set(Ch.TiltR, 16f); set(Ch.LookY, 0.35f); set(Ch.Squash, 0.04f) }
            else -> Unit
        }
        return t
    }

    internal fun settleStill() {
        val t = targets(face)
        for (c in Ch.values()) { x[c.ordinal] = t[c.ordinal]; v[c.ordinal] = 0f }
        mouth = mouthOf(face)
        frame.longValue++
    }

    internal fun mouthOf(f: HumoFace) = when (f) {
        HumoFace.Happy, HumoFace.Laugh, HumoFace.Cheer, HumoFace.Wink -> "big"
        HumoFace.Thinking, HumoFace.Cheeky -> "smirk"
        HumoFace.Proud -> "smile"
        HumoFace.Sad -> "sad"
        HumoFace.Surprised -> "o"
        HumoFace.Sleepy -> "flat"
        HumoFace.Oops -> "frown"
        else -> when (pose) {
            HumoPose.Read, HumoPose.Laptop -> "small"
            HumoPose.Science -> "o"
            else -> "smile"
        }
    }
}

@Composable
fun rememberHumo(alive: Boolean): HumoState = remember(alive) { HumoState(alive) }

private val Spring = mapOf(
    HumoState.Ch.Squash to (320f to 14f), HumoState.Ch.Hop to (260f to 20f), HumoState.Ch.Tilt to (180f to 16f),
    HumoState.Ch.Sway to (120f to 7f), HumoState.Ch.LookX to (1400f to 75f), HumoState.Ch.LookY to (1400f to 75f),
    HumoState.Ch.Happy to (500f to 34f), HumoState.Ch.Brow to (500f to 40f), HumoState.Ch.BrowL to (500f to 30f), HumoState.Ch.TiltR to (500f to 30f),
    HumoState.Ch.HappyR to (500f to 34f), HumoState.Ch.BrowR to (500f to 30f), HumoState.Ch.TiltL to (500f to 30f),
    HumoState.Ch.ArmL to (210f to 15f), HumoState.Ch.ArmR to (210f to 15f), HumoState.Ch.Front to (400f to 40f), HumoState.Ch.LeanX to (260f to 18f),
    HumoState.Ch.OpenL to (500f to 34f), HumoState.Ch.OpenR to (500f to 34f), HumoState.Ch.EyeScale to (500f to 34f), HumoState.Ch.Sad to (500f to 34f),
)

/** One keyframe of a move: only the channels it names change; the rest fall back to the face and pose (humo-live.js MOVES). */
private class MoveKey(val ms: Int, val squash: Float = 0f, val armL: Float? = null, val armR: Float? = null, val lean: Float? = null, val tilt: Float? = null, val eyeScale: Float? = null, val mouth: String? = null)

private val Moves = mapOf(
    HumoMove.Jump to listOf(MoveKey(0, 0.12f, armL = 20f, armR = -20f), MoveKey(120, -0.12f, armL = 120f, armR = -120f, mouth = "big"), MoveKey(450, 0.14f), MoveKey(600, -0.02f), MoveKey(800)),
    HumoMove.Celebrate to listOf(MoveKey(0, 0.12f), MoveKey(120, -0.12f, armL = 135f, armR = -135f, mouth = "big"), MoveKey(450, 0.14f), MoveKey(620, -0.04f), MoveKey(800, 0.06f), MoveKey(950, 0f, armL = 120f, armR = -120f), MoveKey(1500)),
    HumoMove.Nod to listOf(MoveKey(0, 0.06f, tilt = 0f), MoveKey(160, -0.02f), MoveKey(320, 0.05f), MoveKey(480)),
    HumoMove.Shake to listOf(MoveKey(0, lean = -14f, tilt = -4f), MoveKey(120, lean = 14f, tilt = 4f), MoveKey(240, lean = -10f, tilt = -3f), MoveKey(360, lean = 8f, tilt = 2f), MoveKey(480)),
    HumoMove.Surprise to listOf(MoveKey(0, 0.08f), MoveKey(110, -0.08f, eyeScale = 1.16f, mouth = "o"), MoveKey(600)),
)
private val Air = mapOf(HumoMove.Jump to Triple(120, 330, 80f), HumoMove.Celebrate to Triple(120, 330, 88f), HumoMove.Surprise to Triple(110, 200, 30f))

/** Runs Humo's springs while anything moves, blinks now and then, and sleeps when everything has settled. */
@Composable
private fun HumoMotion(h: HumoState) {
    if (!h.alive) { LaunchedEffect(h.face) { h.settleStill() }; return }
    LaunchedEffect(h.face, h.moveSerial, h.tracking, h.talking, h.pose) {
        val t0 = withFrameNanos { it }
        var last = t0
        val face = if (h.talking) HumoFace.Talk else h.face
        val move = if (h.moveSerial != h.playedSerial) h.move else null
        h.playedSerial = h.moveSerial
        val keys = move?.let { Moves[it] }
        val air = move?.let { Air[it] }
        h.mouth = h.mouthOf(face)
        var settled = 0
        while (settled < 20) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1e9f).coerceIn(0.001f, 0.033f)
            last = now
            val ms = (now - t0) / 1_000_000f
            val tg = h.targets(face)
            val lk = h.lookSource?.invoke() ?: h.look
            if (lk != null && h.tracking && face == HumoFace.Rest) { tg[HumoState.Ch.LookX.ordinal] = lk.x; tg[HumoState.Ch.LookY.ordinal] = lk.y }
            var moving = h.tracking || face == HumoFace.Talk || h.pose != HumoPose.None
            var keyMouth: String? = null
            if (keys != null && ms <= keys.last().ms + 200) {
                moving = true
                val cur = keys.lastOrNull { ms >= it.ms }
                if (cur != null) {
                    tg[HumoState.Ch.Squash.ordinal] = cur.squash
                    cur.armL?.let { tg[HumoState.Ch.ArmL.ordinal] = it }
                    cur.armR?.let { tg[HumoState.Ch.ArmR.ordinal] = it }
                    cur.lean?.let { tg[HumoState.Ch.LeanX.ordinal] = it }
                    cur.tilt?.let { tg[HumoState.Ch.Tilt.ordinal] = it }
                    cur.eyeScale?.let { tg[HumoState.Ch.EyeScale.ordinal] = it }
                    keyMouth = cur.mouth
                }
                if (move == HumoMove.Celebrate && ms >= 120 && h.burstAt < 0) h.burstAt = now / 1_000_000
            }
            // the pose's own loop: a wave, typing, a shaken tube (humo-live.js `loop`)
            val tms = now / 1_000_000f
            when (h.pose) {
                HumoPose.Wave -> tg[HumoState.Ch.ArmR.ordinal] = -126f + 24f * sin(2f * PI.toFloat() * tms / 420f)
                HumoPose.Laptop -> { tg[HumoState.Ch.ArmL.ordinal] += 8f * sin(2f * PI.toFloat() * tms / 180f); tg[HumoState.Ch.ArmR.ordinal] -= 8f * sin(2f * PI.toFloat() * tms / 180f + 1.6f) }
                HumoPose.Science -> tg[HumoState.Ch.ArmR.ordinal] += 10f * sin(2f * PI.toFloat() * tms / 260f)
                else -> Unit
            }
            if (h.burstAt >= 0 && now / 1_000_000 - h.burstAt in 0..1400) moving = true else if (h.burstAt >= 0 && now / 1_000_000 - h.burstAt > 1400) h.burstAt = -1
            for (c in HumoState.Ch.values()) {
                if (c == HumoState.Ch.Hop) continue
                val (k, d) = Spring[c] ?: continue
                val i = c.ordinal
                val a = k * (tg[i] - h.x[i]) - d * h.v[i]
                h.v[i] += a * dt
                h.x[i] += h.v[i] * dt
                if (abs(tg[i] - h.x[i]) > 0.002f || abs(h.v[i]) > 0.02f) moving = true
            }
            if (air != null) {
                val s = (ms - air.first) / air.second
                h.x[HumoState.Ch.Hop.ordinal] = if (s in 0f..1f) 4f * air.third * s * (1f - s) else 0f
            }
            if (face == HumoFace.Talk) { val f = (ms / 110f).toInt(); h.mouth = listOf("open", "o", "smile", "big", "small", "open", "o")[(f * 7919 + (f shr 2) * 104729).mod(7)] }
            else h.mouth = keyMouth ?: h.mouthOf(face)
            h.frame.longValue = now / 1_000_000
            settled = if (moving) 0 else settled + 1
        }
        h.frame.longValue++
    }
    // Blinks (sometimes two) at random gaps while the screen is alive; never a loop the tests would wait on.
    LaunchedEffect(Unit) {
        val rnd = Random(7)
        while (true) {
            delay(2200L + rnd.nextInt(3800))
            val double = rnd.nextFloat() < 0.2f
            repeat(if (double) 2 else 1) {
                var t = 0f
                var last = withFrameNanos { it }
                while (t < 230f) {
                    val now = withFrameNanos { it }
                    t += (now - last) / 1_000_000f
                    last = now
                    h.blink = if (t < 70f) (t / 70f) * (t / 70f) else if (t < 110f) 1f else max(0f, 1f - (t - 110f) / 120f).let { it * it }
                    h.frame.longValue = now / 1_000_000
                }
                h.blink = 0f
                h.frame.longValue++
                if (double) delay(60)
            }
        }
    }
}

private val Ink = Color(0xFF191818)

private object HumoShapes {
    val body: Path by lazy { PathParser().parsePathString(HumoArt.BODY).toPath() }
    val leafBig: Path by lazy { PathParser().parsePathString(HumoArt.LEAF_BIG).toPath() }
    val leafSmall: Path by lazy { PathParser().parsePathString(HumoArt.LEAF_SMALL).toPath() }
    val smile: Path by lazy { PathParser().parsePathString(HumoArt.MOUTH_SMILE).toPath() }
    val open: Path by lazy { PathParser().parsePathString(HumoArt.MOUTH_OPEN).toPath() }
}

private const val BASE_MINT = 0xFF00AC86
private fun c(hex: Long, a: Float = 1f) = Color(hex).copy(alpha = a)

/** The rig's light layers: [centre, rotation, scale] ellipses with their stops (humo-rig.svg `light-*`). */
private class Light(val tx: Float, val ty: Float, val rot: Float, val sx: Float, val sy: Float, val stops: Array<Pair<Float, Color>>)

private val Lights = listOf(
    Light(219.2f, 183.8f, -156.5f, 620.5f, 1033.8f, arrayOf(0f to c(0xFF6BFFCB), 0.2f to c(0xFF6BFFCB), 0.4f to c(0xFF6BFFCB, 0.638f), 0.6f to c(0xFF6BFFCB, 0.721f), 0.8f to c(0xFF6BFFCB, 0.588f), 1f to c(0xFF6BFFCB, 0f))),
    Light(-302.7f, 96.3f, -66.7f, 461.7f, 2051.8f, arrayOf(0f to c(0xFF64FAC6, 0f), 0.45f to c(0xFF64FAC6, 0.022f), 0.6f to c(0xFF64FAC6), 0.7f to c(0xFF64FAC6), 0.78f to c(0xFF64FAC6, 0.83f), 0.85f to c(0xFF64FAC6, 0.628f), 0.91f to c(0xFF64FAC6, 0.365f), 0.96f to c(0xFF64FAC6, 0.116f), 1f to c(0xFF64FAC6, 0f))),
)
private val EyeLights = listOf(
    Light(344.8f, 696.9f, -9.2f, 128.7f, 73.8f, arrayOf(0f to c(0xFF75FFC9, 0.852f), 0.35f to c(0xFF75FFC9, 0.413f), 0.7f to c(0xFF75FFC9, 0f), 1f to c(0xFF75FFC9, 0f))),
    Light(662.1f, 687.3f, 13.3f, 144.5f, 75.3f, arrayOf(0f to c(0xFF7AFFCC), 0.35f to c(0xFF7AFFCC, 0.508f), 0.7f to c(0xFF7AFFCC, 0f), 1f to c(0xFF7AFFCC, 0f))),
)

private fun DrawScope.light(l: Light) {
    withTransform({ translate(l.tx, l.ty); rotate(l.rot, Offset.Zero); scale(l.sx, l.sy, Offset.Zero) }) {
        drawRect(Brush.radialGradient(*l.stops, center = Offset.Zero, radius = 1f), Offset(-4f, -4f), Size(8f, 8f))
    }
}

/** The rig's `body-paint`: the mint, the key and edge lights, the ground shade, the glow under each eye. */
private fun DrawScope.bodyPaint() {
    drawRect(c(BASE_MINT), Offset(82f, -3f), Size(856f, 961f))
    Lights.forEach { light(it) }
    drawRect(Brush.linearGradient(0f to c(0xFF008A66, 0f), 0.35f to c(0xFF008A66, 0.173f), 0.7f to c(0xFF008A66, 0.096f), 1f to c(0xFF008A66), start = Offset(571f, 458f), end = Offset(493.9f, 1033f)), Offset(82f, -3f), Size(856f, 961f))
    EyeLights.forEach { light(it) }
}

private class Eye(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val rot: Float, val px: Float, val py: Float, val prx: Float, val pry: Float, val prot: Float, val pr: Float, val glints: List<FloatArray>)

private val EyeL = Eye(345.1f, 611.3f, 95.8f, 99.5f, 35.7f, 347.3f, 612f, 67.8f, 71.5f, 37.4f, 69.6f, listOf(floatArrayOf(322.5f, 577.8f, 16.2f, 23.6f, 50.6f), floatArrayOf(387.4f, 577.2f, 7.7f, 8.1f, 128.8f)))
private val EyeR = Eye(674.5f, 611.2f, 96.7f, 98.7f, 155.1f, 672.6f, 612f, 68.1f, 70.3f, 158f, 69.2f, listOf(floatArrayOf(670.1f, 576.6f, 14.7f, 15.5f, 82.6f), floatArrayOf(712.7f, 577.2f, 7.7f, 8.1f, 128.8f)))

private fun DrawScope.ellipseOf(cx: Float, cy: Float, rx: Float, ry: Float, rot: Float, color: Color) {
    withTransform({ rotate(rot, Offset(cx, cy)) }) { drawOval(color, Offset(cx - rx, cy - ry), Size(2 * rx, 2 * ry)) }
}

private fun DrawScope.eye(e: Eye, h: HumoState, left: Boolean, layers: HumoLayers) {
    val happy = h.x[(if (left) HumoState.Ch.Happy else HumoState.Ch.HappyR).ordinal].coerceIn(0f, 1f)
    val blink = h.blink
    val side = h.x[(if (left) HumoState.Ch.OpenL else HumoState.Ch.OpenR).ordinal].coerceIn(0f, 1f)
    val lookX = h.x[HumoState.Ch.LookX.ordinal]
    val lookY = h.x[HumoState.Ch.LookY.ordinal]
    val brow = h.x[HumoState.Ch.Brow.ordinal].coerceIn(0f, 1f)
    val sad = h.x[HumoState.Ch.Sad.ordinal].coerceIn(0f, 1f)
    val bw = 0.55f * e.rx
    val byy = e.cy - 1.32f * e.ry
    if (brow > 0.01f) {
        val dy = (h.x[(if (left) HumoState.Ch.BrowL else HumoState.Ch.BrowR).ordinal] + 0.25f * sad) * e.ry
        val tilt = h.x[(if (left) HumoState.Ch.TiltL else HumoState.Ch.TiltR).ordinal] + (if (left) -16f else 16f) * sad
        withTransform({ translate(0f, dy); rotate(tilt, Offset(e.cx, byy)) }) {
            val p = Path().apply { moveTo(e.cx - bw, byy + 0.1f * e.ry); quadraticTo(e.cx, byy - 0.12f * e.ry, e.cx + bw, byy + 0.1f * e.ry) }
            drawPath(p, Ink.copy(alpha = brow), style = Stroke(0.14f * e.ry, cap = StrokeCap.Round))
        }
    }
    val r = 0.62f * e.rx
    if (happy > 0.01f) {
        val p = Path().apply { moveTo(e.cx - r, e.cy + 0.18f * e.ry); quadraticTo(e.cx, e.cy - 0.55f * e.ry, e.cx + r, e.cy + 0.18f * e.ry) }
        drawPath(p, Ink.copy(alpha = happy), style = Stroke(0.2f * e.ry, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    val open = (1f - blink) * (1f - happy) * side
    if (open >= 0.3f) {
        val yc = e.cy + 0.35f * e.ry
        val clip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(e.cx - e.rx, e.cy - e.ry, e.cx + e.rx, e.cy + e.ry)) }
        withTransform({ translate(e.cx, yc); scale(1f, max(0.05f, open), Offset.Zero); translate(-e.cx, -yc) }) {
            withTransform({ rotate(e.rot, Offset(e.cx, e.cy)) }) {
                clipPath(clip) {
                    withTransform({ rotate(-e.rot, Offset(e.cx, e.cy)) }) {
                        layerImage(layers.whites, layers.s)
                        withTransform({ translate(lookX * 0.14f * e.rx, lookY * 0.14f * e.ry) }) { layerImage(layers.pupils, layers.s) }
                    }
                }
            }
        }
    }
    if (open < 0.3f && happy < 0.5f) {
        val p = Path().apply { moveTo(e.cx - r, e.cy + 0.3f * e.ry); quadraticTo(e.cx, e.cy + 0.62f * e.ry, e.cx + r, e.cy + 0.3f * e.ry) }
        drawPath(p, Ink, style = Stroke(0.2f * e.ry, cap = StrokeCap.Round))
    }
}

private fun DrawScope.mouth(kind: String) {
    val sm = HumoShapes.smile.getBounds()
    val mcx = sm.left + sm.width / 2f
    val mw = sm.width
    val mh = sm.height
    when (kind) {
        "smile" -> drawPath(HumoShapes.smile, Ink)
        "open" -> drawPath(HumoShapes.open, Ink)
        "big" -> drawPath(Path().apply {
            moveTo(sm.left - .06f * mw, sm.top + .1f * mh)
            cubicTo(sm.left + .25f * mw, sm.top + .32f * mh, sm.left + .75f * mw, sm.top + .32f * mh, sm.left + 1.06f * mw, sm.top + .1f * mh)
            cubicTo(sm.left + 1.02f * mw, sm.top + 2.1f * mh, sm.left - .02f * mw, sm.top + 2.1f * mh, sm.left - .06f * mw, sm.top + .1f * mh)
            close()
        }, Ink)
        "sad" -> drawPath(Path().apply { moveTo(mcx - .3f * mw, sm.top + .95f * mh); quadraticTo(mcx, sm.top - .35f * mh, mcx + .3f * mw, sm.top + .95f * mh) }, Ink, style = Stroke(0.42f * mh, cap = StrokeCap.Round))
        "flat" -> drawPath(Path().apply { moveTo(mcx - .22f * mw, sm.top + .5f * mh); lineTo(mcx + .22f * mw, sm.top + .5f * mh) }, Ink, style = Stroke(0.42f * mh, cap = StrokeCap.Round))
        "frown" -> drawPath(Path().apply { moveTo(mcx - .3f * mw, sm.top + .9f * mh); quadraticTo(mcx, sm.top - .1f * mh, mcx + .3f * mw, sm.top + .9f * mh) }, Ink, style = Stroke(0.42f * mh, cap = StrokeCap.Round))
        "o" -> drawOval(Ink, Offset(509.4f - 23.2f, 685.5f - 30.1f), Size(46.4f, 60.2f))
        "small" -> drawPath(Path().apply { moveTo(mcx - .26f * mw, sm.top + .3f * mh); quadraticTo(mcx, sm.top + .9f * mh, mcx + .26f * mw, sm.top + .3f * mh) }, Ink, style = Stroke(0.42f * mh, cap = StrokeCap.Round))
        "smirk" -> drawPath(Path().apply { moveTo(mcx - .34f * mw, sm.top + .55f * mh); quadraticTo(mcx + .1f * mw, sm.top + .75f * mh, mcx + .4f * mw, sm.top + .05f * mh) }, Ink, style = Stroke(0.42f * mh, cap = StrokeCap.Round))
        else -> drawPath(HumoShapes.smile, Ink)
    }
}

private const val SHOULDER_Y = 611.3f + 1.05f * 99.5f

/** The arm nub of the rig (humo-live.js `armShape`): body mint at the root, lit at the tip, hung from the shoulder. */
private object ArmShape {
    private val bb get() = HumoShapes.body.getBounds()
    val al get() = 0.27f * bb.width
    val aw get() = 0.11f * bb.width
    val ar get() = 0.15f * bb.width
    val leftX get() = bb.left + 0.09f * bb.width + 0.02f * bb.width
    val rightX get() = bb.right - 0.09f * bb.width - 0.02f * bb.width
    val path: Path by lazy {
        val al = al; val aw = aw; val ar = ar
        Path().apply {
            moveTo(-ar / 2, 0f)
            cubicTo(-ar / 2, al * .45f, -aw / 2, al * .6f, -aw / 2, al - aw / 2)
            arcTo(androidx.compose.ui.geometry.Rect(-aw / 2, al - aw, aw / 2, al), 180f, -180f, false)
            cubicTo(aw / 2, al * .6f, ar / 2, al * .45f, ar / 2, 0f)
            close()
        }
    }
}

private fun DrawScope.arms(h: HumoState, behind: Boolean) {
    val paint = Brush.verticalGradient(0f to c(BASE_MINT), 1f to c(0xFF6EF0BE), startY = 0f, endY = ArmShape.al)
    for ((ch, px) in listOf(HumoState.Ch.ArmL to ArmShape.leftX, HumoState.Ch.ArmR to ArmShape.rightX)) {
        val ang = h.x[ch.ordinal]
        if (behind && abs(ang) < 8f) continue
        withTransform({ rotate(ang, Offset(px, SHOULDER_Y)); translate(px, SHOULDER_Y) }) {
            drawPath(ArmShape.path, paint)
            if (ch == HumoState.Ch.ArmR && h.pose == HumoPose.Science) tube()
        }
    }
}

/** The test tube at the end of the right arm (arm-local coordinates). */
private fun DrawScope.tube() {
    val tl = 0.3f * HumoShapes.body.getBounds().width
    val top = ArmShape.al - 6f
    withTransform({ rotate(180f, Offset(0f, top)) }) {
        drawRoundRect(Color.White, Offset(-18f, top), Size(36f, tl), androidx.compose.ui.geometry.CornerRadius(14f))
        drawRoundRect(Ink, Offset(-18f, top), Size(36f, tl), androidx.compose.ui.geometry.CornerRadius(14f), style = Stroke(5f))
        drawRoundRect(c(BASE_MINT), Offset(-14f, top), Size(28f, tl * 0.45f), androidx.compose.ui.geometry.CornerRadius(11f))
    }
}

private fun DrawScope.book() {
    val b = HumoShapes.body.getBounds()
    val cx = b.left + b.width / 2
    val bw = 0.42f * b.width
    val bh = 0.2f * b.width
    val bt = b.bottom - 0.26f * b.height
    drawPath(Path().apply { moveTo(cx, bt + 10); lineTo(cx - bw / 2, bt); lineTo(cx - bw / 2, bt + bh); lineTo(cx, bt + bh + 10); lineTo(cx + bw / 2, bt + bh); lineTo(cx + bw / 2, bt); close() }, Ink)
    drawPath(Path().apply { moveTo(cx, bt + 4); lineTo(cx - bw / 2 + 12, bt - 6); lineTo(cx - bw / 2 + 12, bt + bh - 12); lineTo(cx, bt + bh - 2); lineTo(cx + bw / 2 - 12, bt + bh - 12); lineTo(cx + bw / 2 - 12, bt - 6); close() }, Color(0xFFFBFAF6))
}

private fun DrawScope.laptop() {
    val b = HumoShapes.body.getBounds()
    val cx = b.left + b.width / 2
    val lw = 0.5f * b.width
    val lt = b.bottom - 0.2f * b.height
    val lh = 0.2f * b.height
    drawRoundRect(Color(0xFFFBFCFD), Offset(cx - lw / 2, lt), Size(lw, lh), androidx.compose.ui.geometry.CornerRadius(14f))
    drawRoundRect(Ink, Offset(cx - lw / 2, lt), Size(lw, lh), androidx.compose.ui.geometry.CornerRadius(14f), style = Stroke(6f))
    drawRoundRect(Ink, Offset(cx - lw / 2 - 16, lt + lh - 8), Size(lw + 32, 22f), androidx.compose.ui.geometry.CornerRadius(10f))
    drawCircle(c(BASE_MINT), 12f, Offset(cx, lt + lh / 2))
}

private val ConfettiColours = listOf(Color(0xFF23A06C), Color(0xFF3DC98C), Color(0xFF66DDA8), Color(0xFFA9F0CF), Color.White, Ink)

/** The celebrate burst (humo-live.js confetti): 26 bits thrown up from his head and falling, gone after 1.4 s. */
private fun DrawScope.confetti(h: HumoState) {
    if (h.burstAt < 0) return
    val u = (h.frame.longValue - h.burstAt) / 1000f
    if (u < 0f || u > 1.4f) return
    val b = HumoShapes.body.getBounds()
    withTransform({ translate(0f, 0f) }) {
        for (i in 0 until 26) {
            val a = -PI.toFloat() / 2 + (i / 25f - .5f) * 2.4f
            val v = 700f + 500f * ((i * 37) % 11) / 11f
            val spin = (((i * 53) % 7) - 3) * 160f
            val px = b.left + b.width / 2 + cos(a) * v * u * 0.55f
            val py = b.top + 120f + sin(a) * v * u * 0.55f + 900f * u * u
            withTransform({ translate(px, py); rotate(spin * u, Offset.Zero) }) {
                drawRoundRect(ConfettiColours[i % ConfettiColours.size].copy(alpha = max(0f, 1f - u / 1.4f)), Offset.Zero, Size(18f, 30f), androidx.compose.ui.geometry.CornerRadius(5f))
            }
        }
    }
}

private const val HEAD = 1024f * 0.14f

/** The parts that never change shape, painted once into bitmaps at the drawn size (the light layers are slow to paint every frame on a cheap GPU). */
private class HumoLayers(val s: Float, val body: ImageBitmap, val hair: ImageBitmap, val whites: ImageBitmap, val pupils: ImageBitmap)

private fun DrawScope.layerImage(img: ImageBitmap, s: Float) {
    withTransform({ translate(0f, -HEAD); scale(1f / s, 1f / s, Offset.Zero) }) { drawImage(img) }
}

private fun androidx.compose.ui.draw.CacheDrawScope.bakeLayers(): HumoLayers {
    val vbH = 1024f * 1.14f
    val s = min(size.width / 1024f, size.height / vbH).coerceAtLeast(0.01f)
    val w = max(1, (1024f * s).roundToInt())
    val h = max(1, (vbH * s).roundToInt())
    fun layer(block: DrawScope.() -> Unit): ImageBitmap {
        val bmp = ImageBitmap(w, h)
        CanvasDrawScope().draw(this, layoutDirection, androidx.compose.ui.graphics.Canvas(bmp), Size(w.toFloat(), h.toFloat())) {
            withTransform({ scale(s, s, Offset.Zero); translate(0f, HEAD) }) { block() }
        }
        return bmp
    }
    return HumoLayers(
        s,
        body = layer { clipPath(HumoShapes.body) { bodyPaint() } },
        hair = layer { clipPath(HumoShapes.leafBig) { bodyPaint() }; clipPath(HumoShapes.leafSmall) { bodyPaint() } },
        whites = layer {
            for (e in listOf(EyeL, EyeR)) withTransform({ rotate(e.rot, Offset(e.cx, e.cy)); translate(e.cx, e.cy); scale(e.rx, e.ry, Offset.Zero) }) {
                drawOval(Brush.radialGradient(0f to c(0xFFFCFCFC), 0.72f to c(0xFFFCFCFC), 0.8f to c(0xFFFEFEFE), 0.92f to c(0xFFF8F8F8), 0.96f to c(0xFFEEEEEE), 1f to c(0xFFDDE5E2), center = Offset.Zero, radius = 1f), Offset(-1f, -1f), Size(2f, 2f))
            }
        },
        pupils = layer {
            for (e in listOf(EyeL, EyeR)) {
                withTransform({ rotate(e.prot, Offset(e.px, e.py)) }) {
                    drawOval(Brush.radialGradient(0.083f to c(0xFF0B0706), 0.25f to c(0xFF0E0706), 0.583f to c(0xFF180F0E), 0.917f to c(0xFF2E2321), center = Offset(e.px, e.py), radius = e.pr), Offset(e.px - e.prx, e.py - e.pry), Size(2 * e.prx, 2 * e.pry))
                }
                e.glints.forEach { g -> ellipseOf(g[0], g[1], g[2], g[3], g[4], Color.White) }
            }
        },
    )
}

/** The body, sprout and face in one picture; the view box is the rig's with 14 % headroom for jumps (humo-live.js). */
private fun DrawScope.humoPicture(h: HumoState, layers: HumoLayers) {
    // Squash kept to 35 %: a soft body, never a flattened one (Boss).
    val sq = 0.35f * h.x[HumoState.Ch.Squash.ordinal]
    val sy = 1f - sq
    val sx = 1f + sq * 0.5f
    val hop = h.x[HumoState.Ch.Hop.ordinal]
    val tilt = h.x[HumoState.Ch.Tilt.ordinal]
    val fx = 509.4f
    val fy = 917.5f
    val k = max(0.5f, 1f - hop / 300f)
    withTransform({ translate(501.3f, 915.7f); scale(k * sx, k, Offset.Zero); translate(-501.3f, -915.7f) }) {
        withTransform({ translate(501.3f, 915.7f); scale(357.3f, 32.4f, Offset.Zero) }) {
            drawOval(Brush.radialGradient(0f to Color(0x2A000000), 0.55f to Color(0x16000000), 1f to Color(0x00000000), center = Offset.Zero, radius = 1f), Offset(-1f, -1f), Size(2f, 2f))
        }
    }
    val lean = h.x[HumoState.Ch.LeanX.ordinal]
    val front = h.x[HumoState.Ch.Front.ordinal] > 0.5f
    withTransform({ translate(lean, -hop); rotate(tilt, Offset(fx, fy)); translate(fx, fy); scale(sx, sy, Offset.Zero); translate(-fx, -fy) }) {
        if (!front) arms(h, behind = true)
        withTransform({ rotate(h.x[HumoState.Ch.Sway.ordinal], Offset(536.6f, 253.1f)) }) { layerImage(layers.hair, layers.s) }
        layerImage(layers.body, layers.s)
        for ((e, left) in listOf(EyeL to true, EyeR to false)) {
            val es = h.x[HumoState.Ch.EyeScale.ordinal]
            withTransform({ scale(es, es, Offset(e.cx, e.cy)) }) { eye(e, h, left, layers) }
        }
        mouth(h.mouth)
        when (h.pose) {
            HumoPose.Read -> book()
            HumoPose.Laptop -> laptop()
            else -> Unit
        }
        if (front) arms(h, behind = false)
    }
    confetti(h)
}

/** Humo, drawn from the rig's own paths and moved by [state]. [modifier] sets the size; the picture is fitted and centred. */
@Composable
fun Humo(state: HumoState, modifier: Modifier = Modifier) {
    HumoMotion(state)
    Spacer(
        modifier.drawWithCache {
            val layers = bakeLayers()
            val vbH = 1024f * 1.14f
            val s = min(size.width / 1024f, size.height / vbH)
            onDrawBehind {
                state.frame.longValue
                withTransform({ translate((size.width - 1024f * s) / 2f, (size.height - vbH * s)); scale(s, s, Offset.Zero); translate(0f, HEAD) }) { humoPicture(state, layers) }
            }
        },
    )
}

/** Where a machine at ([x], [y]) in a [w] x [h] board is, as Humo (bottom left of the screen) looks at it: right and up. */
internal fun humoLookAt(x: Float, y: Float, w: Float, h: Float) = Offset((x / w * 1.1f).coerceIn(-1f, 1f), (-(0.9f - 0.5f * (y / h))).coerceIn(-1f, 1f))
