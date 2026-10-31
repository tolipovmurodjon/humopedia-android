package com.mcompany.humo.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mcompany.humo.ui.glass.GlassTheme
import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.LessonPlayer
import com.mcompany.humo.ui.lesson.LessonTheme
import com.mcompany.humo.ui.lesson.Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Round 3: the six states of the web player's phone look (engine/shots/look-390-*.png), played natively at 390 x 844 dp.
 * Each shot lands in build/outputs/roborazzi/round3 and is put beside its web picture in docs/qa/round3.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class LessonRound3Test {
    @get:Rule val compose = createComposeRule()

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/round3/$name.png")

    private fun player(motion: Boolean = false): LessonFlow {
        val flow = LessonFlow(L11)
        compose.setContent {
            GlassTheme { LessonPlayer(PaddingValues(), onExit = {}, onFinish = {}, theme = LessonTheme.Fly, lesson = flow.lesson, flow = flow, motion = motion, humoAlive = false) }
        }
        compose.waitForIdle()
        return flow
    }

    private fun act(block: () -> Unit) { compose.runOnIdle(block); compose.waitForIdle() }

    @Test fun p1Start() {
        player()
        shot("P1-start")
    }

    @Test fun p4Wrong() {
        val flow = player()
        act { flow.goTo(Screen.Page(3)) }
        act { flow.tapTile(1); flow.tapTile(2); flow.check() }
        shot("P4-wrong")
    }

    @Test fun p4Correct() {
        val flow = player()
        act { flow.goTo(Screen.Page(3)) }
        act { flow.tapTile(2); flow.tapTile(2); flow.check() }
        shot("P4-correct")
    }

    @Test fun skillSplash() {
        val flow = player()
        act { flow.beginSkillSplash() }
        shot("SC-splash")
    }

    /** 2 of 3 right: the "Nice work!" score the web picture shows. */
    @Test fun skillScore() {
        val flow = player()
        act { flow.beginSkillSplash(); flow.startSkill() }
        for (j in 0 until 3) {
            act {
                val p = flow.page!!
                if (j == 1) flow.dontKnow() else { flow.tapTile(p.move.x); flow.tapTile(p.move.y); flow.check() }
                flow.nextSkill()
            }
        }
        shot("SC-score")
    }

    /** Get help on a wrong try: Humo asks the question for the learner, looks at where the machines landed and marks the points. */
    @Test fun chatAfterAWrongTry() {
        val flow = player()
        act { flow.goTo(Screen.Page(3)) }
        act { flow.tapTile(1); flow.tapTile(2); flow.check() }
        compose.onNodeWithText("Get help").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Why was my answer wrong?").assertIsDisplayed()
        shot("chat-wrong")
    }

    /** A perfect score: Humo in sunglasses. */
    @Test fun perfectScoreWearsSunglasses() {
        val flow = player()
        act { flow.beginSkillSplash(); flow.startSkill() }
        for (j in 0 until 3) act { val p = flow.page!!; flow.tapTile(p.move.x); flow.tapTile(p.move.y); flow.check(); flow.nextSkill() }
        shot("SC-score-perfect")
    }

    /** The end pass part-way along its route: fire on both nozzles, the smoke trail, the banner. */
    @Test fun endPass() {
        val flow = player(motion = true)
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { flow.goTo(Screen.End) }
        compose.mainClock.advanceTimeBy(450 + 900)
        shot("End-pass")
        compose.mainClock.advanceTimeBy(500)
        shot("End-pass-b")
    }
}
