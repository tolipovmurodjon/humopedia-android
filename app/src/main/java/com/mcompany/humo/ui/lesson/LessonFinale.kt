package com.mcompany.humo.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.mcompany.humo.ui.path.Jet.jet
import com.mcompany.humo.ui.path.Jet.jetShadow
import com.mcompany.humo.ui.path.rememberJetAtlas
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * The end-of-lesson pass's route (Boss, 2026-10-31: "too short, I could not see it"): the SR-71, half the screen's width
 * long, enters at the bottom left corner and flies one long diagonal (22.5 degrees from straight up) across the whole
 * screen and out past the top edge in 3.3 s, easing in and out. Pure numbers so the JVM tests can check the route.
 */
internal object MachPassRoute {
    const val MS = 3300L
    /** The 64-frame atlas turns in 5.625 degree steps; 5 steps is the 28 degrees the web uses. */
    const val DEG = 22.5f
    const val SIZE_OF_WIDTH = 0.5f

    fun ease(u: Float) = if (u < 0.5f) 2f * u * u else 1f - (-2f * u + 2f).pow(2) / 2f

    fun size(w: Float) = w * SIZE_OF_WIDTH
    fun bannerW(s: Float) = s * 1.5f
    fun rope(s: Float) = s * 0.22f
    fun start(w: Float, h: Float) = Offset(-w * 0.1f, h + size(w) * 0.35f)
    fun distance(w: Float, h: Float): Float { val s = size(w); return hypot(w, h) + s * 1.2f + bannerW(s) + rope(s) }
    private fun fwd(): Offset { val d = Math.toRadians(DEG.toDouble()); return Offset(sin(d).toFloat(), -cos(d).toFloat()) }

    /** The jet's centre at progress [u] (0 ... 1). */
    fun at(u: Float, w: Float, h: Float): Offset { val s = start(w, h); val f = fwd(); val d = ease(u) * distance(w, h); return Offset(s.x + f.x * d, s.y + f.y * d) }
}

/** The pass's running state: plain fields redrawn through a frame counter. */
private class PassRun {
    var elapsed = 0f
    var now = 0L
    var active = false
    var done = true
    var boomAt = -1L
    val puffs = ArrayList<Long>()
    val px = ArrayList<Offset>()
    fun reset() { elapsed = 0f; now = 0L; active = false; done = false; boomAt = -1L; puffs.clear(); px.clear() }
}

/**
 * The peak at the end (peak-end rule): when the lesson-complete screen shows, the learner's SR-71 makes one pass from the
 * bottom left to the top right with fire from both nozzles, a continuous soft smoke trail and a white cloth banner on a
 * rope ("Perfect!" when the skill check was 3/3, else "Well done!"); a white ring opens half-way. A tap asks for another
 * pass once it is over. It is drawn over the screen but takes no touch, so the buttons work the whole time.
 */
@Composable
internal fun MachPass(taps: State<Int>, perfect: Boolean, modifier: Modifier = Modifier) {
    val atlas = rememberJetAtlas()
    val density = LocalDensity.current.density
    val cfg = LocalConfiguration.current
    val w = cfg.screenWidthDp * density
    val h = cfg.screenHeightDp * density
    val frame = remember { mutableLongStateOf(0L) }
    val run = remember { PassRun() }
    var runs by remember { mutableIntStateOf(0) }
    val seen = remember { intArrayOf(taps.value) }
    LaunchedEffect(taps.value) { if (taps.value != seen[0]) { seen[0] = taps.value; if (run.done) runs++ } }
    val text = rememberTextMeasurer()
    LaunchedEffect(runs) {
        run.reset()
        delay(if (runs == 0) 450L else 0L)
        LessonSounds.play(Cue.Whoosh)
        run.active = true
        var last = -1L
        while (run.elapsed < MachPassRoute.MS) {
            withFrameMillis { now ->
                val dt = if (last < 0) 16.7f else (now - last).toFloat().coerceIn(1f, 50f)
                last = now
                run.elapsed += dt
                run.now = now
                val u = (run.elapsed / MachPassRoute.MS).coerceAtMost(1f)
                val (tx, ty) = tailPoint(MachPassRoute.at(u, w, h), MachPassRoute.size(w))
                run.puffs.add(now)
                run.px.add(Offset(tx, ty))
                while (run.puffs.isNotEmpty() && now - run.puffs[0] > 1400L) { run.puffs.removeAt(0); run.px.removeAt(0) }
                if (run.boomAt < 0 && u > 0.45f) { run.boomAt = now; LessonSounds.play(Cue.Boom) }
                frame.longValue = now
            }
        }
        run.done = true
        run.active = false
        frame.longValue++
    }
    Canvas(modifier.fillMaxSize()) {
        frame.longValue
        if (!run.active) return@Canvas
        val s = MachPassRoute.size(w)
        val u = (run.elapsed / MachPassRoute.MS).coerceAtMost(1f)
        val pos = MachPassRoute.at(u, w, h)
        drawSmoke(run, s)
        if (run.boomAt >= 0) {
            val k = ((run.now - run.boomAt) / 900f).coerceIn(0f, 1f)
            drawCircle(Color.White, 10.dp.toPx() * (1f + 25f * k), Offset(w * 0.5f, h * 0.55f), alpha = 0.95f * (1f - k), style = Stroke(2.dp.toPx()))
        }
        val heading = Math.toRadians(MachPassRoute.DEG - 90.0).toFloat()
        val (tx, ty) = tailPoint(pos, s)
        drawBanner(text, if (perfect) "Perfect!" else "Well done!", Offset(tx, ty), s, run.now)
        jetShadow(atlas, pos, s, heading, 0f, 0f, 1f, density)
        jet(atlas, pos, s, heading, 0f, 0f, run.now / 1000f, true, 1f)
    }
}

/** Where the cloth's rope is tied and the smoke is born: behind the jet's tail (player.js `tx`, `ty`). */
private fun tailPoint(pos: Offset, s: Float): Pair<Float, Float> {
    val d = Math.toRadians(MachPassRoute.DEG.toDouble())
    val tail = 0.45f * s
    return (pos.x - sin(d).toFloat() * tail) to (pos.y + cos(d).toFloat() * tail * 0.766f)
}

/** One soft grey puff per frame, 0.045 of the jet's size, growing to 3x and fading over 1.4 s. */
private fun DrawScope.drawSmoke(run: PassRun, s: Float) {
    for (i in run.puffs.indices) {
        val u = ((run.now - run.puffs[i]) / 1400f).coerceIn(0f, 1f)
        val e = 1f - (1f - u) * (1f - u)
        drawCircle(Color(0xFF8A8F98), s * 0.045f * (0.7f + 2.3f * e), run.px[i], alpha = 0.55f * (1f - u * u))
    }
}

/**
 * The cloth on its rope (player.js `ban`): white with a thin grey edge and a green leading edge, the word in Figtree 800;
 * the free end ripples most. Drawn in a frame whose origin is the rope's tie point and whose -x axis runs back along the cloth.
 */
private fun DrawScope.drawBanner(text: TextMeasurer, word: String, tie: Offset, s: Float, now: Long) {
    val bw = MachPassRoute.bannerW(s)
    val bh = s * 0.3f
    val rope = MachPassRoute.rope(s)
    val tw = rope + bw
    val d = Math.toRadians(MachPassRoute.DEG.toDouble())
    val back = Math.toDegrees(atan2(cos(d), -sin(d))).toFloat()
    val t = now.toFloat()
    withTransform({ translate(tie.x, tie.y); rotate(back - 180f, Offset.Zero); translate(-tw, -bh / 2f) }) {
        fun pts(y0: Float) = List(9) { i -> Offset(bw * i / 8f, y0 + sin(t / 90f - i * 0.9f) * bh * 0.08f * (1f - i / 8f)) }
        val top = pts(bh * 0.08f)
        val bot = pts(bh * 0.92f).reversed()
        val rope1 = Path().apply { moveTo(tw, bh / 2f); lineTo(bw, bh * 0.1f); moveTo(tw, bh / 2f); lineTo(bw, bh * 0.9f) }
        drawPath(rope1, Color(0xFF6B7079), style = Stroke(1.5f * density))
        val cloth = Path().apply {
            top.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
            bot.forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(cloth, Color.White)
        drawPath(cloth, Color(0xFFE2E2E2), style = Stroke(1f * density))
        val edge = Path().apply { moveTo(bw * 0.965f, top[8].y); lineTo(bw, top[8].y); lineTo(bw, bot[0].y); lineTo(bw * 0.965f, bot[0].y); close() }
        drawPath(edge, Color(0xFF3FCE69))
        val m = text.measure(word, TextStyle(fontFamily = LessonType.Figtree, fontWeight = FontWeight.ExtraBold, fontSize = (bh * 0.52f).toSp(), color = Color(0xFF111111)))
        drawText(m, topLeft = Offset(bw * 0.5f - m.size.width / 2f, bh * 0.67f - m.firstBaseline))
    }
}
