package com.mcompany.humo.ui.path

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcompany.humo.data.course.Level
import com.mcompany.humo.ui.glass.Glass
import com.mcompany.humo.ui.glass.Pane
import com.mcompany.humo.ui.glass.TabIcons
import com.mcompany.humo.ui.glass.pressScale
import com.mcompany.humo.ui.path.LowPoly.bridge
import com.mcompany.humo.ui.path.LowPoly.bush
import com.mcompany.humo.ui.path.LowPoly.doneBadge
import com.mcompany.humo.ui.path.LowPoly.flag
import com.mcompany.humo.ui.path.LowPoly.flagstone
import com.mcompany.humo.ui.path.LowPoly.island
import com.mcompany.humo.ui.path.LowPoly.lock
import com.mcompany.humo.ui.path.Jet.jet
import com.mcompany.humo.ui.path.Jet.jetShadow
import com.mcompany.humo.ui.path.Jet.smoke
import com.mcompany.humo.ui.path.LowPoly.pine
import com.mcompany.humo.ui.path.LowPoly.playBadge
import com.mcompany.humo.ui.path.LowPoly.sparkle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PathRoute(padding: PaddingValues, onOpenLevel: (Level) -> Unit, vm: PathViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    PathScreen(state, padding, onOpenLevel)
}

/** Island art widths (dp): the current one is the biggest thing on the map; every tap box is ≥ 79 dp tall. */
private val IslandW = 88.dp
private val CurrentW = 100.dp
private val TopBarH = 48.dp
/** An island's box is 0.9 of its width tall; the top face's centre sits 44 % down it. */
private const val ISLAND_H = 0.9f
private const val ISLAND_CY = 0.44f
/** The SR-71, nose to tail (dp). */
private const val JET_LEN = 70f

/**
 * The Home tab (PHASE-PATH-V2, Boss 2026-10-29): a low-poly world map with no words on it. A walkable flagstone trail
 * climbs from island to island, crossing the river on a footbridge after each unit. The current island and the next
 * six are in the open (the story); above them a low-poly cloud bank closes the world, and the scroll stops there. The
 * SR-71 flies its loop over it all. Every time Home opens, the current island sits at thumb height, and one tap on it
 * starts the lesson; a done or locked island opens a glass sheet instead. A glass bar on top holds the course, gems
 * and the day count.
 */
@Composable
fun PathScreen(
    state: PathUiState?,
    padding: PaddingValues,
    onOpenLevel: (Level) -> Unit,
    scroll: ScrollState = rememberScrollState(),
    motion: Boolean = rememberSystemMotion(),
) {
    val count = state?.course?.levels?.size ?: 14
    val current = state?.current ?: 1
    var sheetLevel by rememberSaveable { mutableStateOf<Int?>(null) }
    var coursesOpen by rememberSaveable { mutableStateOf(false) }
    val seconds = rememberSceneSeconds(motion)
    val haptics = LocalHapticFeedback.current
    // Set when a lesson opens; when the child comes back, the jet does one small roll for them.
    var lessonOpen by rememberSaveable { mutableStateOf(false) }
    val rollAt = remember { mutableFloatStateOf(-10f) }
    LaunchedEffect(Unit) {
        if (lessonOpen) {
            lessonOpen = false
            if (motion) rollAt.floatValue = 0.6f
        }
    }
    val start: (Level) -> Unit = { level ->
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        sheetLevel = null
        lessonOpen = true
        onOpenLevel(level)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = remember(maxWidth) { PathLayout(count, maxWidth.value) }
        val cut = layout.cut(current)
        val density = LocalDensity.current
        val viewport = with(density) { constraints.maxHeight.toDp().value }
        val visible = viewport - padding.calculateBottomPadding().value
        // Every time Home opens (launch, back from a lesson, back from another tab, rotation): the current island at thumb
        // height, clear of the clouds. The world's height changes with the bank, so wait until the scroll can reach it.
        LaunchedEffect(state?.current, cut, visible) {
            val s = state ?: return@LaunchedEffect
            val target = with(density) { layout.scrollFor(s.current - 1, visible, cut).dp.roundToPx() }
            withTimeoutOrNull(1_000) { snapshotFlow { scroll.maxValue }.first { it != Int.MAX_VALUE && it >= target - 2 } }
            scroll.scrollTo(target)
        }
        Box(Modifier.fillMaxSize().verticalScroll(scroll).semantics { contentDescription = "Path" }) {
            Box(Modifier.fillMaxWidth().height((layout.height - cut).dp + padding.calculateBottomPadding())) {
                Ground(layout, current, cut)
                if (state != null) {
                    val top = layout.storyTop(current)
                    state.course.levels.forEachIndexed { i, level ->
                        // Islands under the bank are not drawn and cannot be tapped: the story ends at the clouds.
                        if (i > top) return@forEachIndexed
                        val at = layout.stone(i).let { Pt(it.x, it.y - cut) }
                        val stone = stoneState(level.n, current)
                        Island(level, stone, at, seconds, motion, inView = { (at.y - scroll.value / density.density) in 60f..(visible - 40f) }) {
                            if (stone == StoneState.Current) {
                                start(level)
                            } else {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                sheetLevel = level.n
                            }
                        }
                    }
                }
            }
        }
        // A screen-sized layer (checker fix 1): redrawn every frame, so it must not be the 2,700 dp content box.
        Sky(layout, current, cut, scroll, viewport, visible, seconds, motion, rollAt)

        TopBar(state, padding, onCourse = { coursesOpen = !coursesOpen })

        val anySheet = sheetLevel != null || coursesOpen
        // A light deep-teal veil only under the level sheet (8 %, never the grey fog); the course list is a dropdown,
        // so the world stays clear and only a clear tap-to-close layer sits over it.
        AnimatedVisibility(anySheet, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(if (sheetLevel != null) Color(0xFF0B3D36).copy(alpha = 0.08f) else Color.Transparent)
                    .clickable(remember { MutableInteractionSource() }, null, onClickLabel = "Yopish") { sheetLevel = null; coursesOpen = false },
            )
        }
        BackHandler(enabled = anySheet) { sheetLevel = null; coursesOpen = false }

        AnimatedVisibility(
            coursesOpen,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = padding.calculateTopPadding() + 16.dp + TopBarH + 8.dp),
            enter = fadeIn() + slideInVertically(spring(dampingRatio = 0.9f, stiffness = 500f)) { -it / 4 },
            exit = fadeOut() + slideOutVertically { -it / 4 },
        ) { CourseSheet(state) }

        // Remember the last level so the sheet keeps its words while it slides away.
        var shown by remember { mutableStateOf<Level?>(null) }
        sheetLevel?.let { n -> shown = state?.course?.levels?.firstOrNull { it.n == n } }
        AnimatedVisibility(
            sheetLevel != null && state != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = padding.calculateBottomPadding() + 8.dp),
            enter = fadeIn() + slideInVertically(spring(dampingRatio = 0.86f, stiffness = 420f)) { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            shown?.let { level -> LevelSheet(level, stoneState(level.n, current), current) { start(level) } }
        }
    }
}

/** Off when the phone's animations are off (Settings › Accessibility › Remove animations): the map holds still. */
@Composable
fun rememberSystemMotion(): Boolean {
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f }
}

/** Seconds since the map appeared, ticking once a frame; read only in draw and layer blocks, so nothing recomposes. */
@Composable
private fun rememberSceneSeconds(motion: Boolean): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    if (motion) {
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            while (true) withFrameNanos { t.floatValue = (it - start) / 1_000_000_000f }
        }
    }
    return t
}

/**
 * River, trail, footbridges and trees: built once per layout into paths, on their own layer (scrolling never redraws
 * them). The map's dp are the full world's; the scrollable box starts [cut] dp down it, at the cloud bank.
 */
@Composable
private fun Ground(layout: PathLayout, current: Int, cut: Float) {
    Box(
        Modifier.fillMaxSize().graphicsLayer().clearAndSetSemantics {}.drawWithCache {
            val k = density
            fun o(p: Pt) = Offset(p.x * k, p.y * k)
            val riverPath = Path().apply {
                layout.river.forEachIndexed { i, p -> if (i == 0) moveTo(p.x * k, p.y * k) else lineTo(p.x * k, p.y * k) }
            }
            val trailPath = Path().apply {
                moveTo(layout.x(-1) * k, layout.y(-1) * k)
                for (i in -1 until layout.count) {
                    val a = layout.stone(i); val b = layout.stone(i + 1); val mid = (a.y + b.y) / 2f
                    cubicTo(a.x * k, mid * k, b.x * k, mid * k, b.x * k, b.y * k)
                }
            }
            val bank = Stroke(PathLayout.RIVER_HALF * 2f * k + 16f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val water = Stroke(PathLayout.RIVER_HALF * 2f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val ripple = Stroke(3f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val dirt = Stroke(30f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
            // Above the current level the world is not walked yet: the trail there is paler.
            val walkedTop = layout.y(current - 1)
            val dirtBrush = Brush.verticalGradient(
                0f to Color(0xFFE7DDC4).copy(alpha = 0.45f),
                (walkedTop / layout.height).coerceIn(0.01f, 0.99f) to Color(0xFFE6D3A6).copy(alpha = 0.85f),
                1f to Color(0xFFE6D3A6).copy(alpha = 0.85f),
                startY = 0f, endY = layout.height * k,
            )
            onDrawBehind {
                translate(0f, -cut * k) {
                    drawPath(riverPath, Glass.Mint1.copy(alpha = 0.75f), style = bank)
                    drawPath(riverPath, Glass.Water, style = water)
                    drawPath(riverPath, Glass.WaterDeep.copy(alpha = 0.35f), style = Stroke(PathLayout.RIVER_HALF * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(riverPath, Color.White.copy(alpha = 0.45f), style = ripple)
                    drawPath(trailPath, dirtBrush, style = dirt)
                    layout.flagstones.forEach { f ->
                        flagstone(o(f.at), f.r * k, f.seed, if (f.at.y < walkedTop) 0.7f else 1f)
                    }
                    layout.bridges.forEach { b -> bridge(o(b.at), b.angle, 62f * k, 26f * k) }
                    layout.trees.forEach { t -> if (t.pine) pine(o(t.at), t.size * k) else bush(o(t.at), t.size * 0.8f * k) }
                }
            }
        },
    )
}

/**
 * One level's island. No words on the map: the state is in the picture (done = moss + a tick that sparkles as it comes
 * into view, current = sunny gold + a waving flag + a play mark + a breathing ring, locked = slate + padlock). TalkBack
 * reads "Level 3, Rotations, current".
 */
@Composable
private fun Island(level: Level, stone: StoneState, at: Pt, seconds: State<Float>, motion: Boolean, inView: () -> Boolean, onTap: () -> Unit) {
    val w = if (stone == StoneState.Current) CurrentW else IslandW
    val h = w * ISLAND_H
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    val status = when (stone) { StoneState.Done -> "done"; StoneState.Current -> "current"; StoneState.Locked -> "locked" }
    val sparkAt = remember { mutableFloatStateOf(-10f) }
    if (stone == StoneState.Done && motion) {
        LaunchedEffect(Unit) {
            snapshotFlow { inView() }.distinctUntilChanged().collect { if (it) sparkAt.floatValue = seconds.value }
        }
    }
    Box(
        Modifier
            .offset { with(density) { IntOffset((at.x.dp - w / 2).roundToPx(), (at.y.dp - h * ISLAND_CY).roundToPx()) } }
            .size(w, h)
            .then(
                if (stone == StoneState.Current && motion) {
                    Modifier.graphicsLayer { translationY = sin(seconds.value * 2f * PI.toFloat() / 2.4f) * 3.dp.toPx() }
                } else Modifier,
            )
            .pressScale(interaction)
            .clickable(interaction, null, role = Role.Button, onClick = onTap)
            .clearAndSetSemantics { contentDescription = "Level ${level.n}, ${level.title}, $status"; role = Role.Button },
    ) {
        if (stone == StoneState.Current) {
            // The one obvious "tap me": a mint ring that breathes out from under the island every 1.6 s.
            Box(
                Modifier.fillMaxSize().drawBehind {
                    val c = Offset(size.width / 2f, size.height * ISLAND_CY)
                    val p = if (motion) (seconds.value % 1.6f) / 1.6f else 0.35f
                    val rx = size.width * 0.46f * (1f + 0.32f * p)
                    val ry = rx * LowPoly.TOP_RATIO
                    drawOval(
                        Glass.Primary.copy(alpha = 0.55f * (1f - p)),
                        topLeft = Offset(c.x - rx, c.y - ry + size.width * 0.08f),
                        size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f),
                        style = Stroke(4.dp.toPx()),
                    )
                },
            )
        }
        Box(
            Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                val c = Offset(size.width / 2f, size.height * ISLAND_CY)
                val rx = size.width * 0.44f
                val look = when (stone) {
                    StoneState.Done -> LowPoly.DoneLook
                    StoneState.Current -> LowPoly.CurrentLook
                    StoneState.Locked -> LowPoly.LockedLook
                }
                onDrawBehind {
                    island(c, rx, look)
                    when (stone) {
                        StoneState.Done -> doneBadge(Offset(c.x + rx * 0.62f, c.y - rx * 0.42f), 11.dp.toPx())
                        StoneState.Current -> playBadge(Offset(c.x - rx * 0.6f, c.y - rx * 0.34f), 12.dp.toPx())
                        StoneState.Locked -> lock(Offset(c.x, c.y - rx * 0.02f), rx * 0.42f)
                    }
                }
            },
        )
        // What moves each frame sits on its own small layer, so the island's facets stay cached.
        if (stone == StoneState.Current) {
            Box(
                Modifier.fillMaxSize().drawBehind {
                    val c = Offset(size.width / 2f, size.height * ISLAND_CY)
                    val rx = size.width * 0.44f
                    val wave = if (motion) sin(seconds.value * 2f * PI.toFloat() / 1.3f) else 0.4f
                    flag(Offset(c.x + rx * 0.08f, c.y - rx * 0.06f), rx * 0.95f, wave)
                },
            )
        } else if (stone == StoneState.Done && motion) {
            Box(
                Modifier.fillMaxSize().drawBehind {
                    val dt = seconds.value - sparkAt.floatValue
                    if (dt !in 0f..1.3f) return@drawBehind
                    val c = Offset(size.width / 2f, size.height * ISLAND_CY)
                    val rx = size.width * 0.44f
                    val tick = Offset(c.x + rx * 0.62f, c.y - rx * 0.42f)
                    for ((n, delay) in listOf(0f, 0.3f).withIndex()) {
                        val p = ((dt - delay) / 0.9f).coerceIn(0f, 1f)
                        if (p <= 0f || p >= 1f) continue
                        val a = sin(p * PI.toFloat())
                        val s = (if (n == 0) 8f else 5f).dp.toPx() * a
                        val off = if (n == 0) Offset(9f, -10f) else Offset(-11f, -4f)
                        sparkle(tick + Offset(off.x.dp.toPx(), off.y.dp.toPx()), s, a)
                    }
                },
            )
        }
    }
}

/**
 * The cloud bank over the world above the story, and the SR-71 flying over everything with its shadow on the ground.
 * Rows of low-poly clouds (flat facets, no blur) start at the bank's edge and fill everything above; each row drifts
 * sideways at its own speed and every cloud breathes a little. One screen-sized canvas, redrawn per frame; the cloud
 * shapes are built once.
 */
@Composable
private fun Sky(
    layout: PathLayout, current: Int, cut: Float, scroll: ScrollState, viewport: Float, visible: Float,
    seconds: State<Float>, motion: Boolean, rollAt: State<Float>,
) {
    val rows = remember(layout, current) { layout.bank(current) }
    Box(
        Modifier.fillMaxSize().graphicsLayer().clearAndSetSemantics {}.drawWithCache {
            val k = density
            val edge = layout.bankEdge(current)
            val shapes = rows.map { row ->
                layout.bankClouds(row, 0f).indices.map { j -> LowPoly.cloudShape(row.width * k, row.seed + j * 5, far = row.depth > 0) }
            }
            val fillTop = rows.lastOrNull()?.y ?: edge
            // The bank's shadow on the ground: a faceted band just under its edge (light from the top-left).
            val shadow = Path().apply {
                moveTo(-20f * k, (edge - 6f) * k)
                var x = -20f
                var i = 0
                while (x <= layout.width + 40f) { lineTo(x * k, (edge + if (i % 2 == 0) 14f else 24f) * k); x += 28f; i++ }
                lineTo((layout.width + 40f) * k, (edge - 6f) * k)
                close()
            }
            val cloudShadow = Color(0xFF1F5E57).copy(alpha = 0.10f)
            val fill = Color(0xFFDCE8EF)
            onDrawBehind {
                val t = if (motion) seconds.value else 0f
                val scrollDp = scroll.value / k + cut
                val fix = layout.plane(current, t, scrollDp, visible)
                val jetOn = fix.at.y - scrollDp in -60f..viewport + 60f
                val roll = ((t - rollAt.value) / 1.1f).let { if (it in 0f..1f) it else 0f }
                translate(0f, -scrollDp * k) {
                    if (jetOn) jetShadow(Offset(fix.at.x * k, fix.at.y * k), JET_LEN * k, fix.heading, fix.bank, roll, fix.altitude, k)
                    if (edge + 30f > scrollDp) {
                        drawPath(shadow, cloudShadow)
                        // Behind the rows, so no gap between two puffs ever shows the world above.
                        drawRect(fill, Offset(0f, (fillTop - 300f) * k), androidx.compose.ui.geometry.Size(layout.width * k, (edge - fillTop + 300f - 16f) * k))
                        for (r in rows.indices.reversed()) {
                            val row = rows[r]
                            val screenY = row.y - scrollDp
                            if (screenY < -90f || screenY > viewport + 40f) continue
                            layout.bankClouds(row, t).forEachIndexed { j, p ->
                                val breathe = 1f + if (motion) 0.03f * sin(t * 2f * PI.toFloat() / (4.5f + (row.seed + j) % 3) + j) else 0f
                                withTransform({ translate(p.x * k, p.y * k); scale(breathe, breathe, Offset.Zero) }) {
                                    shapes[r][j].forEach { (path, col) -> drawPath(path, col) }
                                }
                            }
                        }
                    }
                    if (jetOn) {
                        // A soft trail of puffs behind the tail, where the jet was a moment ago.
                        val past = (1..9).map { i ->
                            val f = layout.plane(current, t - i * 0.16f, scrollDp, visible)
                            Offset((f.at.x - cos(f.heading) * JET_LEN * 0.62f) * k, (f.at.y - sin(f.heading) * JET_LEN * 0.62f) * k)
                        }
                        smoke(past, k)
                        jet(Offset(fix.at.x * k, fix.at.y * k), JET_LEN * k, fix.heading, fix.bank, roll, t, motion)
                    }
                }
            }
        },
    )
}

/**
 * The glass bar on top (fixed height, so nothing moves when a count grows): the course chip opens the course list;
 * gems and the day count are numbers beside icons. Words live only in TalkBack's labels.
 */
@Composable
private fun TopBar(state: PathUiState?, padding: PaddingValues, onCourse: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = padding.calculateTopPadding() + 16.dp, start = 16.dp, end = 16.dp).height(TopBarH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val interaction = remember { MutableInteractionSource() }
        val course = state?.course
        Pane(
            Modifier.height(TopBarH).pressScale(interaction)
                .clickable(interaction, null, role = Role.Button, onClick = onCourse)
                .clearAndSetSemantics { contentDescription = "Kurs: ${course?.title ?: ""}"; role = Role.Button },
            radius = TopBarH / 2,
        ) {
            Row(Modifier.padding(start = 14.dp, end = 10.dp).align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                Icon(TabIcons.CourseFilled, null, tint = Glass.Primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(course?.let { "${it.number}-kurs" } ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Glass.Ink, maxLines = 1)
                Spacer(Modifier.width(4.dp))
                Icon(TabIcons.ChevronDown, null, tint = Glass.Ink.copy(alpha = 0.72f), modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Pane(Modifier.height(TopBarH), radius = TopBarH / 2) {
            Row(Modifier.padding(horizontal = 14.dp).align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                Count(TabIcons.GemFilled, Color(0xFF1E8FCB), state?.gems ?: 0, "Olmos")
                Spacer(Modifier.width(16.dp))
                Count(TabIcons.StreakFilled, Color(0xFFD0600E), state?.streakDays ?: 0, "Ketma-ket kunlar")
            }
        }
    }
}

@Composable
private fun Count(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, n: Int, label: String) {
    Row(Modifier.clearAndSetSemantics { contentDescription = "$label: $n" }, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text("$n", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Glass.Ink, maxLines = 1)
    }
}

/** The course list under the chip. One course is built; the rest are named as coming, not faked. */
@Composable
private fun CourseSheet(state: PathUiState?) {
    Pane(Modifier.padding(horizontal = 16.dp).widthIn(max = 440.dp).fillMaxWidth(), radius = 24.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Glass.PrimarySoft).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(TabIcons.CourseFilled, null, tint = Glass.Primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${state?.course?.number ?: ""}-kurs", style = MaterialTheme.typography.labelSmall, color = Glass.PrimaryInk)
                    Text(state?.course?.title ?: "", style = MaterialTheme.typography.titleMedium, color = Glass.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Boshqa kurslar tez orada.", style = MaterialTheme.typography.bodyMedium, color = Glass.Muted, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

/** The sheet a tap on an island opens: the level's name, how many lessons, and one big button (or why it is shut). */
@Composable
private fun LevelSheet(level: Level, stone: StoneState, current: Int, onGo: () -> Unit) {
    Pane(Modifier.padding(horizontal = 16.dp).widthIn(max = 440.dp).fillMaxWidth(), radius = 28.dp) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${level.n}-daraja", style = MaterialTheme.typography.labelSmall, color = Glass.PrimaryInk, fontWeight = FontWeight.SemiBold)
            Text(level.title, style = MaterialTheme.typography.headlineSmall, color = Glass.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                when (stone) {
                    StoneState.Done -> "${level.lessons} ta dars · tugatilgan"
                    StoneState.Current -> "${level.lessons} ta dars"
                    StoneState.Locked -> "Avval $current-darajani tugating."
                },
                style = MaterialTheme.typography.bodyLarge, color = Glass.Muted,
            )
            Spacer(Modifier.height(12.dp))
            if (stone != StoneState.Locked) {
                BigButton(if (stone == StoneState.Done) "Takrorlash" else "Davom etish", onGo)
            } else {
                // The button's slot is kept, so the sheet is one height for every island.
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(Glass.Ink.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                    Text("Yopiq", style = MaterialTheme.typography.labelLarge, color = Glass.Muted)
                }
            }
        }
    }
}

/** The one primary action: mint #1C7E69 with white words (5.1:1), 56 dp tall, presses to 0.97. */
@Composable
private fun BigButton(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).pressScale(interaction).clip(RoundedCornerShape(28.dp))
            .background(Glass.Primary)
            .clickable(interaction, null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(vertical = 14.dp))
    }
}
