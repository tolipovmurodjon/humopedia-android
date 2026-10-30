package com.mcompany.humo.ui

import androidx.compose.ui.geometry.Offset
import com.mcompany.humo.ui.lesson.Craft
import com.mcompany.humo.ui.lesson.Flying
import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.L12
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.MachPassPath
import com.mcompany.humo.ui.lesson.Node
import com.mcompany.humo.ui.lesson.Words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/** The machines' motion without a screen: take-off, climb, landing, the glued drag, the words and the Mach 3 pass. */
class LessonFlightTest {
    @Test fun `a flight climbs, lands exactly on its target and is calm`() {
        val c = Craft(Offset(100f, 400f))
        val to = Offset(300f, 200f)
        val ms = Flying.plan(c, to, 0L, density = 2.6f)
        assertTrue("2 to 3.6 s, was $ms", ms in 2000L..3600L)
        var peak = 0f
        var landed = false
        var t = 0L
        while (!landed && t < 5000L) {
            t += 16
            landed = Flying.fly(c, t, 16f, cell = 40f)
            peak = maxOf(peak, c.alt)
            assertTrue(c.alt in 0f..1f)
        }
        assertTrue(landed)
        assertTrue("it climbed (peak $peak)", peak > 0.9f)
        assertEquals(to.x, c.pos.x, 0.01f); assertEquals(to.y, c.pos.y, 0.01f)
        assertEquals(0f, c.alt, 0f)
        assertNull(c.flight)
    }

    @Test fun `no flight when the machine is already there, and a ground slide never climbs`() {
        val c = Craft(Offset(50f, 50f))
        assertEquals(0L, Flying.plan(c, Offset(50.5f, 50f), 0L, 2.6f))
        assertNull(c.flight)
        assertEquals(500L, Flying.plan(c, Offset(250f, 50f), 0L, 2.6f, climb = false))
        Flying.fly(c, 250L, 16f, 40f)
        assertEquals(0f, c.alt, 0f)
    }

    @Test fun `the nose turns the short way, to the bearing of the move`() {
        assertEquals(90f, Flying.bearing(Offset(0f, 0f), Offset(10f, 0f)), 0.01f)
        assertEquals(0f, Flying.bearing(Offset(0f, 10f), Offset(0f, 0f)), 0.01f)
        assertEquals(0f, Flying.bearing(Offset(5f, 5f), Offset(5f, 5f)), 0f)
        assertEquals(-20f, Flying.angDiff(10f, -10f), 0.01f)
        assertEquals(20f, Flying.angDiff(170f, -170f), 0.01f)
    }

    @Test fun `glued to the handle - the machine is on the point, the nose eases, no smoke without a flight`() {
        val c = Craft(Offset(100f, 100f))
        val rnd = Random(1)
        var h = Offset(100f, 100f)
        for (i in 1..30) {
            h = Offset(100f + i * 6f, 100f)
            Flying.glue(c, h, 16.7f, i * 17L, cell = 40f, size = 60f, minMove = 5f, steering = true, flying = false, rnd = rnd)
            assertEquals("no lag", h, c.pos)
        }
        assertTrue("nose turned right, was ${c.deg}", c.deg > 60f && c.deg <= 90.5f)
        assertTrue(c.puffs.isEmpty())
        // the same drag as a flight leaves two trails
        val f = Craft(Offset(100f, 100f))
        for (i in 1..30) Flying.glue(f, Offset(100f + i * 3f, 100f), 16.7f, i * 100L, cell = 40f, size = 60f, minMove = 5f, steering = true, flying = true, rnd = rnd)
        assertTrue(f.puffs.size >= 2 && f.puffs.size % 2 == 0)
    }

    @Test fun `the words name the machine, the maths words stay`() {
        assertEquals("Move the SR-71 3 steps to the right and 1 up.", Words.machine("Move the point 3 steps to the right and 1 up."))
        assertEquals("Translate the SR-71s to the targets.", Words.machine("Translate the points to the targets."))
        assertEquals("The SR-71 flies by (2, 3)", Words.machine("A point is translated by (2, 3)"))
        assertEquals("The SR-71 starts at (2, 1).", Words.machine("The point starts at (2, 1)."))
        assertEquals("A translation by (*a*, 0) moves the SR-71 to the right.", Words.machine("A translation by (*a*, 0) moves a point to the right."))
    }

    @Test fun `the Mach 3 pass crosses the screen from bottom-left to top-right in 3 seconds`() {
        val (a, _) = MachPassPath.at(0f, 400f, 800f)
        val (b, degB) = MachPassPath.at(1f, 400f, 800f)
        assertTrue(a.x < 0f && a.y > 700f)
        assertTrue(b.x > 400f && b.y < 0f)
        assertTrue("it ends heading up and right, was $degB", degB in 1f..89f)
        assertEquals(3f, MachPassPath.SECONDS, 0f)
    }

    @Test fun `a checked board is locked while the machines fly`() {
        val flow = LessonFlow(L11)
        flow.goTo(com.mcompany.humo.ui.lesson.Screen.Page(3))
        flow.tapTile(2); flow.tapTile(2)
        assertTrue(flow.canCheck)
        flow.flying = true
        assertFalse(flow.canCheck)
        assertTrue(flow.locked)
        flow.flying = false
        assertTrue(flow.check())
        assertTrue(flow.answered)
        assertNotNull(flow.verdict)
        assertEquals(Node(2, 2), flow.slots.move)
        assertEquals("L1.2 uses the same machine words", "SR-71", Words.MACHINE)
        assertEquals(7, L12.pages.size)
    }
}
