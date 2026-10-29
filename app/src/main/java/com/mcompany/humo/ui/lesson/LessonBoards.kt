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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mcompany.humo.ui.glass.Glass
import com.mcompany.humo.ui.glass.pressScale
import com.mcompany.humo.ui.path.HeadingSpring
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/** Brilliant's page easing (cubic-bezier(.2,.8,.2,1)) and the decelerating glide measured on P7's Play (≈ ease-out cubic). */
internal val BrilliantEase = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
internal val GlideEase = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
private val FlightEase = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private const val NOSE_UP = -PI.toFloat() / 2f

/**
 * The SR-71 that follows Brilliant's point (fly-layer.js): it closes 16 % of the gap each 60 Hz frame, turns its nose
 * into the move through a heading spring, burns while it moves and leaves a puff of smoke every 70 ms.
 */
internal class FlyCraft(var pos: Offset) {
    private val spring = HeadingSpring(maxDegPerFrame = 9f, share = 0.3f).also { it.snapTo(NOSE_UP) }
    val heading: Float get() = spring.value
    var speed = 0f
        private set
    val puffs = ArrayList<Pair<Offset, Long>>()
    private var lastPuff = 0L
    var burstAt = -10_000L

    /** One frame toward [target]; returns true while anything still moves. */
    fun step(target: Offset, dtMs: Float, now: Long, cell: Float, len: Float): Boolean {
        val k = 1f - 0.84f.pow(dtMs / 16.7f)
        val d = target - pos
        val far = d.getDistance()
        if (far > 0.5f) {
            pos += d * k
            if (far > 3f) spring.step(atan2(d.y, d.x), dtMs / 16.7f)
        } else pos = target
        speed = (far / cell).coerceAtMost(1f)
        if (speed > 0.08f && now - lastPuff > 70) {
            lastPuff = now
            val h = heading
            puffs.add(Offset(pos.x - kotlin.math.cos(h) * len * 0.45f, pos.y - kotlin.math.sin(h) * len * 0.45f) to now)
        }
        puffs.removeAll { now - it.second > 1100 }
        return far > 0.5f || puffs.isNotEmpty() || now - burstAt < 650
    }

    fun place(at: Offset, heading: Float) { pos = at; spring.snapTo(heading); speed = 0f; puffs.clear() }
}

/**
 * P1, P2: Brilliant's "Graph" with one draggable point (spec §3). Drag it (it snaps to the nearest node on release), or
 * on the fly layer tap the jet and then a node: it takes off and flies there. Check shows the tick or the cross; the
 * point (or the jet) stays where the child put it.
 */
@Composable
internal fun DragBoard(page: LessonPage, flow: LessonFlow, fly: Boolean, motion: Boolean, width: Dp) {
    val density = LocalDensity.current
    val wPx = with(density) { width.toPx() }
    val g = remember(page, wPx) { BoardGeo(page.graph, wPx / page.graph.boxW) }
    val text = rememberTextMeasurer()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val frames = if (fly) JetFrames.map { ImageBitmap.imageResource(it) } else null
    val start = g.px(page.points[0])
    val handle = remember(page, wPx) { Animatable(flow.dragAt?.let { g.px(it) } ?: start, Offset.VectorConverter) }
    var dragging by remember(page) { mutableStateOf(false) }
    var armed by remember(page) { mutableStateOf(false) }
    val dotR by animateFloatAsState(if (dragging) 15f else 5f, tween(if (motion) 120 else 0), label = "dot")
    val len = g.cell * 0.9f
    val craft = remember(page, wPx) { FlyCraft(handle.value) }
    val tick = remember { mutableLongStateOf(0L) }
    val turn = remember { mutableFloatStateOf(0f) }

    // The point follows the lesson's state: Start over puts it back where it began; a node set from outside a gesture
    // (state restore, tests) moves it there. A drop or a flight already animating is left alone.
    LaunchedEffect(flow.dragAt) {
        val to = flow.dragAt?.let { g.px(it) } ?: start
        if (dragging || handle.isRunning || handle.value == to) return@LaunchedEffect
        if (motion && flow.dragAt == null) handle.animateTo(to, tween(200, easing = BrilliantEase)) else handle.snapTo(to)
    }
    // The jet's frame loop runs only while something moves; then it waits for the point to move or the jet to be armed.
    LaunchedEffect(page, fly, motion) {
        if (!fly || !motion) return@LaunchedEffect
        var last = -1L
        while (isActive) {
            var moving = true
            while (moving || armed) {
                withFrameMillis { now ->
                    val dt = if (last < 0) 16.7f else (now - last).toFloat().coerceIn(1f, 64f)
                    last = now
                    moving = craft.step(handle.value, dt, now, g.cell, len)
                    turn.floatValue = (turn.floatValue + dt * 0.09f) % 360f
                    tick.longValue = now
                }
            }
            last = -1L
            snapshotFlow { handle.value to armed }.first { (h, a) -> a || dist(h, craft.pos) > 0.5f }
        }
    }

    fun drop(n: Node) {
        flow.dropAt(n)
        LessonSounds.play(Cue.Toggle)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    fun flyTo(n: Node) {
        val to = g.px(n)
        val from = handle.value
        val steps = max(18, (dist(from, to) / (5f * g.s)).roundToInt())
        craft.burstAt = tick.longValue
        scope.launch {
            if (motion) handle.animateTo(to, tween(steps * 16, easing = FlightEase)) else handle.snapTo(to)
            drop(n)
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
                            scope.launch { handle.snapTo(p) }
                            val n = g.snapPx(p)
                            if (n != lastNode) { lastNode = n; LessonSounds.play(Cue.Toggle) }
                        }
                        change.consume()
                    }
                    if (dragged) {
                        dragging = false
                        val n = g.snapPx(handle.value)
                        scope.launch { if (motion) handle.animateTo(g.px(n), tween(90, easing = GlideEase)) else handle.snapTo(g.px(n)) }
                        drop(n)
                    } else if (fly) {
                        armed = !armed
                        LessonSounds.play(Cue.Tick)
                    }
                }
            },
    ) {
        tick.longValue
        graph(g, text, dragging)
        val s = g.s
        drawCircle(BoardColors.Axis, 5f * s, g.px(Node(0, 0)))
        drawCircle(BoardColors.Start, 5f * s, start)
        val p = handle.value
        val ok = flow.phase == Phase.Correct
        val bad = flow.phase == Phase.Wrong
        if (frames != null) {
            val at = if (motion) craft.pos else p
            val heading = when {
                motion -> craft.heading
                moved -> atan2(p.y - start.y, p.x - start.x)
                else -> NOSE_UP
            }
            if (!motion) craft.place(p, heading)
            puffs(craft.puffs.map { it.first to ((tick.longValue - it.second) / 1100f).coerceIn(0f, 1f) }, g.cell)
            val sinceBurst = tick.longValue - craft.burstAt
            if (motion && sinceBurst in 0..650) {
                val u = sinceBurst / 650f
                drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(0f to Color(0xFFFFF6D6), 0.4f to Color(0xFFFFB347), 1f to Color.Transparent, center = at, radius = g.cell * 0.9f * (0.3f + u)), g.cell * 0.9f * (0.3f + u), at, alpha = 0.95f * (1f - u))
            }
            jetAt(frames, at, heading, len, flames = if (motion) craft.speed else 0f, t = tick.longValue / 1000f, motion = motion)
            if (armed) armedRing(at, len * 0.62f, turn.floatValue, s)
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
    val frames = if (fly) JetFrames.map { ImageBitmap.imageResource(it) } else null
    Canvas(Modifier.width(width).height(width * (page.graph.boxH / page.graph.boxW)).semantics { contentDescription = "Two points, each moved a steps right and b steps up." }) {
        explainer(g, page, text, frames)
    }
}

/**
 * The transformation workspace (P4-P6, the skill check) and the recap (P7), spec §5-6: the graph in a framed box with the
 * "translate by ( , )" row under it, the tile tray, and Start over. When both slots are full the points glide by that
 * translation; pale ghosts stay where they began. P7 has fixed tiles and a Play button instead of the tray.
 */
@Composable
internal fun WorkspaceBoard(page: LessonPage, flow: LessonFlow, fly: Boolean, motion: Boolean, width: Dp) {
    val density = LocalDensity.current
    val u = width / 350f
    val wPx = with(density) { width.toPx() }
    val s = wPx / 350f
    val g = remember(page, wPx) { BoardGeo(page.graph, s) }
    val text = rememberTextMeasurer()
    val frames = if (fly) JetFrames.map { ImageBitmap.imageResource(it) } else null
    val recap = page.kind == PageKind.Recap
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val shown = flow.shownMove
    fun cells(n: Node?) = n?.let { Offset(it.x.toFloat(), it.y.toFloat()) } ?: Offset.Zero
    val disp = remember(page) { Animatable(cells(shown), Offset.VectorConverter) }
    var heading by remember(page) { mutableFloatStateOf(if (shown == null || shown == Node(0, 0)) NOSE_UP else atan2(-shown.y.toFloat(), shown.x.toFloat())) }
    var playing by remember(page) { mutableStateOf(false) }

    LaunchedEffect(shown) {
        val to = cells(shown)
        if (disp.value == to) return@LaunchedEffect
        val d = to - disp.value
        heading = atan2(-d.y, d.x)
        if (!motion) { disp.snapTo(to); return@LaunchedEffect }
        delay(200)
        disp.animateTo(to, tween(700, easing = GlideEase))
    }

    val frameH = (12f + page.graph.boxH) * u
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
                    val run = disp.value
                    val moving = disp.isRunning
                    if (!recap || playing) page.targets.forEach { target(g.px(it), s) }
                    page.points.forEach { start ->
                        val a = g.px(start)
                        val b = g.px(start.x + run.x, start.y + run.y)
                        if (dist(a, b) > 0.5f) ghost(a, b, flow.phase.takeIf { !flow.inSkill } ?: Phase.Idle, s)
                    }
                    val phase = if (flow.inSkill) Phase.Idle else flow.phase
                    page.points.forEach { start ->
                        val b = g.px(start.x + run.x, start.y + run.y)
                        if (frames != null) {
                            if (moving) for (i in 1..5) {
                                val back = Offset(start.x + run.x, start.y + run.y) - Offset(kotlin.math.cos(heading), -kotlin.math.sin(heading)) * (i * 0.22f)
                                drawCircle(Color(0xFF787C84), g.cell * 0.07f * (1f + i * 0.35f), g.px(back.x, back.y), alpha = 0.3f * (1f - i / 6f))
                            }
                            jetAt(frames, b, heading, g.cell * 0.9f, flames = if (moving) 1f else 0f, t = 0f, motion = motion)
                            if (phase != Phase.Idle) badge(Offset(b.x + g.cell * 0.42f, b.y - g.cell * 0.42f), phase == Phase.Correct, s)
                        } else workPoint(b, phase, s)
                    }
                    drawCircle(BoardColors.Axis, 5f * s, g.px(Node(0, 0)))
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
                            heading = atan2(-page.move.y.toFloat(), page.move.x.toFloat())
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
                    Text("Play", fontFamily = Glass.Body, fontWeight = FontWeight.Medium, fontSize = with(density) { (16.dp * (u / 1.dp)).toSp() }, color = if (playing) Color(0xFFBDBDBD) else Color.Black)
                }
            }
        } else {
            // The tray keeps its place even when hidden, so nothing under it jumps (RULES: no layout jumps).
            Spacer(Modifier.height(14.dp * (u / 1.dp)))
            val tray = flow.slots.trayShown && !flow.locked
            Box(
                Modifier.width(width).height(60.dp * (u / 1.dp)).alpha(if (tray) 1f else 0f)
                    .clip(RoundedCornerShape(4.dp * (u / 1.dp))).background(Color(0xFFDAE2FF)),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp * (u / 1.dp))) {
                    L11.tiles.forEach { v ->
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
            Text("$v", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = with(density) { (16.dp * k).toSp() }, color = Color.Black)
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
            if (right) Canvas(Modifier.size(16.dp * k)) { badge(Offset(size.width / 2f, size.height / 2f), true, size.width / 16f) }
            else Text("1", fontFamily = FontFamily.Monospace, fontSize = with(density) { (14.dp * k).toSp() }, color = Color(0x99000000))
        }
        Row(
            Modifier.height(32.dp * k).clip(RoundedCornerShape(4.dp * k)).background(Color(0xFFF2F2F2)).padding(horizontal = 8.dp * k),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("translate", fontFamily = Glass.Body, fontWeight = FontWeight.Medium, fontSize = fs, color = Color(0xFF294BC6))
            Text(" by (", fontFamily = Glass.Body, fontSize = fs, color = Color.Black)
            if (recap) { Fixed("${page.move.x}", u); Text(",", fontSize = fs); Fixed("${page.move.y}", u) }
            else { Slot(0, flow, u); Text(",", fontSize = fs, color = Color.Black); Slot(1, flow, u) }
            Text(")", fontFamily = Glass.Body, fontSize = fs, color = Color.Black)
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
        if (v != null) Text("$v", fontFamily = FontFamily.Monospace, fontSize = with(LocalDensity.current) { (16.dp * k).toSp() }, color = Color.Black)
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
