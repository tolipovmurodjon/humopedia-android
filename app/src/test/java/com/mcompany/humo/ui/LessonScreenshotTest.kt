package com.mcompany.humo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.mcompany.humo.ui.lesson.Craft
import com.mcompany.humo.ui.lesson.Flying
import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.L12
import com.mcompany.humo.ui.lesson.LessonContent
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.LessonPlayer
import com.mcompany.humo.ui.lesson.LessonTheme
import com.mcompany.humo.ui.lesson.Node
import com.mcompany.humo.ui.lesson.PageKind
import com.mcompany.humo.ui.lesson.Screen
import com.mcompany.humo.ui.lesson.drawPuffs
import com.mcompany.humo.ui.lesson.jetAt
import com.mcompany.humo.ui.path.rememberJetAtlas
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * L1.1 on the A05s's size: every page's start, wrong and right state, plain (Brilliant 1:1) and with the SR-71, the Why?
 * sheet, the skill check, the score and the end card; also at 360x800 and 1.5x font. The board is driven with real
 * touches once (drag, tile taps) to prove the gestures, then by state for the pictures.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class LessonScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/lesson/$name.png")

    private fun player(theme: LessonTheme, flow: LessonFlow = LessonFlow(), fontScale: Float = 1f, finished: (Int) -> Unit = {}, motion: Boolean = false): LessonFlow {
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                GlassTheme { LessonPlayer(PaddingValues(), onExit = {}, onFinish = finished, theme = theme, lesson = flow.lesson, flow = flow, motion = motion, humoAlive = false) }
            }
        }
        compose.waitForIdle()
        return flow
    }

    private fun act(block: () -> Unit) { compose.runOnIdle(block); compose.waitForIdle() }

    /** Humo's line comes 0.5 s (right) or 0.65 s (wrong) after the state; the pictures wait for it. */
    private fun settle() { compose.mainClock.advanceTimeBy(800); compose.waitForIdle() }

    /** Every page: start, then (graded pages) a wrong answer, then the right one. */
    private fun everyPage(theme: LessonTheme, tag: String, fontScale: Float = 1f, lesson: LessonContent = L11) {
        val flow = player(theme, LessonFlow(lesson), fontScale = fontScale)
        for (i in lesson.pages.indices) {
            act { flow.goTo(Screen.Page(i)) }
            val p = lesson.pages[i]
            shot("$tag-${p.id}-start")
            when (p.kind) {
                PageKind.Drag -> {
                    act { flow.dropAt(p.points[0] + Node(p.move.y, p.move.x).let { Node(minOf(it.x, 2), minOf(it.y, 2)) }); flow.check() }
                    settle(); shot("$tag-${p.id}-wrong")
                    act { flow.tryAgain(); flow.dropAt(p.points[0] + p.move); flow.check() }
                    settle(); shot("$tag-${p.id}-correct")
                }
                PageKind.Translate -> {
                    // a wrong try: the open slot(s) one off the answer (L1.1: the second tile; L1.2: the one open slot)
                    fun fill(dx: Int, dy: Int) { if (p.fixedX == null) flow.tapTile(p.move.x + dx); if (p.fixedY == null) flow.tapTile(p.move.y + dy) }
                    if (p.fixedX == null && p.fixedY == null) { act { flow.tapTile(p.move.x) }; shot("$tag-${p.id}-one-tile"); act { flow.tapTile(p.move.y + 1); flow.check() } }
                    else act { fill(if (p.move.x > 0) -1 else 1, if (p.move.y > 0) -1 else 1); flow.check() }
                    settle(); shot("$tag-${p.id}-wrong")
                    act { flow.tryAgain(); flow.startOver(); fill(0, 0); flow.check() }
                    settle(); shot("$tag-${p.id}-correct")
                }
                else -> Unit
            }
        }
    }

    @Test fun everyPagePlain() = everyPage(LessonTheme.Plain, "plain")

    @Test fun everyPageFly() = everyPage(LessonTheme.Fly, "fly")

    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    @Test fun everyPageSmallPhone() = everyPage(LessonTheme.Fly, "360x800")

    @Test fun everyPageBigFont() = everyPage(LessonTheme.Fly, "font150", fontScale = 1.5f)

    @Test fun l12EveryPagePlain() = everyPage(LessonTheme.Plain, "l12-plain", lesson = L12)

    @Test fun l12EveryPageFly() = everyPage(LessonTheme.Fly, "l12-fly", lesson = L12)

    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    @Test fun l12SmallPhoneBigFont() = everyPage(LessonTheme.Fly, "l12-360-font150", fontScale = 1.5f, lesson = L12)

    @Config(qualifiers = "+night")
    @Test fun darkPhone() = everyPage(LessonTheme.Fly, "dark", lesson = L12)

    @Test fun dragThePointWithAFinger() {
        val flow = player(LessonTheme.Plain)
        compose.onNodeWithText("Check").assertIsNotEnabled()
        compose.onNodeWithTag("board").performTouchInput {
            val s = width / 350f
            fun at(x: Int, y: Int) = Offset((48f + x * 42.67f) * s, (308f - y * 42.67f) * s)
            swipe(at(1, 2), Offset(at(5, 3).x - 9f * s, at(5, 3).y + 11f * s), durationMillis = 400)
        }
        compose.waitForIdle()
        assertEquals("snapped to the nearest node", Node(5, 3), flow.dragAt)
        compose.onNodeWithText("Check").assertIsEnabled().performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        compose.onNodeWithText("Nice, that's it!").assertIsDisplayed()
        compose.onNodeWithText("Why?").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Why is this the answer?").assertIsDisplayed()
        shot("plain-P1-why")
    }

    @Test fun theJetFliesByTapThenTap() {
        val flow = player(LessonTheme.Fly)
        compose.onNodeWithTag("board").performTouchInput {
            val s = width / 350f
            fun at(x: Int, y: Int) = Offset((48f + x * 42.67f) * s, (308f - y * 42.67f) * s)
            click(at(1, 2))
        }
        compose.waitForIdle()
        shot("fly-P1-armed")
        compose.onNodeWithTag("board").performTouchInput {
            val s = width / 350f
            click(Offset((48f + 5 * 42.67f) * s, (308f - 3 * 42.67f) * s))
        }
        compose.waitForIdle()
        assertEquals(Node(5, 3), flow.dragAt)
        shot("fly-P1-flown")
    }

    @Test fun tilesFillTheSlotsByTouch() {
        val flow = player(LessonTheme.Plain)
        compose.runOnIdle { flow.goTo(Screen.Page(4)) }
        compose.onNodeWithContentDescription("Tile 3").performClick()
        compose.onNodeWithContentDescription("Slot 1, 3").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tile 2").performClick()
        compose.waitForIdle()
        assertEquals(Node(3, 2), flow.slots.move)
        compose.onNodeWithText("Check").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Why?").performClick()
        compose.waitForIdle()
        shot("plain-P5-why")
    }

    @Test fun recapPlay() {
        val flow = player(LessonTheme.Fly)
        act { flow.goTo(Screen.Page(6)) }
        compose.onNodeWithContentDescription("Play").performClick()
        compose.waitForIdle()
        shot("fly-P7-played")
    }

    @Test fun skillCheckScoreAndEnd() {
        val flow = player(LessonTheme.Fly)
        act { flow.beginSkillSplash() }
        compose.onNodeWithText("Skill check").assertIsDisplayed()
        shot("skill-splash")
        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()
        assertEquals(Screen.Skill(0), flow.screen)
        shot("skill-SC1-start")
        compose.onNodeWithText("I don't know").assertIsDisplayed()
        act { flow.tapTile(1); flow.tapTile(3) }
        shot("skill-SC1-placed")
        compose.onNodeWithText("Check").performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        compose.onNodeWithText("Correct!").assertIsDisplayed()
        shot("skill-SC1-right")
        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()
        assertEquals(Screen.Skill(1), flow.screen)
        compose.onNodeWithText("I don't know").performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        compose.onNodeWithText("Here's how it goes.").assertIsDisplayed()
        shot("skill-SC2-idk")
        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()
        act { flow.tapTile(1); flow.tapTile(4) }
        compose.onNodeWithText("Check").performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
        compose.onNodeWithText("Not quite.").assertIsDisplayed()
        shot("skill-SC3-wrong")
        compose.onNodeWithText("Finish").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("That was tricky!").assertIsDisplayed()
        compose.onNodeWithText("Review with Humo").assertIsDisplayed()
        shot("skill-score")
        act { flow.finish() }
        compose.onNodeWithText("Lesson complete").assertIsDisplayed()
        compose.onNodeWithText("Restart lesson").assertIsDisplayed()
        shot("end-card")
        compose.onNodeWithText("Restart lesson").performClick()
        compose.waitForIdle()
        assertEquals(Screen.Page(0), flow.screen)
        assertEquals(0, flow.sparks)
    }

    /** The close button on the end screen finishes the lesson (the level is marked done); elsewhere it asks first. */
    @Test fun closingTheEndScreenFinishes() {
        var sparks = -1
        val flow = player(LessonTheme.Fly, finished = { sparks = it })
        act { flow.finish() }
        compose.onNodeWithContentDescription("Exit lesson").performClick()
        compose.waitForIdle()
        assertEquals(0, sparks)
    }

    /** Take-off in three steps (altitude 0, 0.5, 1): the jet grows and its shadow drops away; two afterburners, two smoke trails. */
    @Test fun climbingJetWithTwinSmoke() {
        compose.setContent {
            val atlas = rememberJetAtlas()
            Box(Modifier.fillMaxSize().background(Color.White).drawBehind {
                val len = 70f * density
                for ((i, alt) in listOf(0f, 0.5f, 1f).withIndex()) {
                    val craft = Craft(Offset(size.width * (i + 0.5f) / 3f, size.height * 0.4f), 35f)
                    craft.alt = alt
                    craft.speed = 1f
                    val rnd = Random(2)
                    for (k in 0 until 6) { craft.pos = craft.pos + Offset(0f, 14f * density); Flying.puff(craft, 100L * (k + 1), len / 0.857f, true, rnd) }
                    craft.pos = Offset(size.width * (i + 0.5f) / 3f, size.height * 0.4f)
                    drawPuffs(craft.puffs, 700L, len * 0.07f)
                    jetAt(atlas, craft.pos, craft.heading, len, flames = 1f, t = 0f, motion = false, alt = alt)
                }
            })
        }
        compose.waitForIdle()
        shot("fly-climb-and-smoke")
    }

    // The flights with motion ON, frame by frame on the test clock (the CI emulator runs with animations off).

    /** Check on a tile page: the machines wait, take off and climb, cruise, land; only then comes the verdict. */
    @Test fun tileFlightInTheAir() {
        val flow = player(LessonTheme.Fly, motion = true)
        act { flow.goTo(Screen.Page(3)) }
        act { flow.tapTile(2) }
        shot("motion-P4-one-number-waiting")
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { flow.tapTile(1) }
        compose.mainClock.advanceTimeBy(700)
        shot("motion-P4-takeoff")
        compose.mainClock.advanceTimeBy(900)
        shot("motion-P4-cruise")
        compose.mainClock.advanceTimeBy(4500)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithText("Check").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Continue").assertIsDisplayed()
        shot("motion-P4-landed")
    }

    /** Tap the jet (armed), tap a spot: the nose turns first, then a calm flight; nothing else is heard meanwhile. */
    @Test fun tapFlyTurnsThenFlies() {
        val flow = player(LessonTheme.Fly, motion = true)
        compose.mainClock.autoAdvance = false
        fun at(x: Int, y: Int, k: Float) = Offset((48f + x * 42.67f) * k, (308f - y * 42.67f) * k)
        compose.onNodeWithTag("board").performTouchInput { click(at(1, 2, width / 350f)) }
        compose.mainClock.advanceTimeBy(100)
        shot("motion-P1-armed")
        compose.onNodeWithTag("board").performTouchInput { click(at(5, 3, width / 350f)) }
        compose.mainClock.advanceTimeBy(300)
        shot("motion-P1-turning")
        compose.mainClock.advanceTimeBy(900)
        shot("motion-P1-flying")
        compose.mainClock.advanceTimeBy(4000)
        assertEquals(Node(5, 3), flow.dragAt)
        shot("motion-P1-landed")
    }

    /** The 3-second Mach 3 pass over the end card: the jet, then the white ring, then it is gone. */
    @Test fun machPassAtTheEnd() {
        val flow = player(LessonTheme.Fly, motion = true)
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { flow.goTo(Screen.End); androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications() }
        compose.mainClock.advanceTimeBy(1450)
        shot("motion-end-mach3-a")
        compose.mainClock.advanceTimeBy(900)
        shot("motion-end-mach3-ring")
        compose.mainClock.advanceTimeBy(2500)
        shot("motion-end-mach3-done")
    }

    @Test fun leavingAsksFirst() {
        player(LessonTheme.Fly)
        compose.onNodeWithContentDescription("Exit lesson").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Leave the lesson?").assertIsDisplayed()
        shot("exit-dialog")
    }
}
