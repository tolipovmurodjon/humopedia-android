package com.mcompany.humo.ui.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.draggable
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

/** False until the lesson's first frame is on screen: the chrome is drawn flat, then the glass turns on (the shell does the same, GlassStage). */
internal val LocalLessonGlass = androidx.compose.runtime.compositionLocalOf { true }

/** Glass in the shape of a rounded rectangle; without a page behind it (or before the glass is turned on) a flat tinted pane. */
@Composable
internal fun Modifier.chromeGlass(radius: Dp, tint: Tint = Tint.Clear): Modifier {
    val page = LocalPageBackdrop.current
    val shape = RoundedCornerShape(radius)
    val surface = if (page == null || !GlassStage.on || !LocalLessonGlass.current) {
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

/** The row that holds the lesson's main buttons (Check, Continue): the buttons only, each one shape, no box under them. */
@Composable
internal fun ButtonBar(bottomInset: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp + bottomInset).testTag("dock"),
        horizontalArrangement = Arrangement.spacedBy(12.dp), content = content,
    )
}

internal class SheetSpec(
    val key: String, val ok: Boolean, val calm: Boolean, val label: String, val detail: String, val kind: Kind, val primary: String,
    val secondary: ChunkyIcon?, val flag: Boolean, val onPrimary: () -> Unit,
)

/** Counts how many times a feedback sheet has entered the composition: once per verdict, never again while it stays. For the test. */
internal object FeedbackStats { @Volatile var enters = 0 }

/**
 * The verdict as a bottom sheet (Duolingo's): full width from the very bottom edge, a handle, the icon and the verdict, and the main
 * button in EXACTLY the spot and size the Check button had, so the thumb never moves. Opaque, so it covers the Check bar. Drag it down
 * for a peek (handle, verdict, button), up for the full line; a fling or a drag past 48 dp snaps to the other state. On a translate page
 * the row is the Check row (a quarter for the icon button, three quarters for the main one); elsewhere the icon button sits in the full view.
 */
@Composable
internal fun FeedbackSheet(spec: SheetSpec, bottomInset: Dp, translateRow: Boolean, onFlag: (() -> Unit)?, secondary: (@Composable (Modifier) -> Unit)?) {
    androidx.compose.runtime.DisposableEffect(Unit) { FeedbackStats.enters++; onDispose { } }
    var expanded by remember(spec.key) { androidx.compose.runtime.mutableStateOf(false) }
    var dragged by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val threshold = with(density) { 48.dp.toPx() }
    val ink = if (spec.calm) Color(0xFF4A5159) else if (spec.ok) Chrome.MintInk else Chrome.YellowInk
    val bg = if (spec.calm) Color(0xFFF1F3F4) else if (spec.ok) Color(0xFFE3F7EC) else Color(0xFFFFF3D6)
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    Column(
        Modifier.fillMaxWidth().testTag("feedback-panel").semantics { stateDescription = if (expanded) "expanded" else "peek" }
            .clip(shape).background(bg)
            .draggable(
                orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                state = androidx.compose.foundation.gestures.rememberDraggableState { dragged += it },
                onDragStarted = { dragged = 0f },
                onDragStopped = { v ->
                    if (v < -400f || dragged < -threshold) expanded = true else if (v > 400f || dragged > threshold) expanded = false
                    dragged = 0f
                },
            )
            .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp).size(36.dp, 4.dp).clip(CircleShape).background(ink.copy(alpha = 0.28f)))
        Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(34.dp)) {
                val k = size.width / 34f
                if (spec.ok) {
                    drawCircle(Chrome.Mint, size.minDimension / 2f)
                    drawPath(Path().apply { moveTo(9f * k, 17.5f * k); lineTo(14.5f * k, 23f * k); lineTo(25f * k, 11f * k) }, Color.White, style = Stroke(3.6f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
                } else {
                    drawCircle(if (spec.calm) Color(0xFF8A929A) else Chrome.Yellow, size.minDimension / 2f)
                    val x = if (spec.calm) Color.White else Color(0xFF4A3600)
                    drawLine(x, Offset(11f * k, 11f * k), Offset(23f * k, 23f * k), 3.6f * k, StrokeCap.Round)
                    drawLine(x, Offset(23f * k, 11f * k), Offset(11f * k, 23f * k), 3.6f * k, StrokeCap.Round)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(spec.label, Modifier.weight(1f), fontFamily = LessonType.Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp, color = ink)
            // off a translate page the primary button is full width (like Check), so the icon button sits up here, in the peek too
            if (!translateRow && secondary != null) Box(Modifier.width(64.dp).padding(end = 8.dp)) { secondary(Modifier.fillMaxWidth()) }
            if (onFlag != null) Box(Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Report this page", onClick = onFlag).semantics { contentDescription = "Report this page" }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(22.dp)) {
                    val k = size.width / 24f
                    drawPath(Path().apply { moveTo(5f * k, 2f * k); lineTo(7f * k, 2f * k); lineTo(7f * k, 22f * k); lineTo(5f * k, 22f * k); close() }, ink)
                    drawPath(Path().apply { moveTo(8f * k, 3f * k); lineTo(19f * k, 3f * k); lineTo(16.5f * k, 7.5f * k); lineTo(19f * k, 12f * k); lineTo(8f * k, 12f * k); close() }, ink)
                }
            }
        }
        if (expanded) {
            Text(spec.detail, Modifier.padding(start = 66.dp, end = 20.dp, top = 6.dp), fontFamily = LessonType.Figtree, fontSize = 16.sp, lineHeight = 23.sp, color = ink)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp + bottomInset).testTag("sheet-buttons"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (translateRow) Box(Modifier.weight(1f)) { secondary?.invoke(Modifier.fillMaxWidth()) }
            Chunky(spec.primary, spec.kind, true, Modifier.weight(if (translateRow) 3f else 1f), onClick = spec.onPrimary)
        }
    }
}


/** The close button: a 44 dp round glass button with a clean dark-grey cross (same family as the other glass controls). */
@Composable
internal fun CloseButton(onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).chromeGlass(22.dp).clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Exit lesson", onClick = onClick).semantics { contentDescription = "Exit lesson" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val w = 2.6.dp.toPx()
            drawLine(Color(0xFF4A4F57), Offset(0f, 0f), Offset(size.width, size.height), w, StrokeCap.Round)
            drawLine(Color(0xFF4A4F57), Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
        }
    }
}

/**
 * The progress bar (Boss: like Duolingo's): 16 dp thick and round, a clean green fill with a lit stripe along its top, in a baked
 * glass groove (no live backdrop: it is always on screen). No jet and no icon on the tip; the caller animates [progress].
 */
@Composable
internal fun ProgressTrack(progress: Float, modifier: Modifier = Modifier) {
    val shape = CircleShape
    Box(
        modifier.height(16.dp).clip(shape).background(Brush.verticalGradient(listOf(Color(0x1F0F1630), Color.White.copy(alpha = 0.55f))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color(0x33FFFFFF))), shape)
            .semantics { contentDescription = "Lesson progress ${(progress * 100).toInt()} %" },
    ) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0.04f, 1f)).height(16.dp).clip(shape).background(Chrome.Mint)) {
            Box(Modifier.padding(start = 6.dp, end = 6.dp, top = 3.dp).fillMaxWidth().height(4.dp).clip(shape).background(Color.White.copy(alpha = 0.38f)))
        }
    }
}

/** One reveal step of the bubble: a word every 330 ms, so the text keeps the pace of his voice (Boss: "natural real-time"). */
private const val WORD_MS = 330L

/**
 * Humo at the top of the page with his bubble. The bubble shows exactly what Humo says now (the task, then his reply), replaced in
 * place; there is no second text box. Humo is 76 dp (60 dp on short phones), no label, no button chrome; tapping him opens the tutor
 * ([onTap] null = not tappable, as in the skill check). The words appear one by one while he talks; without motion (screenshot
 * tests, reduced motion) the whole line is there at once. Numbers and the words steps, right, left, up, down are tinted mint, normal
 * weight; digits and *letters* are in the maths serif.
 */
@Composable
internal fun HumoHeader(text: String, humo: HumoState, onTap: (() -> Unit)?, modifier: Modifier = Modifier, compact: Boolean = false) {
    val words = remember(text) { text.replace("*", "").trim().split(Regex("\\s+")).size }
    // a line already shown in full is shown whole again (after Try again the task is not written out a second time)
    var shown by remember(text) { mutableIntStateOf(if (humo.alive && text !in humo.revealed) 0 else words) }
    var cutAt by remember(text) { mutableIntStateOf(-1) }
    val hush = humo.hush
    LaunchedEffect(hush) { if (hush > 0 && humo.alive && shown < words && cutAt < 0) cutAt = shown }
    LaunchedEffect(text) {
        if (!humo.alive || text in humo.revealed) return@LaunchedEffect
        shown = 0
        delay(180)
        while (shown < words && cutAt < 0) { shown++; delay(WORD_MS) }
        if (shown >= words) humo.revealed.add(text)
    }
    val spoken = if (cutAt >= 0) text.replace("*", "").trim().split(Regex("\\s+")).take(cutAt).joinToString(" ") else text
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        val tap = if (onTap != null) Modifier.clickable(role = Role.Button, onClickLabel = "Ask Humo", onClick = onTap) else Modifier
        Humo(humo, tap.size(if (compact) 60.dp else 76.dp).semantics { contentDescription = if (onTap != null) "Ask Humo" else "Humo" })
        Spacer(Modifier.width(14.dp))
        val shape = RoundedCornerShape(16.dp)
        Box(
            Modifier.weight(1f).testTag("humo-bubble").drawBehind {
                // the small tail toward Humo
                val cy = size.height / 2f
                val t = 8.dp.toPx()
                drawPath(Path().apply { moveTo(1f, cy - t); lineTo(-t, cy); lineTo(1f, cy + t); close() }, Color.White.copy(alpha = 0.92f))
            }.clip(shape).background(Color.White.copy(alpha = 0.92f)).border(0.75.dp, Color.White, shape)
                .padding(horizontal = 18.dp, vertical = if (compact) 12.dp else 16.dp),
        ) {
            Text(
                task(spoken, if (cutAt >= 0) cutAt else shown),
                style = TextStyle(fontFamily = LessonType.Figtree, fontSize = if (compact) 16.sp else 18.sp, lineHeight = if (compact) 23.sp else 26.sp, fontWeight = FontWeight.Normal, color = Chrome.Ink),
                modifier = Modifier.semantics { heading(); contentDescription = text.replace("*", "") },
            )
        }
    }
}

private val KeyWords = Regex("(?<![\\w-])\\d+(?![\\w-])|\\b(steps?|right|left|up|down)\\b")

/** The line as styled text: numbers and direction words tinted, digits and *letters* in the maths serif; every word after the [shown]th is invisible but still takes its room. */
private fun task(text: String, shown: Int): AnnotatedString {
    val maths = SpanStyle(fontFamily = FontFamily.Serif)
    val raw = text.replace("**", "")
    val tinted = BooleanArray(raw.length).also { t -> KeyWords.findAll(raw).forEach { m -> for (i in m.range) t[i] = true } }
    var words = 0
    var inWord = false
    return buildAnnotatedString {
        var italic = false
        for ((i, ch) in raw.withIndex()) {
            if (ch == '*') { italic = !italic; continue }
            if (ch.isWhitespace()) { if (inWord) { words++; inWord = false } } else inWord = true
            var style = when {
                italic -> maths.copy(fontStyle = FontStyle.Italic)
                ch.isDigit() -> maths
                else -> SpanStyle()
            }
            if (tinted[i]) style = style.copy(color = Chrome.MintInk)
            val hide = words >= shown && !ch.isWhitespace()
            withStyle(if (hide) style.copy(color = Color.Transparent) else style) { append(ch) }
        }
    }
}

/** The two slots' colours (Boss's K1 pick): a blue, b mint. Digits are the darker ink (4.5:1 on the fill); marks and rings use the brighter one. */
internal object SlotColors {
    val BlueMark = Color(0xFF2F6FE4); val BlueFill = Color(0xFFEAF1FD); val BlueInk = Color(0xFF2A62CC); val BlueLip = Color(0x662A62CC)
    val MintMark = Color(0xFF1C9A74); val MintFill = Color(0xFFE6F5EF); val MintInk = Color(0xFF12755A); val MintLip = Color(0x661C7E69)
    fun mark(i: Int) = if (i == 0) BlueMark else MintMark
    fun fill(i: Int) = if (i == 0) BlueFill else MintFill
    fun ink(i: Int) = if (i == 0) BlueInk else MintInk
    fun lip(i: Int) = if (i == 0) BlueLip else MintLip
}

/**
 * The two-row pad (Boss's K1): 1 2 3 4 5 and back, 6 7 8 9 0 and minus, low above the button row, tinted to the slot it is filling
 * (blue for a, mint for b). A digit fills the active slot and the cursor moves on to b; there is no tick key, Check commits.
 */
@Composable
internal fun Keypad(flow: LessonFlow, negatives: Boolean, keyHeight: Dp, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    var minus by remember(flow.screen) { androidx.compose.runtime.mutableStateOf(false) }
    val live = !flow.locked
    val slot = flow.slots.active ?: 1
    fun digit(d: Int) {
        if (flow.slots.active == null) flow.tapSlot(1)
        flow.tapTile(if (minus) -d else d)
        minus = false
        LessonSounds.play(Cue.Toggle)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }
    val h = maxOf(keyHeight, 42.dp)
    Column(modifier.padding(horizontal = 16.dp).testTag("keypad"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in listOf(listOf("1", "2", "3", "4", "5", "undo"), listOf("6", "7", "8", "9", "0", "−"))) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (k in row) when (k) {
                    "undo" -> PadKey(Modifier.weight(1f), h, live, "undo", "undo", slot = slot) { flow.undo(); LessonSounds.play(Cue.Toggle) }
                    "−" -> PadKey(Modifier.weight(1f), h, live && negatives, "minus", "−", on = minus, slot = slot) { minus = !minus; LessonSounds.play(Cue.Toggle) }
                    else -> PadKey(Modifier.weight(1f), h, live, "tile-$k", k, slot = slot) { digit(k.toInt()) }
                }
            }
        }
    }
}

/** One key as ONE shape: a white face over a solid lip, drawn by this node; the face presses into the lip. "undo" and the minus draw their icons. */
@Composable
private fun PadKey(modifier: Modifier, keyHeight: Dp, enabled: Boolean, tag: String, label: String, on: Boolean = false, slot: Int = 0, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val edge = SlotColors.lip(slot)
    val ink = SlotColors.ink(slot)
    val face by androidx.compose.animation.animateColorAsState(if (on) Chrome.PanelGood else SlotColors.fill(slot), androidx.compose.animation.core.tween(120), label = "key")
    Box(
        modifier.height(keyHeight).alpha(if (enabled) 1f else 0.4f).testTag(tag).clickable(interaction, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = when (label) { "undo" -> "Undo"; "tick" -> "Done"; else -> "Key $label" } }
            .drawBehind {
                val lip = 3.dp.toPx()
                val r = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx())
                val faceSize = Size(size.width, size.height - lip)
                val dy = if (pressed) lip else 0f
                if (!pressed) drawRoundRect(edge, Offset(0f, lip), faceSize, r)
                drawRoundRect(face, Offset(0f, dy), faceSize, r)
                drawRoundRect(if (on) Chrome.Mint else edge, Offset(0.75.dp.toPx(), dy + 0.75.dp.toPx()), Size(faceSize.width - 1.5.dp.toPx(), faceSize.height - 1.5.dp.toPx()), r, style = Stroke(1.5.dp.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.padding(bottom = 3.dp).offset(y = if (pressed) 3.dp else 0.dp), contentAlignment = Alignment.Center) {
            when (label) {
                "undo" -> Canvas(Modifier.size(22.dp)) {
                    val k = size.width / 24f
                    val st = Stroke(2.4f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    drawPath(Path().apply { moveTo(9f * k, 14f * k); lineTo(4f * k, 9f * k); lineTo(9f * k, 4f * k) }, ink, style = st)
                    drawPath(Path().apply {
                        moveTo(4f * k, 9f * k); lineTo(14.5f * k, 9f * k)
                        arcTo(androidx.compose.ui.geometry.Rect(9f * k, 9f * k, 20f * k, 20f * k), -90f, 180f, false)
                        lineTo(11f * k, 20f * k)
                    }, ink, style = st)
                }
                "tick" -> Canvas(Modifier.size(22.dp)) {
                    val k = size.width / 24f
                    drawPath(Path().apply { moveTo(4f * k, 12.5f * k); lineTo(9.5f * k, 18f * k); lineTo(20f * k, 6f * k) }, Chrome.MintInk, style = Stroke(3f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                "\u2212" -> Canvas(Modifier.size(22.dp)) { drawLine(ink, Offset(4.dp.toPx(), size.height / 2f), Offset(size.width - 4.dp.toPx(), size.height / 2f), 3.dp.toPx(), StrokeCap.Round) }
                else -> Text(label, fontFamily = LessonType.Figtree, fontWeight = FontWeight.Medium, fontSize = 22.sp, color = ink)
            }
        }
    }
}
