package com.mcompany.humo.voice

import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.LessonLines
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Humo speaks only from our bundled clips (Boss, 2026-10-31: some lines were spoken and others silent; the phone's own
 * robotic voice must never speak). Every line the player can show has a clip that exists in assets/voice, and no source file
 * refers to Android's TextToSpeech. To fix a failure: node scripts/bundle-voice.cjs (it makes the missing clips).
 */
class LessonVoiceCoverageTest {
    @Test fun `every line Humo can show in L1_1 has a bundled clip`() {
        val lines = LessonLines.all(L11, L11.praise)
        val missing = lines.filter { it !in L11.voice }
        assertTrue("lines without a clip (run scripts/bundle-voice.cjs): $missing", missing.isEmpty())
        assertTrue("at least the coach, praise and tutor lines", lines.size > 40)
    }

    @Test fun `every clip in the voice map is a file in the app`() {
        val gone = L11.voice.values.filter { !File("src/main/assets/voice/$it").isFile }
        assertEquals("clips named in l11.json but not bundled", emptyList<String>(), gone)
    }

    @Test fun `no source file uses the system text to speech`() {
        val hits = File("src/main").walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "java", "xml") }
            .filter { f -> f.readText().let { "TextToSpeech" in it || "android.speech.tts" in it } }
            .map { it.path }.toList()
        assertEquals("Humo never speaks with the phone's own voice", emptyList<String>(), hits)
    }
}
