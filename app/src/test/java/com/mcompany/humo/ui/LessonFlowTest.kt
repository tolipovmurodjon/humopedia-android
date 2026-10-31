package com.mcompany.humo.ui

import com.mcompany.humo.ui.lesson.Cue
import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.L12
import com.mcompany.humo.ui.lesson.signed
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.LessonSounds
import com.mcompany.humo.ui.lesson.Node
import com.mcompany.humo.ui.lesson.PageKind
import com.mcompany.humo.ui.lesson.Phase
import com.mcompany.humo.ui.lesson.Screen
import com.mcompany.humo.ui.lesson.Slots
import com.mcompany.humo.ui.lesson.snap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** L1.1's player without a screen: Brilliant's answers, grading, snap, the tile flow, sparks and the skill check. */
class LessonFlowTest {
    @Test fun `the lesson is Brilliant's seven pages and three skill items, in order`() {
        assertEquals(listOf("P1", "P2", "P3", "P4", "P5", "P6", "P7"), L11.pages.map { it.id })
        assertEquals(listOf("SC1", "SC2", "SC3"), L11.skill.map { it.id })
        assertEquals(listOf(Node(5, 2)), L11.pages[0].targets)
        assertEquals(listOf(Node(4, 4)), L11.pages[1].targets)
        assertEquals(listOf(Node(3, 3), Node(3, 4)), L11.pages[3].targets)
        assertEquals(listOf(Node(6, 2), Node(4, 3), Node(4, 4)), L11.pages[4].targets)
        assertEquals(listOf(Node(3, 4), Node(3, 5), Node(5, 4), Node(5, 5)), L11.pages[5].targets)
        assertEquals(listOf(Node(3, 2), Node(0, 3), Node(2, 4)), L11.skill.map { it.move })
        assertEquals(listOf(Node(4, 3), Node(6, 3)), L11.skill[0].targets)
    }

    @Test fun `L1_2 is Brilliant's Negative Translations - answers, given slots and the -5 to 5 tray`() {
        assertEquals(listOf("P1", "P2", "P3", "P4", "P5", "P6", "P7"), L12.pages.map { it.id })
        assertEquals(listOf(Node(2, 0), Node(-4, 0), Node(-4, 0), Node(0, -1), Node(0, 3), Node(0, -3), Node(0, -3)), L12.pages.map { it.move })
        assertEquals(listOf(Node(0, -2), Node(3, 0), Node(-2, 0)), L12.skill.map { it.move })
        assertEquals((-5..5).toList(), L12.pages[0].tiles)
        assertEquals(listOf(Node(1, 1), Node(1, -1)), L12.pages[0].targets)
        assertEquals("\u22124", signed(-4))
    }

    @Test fun `L1_2 - a given slot is filled in, never active, and only the open slot is graded by the tray`() {
        val flow = LessonFlow(L12)
        assertEquals(0, flow.slots.b)
        assertEquals(0, flow.slots.active)
        flow.tapSlot(1)
        assertEquals("a given slot cannot be picked", 0, flow.slots.active)
        flow.tapTile(-5)
        assertEquals(Node(-5, 0), flow.slots.move)
        assertFalse(flow.check())
        flow.tryAgain(); flow.startOver()
        assertEquals("start over keeps Brilliant's 0", Slots(null, 0, 0, setOf(1)), flow.slots)
        assertFalse(flow.canStartOver)
        flow.tapTile(2)
        assertTrue(flow.check())
        // P4: the x slot is given, the y slot is the learner's
        flow.goTo(Screen.Page(3))
        assertEquals(1, flow.slots.active)
        flow.tapTile(-1)
        assertTrue(flow.check())
        // SC3: both slots open
        flow.goTo(Screen.Skill(2))
        flow.tapTile(-2); flow.tapTile(0)
        assertTrue(flow.check())
    }

    @Test fun `L1_2 - a perfect skill check goes straight to the end`() {
        val flow = LessonFlow(L12)
        flow.startSkill()
        for (j in 0..2) { val m = flow.page!!.move; if (flow.page!!.fixedX == null) flow.tapTile(m.x); if (flow.page!!.fixedY == null) flow.tapTile(m.y); assertTrue(flow.check()); flow.nextSkill() }
        assertEquals(3, flow.skillRight)
        assertEquals("Perfect!", L11.scoreTitle(3, 3))
    }

    @Test fun `snap picks the nearest node and stays on the board`() {
        assertEquals(Node(5, 2), snap(4.6f, 2.4f, 6, 6))
        assertEquals(Node(3, 4), snap(3.49f, 3.51f, 6, 6))
        assertEquals(Node(0, 0), snap(-0.3f, -0.4f, 6, 6))
        assertEquals(Node(6, 6), snap(6.3f, 7.2f, 6, 6))
    }

    @Test fun `drag pages - only the target node is right, every other kind is wrong`() {
        for (i in 0..1) {
            val flow = LessonFlow().also { it.goTo(Screen.Page(i)) }
            val p = flow.page!!
            assertFalse("untouched board cannot be checked", flow.canCheck)
            val start = p.points[0]
            // the classic slips: moves swapped, only right, only up, one too far, left instead of right
            val wrongs = listOf(start + Node(p.move.y, p.move.x), start + Node(p.move.x, 0), start + Node(0, p.move.y), start + p.move + Node(1, 0), start + Node(-p.move.x, p.move.y).let { Node(maxOf(0, it.x), it.y) })
            for (w in wrongs) {
                flow.dropAt(w)
                assertFalse("$w on ${p.id}", flow.check())
                assertEquals(Phase.Wrong, flow.phase)
                flow.tryAgain()
            }
            flow.dropAt(start + p.move)
            assertTrue(flow.check())
            assertEquals(Phase.Correct, flow.phase)
        }
    }

    @Test fun `tile pages - the slots' translation is graded, swapped tiles are wrong`() {
        for (i in 3..5) {
            val flow = LessonFlow().also { it.goTo(Screen.Page(i)) }
            val m = flow.page!!.move
            flow.tapTile(m.y); flow.tapTile(m.x)
            assertTrue(flow.canCheck)
            assertEquals(m.x == m.y, flow.check())
            flow.tryAgain()
            flow.tapSlot(0); flow.tapTile(m.x)
            flow.tapSlot(1); flow.tapTile(m.y)
            assertTrue(flow.check())
        }
    }

    @Test fun `the tile flow - fill the active slot, move on, hide the tray when full, reopen on a tap`() {
        var s = Slots()
        assertEquals(0, s.active); assertTrue(s.trayShown)
        s = s.place(3)
        assertEquals(3, s.a); assertEquals(1, s.active)
        s = s.place(1)
        assertEquals(Node(3, 1), s.move); assertNull(s.active); assertFalse(s.trayShown)
        assertEquals(s, s.place(5))
        s = s.select(0)
        assertTrue(s.trayShown)
        s = s.place(2)
        assertEquals(Node(2, 1), s.move); assertNull(s.active)
        assertNull(Slots().place(4).move)
    }

    @Test fun `start over clears the board and a checked board is locked`() {
        val flow = LessonFlow().also { it.goTo(Screen.Page(3)) }
        flow.tapTile(1); flow.tapTile(2)
        assertTrue(flow.canStartOver)
        flow.check()
        flow.tapTile(4)
        assertEquals(Node(1, 2), flow.slots.move)
        assertFalse(flow.canStartOver)
        flow.tryAgain()
        flow.startOver()
        assertTrue(flow.slots.empty); assertEquals(0, flow.slots.active)
        assertNull(flow.shownMove)
    }

    @Test fun `sparks - 15 on the first right answer of each graded page, 75 in all, never twice`() {
        val flow = LessonFlow()
        for ((i, p) in L11.pages.withIndex()) {
            flow.goTo(Screen.Page(i))
            when (p.kind) {
                PageKind.Drag -> { flow.dropAt(p.points[0] + p.move); flow.check() }
                PageKind.Translate -> { flow.tapTile(p.move.x); flow.tapTile(p.move.y); flow.check() }
                else -> Unit
            }
        }
        assertEquals(75, flow.sparks)
        flow.goTo(Screen.Page(0)); flow.dropAt(Node(5, 2)); flow.check()
        assertEquals(75, flow.sparks)
    }

    @Test fun `praise and nudges rotate in Brilliant's order`() {
        val flow = LessonFlow()
        val said = mutableListOf<String?>()
        repeat(3) { flow.goTo(Screen.Page(0)); flow.dropAt(Node(3, 3)); flow.check(); said += flow.verdict }
        repeat(2) { flow.goTo(Screen.Page(0)); flow.dropAt(Node(5, 2)); flow.check(); said += flow.verdict }
        assertEquals(listOf(L11.nudge[0], L11.nudge[1], L11.nudge[0], "Good!", "Nice!"), said)
    }

    @Test fun `Humo has one teacher line per page and a translate page says how it went wrong`() {
        val flow = LessonFlow(L11)
        assertEquals("Let's get started. Count the steps, then move it.", flow.coach)
        flow.goTo(Screen.Page(3))
        assertEquals("Now you pick the numbers.", flow.coach)
        flow.machineName = "SR-71"
        flow.tapTile(1); flow.tapTile(1); flow.check()
        assertEquals("Not all the SR-71s landed on their targets.", flow.verdict)
        assertTrue("the talk keeps the coach line and the verdict", flow.chat.lines.any { it.text.startsWith("Not all") })
    }

    @Test fun `Try again on a translate page clears both numbers and Humo's marks`() {
        val flow = LessonFlow(L11)
        flow.goTo(Screen.Page(3))
        flow.tapTile(1); flow.tapTile(1); flow.check()
        flow.marks = listOf(Node(1, 1))
        flow.tryAgain()
        assertEquals(Phase.Idle, flow.phase)
        assertNull(flow.slots.a)
        assertNull(flow.slots.b)
        assertTrue(flow.marks.isEmpty())
    }

    @Test fun `the walk - pages, skill check with no verdict, score, review, retry, end`() {
        val flow = LessonFlow()
        repeat(7) { flow.next() }
        assertEquals(Screen.SkillSplash, flow.screen)
        flow.startSkill()
        assertEquals(Screen.Skill(0), flow.screen)
        flow.tapTile(3); flow.tapTile(2)
        assertTrue(flow.check())
        assertEquals("no verdict in the skill check", Phase.Idle, flow.phase)
        assertNull(flow.verdict)
        assertTrue(flow.locked)
        flow.nextSkill()
        flow.dontKnow(); flow.nextSkill()
        flow.tapTile(1); flow.tapTile(4); assertFalse(flow.check()); flow.nextSkill()
        assertEquals(Screen.Score, flow.screen)
        assertEquals(listOf(true, false, false), flow.skillResults.toList())
        assertEquals("That was tricky!", L11.scoreTitle(flow.skillRight, 3))
        flow.startReview()
        assertEquals(Screen.Review(0), flow.screen)
        assertEquals(Phase.Wrong, flow.phase)
        flow.tryAgain(); flow.tapTile(0); flow.tapTile(3); assertTrue(flow.check()); flow.next()
        assertEquals(Screen.Review(1), flow.screen)
        assertEquals("the learner's own wrong tiles come back", Node(1, 4), flow.slots.move)
        flow.next()
        assertEquals(Screen.Retry, flow.screen)
        flow.finish()
        assertEquals(Screen.End, flow.screen)
    }

    @Test fun `score titles as Brilliant words them`() {
        assertEquals("Perfect!", L11.scoreTitle(3, 3))
        assertEquals("Nice work!", L11.scoreTitle(2, 3))
        assertEquals("That was tricky!", L11.scoreTitle(1, 3))
        assertEquals("Keep practicing!", L11.scoreTitle(0, 3))
    }

    @Test fun `back only from page 2, and the progress runs page by page`() {
        val flow = LessonFlow()
        assertFalse(flow.canGoBack)
        assertEquals(0f, flow.progress, 0.001f)
        flow.next(); flow.next()
        assertTrue(flow.canGoBack)
        assertEquals(2f / 6f, flow.progress, 0.001f)
        flow.back()
        assertEquals(Screen.Page(1), flow.screen)
    }

    @Test fun `our sounds are short, clean and never clip`() {
        val limits = mapOf(Cue.Correct to 0.5, Cue.Incorrect to 0.4, Cue.Tick to 0.06, Cue.Toggle to 0.08, Cue.Start to 0.7, Cue.Complete to 1.5)
        for ((cue, max) in limits) {
            val pcm = LessonSounds.pcm(cue)
            assertTrue("$cue is ${pcm.size / 44_100.0} s", pcm.size / 44_100.0 <= max)
            assertTrue(pcm.maxOf { abs(it.toInt()) } <= (0.9 * Short.MAX_VALUE).toInt() + 1)
            assertEquals("$cue ends in silence", 0, abs(pcm.last().toInt()) / 200)
        }
        for ((big, max) in mapOf(false to 0.7, true to 1.2)) {
            val pcm = LessonSounds.whooshPcm(big)
            assertTrue("whoosh $big is ${pcm.size / 44_100.0} s", pcm.size / 44_100.0 <= max)
            assertTrue(pcm.maxOf { abs(it.toInt()) } <= (0.9 * Short.MAX_VALUE).toInt() + 1)
            assertEquals("whoosh $big ends in silence", 0, abs(pcm.last().toInt()) / 200)
        }
    }
}
