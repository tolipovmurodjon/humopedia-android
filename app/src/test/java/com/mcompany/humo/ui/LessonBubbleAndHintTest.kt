package com.mcompany.humo.ui

import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.isFarOffBoard
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.Node
import com.mcompany.humo.ui.lesson.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Child loop round 1: a slide never opens with the last slide's cue in the bubble, and the bulb never gives the answer before the third miss. */
class LessonBubbleAndHintTest {
    @Test fun everySlideOpensWithItsOwnTask() {
        val flow = LessonFlow(L11)
        for (i in 0 until L11.pages.size) {
            flow.goTo(Screen.Page(i))
            flow.dropAt(Node(1, 1)) // a move says a cue ("Ooh, nice.")
            flow.goTo(Screen.Page(i))
            assertNull("slide ${i + 1} opens with a stale cue", flow.remark)
            assertNull("slide ${i + 1} opens with a stale verdict", flow.verdict)
        }
    }

    @Test fun bulbShowsTheMethodNotTheAnswerUntilTheThirdMiss() {
        val flow = LessonFlow(L11)
        flow.goTo(Screen.Page(3))
        flow.hint()
        assertFalse("the hint gave the answer before any try", flow.showHow)
        assertEquals(listOf(flow.page!!.points[0]), flow.marks)
        repeat(3) { val m = flow.page!!.move; flow.tapTile(m.x + 1); flow.tapTile(m.y + 1); flow.check(); flow.tryAgain() }
        flow.hint()
        assertTrue("after the third miss the bulb shows the worked answer", flow.showHow)
    }

    /** Humo's fix-it and gate lines stay short: at most 8 words each. */
    @Test fun fixItLinesAreShort() {
        for (key in listOf("gate", "fixTitle", "fixLook", "fixLook2", "fixNone", "fixThen", "fixNew", "fixPass")) for (line in L11.fx(key))
            assertTrue("\"$line\" has more than 8 words", line.split(" ").size <= 8)
    }

    /** Boss: on each digit a ghost jet flies to where that move lands; one number alone moves that leg only. */
    @Test fun previewFollowsEveryDigit() {
        assertEquals(null, com.mcompany.humo.ui.lesson.previewMove(com.mcompany.humo.ui.lesson.Slots()))
        assertEquals(Node(2, 0), com.mcompany.humo.ui.lesson.previewMove(com.mcompany.humo.ui.lesson.Slots(a = 2)))
        assertEquals(Node(0, 3), com.mcompany.humo.ui.lesson.previewMove(com.mcompany.humo.ui.lesson.Slots(b = 3)))
        assertEquals(Node(2, 1), com.mcompany.humo.ui.lesson.previewMove(com.mcompany.humo.ui.lesson.Slots(a = 2, b = 1)))
    }

    /** Boss: "it is hard to move the jet": a finger within 56 dp across (40 dp radius) still grabs it, more on the big sprite. */
    @Test fun jetGrabAreaIsGenerous() {
        assertTrue(com.mcompany.humo.ui.lesson.grabRadius(3f, 10f, false) >= 120f)
        assertTrue(com.mcompany.humo.ui.lesson.grabRadius(3f, 400f, true) >= 280f)
    }

    /** N16: a big number sends the jet off the screen; a near miss stays on the board. Humo's joke never repeats twice in a row. */
    @Test fun bigNumbersLeaveTheScreenAndTheJokeVaries() {
        assertTrue(isFarOffBoard(12f * 40f + 50f, 100f, 350f, 330f, 40f))
        assertFalse(isFarOffBoard(380f, 100f, 350f, 330f, 40f))
        val flow = LessonFlow(L11)
        flow.goTo(Screen.Page(3))
        var last: String? = null
        repeat(8) { flow.farMiss(); assertTrue(flow.remark != last); last = flow.remark }
    }
}
