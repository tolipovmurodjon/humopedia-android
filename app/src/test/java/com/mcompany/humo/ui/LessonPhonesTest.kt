package com.mcompany.humo.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mcompany.humo.ui.glass.GlassTheme
import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.L12
import com.mcompany.humo.ui.lesson.LessonContent
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.LessonPlayer
import com.mcompany.humo.ui.lesson.LessonTheme
import com.mcompany.humo.ui.lesson.Node
import com.mcompany.humo.ui.lesson.Screen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Boss: "work fully on all phones". On five phone sizes (and 1.5x font on a 360x800) every state of the lesson keeps the board,
 * the keypad and the primary button wholly on the screen: nothing cut, nothing to scroll to before answering. A failure
 * names the state, the thing and its bounds.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class LessonPhonesTest {
    @get:Rule val compose = createComposeRule()

    private fun player(lesson: LessonContent, fontScale: Float): LessonFlow {
        val flow = LessonFlow(lesson)
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                GlassTheme { LessonPlayer(PaddingValues(), onExit = {}, onFinish = {}, theme = LessonTheme.Fly, lesson = lesson, flow = flow, motion = false, humoAlive = false) }
            }
        }
        compose.waitForIdle()
        return flow
    }

    private fun act(block: () -> Unit) { compose.runOnIdle(block); compose.waitForIdle() }
    private fun settle() { compose.mainClock.advanceTimeBy(900); compose.waitForIdle() }

    private fun phone(tag: String, fontScale: Float = 1f) {
        val flow = player(L11, fontScale)
        val root: Rect = compose.onRoot().fetchSemanticsNode().boundsInRoot
        fun inside(state: String, what: String, n: SemanticsNodeInteraction) {
            val r = n.fetchSemanticsNode().boundsInRoot
            assertTrue("$tag $state: $what is cut or off screen: $r in $root", r.left >= root.left - 1f && r.right <= root.right + 1f && r.top >= root.top - 1f && r.bottom <= root.bottom + 1f)
        }
        fun state(name: String, vararg texts: String, tiles: List<Int> = emptyList(), board: Boolean = true) {
            if (board) inside(name, "the board", compose.onNodeWithTag("board"))
            texts.forEach { inside(name, "\"$it\"", compose.onNodeWithText(it)) }
            tiles.forEach { inside(name, "tile $it", compose.onNodeWithContentDescription("Tile $it")) }
            compose.onRoot().captureRoboImage("build/outputs/roborazzi/phones/$tag-$name.png")
        }

        state("P1-start", "CHECK", "HUMO")
        act { flow.dropAt(Node(1, 5)); flow.check() }; settle()
        state("P1-wrong", "GET HELP", "TRY AGAIN")
        compose.onNodeWithText("GET HELP").performClick(); compose.waitForIdle(); settle()
        val board = compose.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot
        val sheet = compose.onNodeWithTag("tutor-sheet").fetchSemanticsNode().boundsInRoot
        assertTrue("$tag tutor: the board ($board) must sit above the sheet ($sheet)", board.bottom <= sheet.top + 1f)
        state("P1-tutor", "DONE")
        compose.onNodeWithText("DONE").performClick(); compose.waitForIdle()

        act { flow.goTo(Screen.Page(3)) }
        state("P4-start", "CHECK", "HUMO", tiles = listOf(0, 5))
        act { flow.tapTile(1); flow.tapTile(2); flow.check() }; settle()
        state("P4-wrong", "GET HELP", "TRY AGAIN")
        act { flow.goTo(Screen.Page(5)) }
        state("P6-start", "CHECK", "HUMO", tiles = listOf(0, 5))
        act { flow.goTo(Screen.Page(6)) }
        state("P7-recap", "CONTINUE", "HUMO")
        act { flow.beginSkillSplash(); flow.startSkill() }
        state("SC1-start", "CHECK", "I DON'T KNOW", tiles = listOf(0, 5))
    }

    private fun phoneL12(tag: String, fontScale: Float = 1f) {
        val flow = player(L12, fontScale)
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val tray = compose.onNodeWithContentDescription("Tile -5").fetchSemanticsNode().boundsInRoot
        val check = compose.onNodeWithText("CHECK").fetchSemanticsNode().boundsInRoot
        assertTrue("$tag L1.2: the two-row keypad is cut: $tray in $root", tray.bottom <= root.bottom + 1f && tray.left >= -1f)
        assertTrue("$tag L1.2: CHECK is off screen: $check", check.bottom <= root.bottom + 1f)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/phones/$tag-L12-P1.png")
        act { flow.goTo(Screen.Page(0)) }
    }

    @Config(qualifiers = "w320dp-h568dp-xxhdpi") @Test fun phone320x568() { phone("320x568"); }
    @Config(qualifiers = "w360dp-h640dp-xxhdpi") @Test fun phone360x640() { phone("360x640") }
    @Config(qualifiers = "w360dp-h800dp-xxhdpi") @Test fun phone360x800() { phone("360x800") }
    @Config(qualifiers = "w390dp-h844dp-xxhdpi") @Test fun phone390x844() { phone("390x844") }
    @Config(qualifiers = "w412dp-h915dp-xxhdpi") @Test fun phone412x915() { phone("412x915") }
    @Config(qualifiers = "w360dp-h800dp-xxhdpi") @Test fun phone360x800BigFont() { phone("360x800-font150", fontScale = 1.5f) }
    @Config(qualifiers = "w320dp-h568dp-xxhdpi") @Test fun l12Phone320x568() { phoneL12("320x568") }
    @Config(qualifiers = "w360dp-h800dp-xxhdpi") @Test fun l12Phone360x800BigFont() { phoneL12("360x800-font150", fontScale = 1.5f) }
}
