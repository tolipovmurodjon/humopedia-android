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

    /** Idea 10: the camera lean is a translate of at most 16 dp, only near or past the frame edge, mirrored, nothing in the middle. */
    @Test fun cameraLeanIsATranslateNearTheEdge() {
        assertEquals(Pair(0f, 0f), cameraLean(200f, 150f, 400f, 300f, 40f))
        val (txL, _) = cameraLean(0f, 150f, 400f, 300f, 40f); val (txR, _) = cameraLean(400f, 150f, 400f, 300f, 40f)
        assertEquals(-txL, txR, 0.001f)
        assertTrue(txR < 0f && kotlin.math.abs(txR) <= 16f)
        assertEquals(16f, cameraLean(-500f, 150f, 400f, 300f, 40f).first, 0.001f)
        assertTrue(cameraLean(200f, 330f, 400f, 300f, 40f).second < 0f)
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

    /** A1: inside the graph the finger is the jet; past the edge it moves at 35 % and stops 1.5 cells out. */
    @Test fun rubberBandPastTheEdge() {
        assertEquals(3f, com.mcompany.humo.ui.lesson.rubberBand(3f, 7f), 0f)
        assertEquals(7f + 1f * 0.35f, com.mcompany.humo.ui.lesson.rubberBand(8f, 7f), 0.001f)
        assertEquals(7f + 1.5f, com.mcompany.humo.ui.lesson.rubberBand(30f, 7f), 0.001f)
        assertEquals(-1.5f, com.mcompany.humo.ui.lesson.rubberBand(-20f, 7f), 0.001f)
    }
}
