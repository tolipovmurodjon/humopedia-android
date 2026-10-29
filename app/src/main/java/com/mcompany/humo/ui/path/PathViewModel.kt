package com.mcompany.humo.ui.path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcompany.humo.data.course.CoordinateTransformations
import com.mcompany.humo.data.course.Course
import com.mcompany.humo.data.progress.ProgressStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The path as the screen draws it: the course, the level the learner is on, and the top bar's two counts. */
data class PathUiState(val course: Course, val current: Int, val gems: Int = 0, val streakDays: Int = 0)

@HiltViewModel
class PathViewModel @Inject constructor(progress: ProgressStore) : ViewModel() {
    /** Null until DataStore answers (a frame or two): the screen draws the map and waits, never the wrong stone. */
    val state: StateFlow<PathUiState?> = combine(progress.currentLevel, progress.gems, progress.streakDays) { level, gems, streak ->
        PathUiState(CoordinateTransformations, level.coerceIn(1, CoordinateTransformations.levels.size), gems, streak)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
