package com.mcompany.humo.ui

import com.mcompany.humo.data.course.CoordinateTransformations
import com.mcompany.humo.data.course.firstLessonLabel
import com.mcompany.humo.ui.path.PathLayout
import com.mcompany.humo.ui.path.StoneState
import com.mcompany.humo.ui.path.stoneState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathLayoutTest {
    private val layout = PathLayout(14, 390f)

    @Test fun `course 15 has the 14 levels of the curriculum, in order`() {
        val levels = CoordinateTransformations.levels
        assertEquals("trans", CoordinateTransformations.id)
        assertEquals((1..14).toList(), levels.map { it.n })
        assertEquals("Translations", levels.first().title)
        assertEquals("Stretching", levels.last().title)
        assertEquals("Lesson 1.1 · Translating Points", levels.first().firstLessonLabel())
        assertEquals("Lesson 10.1 · Congruence", levels[9].firstLessonLabel())
    }

    @Test fun `level 1 is at the bottom and the river climbs`() {
        for (i in 1 until 14) assertTrue(layout.y(i) < layout.y(i - 1))
        assertTrue(layout.y(0) < layout.height)
        assertTrue(layout.y(13) > 0f)
    }

    @Test fun `every stone stays on screen sideways`() {
        for (i in 0 until 14) assertTrue(layout.x(i) - 56f >= 0f && layout.x(i) + 56f <= 390f)
    }

    @Test fun `the current stone opens at thumb height`() {
        val viewport = 844f
        val scroll = layout.scrollFor(2, viewport)
        assertEquals(viewport * PathLayout.THUMB, layout.y(2) - scroll, 0.5f)
    }

    @Test fun `scroll never goes past either end`() {
        assertEquals(0f, layout.scrollFor(13, 844f), 0f)
        assertEquals(layout.height - 844f, layout.scrollFor(0, 844f).coerceAtMost(layout.height - 844f), 0f)
        assertTrue(layout.scrollFor(0, 844f) <= layout.height - 844f)
    }

    @Test fun `stones before the current are done, after it locked`() {
        assertEquals(StoneState.Done, stoneState(2, 3))
        assertEquals(StoneState.Current, stoneState(3, 3))
        assertEquals(StoneState.Locked, stoneState(4, 3))
    }
}
