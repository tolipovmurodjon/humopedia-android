package com.mcompany.humo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mcompany.humo.ui.glass.AuroraBackground
import com.mcompany.humo.ui.glass.GlassTheme
import com.mcompany.humo.ui.lesson.AskPillPreview
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
 * Round 6 (Boss's picked mobile design): a child plays all of L1.1: a wrong try on each kind of page, the amber and mint
 * glass panels, GET HELP and Humo's sheet (it covers the Check bar and the board stays above it), SHOW ME HOW, the skill
 * check with a miss, Humo's fix-it round (replay, two fresh problems, a miss, a new pair, the pass), a clean 3 of 3, the quit
 * sheet and "See you soon". At 360x800, 390x844, 412x915 and at 1.5x font. Shots: build/outputs/roborazzi/round6.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class LessonRound6Test {
    @get:Rule val compose = createComposeRule()

    private fun player(fontScale: Float = 1f): LessonFlow {
        val flow = LessonFlow(L11)
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                GlassTheme { LessonPlayer(PaddingValues(), onExit = {}, onFinish = {}, theme = LessonTheme.Fly, lesson = flow.lesson, flow = flow, motion = false, humoAlive = false) }
            }
        }
        compose.waitForIdle()
        return flow
    }

    private fun act(block: () -> Unit) { compose.runOnIdle(block); compose.waitForIdle() }
    private fun settle() { compose.mainClock.advanceTimeBy(900); compose.waitForIdle() }
    private fun shotAt(tag: String, name: String) {
        ScreenChecks.check(compose, "round6/$tag-$name")
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/round6/$tag-$name.png")
    }

    /** The board, with Humo's squares on it, is wholly above the sheet. */
    private fun assertBoardAboveSheet(tag: String) {
        val board = compose.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot
        val sheet = compose.onNodeWithTag("tutor-sheet").fetchSemanticsNode().boundsInRoot
        assertTrue("$tag: board bottom ${board.bottom} must be above the sheet top ${sheet.top}", board.bottom <= sheet.top + 1f)
        assertTrue("$tag: the board is not squeezed away", board.height > 60f)
    }

    /** The sheet covers the bar: Check is not on the screen while Humo's sheet is up. */
    private fun assertSheetCoversBar(tag: String) {
        compose.onNodeWithTag("dock").assertDoesNotExistOrNot(tag)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertDoesNotExistOrNot(tag: String) {
        val found = runCatching { fetchSemanticsNode() }.isSuccess
        assertTrue("$tag: the Check bar must be covered by Humo's sheet", !found)
    }

    private fun child(tag: String, fontScale: Float = 1f) {
        val flow = player(fontScale)
        fun shot(state: String) = shotAt(tag, state)
        fun click(label: String) { compose.onNodeWithText(label).performClick(); compose.waitForIdle() }
        fun solve() { act { val m = flow.page!!.move; flow.tapTile(m.x); flow.tapTile(m.y); flow.check() }; settle() }
        compose.onNodeWithContentDescription("0 sparks").assertDoesNotExist()

        // P1: drag. A wrong try, the amber panel, GET HELP (Humo marks the board), a chip, DONE, TRY AGAIN, right, SHOW ME HOW.
        shot("P1-start")
        act { flow.dropAt(Node(1, 5)) }
        act { flow.check() }; settle()
        shot("P1-wrong")
        click("GET HELP"); settle()
        assertTrue("Humo draws squares on the board", flow.marks.isNotEmpty())
        assertBoardAboveSheet(tag)
        assertSheetCoversBar(tag)
        shot("P1-tutor-draws")
        click("Show me"); settle()
        shot("P1-tutor-show-me")
        click("DONE")
        click("TRY AGAIN")
        act { flow.dropAt(Node(5, 3)); flow.check() }; settle()
        assertEquals(15, flow.sparks)
        shot("P1-right")
        click("SHOW ME HOW"); settle()
        compose.onNodeWithTag("how-card").assertIsDisplayed()
        shot("P1-show-me-how")
        click("CONTINUE LESSON")
        shot("P2-start")

        // P3: the explainer, and Humo's sheet on a page nobody got wrong (he is tapped, not a button with a word).
        act { flow.goTo(Screen.Page(2)) }
        shot("P3-explainer")
        compose.onNodeWithContentDescription("Ask Humo").performClick(); compose.waitForIdle(); settle()
        assertBoardAboveSheet(tag)
        shot("P3-tutor")
        click("Give me a hint"); settle()
        shot("P3-tutor-hint")
        click("DONE")

        // P4-P6: the keypad, a wrong try each.
        for (i in 3..5) {
            act { flow.goTo(Screen.Page(i)) }
            val p = flow.page!!
            shot("P${i + 1}-start")
            compose.onNodeWithContentDescription("Key ${p.move.x}").performClick(); compose.waitForIdle()
            shot("P${i + 1}-one-number")
            act { flow.tapTile(if (p.move.y < 5) p.move.y + 1 else p.move.y - 1) }
            shot("P${i + 1}-both-numbers")
            act { flow.check() }; settle()
            shot("P${i + 1}-wrong")
            click("TRY AGAIN")
            solve()
            shot("P${i + 1}-right")
        }
        act { flow.goTo(Screen.Page(6)) }
        shot("P7-recap")

        // Skill check: SC1 wrong, SC2 Show me, SC3 right. A task bubble but no tutor.
        act { flow.beginSkillSplash() }
        compose.onNodeWithText("Show what you know").assertIsDisplayed()
        shot("SC-gate")
        click("CONTINUE")
        shot("SC1-start")
        act { flow.tapTile(0); flow.tapTile(0) }
        click("CHECK"); settle()
        compose.onNodeWithText("Incorrect").assertIsDisplayed()
        shot("SC1-wrong")
        click("CONTINUE")
        click("SHOW ME"); settle()
        compose.onNodeWithText("Here's how it goes.").assertIsDisplayed()
        shot("SC2-show-me")
        click("CONTINUE")
        act { flow.tapTile(flow.page!!.move.x); flow.tapTile(flow.page!!.move.y) }
        click("CHECK"); settle()
        compose.onNodeWithText("Correct!").assertIsDisplayed()
        shot("SC3-right")
        click("FINISH")

        // Fix-it: never a fail screen. Humo replays the miss, two fresh problems, a miss, a new pair, the pass.
        compose.onNodeWithText("Almost! Let's fix one.").assertIsDisplayed()
        shot("fix-intro")
        click("FIX IT WITH HUMO"); settle()
        assertEquals(Screen.Demo, flow.screen)
        shot("fix-demo")
        click("CONTINUE")
        shot("fix-1-start")
        act { flow.tapTile(0); flow.tapTile(0); flow.check() }; settle()
        shot("fix-1-wrong")
        click("CONTINUE")
        solve()
        shot("fix-2-right")
        click("CONTINUE"); settle()
        assertEquals("a miss replays and serves a new pair", Screen.Demo, flow.screen)
        shot("fix-demo-again")
        click("CONTINUE")
        solve(); click("CONTINUE")
        solve()
        shot("fix-4-right")
        click("CONTINUE")
        assertEquals(Screen.End, flow.screen)
        compose.onNodeWithText("Lesson complete").assertIsDisplayed()
        shot("end-after-fix")

        // A clean 3 of 3: the Score, then the end (the jet pass runs with motion; the Mach 3 frames are in LessonScreenshotTest).
        act { flow.restart(); flow.startSkill() }
        repeat(3) { solve(); click(if (it == 2) "FINISH" else "CONTINUE") }
        compose.onNodeWithText("Perfect!").assertIsDisplayed()
        shot("score-perfect")
        click("CONTINUE")
        shot("end-perfect")

        // The quit sheet and the calm goodbye.
        act { flow.restart() }
        compose.onNodeWithContentDescription("Exit lesson").performClick(); compose.waitForIdle()
        shot("quit-sheet")
        click("END LESSON")
        compose.onNodeWithText("See you soon").assertIsDisplayed()
        shot("see-you-soon")
    }

    @Test fun childAt390() = child("390")

    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    @Test fun childAt360() = child("360")

    @Config(qualifiers = "w412dp-h915dp-xxhdpi")
    @Test fun childAt412() = child("412")

    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    @Test fun childAtBigFont() = child("360-font150", fontScale = 1.5f)

    /** Humo's input pill while he listens (the recogniser cannot run on a CI emulator): the X, the dotted waveform, the send arrow. */
    @Test fun talkPillWhileListening() {
        compose.setContent {
            GlassTheme {
                AuroraBackground(Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                        AskPillPreview(listening = true, level = 0.7f)
                        AskPillPreview(listening = false, level = 0f)
                    }
                }
            }
        }
        compose.waitForIdle()
        shotAt("pill", "listening-and-idle")
    }

    /** The jet is on the tip of the progress track and the track is glass (Boss's "1 Boost"). */
    @Test fun progressTrackWithTheJet() {
        val flow = player()
        act { flow.goTo(Screen.Page(3)) }
        shotAt("track", "P4")
        act { flow.goTo(Screen.Page(6)) }
        shotAt("track", "P7")
    }

    /** The fire and smoke while the jet is dragged (web parity), caught mid-drag. */
    @Test fun fireAndSmokeWhileDragging() {
        val flow = LessonFlow(L11)
        compose.setContent {
            GlassTheme { LessonPlayer(PaddingValues(), onExit = {}, onFinish = {}, theme = LessonTheme.Fly, lesson = flow.lesson, flow = flow, motion = true, humoAlive = false) }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        fun at(x: Float, y: Float, k: Float) = Offset((48f + x * 42.67f) * k, (308f - y * 42.67f) * k)
        compose.onNodeWithTag("board").performTouchInput {
            val k = width / 350f
            down(at(1f, 2f, k))
            for (i in 1..12) moveTo(at(1f + i * 0.3f, 2f + i * 0.1f, k))
        }
        compose.mainClock.advanceTimeBy(120)
        shotAt("motion", "P1-mid-drag")
        compose.onNodeWithTag("board").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(600)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertTrue(flow.dragAt != null)
    }
}
