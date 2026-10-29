package com.mcompany.humo.ui

import com.mcompany.humo.data.course.CoordinateTransformations
import com.mcompany.humo.data.course.firstLessonLabel
import com.mcompany.humo.ui.path.PathLayout
import com.mcompany.humo.ui.path.Pt
import com.mcompany.humo.ui.path.StoneState
import com.mcompany.humo.ui.path.stoneState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathLayoutTest {
    private val layout = PathLayout(14, 390f)

    @Test fun `course 15 has the 14 levels of the curriculum, in order`() {
        val levels = CoordinateTransformations.levels
        assertEquals("trans", CoordinateTransformations.id)
        assertEquals((1..14).toList(), levels.map { it.n })
        assertEquals("Translations", levels.first().title)
        assertEquals("Stretching", levels.last().title)
        assertEquals("Lesson 1.1 · Translating Points", levels.first().firstLessonLabel())
        assertEquals("Lesson 10.1 · Congruence", levels[9].firstLessonLabel())
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

    @Test fun `clouds cover only the levels not reached yet`() {
        val clouds = layout.clouds(3)
        assertTrue(clouds.isNotEmpty())
        for (c in clouds) assertTrue("cloud at ${c.at} over the current stone", c.at.y < layout.y(2) - 60f)
        assertTrue(clouds.any { it.at.y < 100f })
    }

    @Test fun `the plane never flies over the stone the child is about to tap`() {
        for (current in 1..14) {
            val stone = layout.stone(current - 1)
            for (s in 0 until 220) {
                val fix = layout.plane(current, s / 10f)
                assertTrue("level $current at ${s / 10f}s", fix.at.dist(stone) > 100f)
                assertTrue(fix.bank in -1f..1f)
            }
        }
    }

    @Test fun `the jet stays on screen and off the current stone wherever the child scrolls`() {
        for (viewport in listOf(740f, 300f)) for (current in listOf(1, 3, 9, 14)) {
            var scroll = 0f
            while (scroll < layout.height) {
                for (s in 0 until 44) {
                    val fix = layout.plane(current, s / 2f, scroll, viewport)
                    assertTrue("level $current scroll $scroll", fix.at.dist(layout.stone(current - 1)) > 100f)
                    assertTrue("far off screen at scroll $scroll", fix.at.y - scroll in 0f..viewport + 160f)
                }
                scroll += 120f
            }
        }
    }

    @Test fun `the map fits a 360 dp phone too`() {
        val small = PathLayout(14, 360f)
        for (i in 0 until 14) assertTrue(small.x(i) - 50f >= 0f && small.x(i) + 50f <= 360f)
        for (r in small.river) assertTrue(r.x in 0f..360f || r.y < 0f || r.y > small.height)
        assertEquals(Pt(0f, 0f).dist(Pt(3f, 4f)), 5f, 0.001f)
    }
}
