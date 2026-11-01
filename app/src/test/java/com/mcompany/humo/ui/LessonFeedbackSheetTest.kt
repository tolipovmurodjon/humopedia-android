package com.mcompany.humo.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mcompany.humo.ui.glass.GlassTheme
import com.mcompany.humo.ui.lesson.FeedbackStats
import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.LessonPlayer
import com.mcompany.humo.ui.lesson.LessonTheme
import com.mcompany.humo.ui.lesson.Node
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * N15: the verdict sheet enters once per verdict (no flip), covers the Check bar so Check is neither visible nor tappable, and its main
 * button sits where Check was (within 2 dp); a drag snaps it between the peek and the full view.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class LessonFeedbackSheetTest {
    @get:Rule val compose = createComposeRule()

    private fun state() = compose.onNodeWithTag("feedback-panel").fetchSemanticsNode().config[SemanticsProperties.StateDescription]

    @Test fun sheetEntersOnceCoversCheckAndSnaps() {
        FeedbackStats.enters = 0
        val flow = LessonFlow(L11)
        compose.setContent {
            GlassTheme { LessonPlayer(PaddingValues(), onExit = {}, onFinish = {}, theme = LessonTheme.Fly, lesson = flow.lesson, flow = flow, motion = false, humoAlive = false) }
        }
        compose.waitForIdle()
        val dp = compose.density.density
        val check = compose.onNodeWithText("CHECK").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { flow.dropAt(Node(2, 5)); flow.check() }
        compose.waitForIdle()
        assertEquals("the sheet entered more than once for one verdict", 1, FeedbackStats.enters)
        val main = compose.onNodeWithText("TRY AGAIN").fetchSemanticsNode().boundsInRoot
        for ((a, b) in listOf(check.left to main.left, check.right to main.right, check.top to main.top, check.bottom to main.bottom))
            assertTrue("the sheet's main button is not where CHECK was ($a vs $b px)", kotlin.math.abs(a - b) / dp <= 2f)
        compose.onNodeWithText("CHECK").assertDoesNotExist()
        assertEquals("peek", state())
        compose.onNodeWithTag("feedback-panel").performTouchInput { swipeUp() }; compose.waitForIdle()
        assertEquals("expanded", state())
        compose.onNodeWithTag("feedback-panel").performTouchInput { swipeDown() }; compose.waitForIdle()
        assertEquals("peek", state())
        val main2 = compose.onNodeWithText("TRY AGAIN").fetchSemanticsNode().boundsInRoot
        assertTrue("the button moved when the sheet changed state", kotlin.math.abs(main2.bottom - main.bottom) / dp <= 2f)
        assertEquals(1, FeedbackStats.enters)
        compose.runOnIdle { flow.tryAgain() }; compose.waitForIdle()
        compose.runOnIdle { flow.dropAt(Node(2, 5)); flow.check() }; compose.waitForIdle()
        assertEquals("a second verdict enters once more", 2, FeedbackStats.enters)
    }

    /** Boss: primary buttons say one or two words in capitals. */
    @Test fun primaryLabelsAreCapitalsAndShort() {
        val files = listOf("LessonScreen.kt", "LessonChat.kt").map { File("src/main/java/com/mcompany/humo/ui/lesson/$it").readText() }
        val text = files.joinToString("
")
        val a = Regex("""(?:Chunky|BtnSpec)\("([^"]+)", Kind\.(?:Primary|Warn|Dark)""").findAll(text).map { it.groupValues[1] }
        val b = Regex("""Kind\.(?:Primary|Warn|Dark), (?:if \([^)]*\) )?"([^"]+)"(?: else "([^"]+)")?""").findAll(text).flatMap { listOf(it.groupValues[1], it.groupValues[2]).filter { l -> l.isNotEmpty() } }
        val labels = (a + b).toList()
        assertTrue("no primary labels found", labels.isNotEmpty())
        for (l in labels) assertTrue("primary label \"$l\" must be capitals and at most 2 words", l == l.uppercase() && l.split(" ").size <= 2)
    }
}
