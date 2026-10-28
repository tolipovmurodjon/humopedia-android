package com.mcompany.humo.ui.path

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcompany.humo.R
import com.mcompany.humo.data.course.Level
import com.mcompany.humo.ui.glass.Glass
import com.mcompany.humo.ui.glass.Pane
import com.mcompany.humo.ui.glass.pressScale
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

@Composable
fun PathRoute(padding: PaddingValues, onOpenLevel: (Level) -> Unit, vm: PathViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    PathScreen(state, padding, onOpenLevel)
}

/**
 * The Yoʻl tab (round 7, NATIVE-BRIEF N0): the course as a river that climbs up the screen. Level 1 is the bottom
 * stone; it opens scrolled so the current stone sits at thumb height; locked stones above fade into soft clouds. One
 * tap on the current stone starts its lesson; no "Oʻynash" button.
 */
@Composable
fun PathScreen(state: PathUiState?, padding: PaddingValues, onOpenLevel: (Level) -> Unit, scroll: ScrollState = rememberScrollState()) {
    val count = state?.course?.levels?.size ?: 14
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
        Box(
            Modifier.fillMaxSize().verticalScroll(scroll).semantics { contentDescription = "Path" },
        ) {
            Box(
                Modifier.fillMaxWidth().height(layout.height.dp + padding.calculateBottomPadding())
                    .river(layout, state?.current ?: 0),
            ) {
                PlaneProp(layout)
                if (state != null) {
                    state.course.levels.forEachIndexed { i, level ->
                        Stone(level, stoneState(level.n, state.current), layout, i, state.current, onOpenLevel)
                    }
                }
                CloudVeil(layout, state?.current ?: 1)
                CourseTitle(state, Modifier.padding(top = padding.calculateTopPadding() + 24.dp))
            }
        }
    }
}

@Composable
private fun CourseTitle(state: PathUiState?, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Course 15", style = MaterialTheme.typography.labelSmall, color = Glass.Muted)
        Text(
            state?.course?.title ?: "Coordinate Transformations",
            style = MaterialTheme.typography.headlineSmall, color = Glass.Ink, textAlign = TextAlign.Center,
        )
    }
}

/**
 * The river under the stones: a wide bank, the water, a lighter current line; one cubic curve through every stone,
 * running off the top and the bottom. The path is built once per layout, never in the draw pass.
 */
private fun Modifier.river(layout: PathLayout, current: Int): Modifier = drawWithCache {
    val px = density
    fun p(i: Int) = Offset(layout.x(i) * px, layout.y(i) * px)
    val path = Path().apply {
        val start = Offset(layout.x(-1) * px, size.height)
        moveTo(start.x, start.y)
        var prev = start
        for (i in 0..layout.count) {
            val next = if (i == layout.count) Offset(layout.x(i) * px, 0f) else p(i)
            val midY = (prev.y + next.y) / 2f
            cubicTo(prev.x, midY, next.x, midY, next.x, next.y)
            prev = next
        }
    }
    val bank = Stroke(width = 118f * px, cap = StrokeCap.Round)
    val water = Stroke(width = 92f * px, cap = StrokeCap.Round)
    val shine = Stroke(width = 6f * px, cap = StrokeCap.Round)
    // The water is deepest where the learner is and pales toward the clouds above.
    val doneY = if (current > 0) layout.y(current - 1) * px else size.height
    val waterBrush = Brush.verticalGradient(
        0f to Glass.Water.copy(alpha = 0.25f),
        (doneY / size.height - 0.18f).coerceIn(0.01f, 0.98f) to Glass.Water.copy(alpha = 0.85f),
        1f to Glass.WaterDeep.copy(alpha = 0.9f),
    )
    val bankBrush = Brush.verticalGradient(0f to Glass.Mint1.copy(alpha = 0.2f), 0.5f to Glass.Mint1.copy(alpha = 0.75f), 1f to Glass.Mint2.copy(alpha = 0.7f))
    onDrawBehind {
        drawPath(path, bankBrush, style = bank)
        drawPath(path, waterBrush, style = water)
        drawPath(path, Color.White.copy(alpha = 0.35f), style = shine)
    }
}

@Composable
private fun Stone(level: Level, stone: StoneState, layout: PathLayout, i: Int, current: Int, onOpen: (Level) -> Unit) {
    val size = if (stone == StoneState.Current) 112.dp else 92.dp
    val x = layout.x(i).dp
    val y = layout.y(i).dp
    // Locked stones fade the further they sit above the learner, into the cloud veil.
    val alpha = if (stone == StoneState.Locked) (1f - 0.22f * (level.n - current)).coerceAtLeast(0.18f) else 1f
    val interaction = remember { MutableInteractionSource() }
    val sprite = when (stone) {
        StoneState.Done -> R.drawable.stone_done
        StoneState.Current -> R.drawable.stone_current
        StoneState.Locked -> R.drawable.stone_locked
    }
    val status = when (stone) { StoneState.Done -> "done"; StoneState.Current -> "current"; StoneState.Locked -> "locked" }
    val density = LocalDensity.current
    Box(
        Modifier
            .offset { with(density) { IntOffset((x - size / 2).roundToPx(), (y - size / 2).roundToPx()) } }
            .size(size)
            .graphicsLayer { this.alpha = alpha }
            .then(if (stone == StoneState.Locked) Modifier else Modifier.pressScale(interaction).clickable(interaction, null, role = Role.Button) { onOpen(level) })
            .clearAndSetSemantics { contentDescription = "Level ${level.n}, ${level.title}, $status" },
    ) {
        Image(painterResource(sprite), contentDescription = null, modifier = Modifier.fillMaxSize())
    }
    // The level's name beside its stone, on the side the river swings away from.
    val labelLeft = layout.x(i) > layout.width / 2f
    val labelW = 132.dp
    val labelX = if (labelLeft) x - size / 2 - labelW - 4.dp else x + size / 2 + 4.dp
    if (stone == StoneState.Current) {
        Pane(
            Modifier.offset { with(density) { IntOffset(labelX.roundToPx(), (y - 34.dp).roundToPx()) } }.width(labelW),
            radius = 18.dp,
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).clearAndSetSemantics {}) {
                Text("Level ${level.n}", style = MaterialTheme.typography.labelSmall, color = Glass.PrimaryInk, fontWeight = FontWeight.SemiBold)
                Text(level.title, style = MaterialTheme.typography.titleMedium, color = Glass.Ink, maxLines = 2)
            }
        }
    } else {
        Column(
            Modifier.offset { with(density) { IntOffset(labelX.roundToPx(), (y - 20.dp).roundToPx()) } }.width(labelW)
                .graphicsLayer { this.alpha = alpha }.clearAndSetSemantics {},
            horizontalAlignment = if (labelLeft) Alignment.End else Alignment.Start,
        ) {
            Text("Level ${level.n}", style = MaterialTheme.typography.labelSmall, color = Glass.Muted)
            Text(
                level.title, style = MaterialTheme.typography.bodyMedium, color = Glass.Ink, maxLines = 2,
                textAlign = if (labelLeft) TextAlign.End else TextAlign.Start,
            )
        }
    }
}

/** The SR-71 toy parked on the bank by level 1 (round 7 sprite, as it is: the models get upgraded later). */
@Composable
private fun PlaneProp(layout: PathLayout) {
    val density = LocalDensity.current
    val leftSide = layout.x(0) > layout.width / 2f
    val x = if (leftSide) 12.dp else (layout.width - 12f - 104f).dp
    val y = (layout.y(0) + 40f).dp
    Image(
        painterResource(R.drawable.plane_sr71), contentDescription = null,
        modifier = Modifier.offset { with(density) { IntOffset(x.roundToPx(), y.roundToPx()) } }.size(104.dp),
    )
}

/**
 * Soft clouds over the locked stones: white blobs that thicken toward the top, from two stones above the current one.
 * Built once per layout; they sit over the stones, so the far levels melt away instead of ending in a hard edge.
 */
@Composable
private fun CloudVeil(layout: PathLayout, current: Int) {
    Box(
        Modifier.fillMaxSize().clearAndSetSemantics {}.drawWithCache {
            val px = density
            val start = layout.y((current + 1).coerceAtMost(layout.count - 1)) * px
            val veil = Brush.verticalGradient(0f to Glass.Page, 0.55f to Glass.Page.copy(alpha = 0.55f), 1f to Color.Transparent, startY = 0f, endY = start)
            val puffs = (0 until 9).map { k ->
                val cx = size.width * listOf(0.12f, 0.5f, 0.86f, 0.3f, 0.7f, 0.05f, 0.95f, 0.42f, 0.62f)[k]
                val cy = start * listOf(0.15f, 0.08f, 0.2f, 0.45f, 0.4f, 0.62f, 0.66f, 0.78f, 0.9f)[k]
                val r = (70f + 30f * (k % 3)) * px
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.4f), Color.Transparent), Offset(cx, cy), r) to Pair(Offset(cx, cy), r)
            }
            onDrawBehind {
                drawRect(veil, size = size.copy(height = start))
                puffs.forEach { (b, c) -> drawCircle(b, radius = c.second, center = c.first) }
            }
        },
    )
}
