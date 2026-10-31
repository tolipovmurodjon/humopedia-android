package com.mcompany.humo.ui.lesson

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
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
    fun finish(n: Int, sparks: Int, done: () -> Unit) {
        viewModelScope.launch {
            progress.lessonDone(n, sparks)
            done()
        }
    }
}

/** Lesson [n] of the course (its place on the path); finishing saves it and returns to the path. */
@Composable
fun LessonEntry(n: Int, padding: PaddingValues, close: () -> Unit, vm: LessonViewModel = hiltViewModel()) {
    // Cold start to the lesson's first drawn frame, written to the log for the CI walkthrough's limit (docs/qa/round5).
    val opened = remember { android.os.SystemClock.elapsedRealtime() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        withFrameNanos { }
        android.util.Log.i("HumoPerf", "lesson-first-frame ms=${android.os.SystemClock.elapsedRealtime() - opened}")
    }
    LessonPlayer(padding, onExit = close, onFinish = { sparks -> vm.finish(n, sparks, close) }, lesson = lessonAt(n))
}

/** The built lesson at place [n] on the path (L1.1 when the place has none yet, which the path never opens). */
fun lessonAt(n: Int): LessonContent = Lessons[CoordinateTransformations.lessons.getOrNull(n - 1)?.code] ?: L11
