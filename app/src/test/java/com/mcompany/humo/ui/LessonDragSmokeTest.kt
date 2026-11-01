package com.mcompany.humo.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mcompany.humo.ui.glass.GlassTheme
import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.LessonPlayer
import com.mcompany.humo.ui.lesson.LessonTheme
import com.mcompany.humo.ui.lesson.PerfV2
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Boss: "why can't I drag, and the SR-71 has no smoke?" A finger drag on P1 moves the jet and leaves a trail of puffs while it moves. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class LessonDragSmokeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun draggingMovesTheJetAndSpawnsSmoke() {
        PerfV2.boardProbe = true
        val flow = LessonFlow(L11)
        compose.setContent {
            GlassTheme { LessonPlayer(PaddingValues(), onExit = {}, onFinish = {}, theme = LessonTheme.Fly, lesson = flow.lesson, flow = flow, motion = true, humoAlive = false) }
        }
        compose.waitForIdle()
        val spec = L11.pages[0].graph
        val r = compose.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot
        val k = r.width / spec.boxW
        fun at(cx: Float, cy: Float) = Offset(r.left + (spec.originX + cx * spec.cell) * k, r.top + (spec.originY - cy * spec.cell) * k)
        fun desc() = compose.onNodeWithTag("board").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        compose.onRoot().performTouchInput { down(at(1f, 2f)) }
        var maxPuffs = 0
        for (i in 1..12) {
            compose.onRoot().performTouchInput { moveTo(at(1f + i * 0.4f, 2f + i * 0.3f)) }
            compose.mainClock.advanceTimeBy(120)
            maxPuffs = maxOf(maxPuffs, desc().substringAfter("puffs=").toInt())
        }
        val (x, y) = desc().removePrefix("at ").substringBefore(" puffs").split(",").map { it.toFloat() }
        assertTrue("the jet did not follow the finger: $x,$y", x > 4f && y > 4.5f)
        assertTrue("no smoke while dragging", maxPuffs > 0)
        compose.onRoot().performTouchInput { up() }
    }
}
