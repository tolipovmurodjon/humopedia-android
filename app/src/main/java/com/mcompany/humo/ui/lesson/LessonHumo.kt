package com.mcompany.humo.ui.lesson

import androidx.compose.foundation.Canvas
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
import kotlin.math.sin

/** What Humo's face shows (humo-live.js FACES: the ones the lesson uses). */
enum class HumoFace { Rest, Happy, Thinking, Cheer, Laugh, Proud, Talk }

/** The one-shot moves (humo-live.js MOVES). */
enum class HumoMove { Jump, Celebrate, Nod }

/**
 * Humo's animation state, springs and all (humo-live.js ported): the screen sets the [face] and calls [play]; the
 * drawing reads [frame] so only the Canvas redraws. [look] is where the machine is, -1..1 each way (null = ahead).
 * When [alive] is false nothing runs by itself: a still pose per face, for the screenshot tests and reduced motion.
 */
@Stable
class HumoState(val alive: Boolean) {
    var face by mutableStateOf(HumoFace.Rest)
    var look: Offset? = null
    /** Read every frame while [tracking]; the lesson points it at the flow's machine. */
    var lookSource: (() -> Offset?)? = null
    /** True while the machine flies: Humo's eyes follow [look] every frame instead of settling. */
    var tracking by mutableStateOf(false)
    val frame = mutableLongStateOf(0L)
    internal var moveSerial by mutableStateOf(0)
    internal var move: HumoMove? = null
    internal var playedSerial = 0

    internal val x = FloatArray(Ch.values().size)
    internal val v = FloatArray(Ch.values().size)
    internal var blink = 0f
    internal var mouth = "smile"

    fun play(m: HumoMove) { move = m; moveSerial++ }

    internal enum class Ch { Squash, Hop, Tilt, Sway, LookX, LookY, Happy, Brow, BrowL, TiltR }

    internal fun settleStill() {
        val t = targets(face)
        for (c in Ch.values()) { x[c.ordinal] = t[c.ordinal]; v[c.ordinal] = 0f }
        mouth = mouthOf(face)
        frame.longValue++
    }

    internal fun targets(f: HumoFace): FloatArray {
        val t = FloatArray(Ch.values().size)
        fun set(c: Ch, value: Float) { t[c.ordinal] = value }
        when (f) {
            HumoFace.Happy -> { set(Ch.Happy, 1f); set(Ch.Squash, 0.02f) }
            HumoFace.Laugh -> { set(Ch.Happy, 1f); set(Ch.Squash, 0.03f) }
            HumoFace.Cheer -> { set(Ch.Brow, 1f); set(Ch.BrowL, -0.25f); set(Ch.Tilt, 3f) }
            HumoFace.Proud -> { set(Ch.Brow, 1f); set(Ch.BrowL, -0.22f); set(Ch.Tilt, -4f) }
            HumoFace.Thinking -> { set(Ch.Brow, 1f); set(Ch.BrowL, -0.32f); set(Ch.TiltR, 6f); set(Ch.LookX, -0.7f); set(Ch.LookY, -0.9f); set(Ch.Tilt, -5f); set(Ch.Sway, -4f) }
            else -> Unit
        }
        return t
    }

    internal fun mouthOf(f: HumoFace) = when (f) {
        HumoFace.Happy, HumoFace.Laugh, HumoFace.Cheer -> "big"
        HumoFace.Thinking -> "smirk"
        HumoFace.Proud -> "smile"
        else -> "smile"
    }
}

@Composable
fun rememberHumo(alive: Boolean): HumoState = remember(alive) { HumoState(alive) }

private val Spring = mapOf(
    HumoState.Ch.Squash to (320f to 14f), HumoState.Ch.Hop to (260f to 20f), HumoState.Ch.Tilt to (180f to 16f),
    HumoState.Ch.Sway to (120f to 7f), HumoState.Ch.LookX to (1400f to 75f), HumoState.Ch.LookY to (1400f to 75f),
    HumoState.Ch.Happy to (500f to 34f), HumoState.Ch.Brow to (500f to 40f), HumoState.Ch.BrowL to (500f to 30f), HumoState.Ch.TiltR to (500f to 30f),
)

/** Keyframes [ms, squash, hop apex or -1] of each move (humo-live.js MOVES + AIR); the springs do the in-betweens. */
private val Moves = mapOf(
    HumoMove.Jump to listOf(0 to 0.12f, 120 to -0.12f, 450 to 0.14f, 600 to -0.02f, 800 to 0f),
    HumoMove.Celebrate to listOf(0 to 0.12f, 120 to -0.12f, 450 to 0.14f, 620 to -0.04f, 800 to 0.06f, 950 to 0f),
    HumoMove.Nod to listOf(0 to 0.06f, 160 to -0.02f, 320 to 0.05f, 480 to 0f),
)
private val Air = mapOf(HumoMove.Jump to Triple(120, 330, 80f), HumoMove.Celebrate to Triple(120, 330, 88f))

/** Runs Humo's springs while anything moves, blinks now and then, and sleeps when everything has settled. */
@Composable
private fun HumoMotion(h: HumoState) {
    if (!h.alive) { LaunchedEffect(h.face) { h.settleStill() }; return }
    LaunchedEffect(h.face, h.moveSerial, h.tracking) {
        val t0 = withFrameNanos { it }
        var last = t0
        val face = h.face
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
            var moving = h.tracking
            if (keys != null && ms <= keys.last().first + 200) {
                moving = true
                val cur = keys.lastOrNull { ms >= it.first }
                if (cur != null) tg[HumoState.Ch.Squash.ordinal] = cur.second
            }
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
            h.mouth = if (move == HumoMove.Jump || move == HumoMove.Celebrate) (if (ms < 700) "big" else h.mouthOf(face)) else h.mouthOf(face)
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

private fun DrawScope.eye(e: Eye, h: HumoState, left: Boolean) {
    val happy = h.x[HumoState.Ch.Happy.ordinal].coerceIn(0f, 1f)
    val blink = h.blink
    val lookX = h.x[HumoState.Ch.LookX.ordinal]
    val lookY = h.x[HumoState.Ch.LookY.ordinal]
    val brow = h.x[HumoState.Ch.Brow.ordinal].coerceIn(0f, 1f)
    val bw = 0.55f * e.rx
    val byy = e.cy - 1.32f * e.ry
    if (brow > 0.01f) {
        val dy = (if (left) h.x[HumoState.Ch.BrowL.ordinal] else 0f) * e.ry
        val tilt = if (left) 0f else h.x[HumoState.Ch.TiltR.ordinal]
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
    val open = (1f - blink) * (1f - happy)
    if (open > 0.15f) {
        val yc = e.cy + 0.35f * e.ry
        withTransform({ translate(e.cx, yc); scale(1f, max(0.05f, open), Offset.Zero); translate(-e.cx, -yc) }) {
            withTransform({ rotate(e.rot, Offset(e.cx, e.cy)); translate(e.cx, e.cy); scale(e.rx, e.ry, Offset.Zero) }) {
                drawOval(Brush.radialGradient(0f to c(0xFFFCFCFC), 0.72f to c(0xFFFCFCFC), 0.8f to c(0xFFFEFEFE), 0.92f to c(0xFFF8F8F8), 0.96f to c(0xFFEEEEEE), 1f to c(0xFFDDE5E2), center = Offset.Zero, radius = 1f), Offset(-1f, -1f), Size(2f, 2f))
            }
            val clip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(e.cx - e.rx, e.cy - e.ry, e.cx + e.rx, e.cy + e.ry)) }
            withTransform({ rotate(e.rot, Offset(e.cx, e.cy)) }) {
                clipPath(clip) {
                    withTransform({ rotate(-e.rot, Offset(e.cx, e.cy)); translate(lookX * 0.14f * e.rx, lookY * 0.14f * e.ry) }) {
                        withTransform({ rotate(e.prot, Offset(e.px, e.py)) }) {
                            drawOval(Brush.radialGradient(0.083f to c(0xFF0B0706), 0.25f to c(0xFF0E0706), 0.583f to c(0xFF180F0E), 0.917f to c(0xFF2E2321), center = Offset(e.px, e.py), radius = e.pr), Offset(e.px - e.prx, e.py - e.pry), Size(2 * e.prx, 2 * e.pry))
                        }
                        e.glints.forEach { g -> ellipseOf(g[0], g[1], g[2], g[3], g[4], Color.White) }
                    }
                }
            }
        }
    }
    if (open <= 0.15f && happy < 0.5f) {
        val p = Path().apply { moveTo(e.cx - r * 0.8f, e.cy + 0.3f * e.ry); quadraticTo(e.cx, e.cy + 0.5f * e.ry, e.cx + r * 0.8f, e.cy + 0.3f * e.ry) }
        drawPath(p, Ink, style = Stroke(0.16f * e.ry, cap = StrokeCap.Round))
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
        "smirk" -> drawPath(Path().apply { moveTo(mcx - .34f * mw, sm.top + .55f * mh); quadraticTo(mcx + .1f * mw, sm.top + .75f * mh, mcx + .4f * mw, sm.top + .05f * mh) }, Ink, style = Stroke(0.42f * mh, cap = StrokeCap.Round))
        else -> drawPath(HumoShapes.smile, Ink)
    }
}

/** The body, sprout and face in one picture; the view box is the rig's with 14 % headroom for jumps (humo-live.js). */
private fun DrawScope.humoPicture(h: HumoState) {
    val sq = h.x[HumoState.Ch.Squash.ordinal]
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
    withTransform({ translate(0f, -hop); rotate(tilt, Offset(fx, fy)); translate(fx, fy); scale(sx, sy, Offset.Zero); translate(-fx, -fy) }) {
        withTransform({ rotate(h.x[HumoState.Ch.Sway.ordinal], Offset(536.6f, 253.1f)) }) {
            clipPath(HumoShapes.leafBig) { bodyPaint() }
            clipPath(HumoShapes.leafSmall) { bodyPaint() }
        }
        clipPath(HumoShapes.body) { bodyPaint() }
        eye(EyeL, h, true)
        eye(EyeR, h, false)
        mouth(h.mouth)
    }
}

/** Humo, drawn from the rig's own paths and moved by [state]. [modifier] sets the size; the picture is fitted and centred. */
@Composable
fun Humo(state: HumoState, modifier: Modifier = Modifier) {
    HumoMotion(state)
    Canvas(modifier) {
        state.frame.longValue
        val vbH = 1024f * 1.14f
        val s = min(size.width / 1024f, size.height / vbH)
        withTransform({ translate((size.width - 1024f * s) / 2f, (size.height - vbH * s)); scale(s, s, Offset.Zero); translate(0f, 1024f * 0.14f) }) { humoPicture(state) }
    }
}

/** Where a machine at ([x], [y]) in a [w] x [h] board is, as Humo (bottom left of the screen) looks at it: right and up. */
internal fun humoLookAt(x: Float, y: Float, w: Float, h: Float) = Offset((x / w * 1.1f).coerceIn(-1f, 1f), (-(0.9f - 0.5f * (y / h))).coerceIn(-1f, 1f))
