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
import androidx.compose.ui.semantics.stateDescription
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
import kotlin.math.hypot
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
/** A machine at rest points 30 degrees right of straight up, as the web player draws it. */
private val NOSE_UP = ((30f - 90f) * PI / 180f).toFloat()

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
                    val moving = Flying.glue(craft, handle.value, dt, now, g.cell, len / 0.857f, 5f * density.density, steering = dragging || busy, flying = busy || dragging, rnd = rnd)
                    live = moving || dragging || busy || armed
                    // Humo's eyes follow the jet at 12 Hz, not every frame: his own picture redraws when they move
                    if (now / 16 % 5 == 0L) flow.look(if (busy) humoLookAt(craft.pos.x, craft.pos.y, wPx, wPx * page.graph.boxH / page.graph.boxW) else null)
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
    Box(
        Modifier.width(width).height(width * (page.graph.boxH / page.graph.boxW)).testTag("board")
            .semantics {
                contentDescription = described
                // where the point is right now, in cells, so a test can hold it against the finger
                if (PerfV2.boardProbe) stateDescription = g.cells(handle.value).let { "at ${"%.2f".format(java.util.Locale.US, it.first)},${"%.2f".format(java.util.Locale.US, it.second)}" }
            }
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
                        if (!dragged && dist(change.position, down.position) > viewConfiguration.touchSlop) { dragged = true; dragging = true; armed = false; LessonSounds.play(Cue.DragStart) }
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
                        LessonSounds.play(Cue.DragEnd)
                    } else if (fly) {
                        armed = !armed
                        wake.intValue++
                        LessonSounds.play(Cue.Tick)
                    }
                }
            },
    ) {
        // The graph never changes while a finger moves: its own canvas reads no state, so only the jet's canvas redraws per frame.
        Canvas(Modifier.matchParentSize()) { graph(g, text) }
        Canvas(Modifier.matchParentSize()) {
        PerfCounters.board++
        tick.longValue
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
            val flames = if (motion && (busy || dragging) && craft.speed > 0.02f) max(craft.speed, 0.3f) else 0f
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
}

/**
 * Waits until no machine is mid-flight. If nothing moves for 3 s while a flight is still set (a lost wake-up, a cancelled launch) the
 * flight is landed where it was going and the frame loop is woken: no input lock may outlive its animation (alpha13 soft-lock on fix-it).
 */
internal suspend fun settleFlights(crafts: List<Craft>, wake: androidx.compose.runtime.MutableIntState) {
    var lastPos = crafts.map { it.pos }
    var movedAt = withFrameMillis { it }
    while (crafts.any { it.flight != null }) {
        val now = withFrameMillis { it }
        val pos = crafts.map { it.pos }
        if (pos != lastPos) { lastPos = pos; movedAt = now }
        else if (now - movedAt > 3000) {
            crafts.forEach { c -> c.flight?.let { f -> c.pos = f.to; c.flight = null; c.speed = 0f; c.alt = 0f } }
            wake.intValue++
        }
    }
}

/** P3's still figure. */
@Composable
internal fun FigureBoard(page: LessonPage, fly: Boolean, width: Dp) {
    // The figure is drawn 12 % smaller and centred, so the jets at its right end are never cut by the canvas edge.
    val inner = width * 0.88f
    val wPx = with(LocalDensity.current) { inner.toPx() }
    val g = remember(page, wPx) { BoardGeo(page.graph, wPx / page.graph.boxW) }
    val text = rememberTextMeasurer()
    val atlas = if (fly) rememberJetAtlas() else null
    Box(Modifier.width(width), contentAlignment = Alignment.Center) {
        Canvas(Modifier.width(inner).height(inner * (page.graph.boxH / page.graph.boxW)).testTag("board").semantics { contentDescription = "Two points, each moved a steps right and b steps up." }) {
            explainer(g, page, text, atlas)
        }
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
    val len = jetLength(page.points, g.cell)

    // One machine per point. A board opened already checked (review, state restore) has them landed; otherwise they wait
    // at their start. The recap's ride the points exactly.
    val offBoard = remember(page) { androidx.compose.runtime.mutableStateListOf<Offset?>().also { l -> repeat(page.points.size) { l.add(null) } } }
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
        settleFlights(crafts, wake)
        var launched = false
        crafts.forEachIndexed { i, c ->
            val raw = g.px(if (shown != null) page.points[i] + shown else page.points[i])
            // a target past the board edge: the machine stops at the edge and an arrow shows where it was going (as the web does)
            val m = len * 0.55f
            val bh = wPx * page.graph.boxH / page.graph.boxW
            val want = Offset(raw.x.coerceIn(m, wPx - m), raw.y.coerceIn(m, bh - m))
            offBoard[i] = if (want != raw) raw - want else null
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
        deck.launcher = { settleFlights(crafts, wake) }
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
                    if (now / 16 % 5 == 0L) flow.look(crafts.firstOrNull { it.flight != null }?.let { humoLookAt(it.pos.x, it.pos.y, wPx, wPx * page.graph.boxH / page.graph.boxW) })
                    tick.longValue = now
                }
            }
            last = -1L
            val seen = wake.intValue
            // A flight planned between the last frame and this read would be missed by the wake count alone: look at the crafts too.
            snapshotFlow { wake.intValue }.first { it != seen || crafts.any { c -> c.flight != null } }
        }
    }

    val frameH = u * (12f + page.graph.boxH)
    Column(Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.width(width)) {
            val described = buildString {
                append("Graph. Points at ")
                append(page.points.joinToString(", ") { (it + (shown ?: Node(0, 0))).toString() })
                if (!recap) append(". Targets at ${page.targets.joinToString(", ")}")
                append('.')
            }
            Box(Modifier.width(width).height(frameH).testTag("board").semantics { contentDescription = described }) {
            Canvas(Modifier.matchParentSize()) { translate(2f * s, 12f * s) { graph(g, text) } }
            Canvas(Modifier.matchParentSize()) {
                translate(2f * s, 12f * s) {
                    humoMarks(g, flow.marks)
                    humoBraces(g, flow.braces, text)
                    val now = tick.longValue
                    val run = disp.value
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
                        offBoard[i]?.let { d ->
                            val u1 = Offset(d.x / hypot(d.x, d.y), d.y / hypot(d.x, d.y))
                            val tip = c.pos + u1 * (len * 0.95f)
                            val side = Offset(-u1.y, u1.x) * (len * 0.2f)
                            drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(tip.x - u1.x * len * 0.35f + side.x, tip.y - u1.y * len * 0.35f + side.y); lineTo(tip.x - u1.x * len * 0.35f - side.x, tip.y - u1.y * len * 0.35f - side.y); close() }, SlotColors.MintMark)
                        }
                    }
                    // the rings go on top: a machine on its target never hides it
                    if (!recap || playing) page.targets.forEach { target(g.px(it), s) }
                }
            }
            }
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
                        drawPath(Path().apply { moveTo(size.width * 0.12f, 0f); lineTo(size.width, size.height / 2f); lineTo(size.width * 0.12f, size.height); close() }, if (playing) Color(0xFFBDBDBD) else Color(0xFF1B1F24))
                    }
                    Spacer(Modifier.width(8.dp * (u / 1.dp)))
                    Text("Play", fontFamily = LessonType.Figtree, fontWeight = FontWeight.Medium, fontSize = with(density) { (16.dp * (u / 1.dp)).toSp() }, color = if (playing) Color(0xFFBDBDBD) else Color(0xFF1B1F24))
                }
            }
        }
    }
}

/**
 * Boss's K1 row: the label "translate by" above one baked-glass bar "( a , b )" with the comma inside; slot a is blue, slot b mint
 * (the board's marks use the same colours), the active slot has a 2 dp inset ring in its own colour. A recap page shows the numbers only.
 */
@Composable
private fun ActionRow(page: LessonPage, flow: LessonFlow, recap: Boolean, u: Dp) {
    val k = u / 1.dp
    val ink = Color(0xFF1B1F24)
    Column(Modifier.fillMaxWidth().padding(top = 10.dp * k), horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            Text("translate", fontFamily = LessonType.Figtree, fontSize = 13.sp, color = ink)
            Text(" by", fontFamily = LessonType.Figtree, fontSize = 13.sp, color = Color(0xFF5B6770))
        }
        Spacer(Modifier.height(6.dp))
        val shape = RoundedCornerShape(28.dp)
        Row(
            Modifier.height(72.dp).clip(shape).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.86f), Color.White.copy(alpha = 0.58f))))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x33FFFFFF))), shape).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("(", fontFamily = FontFamily.Monospace, fontSize = 24.sp, color = ink)
            if (recap) { Fixed(signed(page.move.x), u); Text(",", fontFamily = FontFamily.Monospace, fontSize = 24.sp, color = ink); Fixed(signed(page.move.y), u) }
            else { Slot(0, flow, u); Text(",", fontFamily = FontFamily.Monospace, fontSize = 24.sp, color = ink); Slot(1, flow, u) }
            Text(")", fontFamily = FontFamily.Monospace, fontSize = 24.sp, color = ink)
        }
    }
}

@Composable
private fun Fixed(v: String, u: Dp) {
    val k = u / 1.dp
    Box(Modifier.size(32.dp * k, 24.dp * k).clip(RoundedCornerShape(4.dp * k)).background(Color(0x0D000000)).border(2.dp * k, Color(0x0D000000), RoundedCornerShape(4.dp * k)), contentAlignment = Alignment.Center) {
        Text(v, fontFamily = FontFamily.Monospace, fontSize = with(LocalDensity.current) { (16.dp * k).toSp() }, lineHeight = with(LocalDensity.current) { (16.dp * k).toSp() }, maxLines = 1, color = Color(0xFF1B1F24))
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
    val ring by androidx.compose.animation.animateColorAsState(if (active) SlotColors.mark(i) else Color.Transparent, tween(120), label = "ring")
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(48.dp, 40.dp).clip(shape).background(SlotColors.fill(i)).border(2.dp, ring, shape)
                .clickable(interaction, null, enabled = !flow.locked, role = Role.Button) { flow.tapSlot(i); LessonSounds.play(Cue.Tick) }
                .semantics { contentDescription = "Slot ${i + 1}, ${v ?: "empty"}" }.testTag("slot-$i"),
            contentAlignment = Alignment.Center,
        ) {
            if (v != null) Text(signed(v), fontFamily = FontFamily.Monospace, fontSize = 22.sp, lineHeight = 22.sp, maxLines = 1, color = SlotColors.ink(i))
        }
        // which number is which: a is across (right), b is up
        Text(if (i == 0) "→" else "↑", fontSize = 12.sp, lineHeight = 12.sp, color = SlotColors.mark(i).copy(alpha = 0.8f), modifier = Modifier.clearAndSetSemantics {})
    }
}
