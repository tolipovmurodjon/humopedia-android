package com.mcompany.humo.ui

import com.mcompany.humo.ui.lesson.Cue
import com.mcompany.humo.ui.lesson.LessonSounds
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The native event -> sound table equals the web player's (engine/engine.js NAMES, as the web triggers them):
 * tick on every grid node and every keypad tap = toggle-on, pickup = drag-started, drop = drag-released, right = correct,
 * wrong on a lesson page = lesson-failed, wrong in the skill check or fix-it = incorrect, Try again and Ask = lightweight-choice,
 * Show me how = hint, Show me = skip, Continue = continue. The manifest is the one place Boss swaps a sound set.
 */
class LessonSoundTableTest {
    private val web = mapOf(
        Cue.Toggle to "toggle-on", Cue.DragStart to "drag-started", Cue.DragEnd to "drag-released", Cue.Correct to "correct",
        Cue.Failed to "lesson-failed", Cue.Incorrect to "incorrect", Cue.Tick to "lightweight-choice", Cue.Hint to "hint",
        Cue.Skip to "skip", Cue.Continue to "continue",
    )

    private val dir = File("src/main/assets/sounds")

    @Test fun nativeTableEqualsTheWebList() {
        assertEquals(web, LessonSounds.parseManifest(File(dir, "manifest.json").readText()))
    }

    @Test fun everyNamedFileIsThere() {
        web.values.forEach { assertTrue("$it.mp3 missing from the sound set", File(dir, "$it.mp3").length() > 0) }
    }
}
