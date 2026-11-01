package com.mcompany.humo.ui

import com.mcompany.humo.ui.lesson.L11
import com.mcompany.humo.ui.lesson.LessonFlow
import com.mcompany.humo.ui.lesson.PlaytestLog
import com.mcompany.humo.ui.lesson.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The eight playtest events land in one local file, with a session id and nothing that names the child. */
class LessonPlaytestLogTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun eightEventsWithASessionIdOnly() {
        PlaytestLog.sync = true
        PlaytestLog.attach(tmp.root)
        val flow = LessonFlow(L11)
        flow.goTo(Screen.Page(3))
        flow.hint()                                                              // help_tap
        val m = flow.page!!.move; flow.tapTile(m.x + 1); flow.tapTile(m.y + 1); flow.check() // preview_count
        flow.tryAgain()                                                          // retry
        PlaytestLog.log("hesitation", flow.page!!.id, 5000)
        flow.goTo(Screen.Page(4))                                                // page_time
        flow.bye()                                                               // quit_point
        flow.goTo(Screen.Sky)                                                    // one_more
        flow.restart()                                                           // replay
        val text = java.io.File(tmp.root, "playtest.jsonl").readText()
        for (e in PlaytestLog.EVENTS) assertTrue("no $e line in the log", text.contains("\"e\":\"$e\""))
        assertEquals(8, PlaytestLog.EVENTS.size)
        assertTrue(text.lines().filter { it.isNotBlank() }.all { it.contains("\"s\":\"${PlaytestLog.session}\"") })
        assertFalse("a name field", text.contains("name"))
        PlaytestLog.sync = false
    }
}
