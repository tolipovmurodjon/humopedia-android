package com.mcompany.humo.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mcompany.humo.ui.lesson.HumoVoice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Moving to the next slide cuts the old slide's line: after 150 ms nothing of it is left; a line the new slide started is left alone. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = android.app.Application::class)
class LessonVoiceCutTest {
    @Test fun oldSlideLineIsGoneWithin150ms() {
        val voice = HumoVoice(ApplicationProvider.getApplicationContext())
        voice.markSpeakingForTest(100L)
        assertTrue(voice.speaking)
        assertTrue(voice.interruptStartedBefore(200L))
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(150))
        assertFalse(voice.speaking)
    }

    @Test fun aLineTheNewSlideStartedSurvives() {
        val voice = HumoVoice(ApplicationProvider.getApplicationContext())
        voice.markSpeakingForTest(300L)
        assertFalse(voice.interruptStartedBefore(200L))
        assertTrue(voice.speaking)
    }
}
