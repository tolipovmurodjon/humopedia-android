package com.mcompany.humo.ui.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mcompany.humo.ui.glass.EaseOutStrong
import com.mcompany.humo.ui.glass.Glass
import com.mcompany.humo.ui.glass.pressScale
import com.mcompany.humo.ui.path.rememberSystemMotion
import kotlinx.coroutines.delay

/** Brilliant's lesson chrome colours (spec §1-2). */
private object Chrome {
    val Ink = Color(0xFF111111)
    val Line = Color(0xFFE2E2E2)
    val Green = Color(0xFF3FCE69)
    val Yellow = Color(0xFFF6D545)
    val Track = Color(0xFFE6E6E6)
    val Fill = Color(0xFF29CC57)
    val Dark = Color(0xFF2B2B2B)
    val DarkEdge = Color(0xFF111111)
    val Off = Color(0xFFF2F2F2)
    val OffText = Color(0xFF9E9E9E)
    val Gray = Color(0xFFEBEBEB)
    val GrayEdge = Color(0xFFCFCFCF)
    val Teal = Color(0xFF2CBFA7)
    val TealEdge = Color(0xFF1F9784)
    val GoodBubble = Color(0xFFBDF0C9)
    val BadBubble = Color(0xFFFBE8A4)
    val Muted = Color(0xFF6B6B6B)
    val Icon = Color(0xFF9A9A9A)
}

enum class LessonTheme { Plain, Fly }

/**
 * Lesson L1.1 "Translating Points", rebuilt natively 1:1 with Brilliant (PHASE-LESSON-L11): the same seven pages, texts,
 * numbers, grading, Check / Why? / Try again / Start over / Continue, the skill check, the score and the end card, laid
 * out to the numbers in docs/qa/l11-spec.md. On [LessonTheme.Fly] every point is Boss's SR-71 (fly-layer.js).
 * [onFinish] gets the sparks earned; [onExit] leaves without finishing.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LessonPlayer(
    padding: PaddingValues,
    onExit: () -> Unit,
    onFinish: (sparks: Int) -> Unit,
    theme: LessonTheme = LessonTheme.Fly,
    flow: LessonFlow = remember { LessonFlow() },
    motion: Boolean = rememberSystemMotion(),
) {
    var leaving by rememberSaveable { mutableStateOf(false) }
    var muted by remember { mutableStateOf(LessonSounds.muted) }
    BackHandler(enabled = !leaving) { leaving = true }
    val tint by animateColorAsState(
        when { flow.phase == Phase.Correct && !flow.inSkill -> Color(0x213FCE69); flow.phase == Phase.Wrong && flow.screen !is Screen.Review -> Color(0x2EF6D545); else -> Color.Transparent },
        tween(350), label = "tint",
    )
    Box(Modifier.fillMaxSize().background(Color.White).semantics { testTagsAsResourceId = true }) {
        // The faint page tint on a verdict: only the edges, so nothing reflows (spec §1).
        Box(Modifier.fillMaxSize().drawBehind { drawRect(Brush.radialGradient(0.55f to Color.Transparent, 1f to tint, center = center, radius = size.maxDimension * 0.75f)) })
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
            TopBar(flow, onClose = { leaving = true })
            LessonCard(flow, theme == LessonTheme.Fly, motion, muted, onMute = { muted = !muted; LessonSounds.muted = muted }, onFinish = onFinish)
        }
        WhySheet(flow, Modifier.align(Alignment.BottomCenter).padding(bottom = padding.calculateBottomPadding()))
    }
    if (leaving) {
        AlertDialog(
            onDismissRequest = { leaving = false },
            title = { Text("Leave the lesson?") },
            text = { Text("You will start this lesson over next time.") },
            confirmButton = { TextButton(onClick = { leaving = false; onExit() }) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { leaving = false }) { Text("Keep going") } },
        )
    }
}

/** Close, back, the progress bar, the skill check's three dots and the sparks (spec §1, 48 dp tall, slots reserved). */
@Composable
private fun TopBar(flow: LessonFlow, onClose: () -> Unit) {
    val p by animateFloatAsState(flow.progress.coerceAtLeast(0.03f), tween(550, easing = BrilliantEase), label = "progress")
    Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton("Exit lesson", onClose) { c ->
            drawLine(c, Offset(size.width * 0.3f, size.height * 0.3f), Offset(size.width * 0.7f, size.height * 0.7f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(c, Offset(size.width * 0.7f, size.height * 0.3f), Offset(size.width * 0.3f, size.height * 0.7f), 2.dp.toPx(), StrokeCap.Round)
        }
        // The back arrow keeps its slot on page 1 too, so the bar never moves.
        Box(Modifier.size(36.dp)) {
            if (flow.canGoBack) IconButton("Previous page", { LessonSounds.play(Cue.Tick); flow.back() }, tint = Color(0xFF888888)) { c ->
                val y = size.height / 2f
                drawLine(c, Offset(size.width * 0.25f, y), Offset(size.width * 0.78f, y), 2.dp.toPx(), StrokeCap.Round)
                drawLine(c, Offset(size.width * 0.25f, y), Offset(size.width * 0.48f, y - size.height * 0.22f), 2.dp.toPx(), StrokeCap.Round)
                drawLine(c, Offset(size.width * 0.25f, y), Offset(size.width * 0.48f, y + size.height * 0.22f), 2.dp.toPx(), StrokeCap.Round)
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.weight(1f).height(12.dp).clip(CircleShape).background(Chrome.Track)
                .semantics { contentDescription = "Lesson progress ${(flow.progress * 100).toInt()} %" },
        ) {
            Box(Modifier.fillMaxWidth(p).height(12.dp).clip(CircleShape).background(Chrome.Fill))
        }
        Spacer(Modifier.width(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            flow.skill.indices.forEach { j ->
                val r = flow.skillResults[j]
                val now = (flow.screen as? Screen.Skill)?.j == j
                Box(Modifier.size(8.dp).clip(CircleShape).background(when { r == true -> Chrome.Fill; r == false -> Color(0xFF4A4A4A); now -> Color(0xFFA3A3A3); else -> Chrome.Track }))
            }
        }
        Spacer(Modifier.width(12.dp))
        Row(Modifier.widthIn(min = 52.dp).semantics(mergeDescendants = true) { contentDescription = "${flow.sparks} sparks" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            Text("${flow.sparks}", fontFamily = Glass.Heading, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (flow.sparks > 0) Chrome.Ink else Color(0xFF9B9B9B))
            Spacer(Modifier.width(4.dp))
            Spark(flow.sparks > 0, 20.dp)
        }
    }
}

/** Brilliant's four-point spark: grey outline at 0, filled green once earned. */
@Composable
private fun Spark(on: Boolean, size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        val c = center
        val r = this.size.minDimension * 0.46f
        val w = r * 0.28f
        val path = Path().apply {
            moveTo(c.x, c.y - r); quadraticTo(c.x + w * 0.4f, c.y - w * 0.4f, c.x + r, c.y); quadraticTo(c.x + w * 0.4f, c.y + w * 0.4f, c.x, c.y + r)
            quadraticTo(c.x - w * 0.4f, c.y + w * 0.4f, c.x - r, c.y); quadraticTo(c.x - w * 0.4f, c.y - w * 0.4f, c.x, c.y - r); close()
        }
        if (on) drawPath(path, Chrome.Fill) else drawPath(path, Color(0xFF9B9B9B), style = Stroke(1.8.dp.toPx()))
    }
}

@Composable
private fun IconButton(label: String, onClick: () -> Unit, tint: Color = Color(0xFF4A4A4A), draw: androidx.compose.ui.graphics.drawscope.DrawScope.(Color) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.size(36.dp).pressScale(interaction).clip(RoundedCornerShape(10.dp))
            .clickable(interaction, null, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Canvas(Modifier.size(22.dp)) { draw(tint) } }
}

/** The lesson card (spec §1): the page, its verdict border, the bubble and the buttons. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.LessonCard(flow: LessonFlow, fly: Boolean, motion: Boolean, muted: Boolean, onMute: () -> Unit, onFinish: (Int) -> Unit) {
    val skillQuiet = flow.inSkill
    val border by animateColorAsState(
        when { skillQuiet -> Chrome.Line; flow.phase == Phase.Correct -> Chrome.Green; flow.phase == Phase.Wrong && flow.screen is Screen.Review -> Chrome.Teal; flow.phase == Phase.Wrong -> Chrome.Yellow; else -> Chrome.Line },
        tween(250), label = "border",
    )
    Box(
        Modifier.weight(1f).fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .drawBehind {
                if (border != Chrome.Line) drawRoundRect(border.copy(alpha = 0.14f), Offset(-4.dp.toPx(), -4.dp.toPx()), androidx.compose.ui.geometry.Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), CornerRadius(22.dp.toPx()))
            }
            .clip(RoundedCornerShape(18.dp)).background(Color.White).border(1.dp, border, RoundedCornerShape(18.dp)),
    ) {
        AnimatedContent(
            targetState = flow.screen,
            transitionSpec = {
                (fadeIn(tween(if (motion) 320 else 0, easing = BrilliantEase)) + slideInHorizontally(tween(if (motion) 320 else 0, easing = BrilliantEase)) { (it * 0.07f).toInt() })
                    .togetherWith(fadeOut(tween(0)))
            },
            label = "screen",
        ) { screen ->
            when (screen) {
                is Screen.Page, is Screen.Skill, is Screen.Review -> PageView(flow, fly, motion, muted, onMute)
                Screen.SkillSplash -> SkillSplash(flow)
                Screen.Score -> ScoreView(flow)
                Screen.Retry -> CenterScreen(null, "Skill check", null, listOf(
                    BtnSpec("Retry skill check", Kind.Dark) { flow.beginSkillSplash() },
                    BtnSpec("Skip retry", Kind.Outline) { flow.finish() },
                ))
                Screen.End -> EndView(flow, onFinish)
            }
        }
    }
}

private data class BtnSpec(val label: String, val kind: Kind, val enabled: Boolean = true, val weight: Float = 1f, val onClick: () -> Unit)
private enum class Kind { Dark, Gray, Outline, Teal }

/** One page: the prompt and its board, centred in the card, with the bubble and the buttons at the bottom. */
@Composable
private fun PageView(flow: LessonFlow, fly: Boolean, motion: Boolean, muted: Boolean, onMute: () -> Unit) {
    val page = flow.page ?: return
    val haptics = LocalHapticFeedback.current
    var advancing by remember(flow.screen) { mutableStateOf(false) }
    // A skill item moves on by itself half a second after Check (Brilliant: no verdict per item).
    LaunchedEffect(advancing) { if (advancing) { delay(500); flow.nextSkill() } }

    fun check() {
        val ok = flow.check()
        if (flow.inSkill) { advancing = true; return }
        if (ok) { LessonSounds.play(Cue.Correct); haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
        else { LessonSounds.play(Cue.Incorrect); haptics.performHapticFeedback(HapticFeedbackType.Reject) }
    }

    val review = flow.screen is Screen.Review
    val buttons: List<BtnSpec> = when {
        advancing -> emptyList()
        flow.inSkill -> listOf(
            BtnSpec("I don't know", Kind.Outline) { LessonSounds.play(Cue.Tick); flow.dontKnow(); advancing = true },
            BtnSpec("Check", Kind.Dark, enabled = flow.canCheck) { check() },
        )
        page.kind == PageKind.Explainer || page.kind == PageKind.Recap -> listOf(BtnSpec("Continue", Kind.Dark) { LessonSounds.play(Cue.Tick); flow.next() })
        flow.phase == Phase.Correct -> listOf(
            BtnSpec("Why?", Kind.Gray, weight = 0.42f) { LessonSounds.play(Cue.Tick); flow.whyOpen = true },
            BtnSpec("Continue", Kind.Dark) { LessonSounds.play(Cue.Tick); flow.next() },
        )
        flow.phase == Phase.Wrong -> listOf(BtnSpec("Try again", if (review) Kind.Teal else Kind.Dark) { LessonSounds.play(Cue.Tick); flow.tryAgain() })
        else -> listOf(BtnSpec("Check", Kind.Dark, enabled = flow.canCheck) { check() })
    }

    Column(Modifier.fillMaxSize()) {
        // The card's own tools: the +15 chip (when earned) and sound on/off.
        Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            SparkChip(flow.sparkBurst)
            IconButton(if (muted) "Sound off" else "Sound on", onMute, tint = Chrome.Icon) { c ->
                val h = size.height
                drawPath(Path().apply { moveTo(size.width * 0.12f, h * 0.38f); lineTo(size.width * 0.3f, h * 0.38f); lineTo(size.width * 0.52f, h * 0.18f); lineTo(size.width * 0.52f, h * 0.82f); lineTo(size.width * 0.3f, h * 0.62f); lineTo(size.width * 0.12f, h * 0.62f); close() }, c)
                if (!muted) {
                    drawArc(c, -50f, 100f, false, topLeft = Offset(size.width * 0.42f, h * 0.3f), size = androidx.compose.ui.geometry.Size(size.width * 0.3f, h * 0.4f), style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(c, -50f, 100f, false, topLeft = Offset(size.width * 0.42f, h * 0.16f), size = androidx.compose.ui.geometry.Size(size.width * 0.5f, h * 0.68f), style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
                } else drawLine(c, Offset(size.width * 0.66f, h * 0.36f), Offset(size.width * 0.9f, h * 0.64f), 1.8.dp.toPx(), StrokeCap.Round)
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val boardW = (maxWidth - 24.dp).coerceAtMost(420.dp)
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(horizontal = 12.dp).padding(bottom = 64.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Prompt(page.prompt, bold = page.skill, modifier = Modifier.width(boardW))
                Spacer(Modifier.height(if (page.kind == PageKind.Drag || page.kind == PageKind.Explainer) 30.dp else 26.dp))
                when (page.kind) {
                    PageKind.Drag -> DragBoard(page, flow, fly, motion, boardW)
                    PageKind.Explainer -> FigureBoard(page, fly, boardW)
                    PageKind.Translate, PageKind.Recap -> WorkspaceBoard(page, flow, fly, motion, boardW)
                }
                if (page.graded && !page.skill) StartOver(flow, Modifier.width(boardW))
            }
            Bubble(flow.verdict, flow.phase == Phase.Correct, Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp))
        }
        ButtonsRow(buttons, motion, key = "${flow.screen}-${flow.phase}-$advancing")
    }
}

/** The prompt (spec §1): 16 sp / 25.6, digits and *letters* in maths serif, bold 18 sp in the skill check. */
@Composable
private fun Prompt(text: String, bold: Boolean, modifier: Modifier) {
    val maths = SpanStyle(fontFamily = FontFamily.Serif)
    val styled: AnnotatedString = buildAnnotatedString {
        var italic = false
        for (ch in text) when {
            ch == '*' -> italic = !italic
            italic -> withStyle(maths.copy(fontStyle = FontStyle.Italic)) { append(ch) }
            ch.isDigit() -> withStyle(maths) { append(ch) }
            else -> append(ch)
        }
    }
    Text(
        styled,
        modifier = modifier.semantics { heading() },
        style = TextStyle(fontFamily = Glass.Body, fontSize = if (bold) 18.sp else 16.sp, lineHeight = if (bold) 28.8.sp else 25.6.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = Chrome.Ink),
    )
}

@Composable
private fun StartOver(flow: LessonFlow, modifier: Modifier) {
    val on = flow.canStartOver
    val interaction = remember { MutableInteractionSource() }
    Row(modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
        Row(
            Modifier.height(48.dp).pressScale(interaction).clip(RoundedCornerShape(8.dp))
                .clickable(interaction, null, enabled = on, role = Role.Button) { LessonSounds.play(Cue.Tick); flow.startOver() }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val c = if (on) Chrome.Ink else Color(0x66000000)
            RestartIcon(c, 18.dp)
            Spacer(Modifier.width(6.dp))
            Text("Start over", fontFamily = Glass.Body, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = c)
        }
    }
}

/** Koji's verdict line in its bubble (spec §1): green on right, yellow on wrong; it fades in over the board's foot. */
@Composable
private fun Bubble(text: String?, good: Boolean, modifier: Modifier) {
    var last by remember { mutableStateOf("") }
    if (text != null) last = text
    AnimatedVisibility(text != null, modifier, enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 4 }, exit = fadeOut(tween(120))) {
        Text(
            last, Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(12.dp)).background(if (good) Chrome.GoodBubble else Chrome.BadBubble).padding(horizontal = 16.dp, vertical = 12.dp),
            fontFamily = Glass.Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.75.sp, color = Chrome.Ink,
        )
    }
}

/** "+15 ✦": the sparks a first right answer earns, shown for 1.4 s at the card's top right. */
@Composable
private fun SparkChip(burst: Int) {
    var shown by remember { mutableStateOf(false) }
    // Only a new burst shows it: a page opened after one (the chip is rebuilt per page) starts with it already seen.
    val seen = remember { intArrayOf(burst) }
    LaunchedEffect(burst) { if (burst > seen[0]) { seen[0] = burst; shown = true; delay(1400); shown = false } }
    AnimatedVisibility(shown, enter = fadeIn(tween(160)) + scaleIn(tween(220, easing = EaseOutStrong), initialScale = 0.7f), exit = fadeOut(tween(300))) {
        Row(Modifier.padding(end = 6.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFDDF6E4)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("+${L11.SPARKS_PER_PAGE}", fontFamily = Glass.Heading, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF15803D))
            Spacer(Modifier.width(3.dp))
            Spark(true, 14.dp)
        }
    }
}

/** The buttons: one primary, or a grey pill beside it (spec §2); each new set rises 6 dp and fades in over 200 ms. */
@Composable
private fun ButtonsRow(buttons: List<BtnSpec>, motion: Boolean, key: String) {
    AnimatedContent(
        targetState = key to buttons,
        contentKey = { it.first },
        transitionSpec = { (fadeIn(tween(if (motion) 200 else 0)) + slideInVertically(tween(if (motion) 200 else 0)) { 6 * it / 48 }).togetherWith(fadeOut(tween(0))) },
        label = "buttons",
    ) { (_, list) ->
        Row(Modifier.fillMaxWidth().height(72.dp).padding(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            list.forEach { b -> LButton(b.label, b.kind, b.enabled, Modifier.weight(b.weight), b.onClick) }
        }
    }
}

/** Brilliant's button: 48 dp, pill, a 3 dp darker edge under it; disabled = flat light grey. */
@Composable
private fun LButton(label: String, kind: Kind, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val (face, edge, ink) = when {
        !enabled -> Triple(Chrome.Off, Chrome.Off, Chrome.OffText)
        kind == Kind.Dark -> Triple(Chrome.Dark, Chrome.DarkEdge, Color.White)
        kind == Kind.Teal -> Triple(Chrome.Teal, Chrome.TealEdge, Color.White)
        kind == Kind.Gray -> Triple(Chrome.Gray, Chrome.GrayEdge, Color(0xFF222222))
        else -> Triple(Color.White, Chrome.GrayEdge, Color(0xFF222222))
    }
    Box(
        modifier.height(48.dp).pressScale(interaction).clip(RoundedCornerShape(24.dp)).background(edge)
            .clickable(interaction, null, enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Box(
            Modifier.fillMaxWidth().height(if (enabled) 45.dp else 48.dp).clip(RoundedCornerShape(24.dp)).background(face)
                .then(if (kind == Kind.Outline && enabled) Modifier.border(1.dp, Chrome.GrayEdge, RoundedCornerShape(24.dp)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, fontFamily = Glass.Body, fontWeight = FontWeight.SemiBold, fontSize = if (kind == Kind.Dark || kind == Kind.Teal) 16.sp else 17.sp, color = ink, maxLines = 1)
        }
    }
}

/** A centred screen inside the card: a badge, a big title, a line, stacked 290 dp buttons (spec §8). */
@Composable
private fun CenterScreen(big: (@Composable () -> Unit)?, title: String, line: String?, buttons: List<BtnSpec>) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        if (big == null) CheckBadge()
        else big()
        Spacer(Modifier.height(28.dp))
        Text(title, fontFamily = Glass.Heading, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp, color = Chrome.Ink, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        if (line != null) {
            Spacer(Modifier.height(8.dp))
            Text(line, fontFamily = Glass.Body, fontSize = 16.sp, color = Color(0xFF6B6B6B), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(36.dp))
        buttons.forEachIndexed { i, b ->
            if (i > 0) Spacer(Modifier.height(14.dp))
            LButton(b.label, b.kind, b.enabled, Modifier.widthIn(max = 290.dp).fillMaxWidth()) { LessonSounds.play(Cue.Tick); b.onClick() }
        }
    }
}

/** The skill check's mark: a periwinkle disc with a white tick (Brilliant's #7A8CFF shade, drawn, not their art). */
@Composable
private fun CheckBadge() {
    Canvas(Modifier.size(64.dp)) {
        drawCircle(Color(0xFF7A8CFF), size.minDimension / 2f)
        drawCircle(Color(0xFF6274F0), size.minDimension / 2f, style = Stroke(3.dp.toPx()))
        drawPath(Path().apply { moveTo(size.width * 0.3f, size.height * 0.52f); lineTo(size.width * 0.44f, size.height * 0.66f); lineTo(size.width * 0.71f, size.height * 0.36f) }, Color.White, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun SkillSplash(flow: LessonFlow) {
    LaunchedEffect(Unit) { LessonSounds.play(Cue.Start); delay(1700); flow.startSkill() }
    CenterScreen(null, "Skill check", null, emptyList())
}

@Composable
private fun ScoreView(flow: LessonFlow) {
    val n = flow.skillRight
    val total = flow.skill.size
    LaunchedEffect(Unit) { LessonSounds.play(Cue.Start) }
    CenterScreen(
        big = {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$n out of $total" }) {
                Text("$n", fontFamily = Glass.Heading, fontWeight = FontWeight.ExtraBold, fontSize = 72.sp, lineHeight = 72.sp, color = Chrome.Ink)
                Text("/$total", fontFamily = Glass.Heading, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF9B9B9B), modifier = Modifier.offset { IntOffset(0, 8) })
            }
        },
        title = L11.scoreTitle(n, total),
        line = null,
        buttons = if (n == total) listOf(BtnSpec("Continue", Kind.Dark) { flow.finish() })
        else listOf(BtnSpec("Review mistakes", Kind.Dark) { flow.startReview() }, BtnSpec("Skip review", Kind.Outline) { flow.skipReview() }),
    )
}

@Composable
private fun EndView(flow: LessonFlow, onFinish: (Int) -> Unit) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) { LessonSounds.play(Cue.Complete); haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
    CenterScreen(null, "Lesson complete", "${flow.sparks} sparks earned.", listOf(BtnSpec("Continue", Kind.Dark) { onFinish(flow.sparks) }))
}

/**
 * Why? (spec §7): Brilliant opens its tutor chat with the explanation; here a sheet over the card's foot with the same
 * explanation text and the same two answers. Both close it.
 */
@Composable
private fun WhySheet(flow: LessonFlow, modifier: Modifier) {
    val last = flow.page?.explain ?: ""
    AnimatedVisibility(flow.whyOpen, modifier, enter = slideInVertically(tween(280, easing = EaseOutStrong)) { it } + fadeIn(), exit = slideOutVertically(tween(200)) { it } + fadeOut()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)
                .drawBehind { drawRoundRect(Color(0x1F000000), Offset(0f, -3.dp.toPx()), size, CornerRadius(20.dp.toPx())) }
                .clip(RoundedCornerShape(18.dp)).background(Color.White).border(1.dp, Chrome.Line, RoundedCornerShape(18.dp))
                .padding(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton("Close", { flow.whyOpen = false }, tint = Color(0xFF999999)) { c ->
                    drawLine(c, Offset(size.width * 0.3f, size.height * 0.3f), Offset(size.width * 0.7f, size.height * 0.7f), 2.dp.toPx(), StrokeCap.Round)
                    drawLine(c, Offset(size.width * 0.7f, size.height * 0.3f), Offset(size.width * 0.3f, size.height * 0.7f), 2.dp.toPx(), StrokeCap.Round)
                }
            }
            Text(
                "Why is this the answer?", Modifier.align(Alignment.End).clip(RoundedCornerShape(12.dp)).border(1.dp, Chrome.Line, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                fontFamily = Glass.Body, fontSize = 15.sp, color = Chrome.Ink,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "$last\n\nDoes that make sense?", Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFEEF3F8)).padding(horizontal = 14.dp, vertical = 12.dp),
                fontFamily = Glass.Body, fontSize = 15.sp, lineHeight = 21.75.sp, color = Chrome.Ink,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Yes, that makes sense", "Not really").forEach { chip ->
                    Text(
                        chip, Modifier.clip(RoundedCornerShape(18.dp)).border(1.dp, Chrome.GrayEdge, RoundedCornerShape(18.dp))
                            .clickable(role = Role.Button) { LessonSounds.play(Cue.Tick); flow.whyOpen = false }.padding(horizontal = 14.dp, vertical = 9.dp),
                        fontFamily = Glass.Body, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Chrome.Ink,
                    )
                }
            }
        }
    }
}
