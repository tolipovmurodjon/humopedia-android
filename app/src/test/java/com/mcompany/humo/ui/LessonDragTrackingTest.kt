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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Boss's A05s (alpha11): a finger going up and right moved the jet right and DOWN, about 1.7 times as far. This holds the jet
 * against the finger: a pointer path is injected at SCREEN positions computed from where the board really is (its bounds on
 * screen, so a board scaled to fit a short phone is covered), and after every move the jet must be within half a cell of the finger.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class LessonDragTrackingTest {
    @get:Rule val compose = createComposeRule()

    private fun track() {
        com.mcompany.humo.ui.lesson.PerfV2.boardProbe = true
        val flow = LessonFlow(L11)
        compose.setContent {
            GlassTheme { LessonPlayer(PaddingValues(), onExit = {}, onFinish = {}, theme = LessonTheme.Fly, lesson = flow.lesson, flow = flow, motion = false, humoAlive = false) }
        }
        compose.waitForIdle()
        val spec = L11.pages[0].graph
        val r = compose.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot
        val k = r.width / spec.boxW
        fun screenOf(cx: Float, cy: Float) = Offset(r.left + (spec.originX + cx * spec.cell) * k, r.top + (spec.originY - cy * spec.cell) * k)
        fun jet(): Pair<Float, Float> {
            val d = compose.onNodeWithTag("board").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            val (x, y) = d.removePrefix("at ").split(",")
            return x.toFloat() to y.toFloat()
        }
        val path = listOf(2f to 3f, 3f to 4f, 4f to 5f, 5f to 4.5f, 5.5f to 3.5f, 5f to 3f)
        compose.onRoot().performTouchInput {
            down(screenOf(1f, 2f))
        }
        for ((cx, cy) in path) {
            compose.onRoot().performTouchInput { moveTo(screenOf(cx, cy)) }
            compose.waitForIdle()
            val (jx, jy) = jet()
            assertTrue("finger at ($cx, $cy), jet at ($jx, $jy): more than half a cell apart", kotlin.math.abs(jx - cx) <= 0.5f && kotlin.math.abs(jy - cy) <= 0.5f)
        }
        compose.onRoot().performTouchInput { up() }
        compose.waitForIdle()
    }

    @Config(qualifiers = "w360dp-h800dp-xxhdpi") @Test fun jetFollowsTheFinger360x800() = track()
    @Config(qualifiers = "w320dp-h568dp-xxhdpi") @Test fun jetFollowsTheFingerOnAScaledBoard320x568() = track()
    @Config(qualifiers = "w360dp-h640dp-xxhdpi") @Test fun jetFollowsTheFingerOnAScaledBoard360x640() = track()
}
