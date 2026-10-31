package com.mcompany.humo.ui.lesson

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import com.mcompany.humo.R
import java.io.File

/**
 * Everything a lesson needs on its first frame, made once on a background thread a moment after the app starts, so tapping an
 * island does not decode the lesson JSON, the jet atlas and the sounds on the main thread. A file `files/no-warmup` turns the
 * warm-up off: the CI walkthrough uses it to measure the same lesson open cold (the "before" number).
 */
object LessonWarmup {
    @Volatile internal var atlas: ImageBitmap? = null

    fun start(context: Context) {
        val app = context.applicationContext
        Thread({
            LessonSounds.loadLocalPack(app)
            if (File(app.filesDir, "no-warmup").exists()) return@Thread
            val t0 = System.nanoTime()
            Lessons.size // reads both lesson files and builds their pages
            atlas = ImageBitmap.imageResource(app.resources, R.drawable.sr71_atlas64)
            for (cue in listOf(Cue.Tick, Cue.Toggle, Cue.Correct, Cue.Incorrect, Cue.DragStart, Cue.DragEnd, Cue.Continue)) LessonSounds.prepare(cue)
            Log.i("HumoPerf", "warmup ms=${(System.nanoTime() - t0) / 1_000_000}")
        }, "lesson-warmup").start()
    }
}
