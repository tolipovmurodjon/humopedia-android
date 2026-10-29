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
    /** The level the learner is on (1-based). The demo starts at 3, Boss's likely start (NATIVE-BRIEF N0). */
    val currentLevel: Flow<Int> = context.progress.data.map { it[CURRENT_LEVEL] ?: DEMO_START_LEVEL }

    /** Gems earned (shown in the top bar). Nothing earns them yet, so a new learner honestly has 0. */
    val gems: Flow<Int> = context.progress.data.map { it[GEMS] ?: 0 }

    /** Days in a row with a finished lesson (the flame in the top bar); 0 until lessons count it. */
    val streakDays: Flow<Int> = context.progress.data.map { it[STREAK_DAYS] ?: 0 }

    suspend fun setCurrentLevel(level: Int) {
        context.progress.edit { it[CURRENT_LEVEL] = level }
    }

    /** Lessons finished in [level] (shown on its island sheet later). */
    fun lessonsDone(level: Int): Flow<Int> = context.progress.data.map { it[doneKey(level)] ?: 0 }

    /**
     * A finished lesson: one more lesson done in [level] (at most [lessons]), its sparks added to the gems, and when the
     * current level's last lesson is done the next level becomes current.
     */
    suspend fun lessonDone(level: Int, lessons: Int, sparks: Int) {
        context.progress.edit {
            val done = ((it[doneKey(level)] ?: 0) + 1).coerceAtMost(lessons)
            it[doneKey(level)] = done
            it[GEMS] = (it[GEMS] ?: 0) + sparks
            val current = it[CURRENT_LEVEL] ?: DEMO_START_LEVEL
            if (done >= lessons && level == current) it[CURRENT_LEVEL] = current + 1
        }
    }

    companion object {
        const val DEMO_START_LEVEL = 3
        private val CURRENT_LEVEL = intPreferencesKey("trans.current_level")
        private val GEMS = intPreferencesKey("gems")
        private val STREAK_DAYS = intPreferencesKey("streak_days")
        private fun doneKey(level: Int) = intPreferencesKey("trans.done.$level")
    }
}
