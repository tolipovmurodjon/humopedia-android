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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcompany.humo.R
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
import com.mcompany.humo.ui.path.LowPoly.pine
import kotlinx.coroutines.flow.first
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun PathRoute(padding: PaddingValues, onOpenLevel: (Level) -> Unit, vm: PathViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    PathScreen(state, padding, onOpenLevel)
}

/** Island art widths (dp): the current one is the biggest thing on the map; every tap box is ≥ 72 dp tall. */
private val IslandW = 88.dp
private val CurrentW = 100.dp
private val TopBarH = 48.dp

/**
 * The Home tab (PHASE-PATH-V2, Boss 2026-10-29): a low-poly world map with no words on it. A walkable flagstone trail
 * climbs from island to island, crossing the river on a footbridge after each unit; the SR-71 loops over the clouds
 * that hide the levels not reached yet. One tap on an island opens a glass sheet with its name and a big
 * "Davom etish". A glass bar on top holds the course, gems and the day count (numbers and icons only).
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = remember(maxWidth) { PathLayout(count, maxWidth.value) }
        val density = LocalDensity.current
        val viewport = with(density) { constraints.maxHeight.toDp().value }
        // Opens at the current stone once, then the learner's own scroll is kept (rotation, back from a lesson).
        var placed by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(state?.current, placed) {
            val s = state ?: return@LaunchedEffect
            if (placed) return@LaunchedEffect
            snapshotFlow { scroll.maxValue }.first { it > 0 && it != Int.MAX_VALUE }
            scroll.scrollTo(with(density) { layout.scrollFor(s.current - 1, viewport).dp.roundToPx() })
            placed = true
        }
        Box(Modifier.fillMaxSize().verticalScroll(scroll).semantics { contentDescription = "Path" }) {
            Box(Modifier.fillMaxWidth().height(layout.height.dp + padding.calculateBottomPadding())) {
                Ground(layout, current)
                if (state != null) {
                    state.course.levels.forEachIndexed { i, level ->
                        Island(level, stoneState(level.n, current), layout.stone(i), seconds, motion) {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            sheetLevel = level.n
                        }
                    }
                }
                Sky(layout, current, scroll, viewport, seconds, motion)
            }
        }

        TopBar(state, padding, onCourse = { coursesOpen = !coursesOpen })

        val anySheet = sheetLevel != null || coursesOpen
        AnimatedVisibility(anySheet, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(Glass.Ink.copy(alpha = 0.16f))
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
            shown?.let { level ->
                LevelSheet(level, stoneState(level.n, current), current) {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    sheetLevel = null
                    onOpenLevel(level)
                }
            }
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

/** River, trail, footbridges and trees: built once per layout into paths, on their own layer (scrolling never redraws them). */
@Composable
private fun Ground(layout: PathLayout, current: Int) {
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
            val walkedTop = layout.y(current - 1) * k
            val dirtBrush = Brush.verticalGradient(
                0f to Color(0xFFE7DDC4).copy(alpha = 0.45f),
                (walkedTop / size.height).coerceIn(0.01f, 0.99f) to Color(0xFFE6D3A6).copy(alpha = 0.85f),
                1f to Color(0xFFE6D3A6).copy(alpha = 0.85f),
            )
            onDrawBehind {
                drawPath(riverPath, Glass.Mint1.copy(alpha = 0.75f), style = bank)
                drawPath(riverPath, Glass.Water, style = water)
                drawPath(riverPath, Glass.WaterDeep.copy(alpha = 0.35f), style = Stroke(PathLayout.RIVER_HALF * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(riverPath, Color.White.copy(alpha = 0.45f), style = ripple)
                drawPath(trailPath, dirtBrush, style = dirt)
                layout.flagstones.forEach { f ->
                    flagstone(o(f.at), f.r * k, f.seed, if (f.at.y * k < walkedTop) 0.7f else 1f)
                }
                layout.bridges.forEach { b -> bridge(o(b.at), b.angle, 62f * k, 26f * k) }
                layout.trees.forEach { t -> if (t.pine) pine(o(t.at), t.size * k) else bush(o(t.at), t.size * 0.8f * k) }
            }
        },
    )
}

/**
 * One level's island. No words on the map: the state is in the picture (done = moss + tick, current = sunny gold +
 * flag + a slow pulse, locked = slate + padlock, under the clouds). TalkBack reads "Level 3, Rotations, current".
 */
@Composable
private fun Island(level: Level, stone: StoneState, at: Pt, seconds: State<Float>, motion: Boolean, onTap: () -> Unit) {
    val w = if (stone == StoneState.Current) CurrentW else IslandW
    val h = w * 0.86f
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    val status = when (stone) { StoneState.Done -> "done"; StoneState.Current -> "current"; StoneState.Locked -> "locked" }
    Box(
        Modifier
            .offset { with(density) { IntOffset((at.x.dp - w / 2).roundToPx(), (at.y.dp - h * 0.52f).roundToPx()) } }
            .size(w, h)
            .pressScale(interaction)
            .clickable(interaction, null, role = Role.Button, onClick = onTap)
            .clearAndSetSemantics { contentDescription = "Level ${level.n}, ${level.title}, $status"; role = Role.Button },
    ) {
        if (stone == StoneState.Current) {
            // The one obvious "tap me": a ring that breathes out from under the island every 1.6 s.
            Box(
                Modifier.fillMaxSize().drawBehind {
                    val c = Offset(size.width / 2f, size.height * 0.52f)
                    val p = if (motion) (seconds.value % 1.6f) / 1.6f else 0.35f
                    val rx = size.width * 0.46f * (1f + 0.32f * p)
                    drawOval(
                        Color.White.copy(alpha = 0.75f * (1f - p)),
                        topLeft = Offset(c.x - rx, c.y - rx * 0.55f + size.width * 0.06f),
                        size = androidx.compose.ui.geometry.Size(rx * 2f, rx * 1.1f),
                        style = Stroke(3.dp.toPx()),
                    )
                },
            )
        }
        Box(
            Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                val c = Offset(size.width / 2f, size.height * 0.52f)
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
                        StoneState.Current -> flag(Offset(c.x + rx * 0.08f, c.y - rx * 0.06f), rx * 0.95f, 0f)
                        StoneState.Locked -> lock(Offset(c.x, c.y - rx * 0.02f), rx * 0.42f)
                    }
                }
            },
        )
    }
}

/**
 * Low-poly clouds over the levels not reached yet, and the SR-71 flying over them with its shadow on the ground. The
 * clouds drift slowly, lag the scroll a little (parallax) and part sideways as they come into the middle of the
 * screen; one canvas, redrawn per frame, its cloud shapes built once.
 */
@Composable
private fun Sky(layout: PathLayout, current: Int, scroll: ScrollState, viewport: Float, seconds: State<Float>, motion: Boolean) {
    val plane = ImageBitmap.imageResource(R.drawable.plane_sr71)
    val clouds = remember(layout, current) { layout.clouds(current) }
    Box(
        Modifier.fillMaxSize().graphicsLayer().clearAndSetSemantics {}.drawWithCache {
            val k = density
            val shapes = clouds.map { LowPoly.cloudShape(it.width * k, it.seed) }
            val shadowTint = ColorFilter.tint(Color(0xFF1F5E57), BlendMode.SrcIn)
            onDrawBehind {
                val t = if (motion) seconds.value else 0f
                val scrollDp = scroll.value / k
                val fix = layout.plane(current, t)
                drawPlane(plane, fix, k, shadow = true, tint = shadowTint)
                clouds.forEachIndexed { i, c ->
                    // Parallax: a cloud moves a little slower than the ground, by at most 24 dp, so the first row never slides onto the current island.
                    val lag = ((viewport * 0.5f - (c.at.y - scrollDp)) * (0.06f + 0.04f * c.depth)).coerceIn(-24f, 24f)
                    val screenY = c.at.y + lag - scrollDp
                    if (screenY < -120f || screenY > viewport + 120f) return@forEachIndexed
                    // Parting: a cloud slides away from the middle as it reaches the upper-middle of the screen.
                    val part = ((viewport * 0.6f - screenY) / (viewport * 0.6f)).coerceIn(0f, 1f)
                    val away = if (c.at.x < layout.width / 2f) -1f else 1f
                    val drift = if (motion) sin(t * 2f * PI.toFloat() / (26f + c.seed % 9) + c.seed) * 10f else 0f
                    val x = (c.at.x + drift + away * 26f * part * part) * k
                    val y = (c.at.y + lag) * k
                    translate(x, y) { shapes[i].forEach { (p, col) -> drawPath(p, col) } }
                }
                drawPlane(plane, fix, k, shadow = false, tint = null)
            }
        },
    )
}

/** The SR-71 sprite points up; turned to its heading, its wings squeezed by the bank. The shadow sits lower-right, smaller and fainter the higher it flies. */
private fun DrawScope.drawPlane(plane: ImageBitmap, fix: PlaneFix, k: Float, shadow: Boolean, tint: ColorFilter?) {
    val sizePx = 76f * k
    val off = if (shadow) Offset((18f + 22f * fix.altitude) * k, (30f + 36f * fix.altitude) * k) else Offset.Zero
    val c = Offset(fix.at.x * k + off.x, fix.at.y * k + off.y)
    val s = if (shadow) 0.86f - 0.1f * fix.altitude else 1f
    withTransform({
        translate(c.x, c.y)
        rotateRad(fix.heading + PI.toFloat() / 2f, Offset.Zero)
        scale(s * (1f - 0.22f * abs(fix.bank)), s, Offset.Zero)
    }) {
        drawImage(
            plane,
            dstOffset = IntOffset((-sizePx / 2f).roundToInt(), (-sizePx / 2f).roundToInt()),
            dstSize = IntSize(sizePx.roundToInt(), sizePx.roundToInt()),
            alpha = if (shadow) 0.16f + 0.08f * (1f - fix.altitude) else 1f,
            colorFilter = tint,
        )
    }
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
                Text("${course?.number ?: ""}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Glass.Ink, maxLines = 1)
                Spacer(Modifier.width(4.dp))
                Icon(TabIcons.ChevronDown, null, tint = Glass.Ink.copy(alpha = 0.72f), modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Pane(Modifier.height(TopBarH), radius = TopBarH / 2) {
            Row(Modifier.padding(horizontal = 14.dp).align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                Count(TabIcons.GemFilled, Color(0xFF1E8FCB), state?.gems ?: 0, "Olmos")
                Spacer(Modifier.width(16.dp))
                Count(TabIcons.StreakFilled, Color(0xFFE8711F), state?.streakDays ?: 0, "Ketma-ket kunlar")
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
