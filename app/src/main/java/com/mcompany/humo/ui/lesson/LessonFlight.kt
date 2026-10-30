package com.mcompany.humo.ui.lesson

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.animation.core.CubicBezierEasing
import com.mcompany.humo.ui.path.Jet
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** One low-poly smoke puff: where it was born (board px), when, and its random spin and drift. */
internal class Puff(val x: Float, val y: Float, val born: Long, val spin: Float, val jx: Float, val jy: Float)

/** A curved trip of a machine: a quadratic curve from [from] to [to] over [durMs]; [climb] lifts it off the ground. */
internal class Flight(val from: Offset, val to: Offset, val ctrl: Offset, val t0: Long, val durMs: Long, val climb: Boolean)

/**
 * One machine on a board (fly-layer.js's `craft`): its place in board px, its heading in degrees clockwise from
 * nose-up, its altitude 0 ... 1, and what it leaves behind. Plain mutable state, stepped once per frame and read in draw.
 */
internal class Craft(var pos: Offset, var deg: Float = 0f) {
    var alt = 0f
    var speed = 0f
    var flight: Flight? = null
    /** The heading a tap-then-tap flight turns the nose to before it moves. */
    var aim: Float? = null
    /** The heading the last few pixels of motion point at; the nose eases toward it. */
    var want: Float? = null
    var anchor: Offset? = null
    var lane = 1f
    var puffAt = 0L
    var burstAt = -10_000L
    var fadeAt = -10_000L
    val puffs = ArrayList<Puff>()
    /** [deg] in the radians [Jet] uses (y down, 0 = moving right). */
    val heading: Float get() = Math.toRadians(deg - 90.0).toFloat()
}

/** The maths of the machines' motion, as the web layer measured it (docs: fly-layer.js tick / fly / launch). */
internal object Flying {
    val InOut = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
    const val PUFF_MS = 900L

    /** Cubic ease in-out, 0 ... 1. */
    fun ease(t: Float): Float = if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f

    /** The short way round from [a] to [b], degrees. */
    fun angDiff(a: Float, b: Float): Float {
        var d = b - a
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return d
    }

    /** The heading in degrees (clockwise from nose-up) of the way from [from] to [to]; 0 when they coincide. */
    fun bearing(from: Offset, to: Offset): Float {
        val dx = to.x - from.x
        val dy = to.y - from.y
        if (dx == 0f && dy == 0f) return 0f
        return Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
    }

    /** A flight's length in ms for a trip of [distDp] dp: slow and calm, never over 3.6 s. */
    fun duration(distDp: Float): Long = min(3600f, 2000f + distDp * 5f).toLong()

    /** Altitude on the way: 0 on the ground, 1 at the top of the climb, back to 0 at touch-down. */
    fun altitude(u: Float): Float = sin(PI.toFloat() * min(1f, u * 1.05f)).coerceAtLeast(0f).pow(0.7f)

    /**
     * Starts [c] on a curved trip to [to] and returns its length in ms (0 = already there). The curve bows to one side,
     * alternating between machines. [climb] false = a quiet slide on the ground (Start over), 500 ms.
     */
    fun plan(c: Craft, to: Offset, now: Long, density: Float, climb: Boolean = true): Long {
        val from = c.pos
        val dist = hypot(to.x - from.x, to.y - from.y)
        if (dist < 2f) return 0L
        c.lane = -c.lane
        val side = c.lane * 0.22f * dist
        val nx = -(to.y - from.y) / dist
        val ny = (to.x - from.x) / dist
        val dur = if (climb) duration(dist / density) else 500L
        c.flight = Flight(from, to, Offset((from.x + to.x) / 2f + nx * side, (from.y + to.y) / 2f + ny * side), now, dur, climb)
        return dur
    }

    /** One frame of the flight; true on the frame it touches down. */
    fun fly(c: Craft, now: Long, dtMs: Float, cell: Float): Boolean {
        val f = c.flight ?: return false
        val u = ((now - f.t0).toFloat() / f.durMs).coerceIn(0f, 1f)
        val e = ease(u)
        val q = 1f - e
        c.alt = if (f.climb) altitude(u) else 0f
        val x = q * q * f.from.x + 2f * q * e * f.ctrl.x + e * e * f.to.x
        val y = q * q * f.from.y + 2f * q * e * f.ctrl.y + e * e * f.to.y
        val dx = x - c.pos.x
        val dy = y - c.pos.y
        val step = hypot(dx, dy)
        // the nose banks into the arc: it follows the way it moves, a quarter of the gap each 60 Hz frame
        if (step > 0.3f) c.deg += angDiff(c.deg, bearing(Offset.Zero, Offset(dx, dy))) * (1f - 0.75f.pow(dtMs / 16.7f))
        c.pos = Offset(x, y)
        c.speed = min(1f, step * (16.7f / dtMs) / (cell * 0.06f))
        if (u >= 1f) { c.flight = null; c.alt = 0f; c.speed = 0f; return true }
        return false
    }

    /**
     * One frame of a machine glued to Brilliant's drag handle ([handle], board px): it sits exactly on the handle (no lag),
     * and only the nose eases toward the direction of the last [minMove] px of motion while [steering] (a finger drag or a
     * flight). Afterburners and smoke belong to a real flight ([flying]) only. Returns true while anything still moves.
     */
    fun glue(c: Craft, handle: Offset, dtMs: Float, now: Long, cell: Float, size: Float, minMove: Float, steering: Boolean, flying: Boolean, rnd: Random): Boolean {
        val k0 = 1f - 0.84f.pow(dtMs / 16.7f)
        val moved = hypot(handle.x - c.pos.x, handle.y - c.pos.y)
        val a = c.anchor ?: handle
        c.anchor = a
        val mx = handle.x - a.x
        val my = handle.y - a.y
        if (hypot(mx, my) > minMove) {
            if (steering) c.want = bearing(Offset.Zero, Offset(mx, my))
            c.anchor = handle
        }
        c.aim?.let { c.want = it }
        c.want?.let { c.deg += angDiff(c.deg, it) * min(1f, k0 * 0.5f) }
        c.pos = handle
        c.speed = if (flying) min(1f, moved * (16.7f / dtMs) / (cell * 0.06f)) else 0f
        puff(c, now, size, flying, rnd)
        val turning = c.aim?.let { abs(angDiff(c.deg, it)) > 1f } ?: false
        return moved > 0.5f || turning || c.puffs.isNotEmpty()
    }

    /** Low-poly smoke, one trail per engine: a puff each 90 ms while [on] and moving; old ones are dropped. */
    fun puff(c: Craft, now: Long, size: Float, on: Boolean, rnd: Random) {
        c.puffs.removeAll { now - it.born > PUFF_MS }
        if (!on || c.speed < 0.08f || now - c.puffAt < 90L) return
        c.puffAt = now
        for (n in Jet.nozzles(c.heading, size)) {
            c.puffs.add(Puff(c.pos.x + n.x, c.pos.y + n.y + 2f, now, (rnd.nextFloat() - 0.5f) * 90f, (rnd.nextFloat() - 0.5f) * 6f, (rnd.nextFloat() - 0.5f) * 6f))
        }
    }
}

private val PuffLight = Color(0xFFF4F6F8)
private val PuffShade = Color(0xFFC9CED6)

/** A hexagon of radius 1, the low-poly puff. */
private val Hexagon = Path().apply {
    moveTo(0f, -1f); lineTo(0.86f, -0.5f); lineTo(0.86f, 0.5f); lineTo(0f, 1f); lineTo(-0.86f, 0.5f); lineTo(-0.86f, -0.5f); close()
}

/** The smoke: each puff grows from 0.55 to 1.7 of [r], turns a little, drifts and fades over [Flying.PUFF_MS]. */
internal fun DrawScope.drawPuffs(list: List<Puff>, now: Long, r: Float) {
    for (p in list) {
        val u = ((now - p.born).toFloat() / Flying.PUFF_MS).coerceIn(0f, 1f)
        val e = 1f - (1f - u) * (1f - u)
        val k = r * (0.55f + 1.15f * e)
        val brush = Brush.linearGradient(0.48f to PuffLight, 0.52f to PuffShade, start = Offset(-1f, -1f), end = Offset(1f, 1f))
        translate(p.x + p.jx * e, p.y + p.jy * e) {
            rotate(p.spin * e, Offset.Zero) {
                scale(k, k, Offset.Zero) { drawPath(Hexagon, brush, alpha = 0.85f * (1f - u)) }
            }
        }
    }
}

/**
 * The words name the learner's machine (Boss: "not 'a shape' but the SR-71"): only plain text changes, the maths words
 * (translate, target, coordinates) stay. fly-layer.js WORDS.
 */
internal object Words {
    const val MACHINE = "SR-71"
    private val rules = listOf(
        Regex("\\b[Aa] point is translated\\b") to "The $MACHINE flies",
        Regex("\\bthe points\\b") to "the $MACHINE" + "s",
        Regex("\\bThe points\\b") to "The $MACHINE" + "s",
        Regex("\\b(?:a|the) point\\b") to "the $MACHINE",
        Regex("\\b(?:A|The) point\\b") to "The $MACHINE",
    )

    fun machine(text: String): String = rules.fold(text) { t, (re, to) -> re.replace(t, to) }
}

/** The 3-second Mach 3 pass at the end of a lesson: bottom-left to top-right on one curve (fly-layer.js finale). */
internal object MachPassPath {
    const val SECONDS = 3f

    /** The pass's place and heading (degrees from nose-up) at [t] 0 ... 1 on a [w] x [h] screen. */
    fun at(t: Float, w: Float, h: Float): Pair<Offset, Float> {
        val p0 = Offset(-0.15f * w, 0.92f * h)
        val p1 = Offset(0.55f * w, 0.75f * h)
        val p2 = Offset(1.25f * w, -0.1f * h)
        val q = 1f - t
        val x = q * q * p0.x + 2f * q * t * p1.x + t * t * p2.x
        val y = q * q * p0.y + 2f * q * t * p1.y + t * t * p2.y
        val tx = 2f * q * (p1.x - p0.x) + 2f * t * (p2.x - p1.x)
        val ty = 2f * q * (p1.y - p0.y) + 2f * t * (p2.y - p1.y)
        return Offset(x, y) to Flying.bearing(Offset.Zero, Offset(tx, ty))
    }
}
