package com.mcompany.humo.ui.path

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The lesson's SR-71 (docs/courses/15-coordinate-transformations/lessons/fly-layer.js, ART.sr71, viewBox -30 -60 60 120,
 * nose up), seen straight from above like the rest of the 2.5D map (Boss 2026-10-29 13:01: "it should fly directly
 * like it is a 2.5D world, not 3D"). Code-drawn, so it is equally sharp on every phone. Banking is a small squash of the
 * wings plus the lit and shaded halves trading places, never a 3D flip.
 */
internal object Jet {
    /** Nose to tail in art units (-58 … 44). */
    const val ART_LEN = 102f

    private fun poly(vararg xy: Float) = Path().apply {
        moveTo(xy[0], xy[1])
        var i = 2
        while (i < xy.size) { lineTo(xy[i], xy[i + 1]); i += 2 }
        close()
    }

    private val wingL = poly(0f, -58f, -4f, -30f, -9f, -6f, -26f, 30f, -26f, 38f, -12f, 36f, -12f, 44f, 0f, 40f)
    private val wingR = poly(0f, -58f, 4f, -30f, 9f, -6f, 26f, 30f, 26f, 38f, 12f, 36f, 12f, 44f, 0f, 40f)
    private val nacelleL = poly(-16f, 0f, -11f, 0f, -11f, 50f, -16f, 50f)
    private val nacelleR = poly(11f, 0f, 16f, 0f, 16f, 50f, 11f, 50f)
    private val finL = poly(-15f, 22f, -15f, 34f, -18f, 40f)
    private val finR = poly(15f, 22f, 15f, 34f, 18f, 40f)
    private val canopy = poly(-1.6f, -44f, 0f, -50f, 1.6f, -44f, 1.4f, -36f, -1.4f, -36f)
    private val stripe = poly(-2f, -10f, 2f, -10f, 1f, 30f, -1f, 30f)
    private val silhouette = listOf(wingL, wingR, nacelleL, nacelleR, finL, finR)

    // The lesson's real blacks: the half facing the light (top-left) is lighter.
    private val LitWing = Color(0xFF2B2E35)
    private val ShadeWing = Color(0xFF17191E)
    private val LitNacelle = Color(0xFF33373F)
    private val ShadeNacelle = Color(0xFF1E2026)
    private val Fin = Color(0xFF40444D)
    private val Canopy = Color(0xFF9DB4C8)
    private val Stripe = Color(0xFFB23A2E).copy(alpha = 0.55f)
    private val ShadowTeal = Color(0xFF1F5E57)
    private val Smoke = Color(0xFF787C84)

    /** The lesson's afterburner: a radial #FFFBE6 → #FFD45C → #FF8A2A that fades out. */
    private val FlameStops = arrayOf(0f to Color(0xFFFFFBE6), 0.18f to Color(0xFFFFFBE6), 0.32f to Color(0xFFFFD45C), 0.55f to Color(0xFFFF8A2A), 0.75f to Color(0x00FF501E))

    /**
     * One frame of the jet at [c] (px), [lenPx] long, nose along [heading] (radians, y down). [bank] -1 … 1 (+ = turning
     * clockwise); [roll] 0 … 1 is the celebratory barrel roll's progress (0 = none). [t] drives the flame flicker.
     */
    fun DrawScope.jet(c: Offset, lenPx: Float, heading: Float, bank: Float, roll: Float, t: Float, motion: Boolean) {
        val u = lenPx / ART_LEN
        val rollX = if (roll > 0f) cos(2f * PI.toFloat() * roll) else 1f
        val sx = (1f - 0.18f * abs(bank)) * rollX
        // Into a right turn the left wing rises toward the light: it brightens and the right one darkens (and back).
        val b = bank.coerceIn(-1f, 1f)
        val left = if (b >= 0f) lerp(LitWing, Color(0xFF3A3E47), b) else lerp(LitWing, ShadeWing, -b)
        val right = if (b >= 0f) lerp(ShadeWing, Color(0xFF0F1114), b) else lerp(ShadeWing, LitWing, -b)
        val leftN = if (b >= 0f) LitNacelle else lerp(LitNacelle, ShadeNacelle, -b)
        val rightN = if (b >= 0f) ShadeNacelle else lerp(ShadeNacelle, LitNacelle, -b)
        withTransform({
            translate(c.x, c.y)
            rotateRad(heading + PI.toFloat() / 2f, Offset.Zero)
            scale(u * sx, u, Offset.Zero)
        }) {
            // Two afterburners from the nacelle ends, flickering like the lesson's.
            for ((i, nx) in listOf(-13.5f, 13.5f).withIndex()) {
                val flick = if (motion) 0.88f + 0.12f * sin(t * 37f + i * 2.1f) + 0.06f * sin(t * 61f + i) else 1f
                val r = 9f
                withTransform({ translate(nx, 49f); scale(0.48f, 1.25f * flick, Offset.Zero) }) {
                    drawCircle(Brush.radialGradient(*FlameStops, center = Offset(0f, r * 0.3f), radius = r * 1.05f), r, Offset(0f, r * 0.72f))
                }
            }
            drawPath(wingL, left)
            drawPath(wingR, right)
            drawPath(nacelleL, leftN)
            drawPath(nacelleR, rightN)
            drawPath(finL, Fin)
            drawPath(finR, Fin)
            drawPath(canopy, Canopy)
            drawPath(stripe, Stripe)
        }
    }

    /** The same silhouette flat on the ground in 18 % teal: lower-right of the jet, further the higher it flies. */
    fun DrawScope.jetShadow(c: Offset, lenPx: Float, heading: Float, bank: Float, roll: Float, altitude: Float, k: Float) {
        val u = lenPx / ART_LEN * (0.9f - 0.08f * altitude)
        val rollX = if (roll > 0f) abs(cos(2f * PI.toFloat() * roll)) else 1f
        val off = Offset((16f + 20f * altitude) * k, (26f + 32f * altitude) * k)
        withTransform({
            translate(c.x + off.x, c.y + off.y)
            rotateRad(heading + PI.toFloat() / 2f, Offset.Zero)
            scale(u * (1f - 0.18f * abs(bank)) * rollX, u, Offset.Zero)
        }) {
            silhouette.forEach { drawPath(it, ShadowTeal, alpha = 0.18f) }
        }
    }

    /** A soft smoke trail: [past] tail points, newest first; each puff older, bigger and fainter. */
    fun DrawScope.smoke(past: List<Offset>, k: Float) {
        past.forEachIndexed { i, p ->
            val age = (i + 1f) / (past.size + 1f)
            translate(p.x, p.y) { drawCircle(Smoke, (2.5f + 7f * age) * k, Offset.Zero, alpha = 0.26f * (1f - age)) }
        }
    }
}
