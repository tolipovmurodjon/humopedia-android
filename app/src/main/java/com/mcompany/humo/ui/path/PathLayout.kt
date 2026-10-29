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

/** One row of the cloud bank: its cloud centre line [y], cloud width, spacing, sideways speed (dp/s), seed and row index. */
data class BankRow(val y: Float, val width: Float, val spacing: Float, val speed: Float, val seed: Int, val depth: Int)

/** Where the SR-71 is at a moment of its loop: position, heading (radians, y down) and bank (-1 left … 1 right). */
data class PlaneFix(val at: Pt, val heading: Float, val bank: Float, val altitude: Float)

/** The jet's lap: an ellipse round ([cx], [cy]), flown clockwise on screen from its top. */
data class Lane(val cx: Float, val cy: Float, val rx: Float, val ry: Float) {
    fun at(a: Float) = Pt(cx + rx * sin(a), cy - ry * cos(a))
    /** The direction of travel at [a] (radians, y down, 0 = right). */
    fun heading(a: Float): Float = if (rx == 0f && ry == 0f) -PI.toFloat() / 2f else atan2(ry * sin(a), rx * cos(a))
}

/**
 * The map of one course, in dp, inside a content box [height] tall and [width] wide (PHASE-PATH-V2): level 1 at the
 * bottom, a walkable trail of flagstones climbing through every level's island, a river that meets the trail only at
 * the footbridges after each unit, trees on the free ground, a low-poly cloud bank above the story and the SR-71's
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
    fun scrollFor(i: Int, viewport: Float, cut: Float = 0f): Float =
        (y(i) - cut - viewport * THUMB).coerceIn(0f, (height - cut - viewport).coerceAtLeast(0f))

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

    /** The highest island still in the open: the current one and the next [STORY] locked ones are the visible story. */
    fun storyTop(current: Int): Int = ((current - 1).coerceIn(0, count - 1) + STORY).coerceAtMost(count - 1)

    /** The cloud bank's lower edge (dp): halfway between the last visible island and the one above it, or the end of the world. */
    fun bankEdge(current: Int): Float = y(storyTop(current)) - gap * 0.55f

    /**
     * Where the scrollable world starts (dp from the top of the full map): [BANK_SHOWN] above the bank's edge, so the top
     * of the scroll is the bank itself and nothing above it can be scrolled to.
     */
    fun cut(current: Int): Float = (bankEdge(current) - BANK_SHOWN).coerceAtLeast(0f)

    /**
     * The low-poly cloud bank over everything above the story (Boss 2026-10-29: "clouds fixed starting low, whole covered
     * above"): rows of clouds from the bank's edge up past the top of the scroll, the front row lowest; every row drifts
     * sideways at its own speed and wraps around, so the bank moves but never comes down onto an island.
     */
    fun bank(current: Int): List<BankRow> = buildList {
        val edge = bankEdge(current)
        val stop = cut(current) - 140f
        var y = edge
        var r = 0
        while (y > stop) {
            // Back rows are bigger and packed 40 dp apart, so each row's base hides behind the domes of the row in front
            // (no grey shelves): only the front row shows its underside.
            val w = if (r == 0) 156f else 170f + (r % 3) * 12f
            // A cloud's underside reaches 0.085 w below its centre; the front row's sits on the edge.
            val centre = y - w * 0.09f
            val dir = if (r % 2 == 0) 1f else -1f
            add(BankRow(centre, w, spacing = 104f, speed = dir * (5f + 2.5f * (r % 5)), seed = r * 11 + 3, depth = r))
            y -= 40f
            r++
        }
    }

    /**
     * The cloud centres (dp) of [row] at time [seconds]: they slide at the row's speed and wrap off-screen, never popping.
     * Each cloud sits up to 12 dp higher than its row (never lower), so the bank's lower edge is lumpy, not a ruler line.
     */
    fun bankClouds(row: BankRow, seconds: Float): List<Pt> {
        val n = (width / row.spacing).toInt() + 3
        val period = n * row.spacing
        return (0 until n).map { j ->
            val raw = (j * row.spacing + row.speed * seconds + (row.seed % 7) * 13f) % period
            Pt((if (raw < 0f) raw + period else raw) - row.spacing, row.y - ((row.seed + j * 7) % 4) * 4f)
        }
    }

    /** An island's no-fly radius (dp): its art's half width plus 24 dp, around EVERY island (Boss on alpha05). */
    fun noFly(i: Int, current: Int): Float = (if (i == current - 1) CURRENT_R else ISLAND_R) + NO_FLY_MARGIN

    /**
     * The jet's own lane (Boss on alpha05: "the SR-71 should keep its place, it should never go over that lesson"): an
     * ellipse in free air, fixed in the world, chosen once per level and screen. It lies inside what shows when Home
     * opens (the current island at thumb height in a [viewport] dp tall view above the tab bar, under the top bar), at
     * least [JET_EDGE] dp inside the screen's sides, and every point of it is outside every island's no-fly circle.
     * Among the lanes that fit, the widest one nearest the middle of the open sky above the current island wins.
     */
    fun lane(current: Int, viewport: Float = 740f): Lane {
        val c = (current - 1).coerceIn(0, count - 1)
        val top = y(c) - viewport * THUMB + TOP_CLEAR
        val bottom = y(c) + viewport * (1f - THUMB) - BOTTOM_CLEAR
        val near = (0 until count).filter { y(it) in top - 260f..bottom + 260f }
        val want = Pt(width / 2f, (top + y(c)) / 2f)
        var best: Lane? = null
        var bestScore = Float.NEGATIVE_INFINITY
        val rxs = listOf(0.34f, 0.28f, 0.22f, 0.17f, 0.12f, 0.08f).map { (width * it).coerceAtMost(150f) }
        val rys = listOf(60f, 46f, 34f, 24f)
        var cy = top
        while (cy <= bottom) {
            var cx = JET_EDGE
            while (cx <= width - JET_EDGE) {
                if (near.all { Pt(cx, cy).dist(stone(it)) > noFly(it, current) }) for (rx in rxs) for (ry in rys) {
                    if (cx - rx < JET_EDGE || cx + rx > width - JET_EDGE || cy - ry < top || cy + ry > bottom) continue
                    val l = Lane(cx, cy, rx, ry)
                    val score = rx + ry * 0.6f - Pt(cx, cy).dist(want) * 0.25f
                    if (score <= bestScore) continue
                    // 48 samples plus 3 dp to spare, so the curve between two samples stays out too.
                    if ((0 until LANE_SAMPLES).all { k -> val p = l.at(k * 2f * PI.toFloat() / LANE_SAMPLES); near.all { p.dist(stone(it)) > noFly(it, current) + 3f } }) {
                        best = l; bestScore = score
                    }
                }
                cx += 10f
            }
            cy += 10f
        }
        // Nowhere free (never seen on a phone): hold still in the widest gap beside the current island.
        return best ?: Lane(if (x(c) < width / 2f) width - JET_EDGE else JET_EDGE, y(c), 0f, 0f)
    }

    /** Where the SR-71 is [seconds] into its lap of [lane]: one lap every [LOOP_S] s, heading and bank from the curve. */
    fun plane(lane: Lane, seconds: Float): PlaneFix {
        val a = (seconds / LOOP_S) * 2f * PI.toFloat()
        val p = lane.at(a)
        val heading = lane.heading(a)
        val turn = HeadingSpring.wrap(lane.heading(a + 0.05f) - heading)
        val bank = (turn * 12f).coerceIn(-1f, 1f)
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
        /** Island art half widths (PathScreen: 88 dp, the current one 100 dp) and the no-fly margin around each. */
        const val ISLAND_R = 44f
        const val CURRENT_R = 50f
        const val NO_FLY_MARGIN = 24f
        /** The jet's centre stays this far inside the screen's sides (half its frame and a little). */
        const val JET_EDGE = 44f
        /** The top bar and status bar, and the space above the tab bar, that the lane keeps clear of at open. */
        const val TOP_CLEAR = 120f
        const val BOTTOM_CLEAR = 40f
        const val LANE_SAMPLES = 48
        /** Locked islands left in the open above the current one: the story the child can see coming. */
        const val STORY = 6
        /** How much of the cloud bank shows at the very top of the scroll (dp). */
        const val BANK_SHOWN = 250f
        /** Levels that close a unit; the trail crosses the river on a footbridge after each. */
        val BRIDGE_AFTER = listOf(3, 7, 11)
    }
}
