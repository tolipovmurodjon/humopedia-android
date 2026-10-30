package com.mcompany.humo.ui

import com.mcompany.humo.data.course.CoordinateTransformations
import com.mcompany.humo.ui.path.HeadingSpring
import com.mcompany.humo.ui.path.Jet
import com.mcompany.humo.ui.path.PathLayout
import com.mcompany.humo.ui.path.Pt
import com.mcompany.humo.ui.path.StoneState
import com.mcompany.humo.ui.path.stoneState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PathLayoutTest {
    private val layout = PathLayout(14, 390f)

    @Test fun `course 15 is the real course - 14 units, 62 lessons, in Brilliant's order`() {
        val lessons = CoordinateTransformations.lessons
        assertEquals("trans", CoordinateTransformations.id)
        assertEquals(62, lessons.size)
        assertEquals((1..62).toList(), lessons.map { it.n })
        assertEquals(14, lessons.last().unit)
        assertEquals("1.1" to "Translating Points", lessons[0].code to lessons[0].title)
        assertEquals("1.2" to "Negative Translations", lessons[1].code to lessons[1].title)
        assertEquals("10.1" to "Congruence", lessons.first { it.unit == 10 }.let { it.code to it.title })
        assertEquals("14.4" to "Transforming Points Into Targets", lessons.last().code to lessons.last().title)
        assertEquals(listOf("1.1", "1.2"), lessons.filter { it.built }.map { it.code })
        // a footbridge after each unit's last lesson: 1.6, 2.4, 3.4, ...
        assertEquals(13, CoordinateTransformations.unitEnds.size)
        assertEquals(listOf(6, 10, 14), CoordinateTransformations.unitEnds.take(3))
    }

    @Test fun `level 1 is at the bottom and the river climbs`() {
        for (i in 1 until 14) assertTrue(layout.y(i) < layout.y(i - 1))
        assertTrue(layout.y(0) < layout.height)
        assertTrue(layout.y(13) > 0f)
    }

    @Test fun `every stone stays on screen sideways`() {
        for (i in 0 until 14) assertTrue(layout.x(i) - 50f >= 0f && layout.x(i) + 50f <= 390f)
    }

    @Test fun `the current stone opens at thumb height`() {
        val viewport = 844f
        val scroll = layout.scrollFor(2, viewport)
        assertEquals(viewport * PathLayout.THUMB, layout.y(2) - scroll, 0.5f)
        val cut = layout.cut(3)
        assertEquals(viewport * PathLayout.THUMB, layout.y(2) - cut - layout.scrollFor(2, viewport, cut), 0.5f)
    }

    @Test fun `scroll never goes past either end`() {
        assertEquals(0f, layout.scrollFor(13, 844f), 0f)
        assertEquals(layout.height - 844f, layout.scrollFor(0, 844f).coerceAtMost(layout.height - 844f), 0f)
        assertTrue(layout.scrollFor(0, 844f) <= layout.height - 844f)
    }

    @Test fun `stones before the current are done, after it locked`() {
        assertEquals(StoneState.Done, stoneState(2, 3))
        assertEquals(StoneState.Current, stoneState(3, 3))
        assertEquals(StoneState.Locked, stoneState(4, 3))
    }

    @Test fun `the river meets the trail only at the three footbridges`() {
        assertEquals(3, layout.bridges.size)
        val trail = (-1 until 14).flatMap { i -> (0..40).map { layout.trail(i, it / 40f) } }
        for (r in layout.river) {
            if (trail.any { it.dist(r) < 24f }) assertTrue("river touches the trail at $r", layout.bridges.any { it.at.dist(r) < 70f })
        }
    }

    @Test fun `the river never runs through an island`() {
        for (i in 0 until 14) for (r in layout.river) assertTrue(r.dist(layout.stone(i)) > 50f)
    }

    @Test fun `no flagstone sits on an island or a bridge, and the trail is a real trail`() {
        assertTrue(layout.flagstones.size > 60)
        for (f in layout.flagstones) {
            for (i in 0 until 14) assertTrue(f.at.dist(layout.stone(i)) >= PathLayout.ISLAND_CLEAR)
            for (b in layout.bridges) assertTrue(f.at.dist(b.at) >= PathLayout.BRIDGE_CLEAR)
        }
    }

    @Test fun `trees stand on free ground`() {
        assertTrue(layout.trees.size >= 8)
        for (t in layout.trees) {
            for (i in 0 until 14) assertTrue(t.at.dist(layout.stone(i)) >= 70f)
            assertTrue(t.at.x in 0f..390f)
        }
    }

    @Test fun `the current island and the next 5 to 7 are the visible story`() {
        for (current in 1..14) {
            val ahead = layout.storyTop(current) - (current - 1)
            assertTrue("level $current shows $ahead ahead", ahead in 5..7 || layout.storyTop(current) == 13)
        }
    }

    @Test fun `the cloud bank never comes down onto a visible island`() {
        for (current in 1..14) {
            val islandTop = layout.y(layout.storyTop(current)) - 45f
            val rows = layout.bank(current)
            assertTrue(rows.size >= 4)
            for (r in rows) assertTrue("level $current row at ${r.y}", r.y + r.width * 0.085f <= islandTop)
            // The rows cover everything up to past the top of the scroll.
            assertTrue(rows.last().y < layout.cut(current) - 60f)
        }
    }

    @Test fun `the top of the scroll is the bank`() {
        assertEquals(PathLayout.BANK_SHOWN, layout.bankEdge(3) - layout.cut(3), 0.01f)
        assertEquals(0f, layout.cut(14), 0f)
        // The current island always opens below the bank.
        for (current in 1..14) assertTrue(layout.y(current - 1) > layout.bankEdge(current) + 80f)
    }

    @Test fun `bank clouds drift smoothly and wrap off screen`() {
        for (row in layout.bank(3)) {
            val a = layout.bankClouds(row, 10f)
            val b = layout.bankClouds(row, 10.1f)
            for (j in a.indices) {
                assertTrue(a[j].x >= -row.spacing - 0.01f && a[j].x <= 390f + 2 * row.spacing)
                val d = kotlin.math.abs(b[j].x - a[j].x)
                // Either a small slide or a wrap that happens off screen on both sides.
                assertTrue(d < 3f || (minOf(a[j].x, b[j].x) < -row.width * 0.44f && maxOf(a[j].x, b[j].x) > 390f + row.width * 0.44f))
            }
            assertTrue("row ${row.depth} moves", kotlin.math.abs(a[0].x - layout.bankClouds(row, 12f)[0].x) > 5f)
        }
    }

    private val screens = listOf(PathLayout(14, 390f) to 740f, PathLayout(14, 360f) to 680f, PathLayout(14, 412f) to 760f, PathLayout(14, 844f) to 300f)

    @Test fun `the jet never enters any island's no-fly circle, on every level and screen`() {
        for ((map, visible) in screens) for (current in 1..14) {
            val lane = map.lane(current, visible)
            for (f in 0 until (PathLayout.LOOP_S * 60).toInt()) {
                val fix = map.plane(lane, f / 60f)
                for (i in 0 until 14) {
                    val gap = fix.at.dist(map.stone(i))
                    assertTrue("w${map.width} level $current: island ${i + 1} at $gap dp, no-fly ${map.noFly(i, current)}", gap > map.noFly(i, current))
                }
                assertTrue("off the side", fix.at.x in PathLayout.JET_EDGE - 0.5f..map.width - PathLayout.JET_EDGE + 0.5f)
                assertTrue(fix.bank in -1f..1f)
            }
        }
    }

    @Test fun `the jet flies a real lap, in what shows when Home opens`() {
        for ((map, visible) in screens) for (current in 1..14) {
            val lane = map.lane(current, visible)
            assertTrue("w${map.width} level $current has no lap: $lane", lane.rx >= 24f && lane.ry >= 24f)
            val top = map.y(current - 1) - visible * PathLayout.THUMB
            for (s in 0 until 44) assertTrue(map.plane(lane, s / 2f).at.y - top in 0f..visible)
        }
    }

    @Test fun `the jet keeps its place, its lane does not follow the scroll`() {
        assertEquals(layout.lane(3, 740f), layout.lane(3, 740f))
        val a = layout.plane(layout.lane(3, 740f), 5f)
        assertEquals(a, layout.plane(layout.lane(3, 740f), 5f))
    }

    @Test fun `the heading never turns more than 6 degrees in a frame, curve and spring`() {
        for ((map, visible) in screens) for (current in 1..14) {
            val lane = map.lane(current, visible)
            val spring = HeadingSpring()
            var prevCurve = Float.NaN
            var prevSpring = Float.NaN
            for (f in 0 until (PathLayout.LOOP_S * 60 * 2).toInt()) {
                val h = map.plane(lane, f / 60f).heading
                val sh = spring.step(h)
                if (!prevCurve.isNaN()) {
                    assertTrue("curve turned ${deg(h - prevCurve)}°", abs(deg(h - prevCurve)) <= 6f)
                    assertTrue("spring turned ${deg(sh - prevSpring)}°", abs(deg(sh - prevSpring)) <= 6.001f)
                }
                prevCurve = h; prevSpring = sh
            }
        }
    }

    @Test fun `a heading spring never snaps, even when the wanted heading jumps half a turn`() {
        val spring = HeadingSpring()
        spring.snapTo(0f)
        var prev = 0f
        repeat(120) {
            val v = spring.step(Math.PI.toFloat() - 0.01f)
            assertTrue(abs(deg(v - prev)) <= 6.001f)
            prev = v
        }
        assertEquals(180f, abs(deg(prev)), 2f)
    }

    @Test fun `the jet turns only the rest of the heading after its nearest of 64 frames`() {
        val pi = Math.PI.toFloat()
        assertEquals(0f, Jet.rest(-pi / 2f), 0.001f)
        assertEquals(1f, Jet.rest(-pi / 2f + 1f * pi / 180f), 0.01f)
        assertTrue(abs(Jet.rest(-pi / 2f + 1f)) <= 2.8125f + 0.001f)
        assertTrue(Jet.SHADOW_ALPHA <= 0.12f)
    }

    private fun deg(r: Float) = Math.toDegrees(HeadingSpring.wrap(r).toDouble()).toFloat()

    @Test fun `the jet picks the atlas frame facing its heading, clockwise from nose-up`() {
        val pi = Math.PI.toFloat()
        assertEquals(0, Jet.frameOf(-pi / 2f))
        assertEquals(8, Jet.frameOf(-pi / 4f))
        assertEquals(16, Jet.frameOf(0f))
        assertEquals(32, Jet.frameOf(pi / 2f))
        assertEquals(48, Jet.frameOf(pi))
        assertEquals(48, Jet.frameOf(-pi))
        assertEquals(63, Jet.frameOf(-pi / 2f - 0.1f))
        assertEquals(64, Jet.HEADINGS)
    }

    @Test fun `the map fits a 360 dp phone too`() {
        val small = PathLayout(14, 360f)
        for (i in 0 until 14) assertTrue(small.x(i) - 50f >= 0f && small.x(i) + 50f <= 360f)
        for (r in small.river) assertTrue(r.x in 0f..360f || r.y < 0f || r.y > small.height)
        assertEquals(Pt(0f, 0f).dist(Pt(3f, 4f)), 5f, 0.001f)
    }
}
