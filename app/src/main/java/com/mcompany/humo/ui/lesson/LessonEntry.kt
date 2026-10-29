package com.mcompany.humo.ui.lesson

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcompany.humo.data.course.CoordinateTransformations
import com.mcompany.humo.data.progress.ProgressStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Saves a finished lesson on the phone (level progress + the sparks as gems), then hands back to the path. */
@HiltViewModel
class LessonViewModel @Inject constructor(private val progress: ProgressStore) : ViewModel() {
    fun finish(level: Int, sparks: Int, done: () -> Unit) {
        val lessons = CoordinateTransformations.levels.firstOrNull { it.n == level }?.lessons ?: 1
        viewModelScope.launch {
            progress.lessonDone(level, lessons, sparks)
            done()
        }
    }
}

/** The current island's one tap opens L1.1 (the only lesson built so far); finishing returns to the path. */
@Composable
fun LessonEntry(level: Int, padding: PaddingValues, close: () -> Unit, vm: LessonViewModel = hiltViewModel()) {
    LessonPlayer(padding, onExit = close, onFinish = { sparks -> vm.finish(level, sparks, close) })
}
