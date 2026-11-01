package com.mcompany.humo.ui

import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A1: a miss on the first fix-it problem, then the second and the replayed ones are answered: the board is never locked and Check comes alive. */
class LessonFixItPathTest {
    private fun LessonFlow.solveRight() { val m = page!!.move; tapTile(m.x); tapTile(m.y) }
    private fun LessonFlow.solveWrong() { val m = page!!.move; tapTile(m.x + 1); tapTile(m.y + 1) }

    @Test fun missOnProblemOneThenTheRestAreAnswerable() {
        val flow = LessonFlow(L11)
        flow.fixStart()
        assertEquals(Screen.Demo, flow.screen)
        flow.next()
        assertEquals(Screen.Fix(0), flow.screen)
        assertFalse("locked on fix problem 1", flow.locked)
        flow.solveWrong(); assertTrue(flow.canCheck); assertFalse(flow.check())
        flow.next()
        assertEquals(Screen.Fix(1), flow.screen)
        assertFalse("locked on fix problem 2 after a miss", flow.locked)
        flow.solveRight(); assertTrue("Check is dead on fix problem 2", flow.canCheck); assertTrue(flow.check())
        flow.next()
        // one miss: Humo replays the missed problem and serves a new pair
        assertEquals(Screen.Demo, flow.screen)
        flow.next()
        for (k in 0..1) {
            assertEquals(Screen.Fix(k), flow.screen)
            assertFalse("locked on replayed problem ${k + 1}", flow.locked)
            flow.solveRight(); assertTrue("Check is dead on replayed problem ${k + 1}", flow.canCheck); assertTrue(flow.check())
            flow.next()
        }
        assertEquals(Screen.End, flow.screen)
    }
}
