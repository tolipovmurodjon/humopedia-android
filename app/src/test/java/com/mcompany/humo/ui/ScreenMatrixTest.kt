package com.mcompany.humo.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mcompany.humo.data.course.CoordinateTransformations
import com.mcompany.humo.ui.glass.GlassTheme
import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.L12
import com.mcompany.humo.ui.lesson.LessonContent
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.LessonPlayer
import com.mcompany.humo.ui.lesson.LessonTheme
import com.mcompany.humo.ui.lesson.Node
import com.mcompany.humo.ui.lesson.PageKind
import com.mcompany.humo.ui.lesson.Screen
import com.mcompany.humo.ui.lesson.lessonAt
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
 * Every main screen at the three phone widths of our learners (390, 360, 320 dp) and at font 1.0 and 1.5. Each picture
 * goes through [ScreenChecks] (cut text, emoji) and lands in build/outputs/roborazzi/matrix/<width>-<font>/, where
 * .github/scripts/screen-checks.py also scans it for red. Fake data only; no clock is read.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreenMatrix(private val width: Int) {
    @get:Rule val compose = createComposeRule()

    private val demo = PathUiState(CoordinateTransformations, current = 2, gems = 120, streakDays = 3)
    private var tab by mutableIntStateOf(0)
    private var scale by mutableFloatStateOf(1f)
    private var started = false
    private var folder = ""

    private fun begin(font: Float) { folder = "w$width-f${(font * 100).toInt()}" }

    private fun shot(name: String) {
        ScreenChecks.check(compose, "$folder/$name")
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/matrix/$folder/$name.png")
    }

    private fun shell(startTab: Int, fontScale: Float) {
        tab = startTab
        scale = fontScale
        if (!started) {
            started = true
            compose.setContent {
                val d = LocalDensity.current
                key(tab, scale) {
                    CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                        GlassTheme {
                            AppShell(
                                startTab = tab,
                                path = { padding, open -> PathScreen(demo, padding, open, motion = false) },
                                lesson = { n, padding, close -> LessonPlayer(padding, onExit = close, onFinish = { close() }, lesson = lessonAt(n), motion = false) },
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /** One scenario per test: the shell keeps its sheet open between calls, so a scenario never shares a test. */
    private fun shellScenario(font: Float, name: String, steps: () -> Unit) {
        begin(font)
        shell(0, font)
        steps()
        shot("shell-$name")
    }

    private fun click(description: String) {
        compose.onNodeWithContentDescription(description).performClick()
        compose.waitForIdle()
    }

    private fun everyTab(font: Float) {
        begin(font)
        Tab.entries.forEachIndexed { i, t ->
            shell(i, font)
            shot("shell-tab-${i + 1}-${t.name.lowercase()}")
        }
    }

    private fun player(flow: LessonFlow, font: Float) {
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, font)) {
                GlassTheme { LessonPlayer(PaddingValues(), onExit = {}, onFinish = {}, theme = LessonTheme.Fly, lesson = flow.lesson, flow = flow, motion = false, humoAlive = false) }
            }
        }
        compose.waitForIdle()
    }

    private fun act(block: () -> Unit) { compose.runOnIdle(block); compose.waitForIdle() }

    /** Every page: start, then (graded pages) a wrong try, then the right one; then Humo's chat sheet. */
    private fun lessonPages(font: Float, tag: String, lesson: LessonContent) {
        begin(font)
        val flow = LessonFlow(lesson)
        player(flow, font)
        for (i in lesson.pages.indices) {
            act { flow.goTo(Screen.Page(i)) }
            val p = lesson.pages[i]
            shot("$tag-${p.id}-start")
            when (p.kind) {
                PageKind.Drag -> {
                    act { flow.dropAt(p.points[0] + Node(p.move.y, p.move.x)); flow.check() }
                    shot("$tag-${p.id}-wrong")
                    act { flow.tryAgain(); flow.dropAt(p.points[0] + p.move); flow.check() }
                    shot("$tag-${p.id}-correct")
                }
                PageKind.Translate -> {
                    fun fill(dx: Int, dy: Int) { if (p.fixedX == null) flow.tapTile(p.move.x + dx); if (p.fixedY == null) flow.tapTile(p.move.y + dy) }
                    act { fill(if (p.move.x > 0) -1 else 1, if (p.move.y > 0) -1 else 1); flow.check() }
                    shot("$tag-${p.id}-wrong")
                    act { flow.tryAgain(); flow.startOver(); fill(0, 0); flow.check() }
                    shot("$tag-${p.id}-correct")
                }
                else -> Unit
            }
        }
        act { flow.goTo(Screen.Page(0)); flow.chat.open = true }
        shot("$tag-chat-sheet")
    }

    /** The skill check with a miss, Humo's fix-it round and the end card. */
    private fun lessonEnding(font: Float, tag: String, lesson: LessonContent) {
        begin(font)
        val flow = LessonFlow(lesson)
        player(flow, font)
        act { flow.beginSkillSplash() }
        shot("$tag-skill-splash")
        act { flow.startSkill() }
        for (j in flow.skill.indices) {
            val p = flow.skill[j]
            shot("$tag-skill-${p.id}-start")
            if (j == 1) act { flow.dontKnow() } else act {
                if (p.kind == PageKind.Drag) flow.dropAt(p.points[0] + p.move)
                else { if (p.fixedX == null) flow.tapTile(p.move.x); if (p.fixedY == null) flow.tapTile(p.move.y) }
                flow.check()
            }
            act { flow.nextSkill() }
        }
        shot("$tag-fix-intro")
        act { flow.fixStart() }
        shot("$tag-fix-demo")
        act { flow.next() }
        shot("$tag-fix-1")
        act { flow.finish() }
        shot("$tag-end-card")
    }

    @Test fun tabsFont100() = everyTab(1f)
    @Test fun tabsFont150() = everyTab(1.5f)
    @Test fun pathFont100() = shellScenario(1f, "path") {}
    @Test fun pathFont150() = shellScenario(1.5f, "path") {}
    @Test fun doneSheetFont100() = shellScenario(1f, "sheet-done") { click("Lesson 1.1, Translating Points, done") }
    @Test fun doneSheetFont150() = shellScenario(1.5f, "sheet-done") { click("Lesson 1.1, Translating Points, done") }
    @Test fun lockedSheetFont100() = shellScenario(1f, "sheet-locked") { click("Lesson 1.3, Translating Shapes, locked") }
    @Test fun lockedSheetFont150() = shellScenario(1.5f, "sheet-locked") { click("Lesson 1.3, Translating Shapes, locked") }
    @Test fun courseListFont100() = shellScenario(1f, "course-list") { click("Kurs: Coordinate Transformations") }
    @Test fun courseListFont150() = shellScenario(1.5f, "course-list") { click("Kurs: Coordinate Transformations") }
    @Test fun leaveDialogFont100() = shellScenario(1f, "leave-dialog") { click("Lesson 1.2, Negative Translations, current"); click("Exit lesson") }
    @Test fun leaveDialogFont150() = shellScenario(1.5f, "leave-dialog") { click("Lesson 1.2, Negative Translations, current"); click("Exit lesson") }
    @Test fun l11PagesFont100() = lessonPages(1f, "l11", L11)
    @Test fun l11PagesFont150() = lessonPages(1.5f, "l11", L11)
    @Test fun l12PagesFont100() = lessonPages(1f, "l12", L12)
    @Test fun l12PagesFont150() = lessonPages(1.5f, "l12", L12)
    @Test fun l11EndingFont100() = lessonEnding(1f, "l11", L11)
    @Test fun l11EndingFont150() = lessonEnding(1.5f, "l11", L11)
}

@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi", application = android.app.Application::class)
class ScreenMatrix390 : ScreenMatrix(390)

@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi", application = android.app.Application::class)
class ScreenMatrix360 : ScreenMatrix(360)

@Config(sdk = [35], qualifiers = "w320dp-h640dp-xxhdpi", application = android.app.Application::class)
class ScreenMatrix320 : ScreenMatrix(320)
