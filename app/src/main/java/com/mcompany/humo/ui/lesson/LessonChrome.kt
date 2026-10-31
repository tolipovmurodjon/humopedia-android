package com.mcompany.humo.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import com.mcompany.humo.ui.glass.Glass
import com.mcompany.humo.ui.glass.GlassStage
import com.mcompany.humo.ui.glass.LocalPageBackdrop
import com.mcompany.humo.ui.glass.PaneRim
import com.mcompany.humo.ui.path.rememberJetAtlas
import kotlinx.coroutines.delay

/**
 * Liquid glass for the lesson's chrome only (Boss, 2026-10-31: "liquid glass makes it 100x better"): the bottom bar, the
 * right and wrong panels (mint, amber), Humo's sheet, the quit sheet. Never on the board. Same recipe as the shell's bottom
 * bar (rules/liquid-glass-navbar: vibrancy + a lens, light from the top-left, a rim); the state sets the tint.
 */
internal enum class Tint(val wash: Color, val glow: Color, val flat: Color) {
    Clear(Color.White.copy(alpha = 0.50f), Glass.Mint2.copy(alpha = 0.26f), Color(0xFFF7FBF9)),
    Mint(Color(0xFFC4F3DA).copy(alpha = 0.82f), Glass.Mint2.copy(alpha = 0.34f), Chrome.PanelGood),
    Amber(Color(0xFFFFE9A3).copy(alpha = 0.90f), Color(0xFFF7C51E).copy(alpha = 0.30f), Chrome.PanelBad),
}

/** Glass in the shape of a rounded rectangle; without a page behind it (or before the glass is turned on) a flat tinted pane. */
@Composable
internal fun Modifier.chromeGlass(radius: Dp, tint: Tint = Tint.Clear): Modifier {
    val page = LocalPageBackdrop.current
    val shape = RoundedCornerShape(radius)
    val surface = if (page == null || !GlassStage.on) {
        Modifier.clip(shape).background(tint.flat).drawBehind {
            drawRect(Brush.radialGradient(listOf(tint.glow, Color.Transparent), center = Offset.Zero, radius = size.minDimension * 0.9f))
        }
    } else {
        Modifier.drawBackdrop(
            backdrop = page,
            shape = { RoundedRectangle(radius) },
            effects = { vibrancy(); lens(10f.dp.toPx(), 18f.dp.toPx()) },
            highlight = { Highlight.Default },
            shadow = { Shadow(alpha = 0.40f) },
            onDrawSurface = {
                drawRect(tint.wash)
                drawRect(Brush.radialGradient(listOf(tint.glow, Color.Transparent), center = Offset.Zero, radius = size.minDimension * 0.9f))
            },
        )
    }
    return this.then(surface).border(1.25.dp, PaneRim, shape)
}

/** The floating glass bar that holds the lesson's main buttons (Check, Continue). */
@Composable
internal fun GlassDock(bottomInset: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp + bottomInset).testTag("dock").chromeGlass(28.dp)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), content = content,
    )
}

/** The verdict panel: mint glass with a tick for right, amber glass with a cross for wrong (never red); a small flag top right. */
@Composable
internal fun GlassPanel(ok: Boolean, text: String, bottomInset: Dp, onFlag: (() -> Unit)?, buttons: @Composable RowScope.() -> Unit) {
    val ink = if (ok) Chrome.MintInk else Chrome.YellowInk
    Column(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp + bottomInset).testTag("feedback-panel")
            .chromeGlass(28.dp, if (ok) Tint.Mint else Tint.Amber).padding(start = 18.dp, end = 8.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(34.dp)) {
                val k = size.width / 34f
                if (ok) {
                    drawCircle(Chrome.Mint, size.minDimension / 2f)
                    drawPath(Path().apply { moveTo(9f * k, 17.5f * k); lineTo(14.5f * k, 23f * k); lineTo(25f * k, 11f * k) }, Color.White, style = Stroke(3.6f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
                } else {
                    drawCircle(Chrome.Yellow, size.minDimension / 2f)
                    drawLine(Color(0xFF4A3600), Offset(11f * k, 11f * k), Offset(23f * k, 23f * k), 3.6f * k, StrokeCap.Round)
                    drawLine(Color(0xFF4A3600), Offset(23f * k, 11f * k), Offset(11f * k, 23f * k), 3.6f * k, StrokeCap.Round)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(text, Modifier.weight(1f), fontFamily = LessonType.Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 24.sp, color = ink)
            if (onFlag != null) Box(Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Report this page", onClick = onFlag).semantics { contentDescription = "Report this page" }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(22.dp)) {
                    val k = size.width / 24f
                    drawPath(Path().apply { moveTo(5f * k, 2f * k); lineTo(7f * k, 2f * k); lineTo(7f * k, 22f * k); lineTo(5f * k, 22f * k); close() }, ink)
                    drawPath(Path().apply { moveTo(8f * k, 3f * k); lineTo(19f * k, 3f * k); lineTo(16.5f * k, 7.5f * k); lineTo(19f * k, 12f * k); lineTo(8f * k, 12f * k); close() }, ink)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.padding(end = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), content = buttons)
    }
}

/**
 * The progress track (Boss's pick "1 Boost"): a baked glass groove (a soft white fill, a lit top rim, no live backdrop: it
 * is 16 dp high and always on screen), mint filling it, and the SR-71 sitting on the tip of the fill.
 */
@Composable
internal fun ProgressTrack(progress: Float, fly: Boolean, modifier: Modifier = Modifier) {
    val atlas = if (fly) rememberJetAtlas() else null
    val shape = CircleShape
    Box(modifier.height(16.dp)) {
    Box(
        Modifier.matchParentSize().clip(shape).background(Brush.verticalGradient(listOf(Color(0x990F1630).copy(alpha = 0.10f), Color.White.copy(alpha = 0.55f))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x33FFFFFF))), shape)
            .semantics { contentDescription = "Lesson progress ${(progress * 100).toInt()} %" },
    ) {
        Box(Modifier.fillMaxWidth(progress.coerceAtLeast(0.04f)).height(16.dp).clip(shape).background(Brush.horizontalGradient(listOf(Glass.Mint2, Chrome.Mint))))
    }
    if (atlas != null) Canvas(Modifier.matchParentSize()) {
        val x = (size.width * progress.coerceAtLeast(0.04f)).coerceAtLeast(14.dp.toPx())
        jetAt(atlas, Offset(x - 6.dp.toPx(), size.height / 2f), 0f, 30.dp.toPx(), 0f, 0f, false, shadow = false)
    }
    }
}

/** One reveal step of the bubble: a word every 330 ms, so the text keeps the pace of his voice (Boss: "natural real-time"). */
private const val WORD_MS = 330L

/**
 * Humo at the top of the page with his bubble: the bubble IS the task line (Boss's pick). Humo is small and has no label or
 * button chrome; tapping him opens the tutor ([onTap] null = not tappable, as in the skill check). The words appear one by
 * one while he talks; without motion (screenshot tests, reduced motion) the whole line is there at once. Digits and *letters*
 * are in the maths serif.
 */
@Composable
internal fun HumoHeader(text: String, humo: HumoState, onTap: (() -> Unit)?, modifier: Modifier = Modifier, compact: Boolean = false) {
    val words = remember(text) { text.replace("*", "").trim().split(Regex("\\s+")).size }
    var shown by remember(text) { mutableIntStateOf(if (humo.alive) 0 else words) }
    LaunchedEffect(text) {
        if (!humo.alive) return@LaunchedEffect
        shown = 0
        delay(180)
        while (shown < words) { shown++; delay(WORD_MS) }
    }
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        val tap = if (onTap != null) Modifier.clickable(role = Role.Button, onClickLabel = "Ask Humo", onClick = onTap) else Modifier
        Humo(humo, tap.size(if (compact) 50.dp else 62.dp).semantics { contentDescription = if (onTap != null) "Ask Humo" else "Humo" })
        Spacer(Modifier.width(10.dp))
        val shape = RoundedCornerShape(20.dp)
        Box(Modifier.weight(1f).clip(shape).background(Chrome.Line).testTag("humo-bubble")) {
            Box(Modifier.fillMaxWidth().padding(bottom = 3.dp).clip(shape).background(Color.White).border(1.5.dp, Chrome.Line, shape).padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 12.dp)) {
                Text(
                    task(text, shown),
                    style = TextStyle(fontFamily = LessonType.Figtree, fontSize = if (compact) 16.sp else 18.sp, lineHeight = if (compact) 22.sp else 25.sp, fontWeight = FontWeight.Normal, color = Chrome.Ink),
                    modifier = Modifier.semantics { heading(); contentDescription = text.replace("*", "") },
                )
            }
        }
    }
}

/** The task as styled text: digits and *letters* in the maths serif; every word after the [shown]th is invisible but still takes its room. */
private fun task(text: String, shown: Int): AnnotatedString {
    val maths = SpanStyle(fontFamily = FontFamily.Serif)
    var words = 0
    var inWord = false
    return buildAnnotatedString {
        var italic = false
        for (ch in text.replace("**", "")) {
            if (ch == '*') { italic = !italic; continue }
            if (ch.isWhitespace()) { if (inWord) { words++; inWord = false } } else inWord = true
            val style = when {
                italic -> maths.copy(fontStyle = FontStyle.Italic)
                ch.isDigit() -> maths
                else -> SpanStyle()
            }
            val hide = words >= shown && !ch.isWhitespace()
            withStyle(if (hide) style.copy(color = Color.Transparent) else style) { append(ch) }
        }
    }
}

/**
 * The big keypad (Boss: "good"): 1-9, undo, 0, tick in chunky white keys with a thick edge, low on the page right above the
 * Check bar for the thumb. A digit fills the active slot of "translate by ( , )". The tick checks. A lesson with negative
 * numbers swaps the tick for a minus key that signs the next digit. The keypad keeps its room (it fades when nothing can be
 * typed) so nothing under it jumps.
 */
@Composable
internal fun Keypad(flow: LessonFlow, negatives: Boolean, keyHeight: Dp, modifier: Modifier = Modifier, onCheck: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    var minus by remember(flow.screen) { androidx.compose.runtime.mutableStateOf(false) }
    val live = flow.slots.trayShown && !flow.locked
    fun digit(d: Int) {
        flow.tapTile(if (minus) -d else d)
        minus = false
        LessonSounds.play(Cue.Tick)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }
    Column(modifier.padding(horizontal = 16.dp).alpha(if (live) 1f else 0.32f).testTag("keypad"), verticalArrangement = Arrangement.spacedBy(if (keyHeight < 40.dp) 6.dp else 8.dp)) {
        for (row in listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (d in row) PadKey(Modifier.weight(1f), keyHeight, live, "tile-$d", "$d") { digit(d) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (negatives) PadKey(Modifier.weight(1f), keyHeight, live, "minus", "−", on = minus) { minus = !minus; LessonSounds.play(Cue.Tick) }
            else PadKey(Modifier.weight(1f), keyHeight, live, "undo", "undo") { flow.undo(); LessonSounds.play(Cue.Tick) }
            PadKey(Modifier.weight(1f), keyHeight, live, "tile-0", "0") { digit(0) }
            if (negatives) PadKey(Modifier.weight(1f), keyHeight, live, "undo", "undo") { flow.undo(); LessonSounds.play(Cue.Tick) }
            else PadKey(Modifier.weight(1f), keyHeight, live && flow.canCheck, "tick", "tick") { onCheck() }
        }
    }
}

/** One key: a white face on a thick edge that the face presses into. "undo" and "tick" draw their icons. */
@Composable
private fun PadKey(modifier: Modifier, keyHeight: Dp, enabled: Boolean, tag: String, label: String, on: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(14.dp)
    val edge = Color(0xFFD5DDD9)
    Box(
        modifier.height(keyHeight).testTag(tag).clickable(interaction, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = when (label) { "undo" -> "Undo"; "tick" -> "Done"; else -> "Key $label" } },
        propagateMinConstraints = true,
    ) {
        Box(Modifier.matchParentSize().padding(top = 3.dp).clip(shape).background(edge))
        Box(
            Modifier.height(keyHeight - 3.dp).offset(y = if (pressed) 3.dp else 0.dp).clip(shape).background(if (on) Chrome.PanelGood else Color.White).border(1.5.dp, if (on) Chrome.Mint else Chrome.Line, shape),
            contentAlignment = Alignment.Center,
        ) {
            when (label) {
                "undo" -> Canvas(Modifier.size(22.dp)) {
                    val k = size.width / 24f
                    val st = Stroke(2.4f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    drawPath(Path().apply { moveTo(9f * k, 14f * k); lineTo(4f * k, 9f * k); lineTo(9f * k, 4f * k) }, Chrome.Ink, style = st)
                    drawPath(Path().apply {
                        moveTo(4f * k, 9f * k); lineTo(14.5f * k, 9f * k)
                        arcTo(androidx.compose.ui.geometry.Rect(9f * k, 9f * k, 20f * k, 20f * k), -90f, 180f, false)
                        lineTo(11f * k, 20f * k)
                    }, Chrome.Ink, style = st)
                }
                "−" -> Canvas(Modifier.size(22.dp)) { drawLine(Chrome.Ink, Offset(4.dp.toPx(), size.height / 2f), Offset(size.width - 4.dp.toPx(), size.height / 2f), 3.dp.toPx(), StrokeCap.Round) }
                "tick" -> Canvas(Modifier.size(22.dp)) {
                    val k = size.width / 24f
                    drawPath(Path().apply { moveTo(4f * k, 12.5f * k); lineTo(9.5f * k, 18f * k); lineTo(20f * k, 6f * k) }, Chrome.MintInk, style = Stroke(3f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                else -> Text(label, fontFamily = LessonType.Figtree, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Chrome.Ink)
            }
        }
    }
}
