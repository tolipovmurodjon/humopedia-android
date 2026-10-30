package com.mcompany.humo.ui.path

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.mcompany.humo.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The SR-71 as 64 pre-rendered 3D headings in ONE 8 x 8 atlas of 160 px frames (our own Blender render, the Clash of
 * Clans method; Boss 2026-10-30: 16 headings jumped every 22.5 deg when circling). Frame k faces k x 5.625 deg clockwise
 * from nose-up; only the small rest (under 2.8 deg) is rotated, so a turn only moves the window over the atlas.
 * Banking is a small squash across the wings. The render's camera looks down at 50 deg, so ground offsets along the
 * screen's y shrink by [TILT]; the nozzles are placed through the same squash.
 */
internal object Jet {
    const val HEADINGS = 64
    const val COLUMNS = 8
    const val FRAME_PX = 160
    private const val STEP = 360f / HEADINGS

    /** sin(50 deg): the render's vertical foreshortening. */
    private const val TILT = 0.766f
    /** In a nose-up frame: the nozzles' half spread and the tail's distance below the centre, as fractions of the frame. */
    private const val NOZZLE_X = 0.111f
    private const val TAIL_Y = 0.36f

    private val ShadowTint = ColorFilter.tint(Color(0xFF1F5E57), BlendMode.SrcIn)
    /** Shadow strength (Boss on alpha05 saw "2 ones": the old 18 % sharp copy read as a second jet). */
    const val SHADOW_ALPHA = 0.12f

    /** The lesson's afterburner: a radial #FFFBE6 -> #FFD45C -> #FF8A2A that fades out. */
    private val FlameStops = arrayOf(0f to Color(0xFFFFFBE6), 0.18f to Color(0xFFFFFBE6), 0.32f to Color(0xFFFFD45C), 0.55f to Color(0xFFFF8A2A), 0.75f to Color(0x00FF501E))

    /** The fractional frame (0 <= f < 64) for [heading] in radians, y down, 0 = moving right; clockwise from nose-up. */
    fun frameAt(heading: Float): Float {
        val f = (Math.toDegrees((heading + PI / 2).toDouble()) / STEP).toFloat()
        return ((f % HEADINGS) + HEADINGS) % HEADINGS
    }

    /** The nearest whole frame. */
    fun frameOf(heading: Float): Int = frameAt(heading).roundToInt() % HEADINGS

    /** What is left of [heading] after the nearest frame, in degrees (at most half a step). */
    fun rest(heading: Float): Float { val f = frameAt(heading); return (f - f.roundToInt()) * STEP }

    private fun angleOf(f: Float) = (f * STEP) * PI.toFloat() / 180f

    /** A point given in nose-up frame units (x right, y toward the tail), placed at frame angle [a], squashed by [sx]. */
    private fun place(a: Float, x: Float, y: Float, size: Float, sx: Float): Offset {
        val px = x * sx * size; val py = y * size
        return Offset(px * cos(a) - py * sin(a), (px * sin(a) + py * cos(a)) * TILT)
    }

    /** Where the two nozzles are for [heading], from the jet's centre (px). */
    fun nozzles(heading: Float, size: Float): List<Offset> {
        val a = angleOf(frameAt(heading))
        return listOf(place(a, -NOZZLE_X, TAIL_Y, size, 1f), place(a, NOZZLE_X, TAIL_Y, size, 1f))
    }

    /**
     * The jet centred on [c] (px), its frame [size] px square. [bank] -1 ... 1, [roll] 0 ... 1 the celebratory roll's
     * progress (0 = none); [t] drives the flame flicker; [flames] 0 ... 1 how much the afterburners burn.
     */
    fun DrawScope.jet(atlas: ImageBitmap, c: Offset, size: Float, heading: Float, bank: Float, roll: Float, t: Float, motion: Boolean, flames: Float = 1f) {
        val f = frameAt(heading)
        val a = angleOf(f)
        val sx = (1f - 0.12f * abs(bank).coerceAtMost(1f)) * (if (roll > 0f) cos(2f * PI.toFloat() * roll) else 1f)
        if (flames > 0.01f) for ((i, side) in listOf(-1f, 1f).withIndex()) {
            // The lesson's flame is 0.11 x 0.26 of the jet's length, starting at the nozzle's lip, under the jet.
            val n = c + place(a, side * NOZZLE_X, TAIL_Y - 0.02f, size, sx)
            val flick = if (motion) 0.88f + 0.12f * sin(t * 37f + i * 2.1f) + 0.06f * sin(t * 61f + i) else 1f
            val r = size * 0.075f
            withTransform({ translate(n.x, n.y); scale(1f, TILT, Offset.Zero); rotate(f * STEP, Offset.Zero); scale(0.62f, (0.35f + 1.25f * flames) * flick, Offset.Zero) }) {
                drawCircle(Brush.radialGradient(*FlameStops, center = Offset(0f, r * 0.3f), radius = r * 1.05f), r, Offset(0f, r * 0.72f))
            }
        }
        sprite(atlas, c, size, a, sx, 1f, null, turn = rest(heading))
    }

    /**
     * The jet's shadow flat on the ground in deep teal, lower-right of the jet: soft (nine copies a few dp apart in one
     * layer) and faint ([SHADOW_ALPHA]), so it reads as a shadow and never as a second jet. [altitude] 0 ... 1 drops it
     * further away from the jet.
     */
    fun DrawScope.jetShadow(atlas: ImageBitmap, c: Offset, size: Float, heading: Float, bank: Float, roll: Float, altitude: Float, dp: Float) {
        val f = frameAt(heading)
        val sx = (1f - 0.12f * abs(bank).coerceAtMost(1f)) * (if (roll > 0f) abs(cos(2f * PI.toFloat() * roll)) else 1f)
        val at = c + Offset((10f + 12f * altitude) * dp, (16f + 18f * altitude) * dp)
        val s = size * (0.9f - 0.08f * altitude)
        val soft = (3f + 3f * altitude) * dp
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(at.x - s, at.y - s, at.x + s, at.y + s), Paint().apply { alpha = SHADOW_ALPHA })
            for (i in 0 until 9) {
                val o = if (i == 8) Offset.Zero else Offset(soft * cos(i * PI.toFloat() / 4f), soft * sin(i * PI.toFloat() / 4f))
                sprite(atlas, at + o, s, angleOf(f), sx, 0.25f, ShadowTint, turn = rest(heading))
            }
            canvas.restore()
        }
    }

    /**
     * The atlas window of the frame facing [angle], centred; [sx] squashes it across the wings (the frame's own axis);
     * [turn] (degrees, never more than half a frame step) aligns the frame with the exact heading.
     */
    private fun DrawScope.sprite(atlas: ImageBitmap, c: Offset, size: Float, angle: Float, sx: Float, alpha: Float, filter: ColorFilter?, mode: BlendMode = BlendMode.SrcOver, turn: Float = 0f) {
        val deg = angle * 180f / PI.toFloat()
        val k = ((deg / STEP).roundToInt() % HEADINGS + HEADINGS) % HEADINGS
        withTransform({
            translate(c.x, c.y)
            rotate(turn, Offset.Zero)
            rotate(deg, Offset.Zero)
            scale(sx, 1f, Offset.Zero)
            rotate(-deg, Offset.Zero)
        }) {
            val s = size.roundToInt()
            drawImage(
                atlas,
                srcOffset = IntOffset((k % COLUMNS) * FRAME_PX, (k / COLUMNS) * FRAME_PX),
                srcSize = IntSize(FRAME_PX, FRAME_PX),
                dstOffset = IntOffset(-s / 2, -s / 2),
                dstSize = IntSize(s, s),
                alpha = alpha,
                colorFilter = filter,
                blendMode = mode,
                filterQuality = FilterQuality.Medium,
            )
        }
    }

    /** Where the tail is for [heading], from the jet's centre (px): the smoke trail starts there. */
    fun tail(heading: Float, size: Float): Offset = place(angleOf(frameAt(heading)), 0f, TAIL_Y + 0.08f, size, 1f)

    private val Smoke = Color(0xFF787C84)

    /** A soft smoke trail: [past] tail points, newest first; each puff older, bigger and fainter. */
    fun DrawScope.smoke(past: List<Offset>, dp: Float) {
        past.forEachIndexed { i, p ->
            val age = (i + 1f) / (past.size + 1f)
            translate(p.x, p.y) { drawCircle(Smoke, (2.5f + 7f * age) * dp, Offset.Zero, alpha = 0.26f * (1f - age)) }
        }
    }
}

/** The one sprite atlas, decoded once per composition. */
@androidx.compose.runtime.Composable
internal fun rememberJetAtlas(): ImageBitmap = androidx.compose.ui.res.imageResource(R.drawable.sr71_atlas64)

/**
 * The jet's heading as a spring (Boss on alpha05: "turning sharp, not smooth"): each frame it moves a share of the way
 * to the wanted heading, never more than [maxDegPerFrame], always the short way round. Plain state, read in draw.
 */
class HeadingSpring(private val maxDegPerFrame: Float = 6f, private val share: Float = 0.22f) {
    var value = Float.NaN
        private set

    /** Steps toward [target] (radians) over [frames] 60 Hz frames and returns the new heading. */
    fun step(target: Float, frames: Float = 1f): Float {
        if (value.isNaN()) { value = target; return value }
        val d = wrap(target - value)
        val max = Math.toRadians(maxDegPerFrame.toDouble()).toFloat() * frames
        val k = 1f - Math.pow((1f - share).toDouble(), frames.toDouble()).toFloat()
        value = wrap(value + (d * k).coerceIn(-max, max))
        return value
    }

    fun snapTo(heading: Float) { value = heading }

    companion object {
        fun wrap(a: Float): Float {
            val tau = 2f * PI.toFloat()
            return ((a + PI.toFloat()) % tau + tau) % tau - PI.toFloat()
        }
    }
}
