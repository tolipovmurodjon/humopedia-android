package com.mcompany.humo.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mcompany.humo.ui.path.Jet
import com.mcompany.humo.ui.path.Jet.jet
import com.mcompany.humo.ui.path.Jet.jetShadow
import com.mcompany.humo.ui.path.rememberJetAtlas
import kotlinx.coroutines.delay
import java.util.Random

/**
 * The peak at the end (peak-end rule): when the lesson-complete card shows, the learner's SR-71 makes one 3-second pass
 * across the screen, bottom-left to top-right, breaks the sound barrier with a white ring half-way and leaves twin smoke.
 * A tap boosts it ([taps] counts the taps). It is drawn over the card but takes no touch, so the buttons work the whole
 * time; the pass ends by itself (fly-layer.js finale).
 */
@Composable
internal fun MachPass(taps: State<Int>, modifier: Modifier = Modifier) {
    val atlas = rememberJetAtlas()
    val density = LocalDensity.current.density
    val cfg = LocalConfiguration.current
    val w = cfg.screenWidthDp * density
    val h = cfg.screenHeightDp * density
    val frame = remember { mutableLongStateOf(0L) }
    val state = remember { MachState() }
    LaunchedEffect(Unit) {
        delay(450)
        LessonSounds.play(Cue.Whoosh)
        var last = -1L
        var seen = taps.value
        var boost = 1f
        val rnd = Random(11)
        val size = 120f * density
        while (state.t < 1f) {
            withFrameMillis { now ->
                val dt = if (last < 0) 16.7f else (now - last).toFloat().coerceIn(1f, 50f)
                last = now
                if (taps.value != seen) { seen = taps.value; boost = 2.2f; LessonSounds.play(Cue.Whoosh) }
                state.t = (state.t + dt / (MachPassPath.SECONDS * 1000f) * boost).coerceAtMost(1f)
                boost += (1f - boost) * (1f - Math.pow(0.96, dt / 16.7).toFloat())
                val (pos, deg) = MachPassPath.at(state.t, w, h)
                state.pos = pos
                state.deg = deg
                if (now - state.puffAt > 60) {
                    state.puffAt = now
                    val heading = Math.toRadians(deg - 90.0).toFloat()
                    Jet.nozzles(heading, size).forEach { n -> state.puffs.add(Puff(pos.x + n.x, pos.y + n.y + 2f, now, (rnd.nextFloat() - 0.5f) * 90f, 0f, 0f)) }
                }
                state.puffs.removeAll { now - it.born > 1000L }
                if (state.boomAt < 0 && state.t > 0.48f) { state.boomAt = now; state.boomOn = pos; LessonSounds.play(Cue.Boom) }
                state.now = now
                frame.longValue = now
            }
        }
        state.done = true
        frame.longValue++
    }
    Canvas(modifier.fillMaxSize()) {
        frame.longValue
        if (state.done || state.now == 0L) return@Canvas
        drawPuffs(state.puffs, state.now, 9f * density)
        if (state.boomAt >= 0) {
            val u = ((state.now - state.boomAt) / 700f).coerceIn(0f, 1f)
            val e = 1f - (1f - u) * (1f - u)
            val r = 60f * density * (0.2f + 2.4f * e)
            drawCircle(Color(0x2E375CE3), r + 6f * density, state.boomOn, alpha = 1f - u, style = Stroke(6f * density))
            drawCircle(Color.White, r, state.boomOn, alpha = 0.9f * (1f - u), style = Stroke(3f * density))
        }
        val heading = Math.toRadians(state.deg - 90.0).toFloat()
        jetShadow(atlas, state.pos, 120.dp.toPx(), heading, 0f, 0f, 1f, density)
        jet(atlas, state.pos, 120.dp.toPx(), heading, 0f, 0f, state.now / 1000f, true, 0.8f)
    }
}

/** The pass's running numbers; plain fields, redrawn through the frame counter. */
private class MachState {
    var t = 0f
    var now = 0L
    var pos = Offset.Zero
    var deg = 0f
    var puffAt = 0L
    var boomAt = -1L
    var boomOn = Offset.Zero
    var done = false
    val puffs = ArrayList<Puff>()
}
