package com.mcompany.humo.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Round 4: a child plays all of L1.1 as the web player plays it (a wrong try first on every kind of page, Get help and the
 * chat, right, the skill check with one wrong and one "I don't know", the score, Review with Humo, the end card) and every
 * state is photographed. Shots land in build/outputs/roborazzi/round4 as `<tag>-<state>.png` for 390x844, 360x800 and 1.5x font.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class LessonRound4Test {
    @get:Rule val compose = createComposeRule()

    private fun player(fontScale: Float): LessonFlow {
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

    private fun child(tag: String, fontScale: Float = 1f) {
        val flow = player(fontScale)
        fun shot(state: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/round4/$tag-$state.png")
        fun click(label: String) { compose.onNodeWithText(label).performClick(); compose.waitForIdle() }

        // P1: drag. Wrong first, ask for help, the chat, then right.
        shot("P1-start")
        act { flow.dropAt(Node(1, 5)) }
        shot("P1-moved")
        act { flow.check() }; settle()
        shot("P1-wrong")
        click("Get help"); settle()
        shot("P1-chat")
        act { flow.chat.open = false; flow.tryAgain(); flow.dropAt(Node(5, 3)); flow.check() }; settle()
        shot("P1-right")
        assertEquals(15, flow.sparks)
        click("Continue")
        shot("P2-start")
        act { flow.goTo(Screen.Page(2)) }
        shot("P3-explainer")

        // P4-P6: tiles. One number (waiting), both (preview), wrong, help, right.
        act { flow.goTo(Screen.Page(3)) }
        shot("P4-start")
        act { flow.tapTile(2) }
        shot("P4-one-tile")
        act { flow.tapTile(2) }
        shot("P4-both-tiles")
        act { flow.check() }; settle()
        shot("P4-wrong")
        click("Get help"); settle()
        shot("P4-chat")
        act { flow.chat.open = false; flow.tryAgain(); flow.tapTile(2); flow.tapTile(1); flow.check() }; settle()
        shot("P4-right")
        click("Why?"); settle()
        shot("P4-why")
        act { flow.chat.open = false; flow.goTo(Screen.Page(4)) }
        shot("P5-start")
        act { flow.goTo(Screen.Page(5)) }
        shot("P6-start")
        act { flow.goTo(Screen.Page(6)) }
        shot("P7-recap")

        // Skill check: SC1 wrong, SC2 "I don't know", SC3 right.
        act { flow.beginSkillSplash() }
        shot("SC-splash")
        click("Continue")
        shot("SC1-start")
        act { flow.tapTile(3); flow.tapTile(1) }
        shot("SC1-placed")
        click("Check"); settle()
        compose.onNodeWithText("Not quite.").assertIsDisplayed()
        shot("SC1-wrong")
        click("Continue")
        click("I don't know"); settle()
        compose.onNodeWithText("Here's how it goes.").assertIsDisplayed()
        shot("SC2-idk")
        click("Continue")
        act { flow.tapTile(5); flow.tapTile(2) }
        click("Check"); settle()
        compose.onNodeWithText("Correct!").assertIsDisplayed()
        shot("SC3-right")
        click("Finish")
        compose.onNodeWithText("That was tricky!").assertIsDisplayed()
        shot("score")

        // Review with Humo, then the end card.
        click("Review with Humo"); settle()
        shot("review-1")
        act { flow.tryAgain(); flow.tapTile(1); flow.tapTile(3); flow.check() }; settle()
        shot("review-1-right")
        click("Continue"); settle()
        shot("review-2")
        act { flow.next() }
        compose.onNodeWithText("Lesson complete").assertIsDisplayed()
        shot("end")
    }

    @Test fun childAt390() = child("390")

    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    @Test fun childAt360() = child("360")

    @Test fun childAtBigFont() = child("font150", fontScale = 1.5f)
}
