package com.mcompany.humo.ui

import com.mcompany.humo.ui.lesson.Flying
import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.PageKind
import com.mcompany.humo.ui.lesson.Screen
import com.mcompany.humo.ui.lesson.cameraLean
import com.mcompany.humo.ui.lesson.rulerVisible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Boss's game-feel list (PICK.md), the pieces that are plain rules. */
class LessonGameFeelTest {
    /** A6: short flights, 420 ms plus about 110 ms a cell, never over 1.1 s. */
    @Test fun flightsAreShort() {
        assertTrue(Flying.duration(0f) in 420L..440L)
        assertTrue(Flying.duration(42f) in 500L..620L)
        assertEquals(1100L, Flying.duration(2000f))
    }

    /** A13: the lead goes first, wingmen 65 ms apart. */
    @Test fun formationRipple() {
        assertEquals(0L, Flying.rippleMs(0))
        assertEquals(65L, Flying.rippleMs(1))
        assertEquals(130L, Flying.rippleMs(2))
    }

    /** Idea 10: the camera leans toward the jet, nothing at the centre, mirrored at the sides, never more than 6 px and 1.2 degrees. */
    @Test fun cameraLeansTowardTheJet() {
        val (tx0, r0) = cameraLean(200f, 400f)
        assertEquals(0f, tx0, 0.001f); assertEquals(0f, r0, 0.001f)
        val (txL, rL) = cameraLean(0f, 400f); val (txR, rR) = cameraLean(400f, 400f)
        assertEquals(-txL, txR, 0.001f); assertEquals(-rL, rR, 0.001f)
        assertTrue(kotlin.math.abs(txR) <= 6f && kotlin.math.abs(rR) <= 1.2f)
    }

    /** Idea 12: the shadow ruler is for the drag pages, never in the skill check. */
    @Test fun rulerOnDragPagesOnly() {
        assertTrue(rulerVisible(false, PageKind.Drag))
        assertFalse(rulerVisible(true, PageKind.Drag))
        assertFalse(rulerVisible(false, PageKind.Translate))
    }

    /** Idea 07: Fly free is a screen of its own after the lesson. */
    @Test fun flyFreeIsAScreen() {
        val flow = LessonFlow(L11)
        flow.goTo(Screen.End)
        flow.goTo(Screen.Sky)
        assertEquals(Screen.Sky, flow.screen)
        assertFalse(flow.locked && flow.canCheck)
    }
}
