package com.mcompany.humo.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mcompany.humo.ui.glass.GlassTheme
import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.LessonPlayer
import com.mcompany.humo.ui.lesson.LessonTheme
import com.mcompany.humo.ui.lesson.Node
import com.mcompany.humo.ui.lesson.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Round 5 (Duolingo's mobile patterns): a child plays all of L1.1 on the phone UI: a wrong try on each kind of page, the
 * green and yellow panels, GET HELP and the HUMO tutor sheet (the board stays above it, undimmed while Humo draws), SHOW ME
 * HOW, the skill check with one wrong and one "I don't know", the score, review and the end. Shots: build/outputs/roborazzi/round5.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class LessonRound5Test {
    @get:Rule val compose = createComposeRule()

    private fun player(fontScale: Float = 1f, motion: Boolean = false): LessonFlow {
        val flow = LessonFlow(L11)
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                GlassTheme { LessonPlayer(PaddingValues(), onExit = {}, onFinish = {}, theme = LessonTheme.Fly, lesson = flow.lesson, flow = flow, motion = motion, humoAlive = false) }
            }
        }
        compose.waitForIdle()
        return flow
    }

    private fun act(block: () -> Unit) { compose.runOnIdle(block); compose.waitForIdle() }
    private fun settle() { compose.mainClock.advanceTimeBy(900); compose.waitForIdle() }
    private fun shotAt(dir: String, name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/$dir/$name.png")

    /** The board, with Humo's squares on it, is wholly above the sheet. */
    private fun assertBoardAboveSheet() {
        val board = compose.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot
        val sheet = compose.onNodeWithTag("tutor-sheet").fetchSemanticsNode().boundsInRoot
        assertTrue("board bottom ${board.bottom} must be above the sheet top ${sheet.top}", board.bottom <= sheet.top + 1f)
        assertTrue("the board is not squeezed away", board.height > 60f)
    }

    private fun child(tag: String, fontScale: Float = 1f) {
        val flow = player(fontScale)
        fun shot(state: String) = shotAt("round5", "$tag-$state")
        fun click(label: String) { compose.onNodeWithText(label).performClick(); compose.waitForIdle() }
        compose.onNodeWithContentDescription("0 sparks").assertDoesNotExist()

        // P1: drag. A wrong try, the yellow panel, GET HELP (Humo marks the board), a chip, DONE, TRY AGAIN, right, SHOW ME HOW.
        shot("P1-start")
        act { flow.dropAt(Node(1, 5)) }
        act { flow.check() }; settle()
        shot("P1-wrong")
        click("GET HELP"); settle()
        assertTrue("Humo draws squares on the board", flow.marks.isNotEmpty())
        assertBoardAboveSheet()
        shot("P1-tutor-draws")
        click("Show me"); settle()
        shot("P1-tutor-show-me")
        click("DONE")
        shot("P1-after-tutor")
        click("TRY AGAIN")
        act { flow.dropAt(Node(5, 3)); flow.check() }; settle()
        assertEquals(15, flow.sparks)
        shot("P1-right")
        click("SHOW ME HOW"); settle()
        compose.onNodeWithTag("how-card").assertIsDisplayed()
        shot("P1-show-me-how")
        click("CONTINUE LESSON")
        shot("P2-start")

        // P3: the explainer, and the HUMO tutor on a page nobody got wrong.
        act { flow.goTo(Screen.Page(2)) }
        shot("P3-explainer")
        click("HUMO"); settle()
        assertBoardAboveSheet()
        shot("P3-tutor")
        click("Give me a hint"); settle()
        shot("P3-tutor-hint")
        click("DONE")

        // P4-P6: tiles, a wrong try each.
        for (i in 3..5) {
            act { flow.goTo(Screen.Page(i)) }
            val p = flow.page!!
            shot("P${i + 1}-start")
            act { flow.tapTile(p.move.x) }
            shot("P${i + 1}-one-tile")
            act { flow.tapTile(if (p.move.y < 5) p.move.y + 1 else p.move.y - 1) }
            shot("P${i + 1}-both-tiles")
            act { flow.check() }; settle()
            shot("P${i + 1}-wrong")
            if (i == 3) {
                click("GET HELP"); settle()
                assertBoardAboveSheet()
                shot("P4-tutor-draws")
                click("DONE")
            }
            click("TRY AGAIN")
            act { flow.tapTile(p.move.x); flow.tapTile(p.move.y); flow.check() }; settle()
            shot("P${i + 1}-right")
        }
        act { flow.goTo(Screen.Page(6)) }
        shot("P7-recap")

        // Skill check: SC1 wrong, SC2 "I don't know", SC3 right. No Humo, no tutor.
        act { flow.beginSkillSplash() }
        shot("SC-splash")
        click("CONTINUE")
        shot("SC1-start")
        act { flow.tapTile(0); flow.tapTile(0) }
        click("CHECK"); settle()
        compose.onNodeWithText("Not quite.").assertIsDisplayed()
        shot("SC1-wrong")
        click("CONTINUE")
        click("I DON'T KNOW"); settle()
        compose.onNodeWithText("Here's how it goes.").assertIsDisplayed()
        shot("SC2-idk")
        click("CONTINUE")
        act { flow.tapTile(flow.page!!.move.x); flow.tapTile(flow.page!!.move.y) }
        click("CHECK"); settle()
        compose.onNodeWithText("Correct!").assertIsDisplayed()
        shot("SC3-right")
        click("FINISH")
        compose.onNodeWithText("That was tricky!").assertIsDisplayed()
        shot("score")

        // Review with Humo, then the end card with the one sparks number.
        click("REVIEW WITH HUMO"); settle()
        shot("review-1")
        click("GET HELP"); settle()
        assertBoardAboveSheet()
        shot("review-1-tutor")
        click("DONE")
        act { flow.tryAgain(); flow.tapTile(flow.page!!.move.x); flow.tapTile(flow.page!!.move.y); flow.check() }; settle()
        shot("review-1-right")
        click("CONTINUE"); settle()
        shot("review-2")
        act { flow.next() }
        compose.onNodeWithText("Lesson complete").assertIsDisplayed()
        compose.onNodeWithText("+${flow.sparks}").assertIsDisplayed()
        shot("end")
    }

    @Test fun childAt390() = child("390")

    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    @Test fun childAt360() = child("360")

    @Test fun childAtBigFont() = child("font150", fontScale = 1.5f)

    /** The jet shows fire and a smoke trail while it is dragged (web parity), caught mid-drag. */
    @Test fun fireAndSmokeWhileDragging() {
        val flow = player(motion = true)
        compose.mainClock.autoAdvance = false
        fun at(x: Float, y: Float, k: Float) = Offset((48f + x * 42.67f) * k, (308f - y * 42.67f) * k)
        compose.onNodeWithTag("board").performTouchInput {
            val k = width / 350f
            down(at(1f, 2f, k))
            for (i in 1..12) moveTo(at(1f + i * 0.3f, 2f + i * 0.1f, k))
        }
        compose.mainClock.advanceTimeBy(120)
        shotAt("round5", "motion-P1-mid-drag")
        compose.onNodeWithTag("board").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(600)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertTrue(flow.dragAt != null)
    }
}
