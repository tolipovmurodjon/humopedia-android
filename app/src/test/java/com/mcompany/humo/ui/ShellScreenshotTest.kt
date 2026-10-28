package com.mcompany.humo.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mcompany.humo.data.course.CoordinateTransformations
import com.mcompany.humo.ui.glass.GlassTheme
import com.mcompany.humo.ui.path.PathScreen
import com.mcompany.humo.ui.path.PathUiState
import com.mcompany.humo.ui.shell.AppShell
import com.mcompany.humo.ui.shell.Tab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The shell at the A05s's size (390x844 dp class), each tab, the path, the lesson, Boss's font at 1.5x and a phone in
 * dark mode. PNGs go to build/outputs/roborazzi and are kept as a CI artifact for a human to look at.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class ShellScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val demo = PathUiState(CoordinateTransformations, current = 3)

    private var tab by mutableIntStateOf(0)
    private var scale by mutableFloatStateOf(1f)
    private var started = false

    /** One setContent per test (the rule allows only one); later calls swap the tab and font through state. */
    private fun shell(startTab: Int = 0, fontScale: Float = 1f) {
        tab = startTab
        scale = fontScale
        if (!started) {
            started = true
            compose.setContent {
                val d = LocalDensity.current
                key(tab, scale) {
                    CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                        GlassTheme { AppShell(startTab = tab, path = { padding, open -> PathScreen(demo, padding, open) }) }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")

    @Test fun pathOpensAtTheCurrentStone() {
        shell()
        compose.onNodeWithContentDescription("Level 3, Rotations, current").assertIsDisplayed()
        shot("01-home-path")
    }

    @Test fun everyTab() {
        Tab.entries.forEachIndexed { i, tab ->
            if (i == 0) return@forEachIndexed
            shell(startTab = i)
            compose.onNodeWithText(tab.label).assertIsDisplayed()
            shot("02-tab-${i + 1}-${tab.name.lowercase()}")
        }
    }

    @Test fun oneTapOnTheCurrentStoneOpensItsLessonAndBackReturns() {
        shell()
        compose.onNodeWithContentDescription("Level 3, Rotations, current").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Lesson 3.1 · Rotation Around the Origin").assertIsDisplayed()
        shot("03-lesson")
        compose.onNodeWithContentDescription("Orqaga").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Level 3, Rotations, current").assertIsDisplayed()
    }

    @Test fun barTabsAreNamedForTalkBack() {
        shell()
        Tab.entries.forEach { compose.onNodeWithContentDescription(it.label).assertIsDisplayed() }
        compose.onNodeWithContentDescription("Olamlar").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Olamlar").assertIsDisplayed()
    }

    @Test fun pathScrolledToTheTop() {
        shell()
        compose.onNodeWithContentDescription("Level 14, Stretching, locked").assertExists()
        compose.onNodeWithContentDescription("Path").performScrollToTop()
        shot("04-home-path-top")
    }

    @Test fun bigFont() {
        shell(fontScale = 1.5f)
        shot("05-home-path-font150")
        shell(startTab = 4, fontScale = 1.5f)
        shot("06-me-font150")
    }

    @Config(qualifiers = "+night")
    @Test fun darkModePhone() {
        shell()
        shot("07-home-path-dark-phone")
    }

    @Config(qualifiers = "w844dp-h390dp-xxhdpi")
    @Test fun landscape() {
        shell()
        shot("08-home-path-landscape")
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.performScrollToTop() =
    performTouchInput { repeat(6) { swipeDown(startY = top + 40f, endY = bottom - 40f) } }
