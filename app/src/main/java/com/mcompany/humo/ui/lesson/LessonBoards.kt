package com.mcompany.humo.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mcompany.humo.ui.glass.pressScale
import com.mcompany.humo.ui.path.rememberJetAtlas
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt

/** Brilliant's page easing (cubic-bezier(.2,.8,.2,1)) and the decelerating glide measured on P7's Play (~ ease-out cubic). */
internal val BrilliantEase = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
internal val GlideEase = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private const val NOSE_UP = -PI.toFloat() / 2f

/** The puff and the burst are sized from the jet's length. */
private const val PUFF_R = 0.07f

/**
 * Lets the player ask a tile board to fly its machines before a check is graded (Boss: "I enter numbers, I click Check,
 * it should animate and then check the answer"). The board registers itself; a board with nothing to fly leaves it null.
 */
internal class FlightDeck {
    var launcher: (suspend () -> Unit)? = null
    suspend fun fly() { launcher?.invoke() }
}

/**
 * P1, P2: Brilliant's "Graph" with one draggable point (spec §3). Drag it (it snaps to the nearest node on release), or
 * on the fly layer tap the jet and then a node: the nose turns first, then it flies there calmly while every other touch
 * is held back. The jet is glued to the point while it is dragged; Check shows the tick or the cross and the jet stays
 * exactly where the child put it.
 */
@Composable
internal fun DragBoard(page: LessonPage, flow: LessonFlow, fly: Boolean, motion: Boolean, width: Dp) {
    val density = LocalDensity.current
    val wPx = with(density) { width.toPx() }
    val g = remember(page, wPx) { BoardGeo(page.graph, wPx / page.graph.boxW) }
    val text = rememberTextMeasurer()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val atlas = if (fly) rememberJetAtlas() else null
    val start = g.px(page.points[0])
    val handle = remember(page, wPx) { Animatable(flow.dragAt?.let { g.px(it) } ?: start, Offset.VectorConverter) }
    var dragging by remember(page) { mutableStateOf(false) }
    var armed by remember(page) { mutableStateOf(false) }
    var busy by remember(page) { mutableStateOf(false) }
    val wake = remember(page) { mutableIntStateOf(0) }
    val dotR by animateFloatAsState(if (dragging) 15f else 5f, tween(if (motion) 120 else 0), label = "dot")
    val len = g.cell * 1.45f
    val craft = remember(page, wPx) { Craft(handle.value) }
    val tick = remember { mutableLongStateOf(0L) }
    val rnd = remember { Random(3) }

    // The point follows the lesson's state: Start over puts it back where it began; a node set from outside a gesture
    // (state restore, tests) moves it there. A drop or a flight already animating is left alone.
    LaunchedEffect(flow.dragAt) {
        val to = flow.dragAt?.let { g.px(it) } ?: start
        if (dragging || busy || handle.isRunning || handle.value == to) return@LaunchedEffect
        if (motion && flow.dragAt == null) handle.animateTo(to, tween(200, easing = BrilliantEase)) else handle.snapTo(to)
    }
    // The jet's frame loop runs only while something moves; then it waits for the point to move or the jet to be armed.
    LaunchedEffect(page, fly, motion) {
        if (!fly || !motion) return@LaunchedEffect
        var last = -1L
        while (isActive) {
            var live = true
            while (live) {
                withFrameMillis { now ->
                    val dt = if (last < 0) 16.7f else (now - last).toFloat().coerceIn(1f, 64f)
                    last = now
                    val moving = Flying.glue(craft, handle.value, dt, now, g.cell, len / 0.857f, 5f * density.density, steering = dragging || busy, flying = busy, rnd = rnd)
                    live = moving || dragging || busy || armed
                    flow.look(if (busy) humoLookAt(craft.pos.x, craft.pos.y, wPx, wPx * page.graph.boxH / page.graph.boxW) else null)
                    tick.longValue = now
                }
            }
            last = -1L
            val seen = wake.intValue
            snapshotFlow { Triple(handle.value, armed, wake.intValue) }.first { (h, a, w) -> a || w != seen || dist(h, craft.pos) > 0.5f }
        }
    }

    fun land(n: Node) {
        flow.dropAt(n)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    // Tap-then-tap: the nose turns to the spot first (at most 700 ms), then a calm eased flight (40+ frames), a soft tick on
    // every node it crosses; every other touch is ignored until it has landed.
    fun flyTo(n: Node) {
        val to = g.px(n)
        val from = handle.value
        if (!motion) { scope.launch { handle.snapTo(to); land(n) }; return }
        busy = true
        wake.intValue++
        scope.launch {
            try {
                val aim = Flying.bearing(from, to)
                craft.aim = aim
                val t0 = System.nanoTime()
                while (abs(Flying.angDiff(craft.deg, aim)) > 6f && System.nanoTime() - t0 < 700_000_000L) withFrameMillis { }
                craft.burstAt = tick.longValue
                LessonSounds.play(Cue.Whoosh)
                val steps = max(40f, dist(from, to) / density.density / 2.4f)
                val ticks = launch { snapshotFlow { g.snapPx(handle.value) }.distinctUntilChanged().drop(1).collect { LessonSounds.play(Cue.Toggle) } }
                handle.animateTo(to, tween((steps * 16.7f).roundToInt(), easing = Flying.InOut))
                ticks.cancel()
                land(n)
                delay(450)
            } finally {
                craft.aim = null
                busy = false
            }
        }
    }

    val moved = flow.dragAt?.let { it != page.points[0] } ?: false
    val described = "Graph. The point is at ${flow.dragAt ?: page.points[0]}."
    Canvas(
        Modifier.width(width).height(width * (page.graph.boxH / page.graph.boxW)).testTag("board")
            .semantics { contentDescription = described }
            .pointerInput(page, fly, wPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (busy) {
                        // a flight is under way: this touch is held back (a hover cannot steer it)
                        down.consume()
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }
                    if (flow.locked) return@awaitEachGesture
                    val grab = max(28.dp.toPx(), if (fly) len * 0.6f else 0f)
                    if (dist(down.position, handle.value) > grab) {
                        if (fly && armed) {
                            val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                            armed = false
                            flyTo(g.snapPx(up.position))
                        }
                        return@awaitEachGesture
                    }
                    down.consume()
                    var dragged = false
                    var lastNode = g.snapPx(handle.value)
                    val grabOffset = handle.value - down.position
                    drag(down.id) { change ->
                        if (!dragged && dist(change.position, down.position) > viewConfiguration.touchSlop) { dragged = true; dragging = true; armed = false }
                        if (dragged) {
                            val (cx, cy) = g.cells(change.position + grabOffset)
                            val p = g.px(cx.coerceIn(-0.3f, page.graph.xMax + 0.3f), cy.coerceIn(-0.3f, page.graph.yMax + 0.3f))
                            // the jet is glued to the finger: the point moves at once, not on the next dispatch
                            scope.launch(start = CoroutineStart.UNDISPATCHED) { handle.snapTo(p) }
                            val n = g.snapPx(p)
                            if (n != lastNode) { lastNode = n; LessonSounds.play(Cue.Toggle) }
                        }
                        change.consume()
                    }
                    if (dragged) {
                        dragging = false
                        val n = g.snapPx(handle.value)
                        scope.launch { if (motion) handle.animateTo(g.px(n), tween(90, easing = GlideEase)) else handle.snapTo(g.px(n)) }
                        land(n)
                        LessonSounds.play(Cue.Tick)
                    } else if (fly) {
                        armed = !armed
                        wake.intValue++
                        LessonSounds.play(Cue.Tick)
                    }
                }
            },
    ) {
        tick.longValue
        graph(g, text, dragging)
        humoMarks(g, flow.marks)
        val s = g.s
        val p = handle.value
        // The grey start dot shows once the machine has left it; never a dot or ring under a flying jet.
        if (atlas == null || dist(if (motion) craft.pos else p, start) > len * 0.45f) drawCircle(BoardColors.Start, 5f * s, start)
        val ok = flow.phase == Phase.Correct
        val bad = flow.phase == Phase.Wrong
        if (atlas != null) {
            val now = tick.longValue
            val at = if (motion) craft.pos else p
            val heading = when {
                motion -> craft.heading
                moved -> atan2(p.y - start.y, p.x - start.x)
                else -> NOSE_UP
            }
            drawPuffs(craft.puffs, now, len * PUFF_R)
            val sinceBurst = now - craft.burstAt
            if (motion && sinceBurst in 0..650) {
                val u = sinceBurst / 650f
                drawCircle(Brush.radialGradient(0f to Color(0xFFFFF6D6), 0.4f to Color(0xFFFFB347), 1f to Color.Transparent, center = at, radius = g.cell * 0.9f * (0.3f + u)), g.cell * 0.9f * (0.3f + u), at, alpha = 0.95f * (1f - u))
            }
            val flames = if (motion && busy && craft.speed > 0.02f) max(craft.speed, 0.3f) else 0f
            jetAt(atlas, at, heading, len, flames = flames, t = now / 1000f, motion = motion)
            if (armed && !busy) armedRing(at, len * 0.62f, s)
            if (ok || bad) badge(Offset(at.x, at.y - len * 0.55f - 12f * s), ok, s)
        } else {
            val dot = when { ok -> BoardColors.Green; bad -> BoardColors.Yellow; else -> BoardColors.Blue }
            dragHandle(p, dot, dotR * s, s, halo = !ok)
            if (ok || bad) badge(Offset(p.x, p.y - (if (ok) 21f else 36f) * s), ok, s)
        }
    }
}

/** P3's still figure. */
@Composable
internal fun FigureBoard(page: LessonPage, fly: Boolean, width: Dp) {
    val wPx = with(LocalDensity.current) { width.toPx() }
    val g = remember(page, wPx) { BoardGeo(page.graph, wPx / page.graph.boxW) }
    val text = rememberTextMeasurer()
    val atlas = if (fly) rememberJetAtlas() else null
    Canvas(Modifier.width(width).height(width * (page.graph.boxH / page.graph.boxW)).semantics { contentDescription = "Two points, each moved a steps right and b steps up." }) {
        explainer(g, page, text, atlas)
    }
}

/** A layer that fades its content to [a] (0 ... 1) around [c]. */
private fun DrawScope.faded(a: Float, c: Offset, r: Float, block: DrawScope.() -> Unit) {
    if (a >= 0.99f) { block(); return }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(c.x - r, c.y - r, c.x + r, c.y + r), Paint().apply { alpha = a })
        block()
        canvas.restore()
    }
}

/**
 * The transformation workspace (P4-P6, the skill check) and the recap (P7), spec §5-6: the graph in a framed box with the
 * "translate by ( , )" row under it, the tile tray, and Start over. When both slots are full Brilliant's blue points glide
 * by that translation as a preview, with pale ghosts where they began; the machines WAIT where they are while numbers are
 * tried. On Check ([deck]) they take off, climb, fly a gentle curve to the preview and land; then the answer is graded.
 * P7 has fixed tiles and a Play button: the machines ride the points exactly.
 */
@Composable
internal fun WorkspaceBoard(page: LessonPage, flow: LessonFlow, fly: Boolean, motion: Boolean, width: Dp, deck: FlightDeck) {
    val density = LocalDensity.current
    val u = width / 350f
    val wPx = with(density) { width.toPx() }
    val s = wPx / 350f
    val g = remember(page, wPx) { BoardGeo(page.graph, s) }
    val text = rememberTextMeasurer()
    val atlas = if (fly) rememberJetAtlas() else null
    val recap = page.kind == PageKind.Recap
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val shown = flow.shownMove
    val settled = flow.answered
    fun cells(n: Node?) = n?.let { Offset(it.x.toFloat(), it.y.toFloat()) } ?: Offset.Zero
    val disp = remember(page) { Animatable(cells(shown), Offset.VectorConverter) }
    var playing by remember(page) { mutableStateOf(false) }
    val wake = remember(page) { mutableIntStateOf(0) }
    val tick = remember { mutableLongStateOf(0L) }
    val rnd = remember { Random(5) }
    val len = g.cell * 1.45f

    // One machine per point. A board opened already checked (review, state restore) has them landed; otherwise they wait
    // at their start. The recap's ride the points exactly.
    val crafts = remember(page, wPx) {
        page.points.map { p ->
            val begin = g.px(p)
            val end = shown?.let { g.px(p + it) }
            if (end != null && (recap || settled)) Craft(end, Flying.bearing(begin, end)) else Craft(begin)
        }
    }

    LaunchedEffect(shown) {
        val to = cells(shown)
        if (disp.value == to) return@LaunchedEffect
        if (!motion) { disp.snapTo(to); return@LaunchedEffect }
        wake.intValue++
        delay(200)
        disp.animateTo(to, tween(700, easing = GlideEase))
    }
    // Preview before Check (Boss, 2026-10-31): the machines stay while only one number is in; the moment both are in they fly
    // the move, and clearing a number sends them home. A new change waits for the flight in the air, so quick taps never fight.
    LaunchedEffect(shown, atlas != null) {
        if (recap || atlas == null) return@LaunchedEffect
        while (crafts.any { it.flight != null }) withFrameMillis { }
        var launched = false
        crafts.forEachIndexed { i, c ->
            val want = g.px(if (shown != null) page.points[i] + shown else page.points[i])
            if (dist(c.pos, want) < 1f) return@forEachIndexed
            if (motion) {
                Flying.plan(c, want, withFrameMillis { it }, density.density)
                launched = true
            } else { c.deg = Flying.bearing(c.pos, want); c.pos = want }
        }
        if (launched) { LessonSounds.play(Cue.Whoosh); wake.intValue++ }
    }
    // Check only grades: it waits for a preview flight still in the air, then the player grades.
    DisposableEffect(deck, crafts) {
        deck.launcher = { while (crafts.any { it.flight != null }) withFrameMillis { } }
        onDispose { deck.launcher = null }
    }
    LaunchedEffect(page, fly, motion) {
        if (!fly || !motion) return@LaunchedEffect
        var last = -1L
        while (isActive) {
            var live = true
            while (live) {
                withFrameMillis { now ->
                    val dt = if (last < 0) 16.7f else (now - last).toFloat().coerceIn(1f, 64f)
                    last = now
                    var any = disp.isRunning || playing
                    crafts.forEachIndexed { i, c ->
                        if (recap) {
                            val b = g.px(page.points[i].x + disp.value.x, page.points[i].y + disp.value.y)
                            val d = dist(c.pos, b)
                            if (d > g.cell * 0.9f) c.fadeAt = now // the demo jumped back to its start: show it there with a short fade
                            else if (d > 0.5f) c.deg += Flying.angDiff(c.deg, Flying.bearing(c.pos, b)) * 0.3f
                            c.speed = if (d > 0.5f && d <= g.cell * 0.9f) 0.5f else 0f
                            c.pos = b
                        } else {
                            Flying.fly(c, now, dt, g.cell)
                            Flying.puff(c, now, len / 0.857f * (1f + 0.45f * c.alt), c.flight != null, rnd)
                        }
                        any = any || c.flight != null || c.puffs.isNotEmpty()
                    }
                    live = any
                    flow.look(crafts.firstOrNull { it.flight != null }?.let { humoLookAt(it.pos.x, it.pos.y, wPx, wPx * page.graph.boxH / page.graph.boxW) })
                    tick.longValue = now
                }
            }
            last = -1L
            val seen = wake.intValue
            snapshotFlow { wake.intValue }.first { it != seen }
        }
    }

    val frameH = u * (12f + page.graph.boxH)
    Column(Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.width(width).border(1.5.dp * (u / 1.dp), Color(0xFFE5E5E5), RoundedCornerShape(6.dp * (u / 1.dp)))) {
            val described = buildString {
                append("Graph. Points at ")
                append(page.points.joinToString(", ") { (it + (shown ?: Node(0, 0))).toString() })
                if (!recap) append(". Targets at ${page.targets.joinToString(", ")}")
                append('.')
            }
            Canvas(Modifier.width(width).height(frameH).testTag("board").semantics { contentDescription = described }) {
                translate(2f * s, 12f * s) {
                    graph(g, text)
                    humoMarks(g, flow.marks)
                    val now = tick.longValue
                    val run = disp.value
                    if (!recap || playing) page.targets.forEach { target(g.px(it), s) }
                    page.points.forEach { start ->
                        val a = g.px(start)
                        val b = g.px(start.x + run.x, start.y + run.y)
                        if (dist(a, b) > 0.5f) ghost(a, b, flow.phase.takeIf { !flow.inSkill } ?: Phase.Idle, s)
                    }
                    val phase = if (flow.inSkill) Phase.Idle else flow.phase
                    page.points.forEachIndexed { i, start ->
                        val b = g.px(start.x + run.x, start.y + run.y)
                        if (atlas == null) { workPoint(b, phase, s); return@forEachIndexed }
                        val c = crafts[i]
                        // Brilliant's point is the preview of where the numbers send the machine; a machine sitting on it hides it
                        if (!recap && dist(c.pos, b) >= max(6f * s, g.cell * 0.3f)) workPoint(b, phase, s)
                        drawPuffs(c.puffs, now, len * PUFF_R)
                        val flames = if (c.speed > 0.02f && (c.flight != null || recap)) c.speed else 0f
                        faded(((now - c.fadeAt) / 220f).coerceIn(0f, 1f), c.pos, len) {
                            jetAt(atlas, c.pos, c.heading, len, flames = flames, t = now / 1000f, motion = motion, alt = c.alt)
                        }
                        if (phase != Phase.Idle) badge(Offset(c.pos.x + g.cell * 0.42f, c.pos.y - g.cell * 0.42f), phase == Phase.Correct, s)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.5.dp * (u / 1.dp)).background(Color(0xFFE5E5E5)))
            ActionRow(page, flow, recap, u)
        }
        if (recap) {
            Spacer(Modifier.height(14.dp * (u / 1.dp)))
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier.size(90.dp * (u / 1.dp), 48.dp * (u / 1.dp)).pressScale(interaction).clip(RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0x1A000000), RoundedCornerShape(24.dp)).background(Color.White)
                    .clickable(interaction, null, enabled = !playing, role = Role.Button, onClickLabel = "Play") {
                        LessonSounds.play(Cue.Tick)
                        scope.launch {
                            playing = true
                            wake.intValue++
                            disp.snapTo(Offset.Zero)
                            if (motion) { delay(200); disp.animateTo(cells(page.move), tween(600, easing = GlideEase)) } else disp.snapTo(cells(page.move))
                            playing = false
                        }
                    }
                    .semantics { contentDescription = "Play" },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(16.dp * (u / 1.dp))) {
                        drawPath(Path().apply { moveTo(size.width * 0.12f, 0f); lineTo(size.width, size.height / 2f); lineTo(size.width * 0.12f, size.height); close() }, if (playing) Color(0xFFBDBDBD) else Color.Black)
                    }
                    Spacer(Modifier.width(8.dp * (u / 1.dp)))
                    Text("Play", fontFamily = LessonType.Figtree, fontWeight = FontWeight.Medium, fontSize = with(density) { (16.dp * (u / 1.dp)).toSp() }, color = if (playing) Color(0xFFBDBDBD) else Color.Black)
                }
            }
        } else {
            // The tray keeps its place even when hidden, so nothing under it jumps (RULES: no layout jumps).
            Spacer(Modifier.height(14.dp * (u / 1.dp)))
            val tray = flow.slots.trayShown && !flow.locked
            // Brilliant's tray: 0..5 in one row (60 px); -5..5 in two rows of 6 and 5 (104 px), 10 px apart.
            val rows = page.tiles.chunked(6)
            Box(
                Modifier.width(width).height((if (rows.size > 1) 104.dp else 60.dp) * (u / 1.dp)).alpha(if (tray) 1f else 0f)
                    .clip(RoundedCornerShape(4.dp * (u / 1.dp))).background(Color(0xFFDAE2FF)),
                contentAlignment = Alignment.Center,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp * (u / 1.dp)), horizontalAlignment = Alignment.CenterHorizontally) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp * (u / 1.dp))) {
                            row.forEach { v ->
                                Tile(v, u, enabled = tray) {
                                    flow.tapTile(v)
                                    LessonSounds.play(Cue.Tick)
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(v: Int, u: Dp, enabled: Boolean, onTap: () -> Unit) {
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    val k = u / 1.dp
    Box(
        Modifier.size(40.dp * k, 36.dp * k).pressScale(interaction)
            .clickable(interaction, null, enabled = enabled, role = Role.Button, onClick = onTap)
            .semantics { contentDescription = "Tile $v" }.testTag("tile-$v"),
    ) {
        Box(Modifier.offset(y = 2.dp * k).size(40.dp * k, 34.dp * k).clip(RoundedCornerShape(6.dp * k)).background(Color(0x66000000)))
        Box(Modifier.size(40.dp * k, 34.dp * k).clip(RoundedCornerShape(6.dp * k)).background(Color.White), contentAlignment = Alignment.Center) {
            Text(signed(v), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = with(density) { (16.dp * k).toSp() }, color = Color.Black)
        }
    }
}

/** "1  translate by (▢, ▢)": the row number (a green tick once right), the chip and its two slots. */
@Composable
private fun ActionRow(page: LessonPage, flow: LessonFlow, recap: Boolean, u: Dp) {
    val density = LocalDensity.current
    val k = u / 1.dp
    val fs = with(density) { (16.dp * k).toSp() }
    val right = flow.phase == Phase.Correct && !flow.inSkill
    Row(Modifier.fillMaxWidth().height(66.dp * k).padding(start = 10.dp * k), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(34.dp * k), contentAlignment = Alignment.CenterStart) {
            if (right) Canvas(Modifier.size(18.dp * k)) {
                val u1 = size.width / 18f
                drawRoundRect(Color(0xFF22B14C), Offset.Zero, size, androidx.compose.ui.geometry.CornerRadius(4f * u1))
                drawPath(Path().apply { moveTo(4.5f * u1, 9.3f * u1); lineTo(7.5f * u1, 12.3f * u1); lineTo(13.5f * u1, 6f * u1) }, Color.White, style = Stroke(2.2f * u1, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            else Text("1", fontFamily = FontFamily.Monospace, fontSize = with(density) { (14.dp * k).toSp() }, color = Color(0x99000000))
        }
        Row(
            Modifier.height(32.dp * k).clip(RoundedCornerShape(4.dp * k)).background(Color(0xFFF2F2F2)).padding(horizontal = 8.dp * k),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("translate", fontFamily = LessonType.Figtree, fontWeight = FontWeight.Medium, fontSize = fs, color = Color(0xFF294BC6))
            Text(" by (", fontFamily = LessonType.Figtree, fontSize = fs, color = Color.Black)
            if (recap) { Fixed(signed(page.move.x), u); Text(",", fontSize = fs); Fixed(signed(page.move.y), u) }
            else { Slot(0, flow, u); Text(",", fontSize = fs, color = Color.Black); Slot(1, flow, u) }
            Text(")", fontFamily = LessonType.Figtree, fontSize = fs, color = Color.Black)
        }
    }
}

@Composable
private fun Fixed(v: String, u: Dp) {
    val k = u / 1.dp
    Box(Modifier.size(32.dp * k, 24.dp * k).clip(RoundedCornerShape(4.dp * k)).background(Color(0x0D000000)).border(2.dp * k, Color(0x0D000000), RoundedCornerShape(4.dp * k)), contentAlignment = Alignment.Center) {
        Text(v, fontFamily = FontFamily.Monospace, fontSize = with(LocalDensity.current) { (16.dp * k).toSp() }, color = Color.Black)
    }
}

@Composable
private fun Slot(i: Int, flow: LessonFlow, u: Dp) {
    val k = u / 1.dp
    val v = flow.slots[i]
    if (i in flow.slots.fixed && v != null) {
        Box(Modifier.padding(horizontal = 1.dp).semantics { contentDescription = "Slot ${i + 1}, $v, given" }) { Fixed(signed(v), u) }
        return
    }
    val active = flow.slots.active == i && !flow.locked
    val border = when {
        active -> Color(0xFF456DFF)
        v != null -> Color(0x1A000000)
        else -> Color(0xFF999999)
    }
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.padding(horizontal = 1.dp).size(32.dp * k, 24.dp * k).clip(RoundedCornerShape(4.dp * k))
            .background(if (active) Color(0xFFECF0FF) else Color.White)
            .border(2.dp * k, border, RoundedCornerShape(4.dp * k))
            .clickable(interaction, null, enabled = !flow.locked && v != null, role = Role.Button) { flow.tapSlot(i); LessonSounds.play(Cue.Tick) }
            .semantics { contentDescription = "Slot ${i + 1}, ${v ?: "empty"}" }.testTag("slot-$i"),
        contentAlignment = Alignment.Center,
    ) {
        if (v != null) Text(signed(v), fontFamily = FontFamily.Monospace, fontSize = with(LocalDensity.current) { (16.dp * k).toSp() }, color = Color.Black)
    }
}

/** The ↻ glyph of Start over, drawn: an open circle with an arrow head at its top-left end. */
@Composable
internal fun RestartIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension * 0.36f
        val c = center
        drawArc(color, -200f, 300f, false, topLeft = c - Offset(r, r), size = androidx.compose.ui.geometry.Size(2 * r, 2 * r), style = Stroke(this.size.minDimension * 0.11f, cap = StrokeCap.Round))
        val tip = Offset(c.x - r * 0.94f, c.y - r * 0.34f)
        drawPath(Path().apply { moveTo(tip.x - r * 0.42f, tip.y - r * 0.5f); lineTo(tip.x + r * 0.08f, tip.y + r * 0.3f); lineTo(tip.x + r * 0.55f, tip.y - r * 0.38f); close() }, color)
    }
}
