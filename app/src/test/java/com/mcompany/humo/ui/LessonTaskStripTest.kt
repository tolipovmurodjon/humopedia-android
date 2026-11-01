package com.mcompany.humo.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
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

/** Boss: "when Humo's line replaces the task, I don't know where to move." The task strip stays, unchanged, in every state. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class LessonTaskStripTest {
    @get:Rule val compose = createComposeRule()

    private fun strip(): String {
        compose.onNodeWithTag("task-strip").assertIsDisplayed()
        return compose.onNodeWithTag("task-strip").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
    }

    @Test fun theTaskStripNeverChanges() {
        val flow = LessonFlow(L11)
        compose.setContent {
            GlassTheme { LessonPlayer(PaddingValues(), onExit = {}, onFinish = {}, theme = LessonTheme.Fly, lesson = flow.lesson, flow = flow, motion = false, humoAlive = false) }
        }
        compose.waitForIdle()
        for (i in listOf(0, 3)) {
            compose.runOnIdle { flow.goTo(Screen.Page(i)) }; compose.waitForIdle()
            val idle = strip()
            // a move (Humo's cue), a hint, a wrong try, a joke: the strip is the same each time
            compose.runOnIdle { flow.dropAt(Node(1, 1)); flow.hint() }; compose.waitForIdle()
            assertEquals("page ${i + 1} after a move and a hint", idle, strip())
            compose.runOnIdle { flow.farMiss() }; compose.waitForIdle()
            assertEquals("page ${i + 1} after a joke", idle, strip())
            compose.runOnIdle { val m = flow.page!!.move; flow.tapTile(m.x + 1); flow.tapTile(m.y + 1); flow.check() }; compose.waitForIdle()
            assertEquals("page ${i + 1} after a wrong try", idle, strip())
            compose.runOnIdle { flow.tryAgain() }; compose.waitForIdle()
            assertEquals("page ${i + 1} after Try again", idle, strip())
        }
    }
}
