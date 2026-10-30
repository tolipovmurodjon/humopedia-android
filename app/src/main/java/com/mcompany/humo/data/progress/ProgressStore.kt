package com.mcompany.humo.data.progress

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.progress by preferencesDataStore(name = "progress")

/**
 * The learner's place on the path, on the phone only for now (accounts and the server copy come later). DataStore reads
 * on its own thread, so the first frame never waits on the disk.
 */
@Singleton
class ProgressStore @Inject constructor(@ApplicationContext private val context: Context) {
    /**
     * The lesson the learner is on (1-based place on the path; 1 = lesson 1.1). A new key since alpha07: the path's
     * islands became lessons, so the old level number (demo 3) must not carry over.
     */
    val currentLesson: Flow<Int> = context.progress.data.map { it[CURRENT_LESSON] ?: 1 }

    /** Gems earned (shown in the top bar). Nothing earns them yet, so a new learner honestly has 0. */
    val gems: Flow<Int> = context.progress.data.map { it[GEMS] ?: 0 }

    /** Days in a row with a finished lesson (the flame in the top bar); 0 until lessons count it. */
    val streakDays: Flow<Int> = context.progress.data.map { it[STREAK_DAYS] ?: 0 }

    /** A finished lesson: the next one opens (never moves back on a replay) and its sparks are added to the gems. */
    suspend fun lessonDone(n: Int, sparks: Int) {
        context.progress.edit {
            it[CURRENT_LESSON] = maxOf(it[CURRENT_LESSON] ?: 1, n + 1)
            it[GEMS] = (it[GEMS] ?: 0) + sparks
        }
    }

    companion object {
        private val CURRENT_LESSON = intPreferencesKey("trans.current_lesson")
        private val GEMS = intPreferencesKey("gems")
        private val STREAK_DAYS = intPreferencesKey("streak_days")
    }
}
