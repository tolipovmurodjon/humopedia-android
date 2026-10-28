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

    suspend fun setCurrentLevel(level: Int) {
        context.progress.edit { it[CURRENT_LEVEL] = level }
    }

    companion object {
        const val DEMO_START_LEVEL = 3
        private val CURRENT_LEVEL = intPreferencesKey("trans.current_level")
    }
}
