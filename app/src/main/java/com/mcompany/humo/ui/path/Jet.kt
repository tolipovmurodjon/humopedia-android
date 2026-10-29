package com.mcompany.humo.ui.path

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
 * The SR-71 as 16 pre-rendered 3D frames (Boss 2026-10-29 "go sprite", the Clash of Clans method):
 * docs/design/2026-10-28-app-redesign/round-07-models/sr71-sprites, rendered from sr71.blend. Frame k faces k × 22.5°
 * clockwise from nose-up; the bitmap is never rotated (a rotated render looked upside down), only the nearest frame
 * is picked. Banking is a small squash across the wings. The render's camera looks down at a tilt, so up-down
 * distances in a frame are 0.82 of left-right ones: the nozzles are placed through the same squash.
 */
internal object Jet {
    val Frames = listOf(
        R.drawable.sr71_00, R.drawable.sr71_01, R.drawable.sr71_02, R.drawable.sr71_03,
        R.drawable.sr71_04, R.drawable.sr71_05, R.drawable.sr71_06, R.drawable.sr71_07,
        R.drawable.sr71_08, R.drawable.sr71_09, R.drawable.sr71_10, R.drawable.sr71_11,
        R.drawable.sr71_12, R.drawable.sr71_13, R.drawable.sr71_14, R.drawable.sr71_15,
    )

    /** The render's vertical foreshortening (measured: tail 0.35 of the frame below centre nose-up, 0.43 nose-right). */
    private const val TILT = 0.82f
    /** In a nose-up frame: the nozzles' half spread and the tail's distance below the centre, as fractions of the frame. */
    private const val NOZZLE_X = 1.35f / 12.5f
    private const val TAIL_Y = 0.42f

    private val ShadowTint = ColorFilter.tint(Color(0xFF1F5E57), BlendMode.SrcIn)
    private val Smoke = Color(0xFF787C84)

    /** The lesson's afterburner: a radial #FFFBE6 → #FFD45C → #FF8A2A that fades out. */
    private val FlameStops = arrayOf(0f to Color(0xFFFFFBE6), 0.18f to Color(0xFFFFFBE6), 0.32f to Color(0xFFFFD45C), 0.55f to Color(0xFFFF8A2A), 0.75f to Color(0x00FF501E))

    /** [heading] is radians, y down, 0 = moving right; the frame's angle counts clockwise from nose-up. */
    fun frameOf(heading: Float): Int {
        val deg = Math.toDegrees((heading + PI / 2).toDouble())
        return (((deg / 22.5).roundToInt() % 16) + 16) % 16
    }

    private fun frameAngle(k: Int) = (k * 22.5f) * PI.toFloat() / 180f

    /** A point given in nose-up frame units (x right, y toward the tail), placed for frame [k], squashed by [sx] across the wings. */
    private fun place(k: Int, x: Float, y: Float, size: Float, sx: Float): Offset {
        val a = frameAngle(k)
        val px = x * sx * size; val py = y * size
        return Offset(px * cos(a) - py * sin(a), (px * sin(a) + py * cos(a)) * TILT)
    }

    /**
     * One frame of the jet centred on [c] (px), its frame [size] px square. [bank] -1 … 1, [roll] 0 … 1 the celebratory
     * roll's progress (0 = none); [t] drives the flame flicker.
     */
    fun DrawScope.jet(frames: List<ImageBitmap>, c: Offset, size: Float, heading: Float, bank: Float, roll: Float, t: Float, motion: Boolean) {
        val k = frameOf(heading)
        val a = frameAngle(k)
        val sx = (1f - 0.12f * abs(bank).coerceAtMost(1f)) * (if (roll > 0f) cos(2f * PI.toFloat() * roll) else 1f)
        // Two afterburners at the nozzles, pointing back along the frame's heading, under the jet.
        for ((i, side) in listOf(-1f, 1f).withIndex()) {
            // The lesson's flame is 0.11 × 0.26 of the jet's length: here ~8 × 20 dp, starting at the nozzle's lip.
            val n = c + place(k, side * NOZZLE_X, TAIL_Y - 0.02f, size, sx)
            val flick = if (motion) 0.88f + 0.12f * sin(t * 37f + i * 2.1f) + 0.06f * sin(t * 61f + i) else 1f
            val r = size * 0.075f
            withTransform({ translate(n.x, n.y); scale(1f, TILT, Offset.Zero); rotate(k * 22.5f, Offset.Zero); scale(0.62f, 1.6f * flick, Offset.Zero) }) {
                drawCircle(Brush.radialGradient(*FlameStops, center = Offset(0f, r * 0.3f), radius = r * 1.05f), r, Offset(0f, r * 0.72f))
            }
        }
        sprite(frames[k], c, size, a, sx, alpha = 1f, filter = null)
    }

    /** The same frame flat on the ground in 18 % teal: lower-right of the jet, further and smaller the higher it flies. */
    fun DrawScope.jetShadow(frames: List<ImageBitmap>, c: Offset, size: Float, heading: Float, bank: Float, roll: Float, altitude: Float, dp: Float) {
        val k = frameOf(heading)
        val sx = (1f - 0.12f * abs(bank).coerceAtMost(1f)) * (if (roll > 0f) abs(cos(2f * PI.toFloat() * roll)) else 1f)
        val at = c + Offset((16f + 20f * altitude) * dp, (26f + 32f * altitude) * dp)
        sprite(frames[k], at, size * (0.9f - 0.08f * altitude), frameAngle(k), sx, alpha = 0.18f, filter = ShadowTint)
    }

    /** The bitmap as rendered, centred; [sx] squashes it across the wings (the frame's own axis), never turns it. */
    private fun DrawScope.sprite(image: ImageBitmap, c: Offset, size: Float, angle: Float, sx: Float, alpha: Float, filter: ColorFilter?) {
        val deg = angle * 180f / PI.toFloat()
        withTransform({
            translate(c.x, c.y)
            rotate(deg, Offset.Zero)
            scale(sx, 1f, Offset.Zero)
            rotate(-deg, Offset.Zero)
        }) {
            val s = size.roundToInt()
            drawImage(
                image,
                dstOffset = IntOffset(-s / 2, -s / 2),
                dstSize = IntSize(s, s),
                alpha = alpha,
                colorFilter = filter,
                filterQuality = FilterQuality.Medium,
            )
        }
    }

    /** Where the tail is for [heading], from the jet's centre (px): the smoke trail starts there. */
    fun tail(heading: Float, size: Float): Offset = place(frameOf(heading), 0f, TAIL_Y + 0.08f, size, 1f)

    /** A soft smoke trail: [past] tail points, newest first; each puff older, bigger and fainter. */
    fun DrawScope.smoke(past: List<Offset>, dp: Float) {
        past.forEachIndexed { i, p ->
            val age = (i + 1f) / (past.size + 1f)
            translate(p.x, p.y) { drawCircle(Smoke, (2.5f + 7f * age) * dp, Offset.Zero, alpha = 0.26f * (1f - age)) }
        }
    }
}
