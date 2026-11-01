package com.mcompany.humo.ui

import com.mcompany.humo.ui.lesson.L11
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
}
