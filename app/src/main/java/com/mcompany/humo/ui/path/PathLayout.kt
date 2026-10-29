package com.mcompany.humo.ui.path

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class StoneState { Done, Current, Locked }

fun stoneState(level: Int, current: Int): StoneState = when {
    level < current -> StoneState.Done
    level == current -> StoneState.Current
    else -> StoneState.Locked
}

/** A point on the map, in dp from the top-left of the content. */
data class Pt(val x: Float, val y: Float) {
    fun dist(o: Pt) = hypot(x - o.x, y - o.y)
}

/** A flagstone of the trail: centre, radius and a seed for its shape. */
data class Flagstone(val at: Pt, val r: Float, val seed: Int)

/** A footbridge where the trail crosses the river: centre and the trail's direction there (radians, y down). */
data class Bridge(val at: Pt, val angle: Float)

/** A low-poly tree or bush beside the trail. */
data class Tree(val at: Pt, val size: Float, val pine: Boolean)

/** A low-poly cloud over the locked part; [depth] 0 = near (moves more), 1 = far. */
data class Cloud(val at: Pt, val width: Float, val seed: Int, val depth: Float)

/** Where the SR-71 is at a moment of its loop: position, heading (radians, y down) and bank (-1 left … 1 right). */
data class PlaneFix(val at: Pt, val heading: Float, val bank: Float, val altitude: Float)

/**
 * The map of one course, in dp, inside a content box [height] tall and [width] wide (PHASE-PATH-V2): level 1 at the
 * bottom, a walkable trail of flagstones climbing through every level's island, a river that meets the trail only at
 * the footbridges after each unit, trees on the free ground, low-poly clouds over the locked levels and the SR-71's
 * loop. Pure numbers: tested on the JVM, the screen only draws them.
 */
class PathLayout(val count: Int, val width: Float) {
    val gap = 168f
    val top = 320f
    val bottom = 220f
    val height = top + (count - 1) * gap + bottom
    private val swing = (width * 0.24f).coerceAtMost(110f)

    /** The trail's centre at stone [i] (0-based; -1 and [count] are the ends off the screen). */
    fun x(i: Int): Float = width / 2f + swing * sin(i * 0.95f + 0.6f)

    /** [i] is 0-based: 0 = level 1, at the bottom. */
    fun y(i: Int): Float = when {
        i < 0 -> height + 40f
        i >= count -> -40f
        else -> height - bottom - i * gap
    }

    fun stone(i: Int) = Pt(x(i), y(i))

    /**
     * The scroll offset that puts stone [i] at thumb height: 55 % down the [viewport] above the tab bar, the easy reach of a
     * one-handed grip.
     */
    fun scrollFor(i: Int, viewport: Float): Float = (y(i) - viewport * THUMB).coerceIn(0f, (height - viewport).coerceAtLeast(0f))

    // ---- The trail: one cubic per hop, vertical tangents at each island, so it walks out of each stone upward. ----

    /** Point [t] (0..1) on the hop from stone [i] to stone [i + 1]; i = -1 is the hop in from the bottom edge. */
    fun trail(i: Int, t: Float): Pt {
        val a = stone(i); val b = stone(i + 1)
        val mid = (a.y + b.y) / 2f
        val u = 1f - t
        val x = u * u * u * a.x + 3 * u * u * t * a.x + 3 * u * t * t * b.x + t * t * t * b.x
        val y = u * u * u * a.y + 3 * u * u * t * mid + 3 * u * t * t * mid + t * t * t * b.y
        return Pt(x, y)
    }

    /** Hops whose middle is a footbridge: after levels 3, 7 and 11 (the unit ends), where the river crosses. */
    val bridgeHops: List<Int> = BRIDGE_AFTER.map { it - 1 }.filter { it + 1 < count }

    val bridges: List<Bridge> = bridgeHops.map { i ->
        val a = stone(i); val b = stone(i + 1)
        Bridge(trail(i, 0.5f), atan2(0.75f * (b.y - a.y), 1.5f * (b.x - a.x)))
    }

    /** Flagstones every ~16 dp along the whole trail, in pairs side by side; none on an island or a bridge. */
    val flagstones: List<Flagstone> = buildList {
        var seed = 0
        for (i in -1 until count) {
            val steps = 40
            var walked = 0f
            var prev = trail(i, 0f)
            for (s in 1..steps) {
                val p = trail(i, s / steps.toFloat())
                walked += p.dist(prev)
                if (walked >= STONE_STEP) {
                    walked = 0f
                    val dx = p.x - prev.x; val dy = p.y - prev.y
                    val len = hypot(dx, dy).coerceAtLeast(0.001f)
                    val nx = -dy / len; val ny = dx / len
                    val side = if (seed % 2 == 0) 7f else -7f
                    val at = Pt(p.x + nx * side, p.y + ny * side)
                    val clearOfIsland = (0 until count).none { at.dist(stone(it)) < ISLAND_CLEAR }
                    val clearOfBridge = bridges.none { at.dist(it.at) < BRIDGE_CLEAR }
                    if (clearOfIsland && clearOfBridge) add(Flagstone(at, 8.5f + (seed * 7 % 5) * 0.5f, seed))
                    seed++
                }
                prev = p
            }
        }
    }

    // ---- The river: beside the trail, on the side that flips at each footbridge. ----

    private fun side(i: Int): Float {
        val flips = bridgeHops.count { it < i }
        return if (flips % 2 == 0) 1f else -1f
    }

    /** The river's control points from the bottom edge to the top edge: off to one side of each island, and through each bridge. */
    val riverPoints: List<Pt> = buildList {
        fun beside(i: Int): Pt {
            val x = (x(i) + side(i) * RIVER_OFFSET).coerceIn(RIVER_HALF + 6f, width - RIVER_HALF - 6f)
            return Pt(x, y(i))
        }
        add(Pt(beside(0).x, height + 40f))
        for (i in 0 until count) {
            add(beside(i))
            bridges.firstOrNull { bridgeHops[bridges.indexOf(it)] == i }?.let { add(it.at) }
        }
        add(Pt(beside(count - 1).x, -40f))
    }

    /** The river as a smooth line (Catmull-Rom through [riverPoints]), sampled every few dp. */
    val river: List<Pt> = buildList {
        val p = riverPoints
        for (k in 0 until p.size - 1) {
            val p0 = p[(k - 1).coerceAtLeast(0)]; val p1 = p[k]; val p2 = p[k + 1]; val p3 = p[(k + 2).coerceAtMost(p.size - 1)]
            val n = 16
            for (s in 0 until n) {
                val t = s / n.toFloat(); val t2 = t * t; val t3 = t2 * t
                fun cr(a: Float, b: Float, c: Float, d: Float) = 0.5f * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3)
                add(Pt(cr(p0.x, p1.x, p2.x, p3.x), cr(p0.y, p1.y, p2.y, p3.y)))
            }
        }
        add(p.last())
    }

    private val trailSamples: List<Pt> = (-1 until count).flatMap { i -> (0..20).map { trail(i, it / 20f) } }

    /** Pines and bushes on the free ground between the hops: away from the trail, the river and the screen edge. */
    val trees: List<Tree> = buildList {
        for (i in -1 until count) {
            val y0 = (y(i) + y(i + 1)) / 2f
            var placed = 0
            for ((k, fx) in listOf(0.1f, 0.9f, 0.2f, 0.8f).withIndex()) {
                if (placed == 2) break
                val at = Pt(width * fx, y0 + (if (k % 2 == 0) -18f else 22f))
                if (at.y < 60f || at.y > height - 20f) continue
                val free = trailSamples.none { it.dist(at) < 44f } && river.none { it.dist(at) < RIVER_HALF + 24f } &&
                    (0 until count).none { stone(it).dist(at) < 70f } && none { it.at.dist(at) < 50f }
                if (free) {
                    add(Tree(at, if ((i + k) % 3 == 0) 30f else 38f, pine = (i + k) % 3 != 0))
                    placed++
                }
            }
        }
    }

    /**
     * Clouds over the levels not reached yet: two per row, rows 120 dp apart, so the locked islands peek between them;
     * the first row sits well above the current island, and a fuller row of three closes the top (the part of the
     * world not built yet).
     */
    fun clouds(current: Int): List<Cloud> = buildList {
        var y = y((current - 1).coerceIn(0, count - 1)) - gap * 0.85f
        var row = 0
        while (y > -20f) {
            val xs = when {
                y < 150f -> listOf(0.15f, 0.5f, 0.85f)
                row == 0 -> listOf(0.14f, 0.86f)
                row % 2 == 1 -> listOf(0.3f, 0.9f)
                else -> listOf(0.1f, 0.68f)
            }
            xs.forEachIndexed { k, fx ->
                val seed = row * 7 + k * 3
                val w = 130f + (seed * 17 % 5) * 10f
                add(Cloud(Pt(width * fx, y + ((seed * 11) % 7 - 3) * 4f), w, seed, depth = (seed % 3) / 2f))
            }
            y -= 120f
            row++
        }
    }

    /**
     * The SR-71's lazy figure-eight over the clouds, above the current stone (never over the stone the child is about
     * to tap). One loop every [LOOP_S] seconds; heading and bank come from the curve itself.
     */
    fun plane(current: Int, seconds: Float, scrollDp: Float? = null, viewport: Float? = null): PlaneFix {
        val c = (current - 1).coerceIn(0, count - 1)
        val home = if (c >= count - 2) y(c) + gap * 1.55f else y(c) - gap * 1.6f
        // Kept on screen (checker fix 5): between the top bar and 170 dp above thumb height, where the current stone opens.
        val cy = if (scrollDp == null || viewport == null) home else {
            val lo = scrollDp + 110f
            val hi = scrollDp + viewport * THUMB - 170f
            val kept = if (hi >= lo) home.coerceIn(lo, hi) else lo
            // Never over the stone the child is about to tap, wherever they have scrolled it.
            if (abs(kept - y(c)) >= 170f) kept else if (y(c) - 170f >= scrollDp + 60f) y(c) - 170f else y(c) + 170f
        }
        val cx = width / 2f
        val rx = width * 0.3f
        val ry = 54f
        val a = (seconds / LOOP_S) * 2f * PI.toFloat()
        fun at(a: Float) = Pt(cx + rx * sin(a), cy + ry * sin(2f * a))
        val p = at(a)
        val q = at(a + 0.01f)
        val heading = atan2(q.y - p.y, q.x - p.x)
        val r = at(a + 0.02f)
        val turn = atan2(r.y - q.y, r.x - q.x) - heading
        val wrapped = ((turn + PI.toFloat()) % (2f * PI.toFloat()) + 2f * PI.toFloat()) % (2f * PI.toFloat()) - PI.toFloat()
        val bank = (wrapped * 40f).coerceIn(-1f, 1f)
        return PlaneFix(p, heading, bank, altitude = 0.5f + 0.5f * abs(cos(a)))
    }

    companion object {
        const val THUMB = 0.55f
        const val STONE_STEP = 15f
        const val ISLAND_CLEAR = 46f
        const val BRIDGE_CLEAR = 26f
        const val RIVER_OFFSET = 88f
        const val RIVER_HALF = 20f
        const val LOOP_S = 22f
        /** Levels that close a unit; the trail crosses the river on a footbridge after each. */
        val BRIDGE_AFTER = listOf(3, 7, 11)
    }
}
