package com.mcompany.humo.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mcompany.humo.ui.glass.GlassTheme
import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.LessonPlayer
import com.mcompany.humo.ui.lesson.LessonTheme
import com.mcompany.humo.ui.lesson.Node
import com.mcompany.humo.ui.lesson.Phase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** P3: Quit then Keep learning gives back exactly the page the child left: the dropped point, the verdict, the open tutor. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class LessonKeepLearningTest {
    @get:Rule val compose = createComposeRule()

    @Test fun keepLearningRestoresThePage() {
        val flow = LessonFlow(L11)
        compose.setContent {
            GlassTheme { LessonPlayer(PaddingValues(), onExit = {}, onFinish = {}, theme = LessonTheme.Fly, lesson = flow.lesson, flow = flow, motion = false, humoAlive = false) }
        }
        compose.waitForIdle()
        compose.runOnIdle { flow.dropAt(Node(2, 5)); flow.check() }
        compose.waitForIdle()
        val before = listOf(flow.dragAt, flow.phase, flow.verdict, flow.screen, flow.chat.open)
        assertEquals(Phase.Wrong, flow.phase)
        compose.onNodeWithContentDescription("Exit lesson").performClick(); compose.waitForIdle()
        compose.onNodeWithText("CONTINUE").performClick(); compose.waitForIdle()
        assertEquals(before, listOf(flow.dragAt, flow.phase, flow.verdict, flow.screen, flow.chat.open))
        compose.onNodeWithText("TRY AGAIN").assertExists("the Try again button is gone after Keep learning")
    }
}
