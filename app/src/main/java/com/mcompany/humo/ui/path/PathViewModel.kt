package com.mcompany.humo.ui.path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcompany.humo.data.course.CoordinateTransformations
import com.mcompany.humo.data.course.Course
import com.mcompany.humo.data.progress.ProgressStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The path as the screen draws it: the course and the level the learner is on. */
data class PathUiState(val course: Course, val current: Int)

@HiltViewModel
class PathViewModel @Inject constructor(progress: ProgressStore) : ViewModel() {
    /** Null until DataStore answers (a frame or two): the screen draws the river and waits, never the wrong stone. */
    val state: StateFlow<PathUiState?> = progress.currentLevel
        .map { PathUiState(CoordinateTransformations, it.coerceIn(1, CoordinateTransformations.levels.size)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
