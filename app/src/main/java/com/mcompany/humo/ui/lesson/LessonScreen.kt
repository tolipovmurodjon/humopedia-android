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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput

/** The lesson chrome's colours, measured on the web player (engine/play.html) that this screen copies 1:1. */
private object Chrome {
    val Ink = Color(0xFF111111)
    val Line = Color(0xFFE2E2E2)
    val Green = Color(0xFF3FCE69)
    val Yellow = Color(0xFFF6D545)
    val Track = Color(0xFFE6E6E6)
    val Fill = Color(0xFF2ECC5F)
    val DotWrong = Color(0xFF3D3D3D)
    val DotNow = Color(0xFFBDBDBD)
    val Bolt = Color(0xFFB6CF0E)
    val StarOff = Color(0xFF9A9A9A)
    val Dark = Color(0xFF2B2B2B)
    val DarkEdge = Color(0xFF111111)
    val Off = Color(0xFFF4F4F4)
    val OffText = Color(0xFFB0B0B0)
    val Gray = Color(0xFFEBEBEB)
    val GrayEdge = Color(0xFFCFCFCF)
    val Teal = Color(0xFF2CBFA7)
    val TealEdge = Color(0xFF1F9784)
    val GoodBubble = Color(0xFFBDF0C9)
    val BadBubble = Color(0xFFFBE8A4)
    val SayBubble = Color(0xFFEEF3F8)
    val SayEdge = Color(0xFFCDDAE6)
    val PageGood = Color(0xFFF1FBF4)
    val PageBad = Color(0xFFFEFBEC)
    val Muted = Color(0xFF6B7079)
    val Icon = Color(0xFFA3A3A3)
    val Close = Color(0xFF5A5F68)
    val Medal = Color(0xFF6170EC)
}

enum class LessonTheme { Plain, Fly }

/**
 * A Brilliant lesson (L1.1 "Translating Points", L1.2 "Negative Translations"), rebuilt natively and dressed like our web
 * player (engine/play.html): Figtree, the progress bar, skill dots and sparks on top, the card with its verdict glow, Humo
 * bottom left with what Humo says above the buttons, the bare medal screens, the Mach 3 pass. On [LessonTheme.Fly]
 * every point is Boss's SR-71. [onFinish] gets the sparks earned; [onExit] leaves without finishing.
 * [humoAlive] runs Humo's springs and blinks (off in the screenshot tests, which want a still pose).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LessonPlayer(
    padding: PaddingValues,
    onExit: () -> Unit,
    onFinish: (sparks: Int) -> Unit,
    theme: LessonTheme = LessonTheme.Fly,
    lesson: LessonContent = L11,
    flow: LessonFlow = remember(lesson) { LessonFlow(lesson) },
    motion: Boolean = rememberSystemMotion(),
    humoAlive: Boolean = motion,
) {
    var leaving by rememberSaveable { mutableStateOf(false) }
    val atEnd = flow.screen is Screen.End
    val close: () -> Unit = { if (atEnd) onFinish(flow.sparks) else { leaving = true } }
    BackHandler(enabled = !leaving) { close() }
    val page by animateColorAsState(
        when {
            flow.phase == Phase.Correct -> Chrome.PageGood
            flow.phase == Phase.Wrong && flow.screen !is Screen.Review -> Chrome.PageBad
            else -> Color.White
        },
        tween(250), label = "page",
    )
    val humo = rememberHumo(humoAlive)
    flow.machineName = if (theme == LessonTheme.Fly) Words.MACHINE else "point"
    val voice = rememberHumoVoice()
    LaunchedEffect(flow.screen) { voice.stop() }
    HumoMoods(flow, humo, voice)
    // A tap anywhere asks the Mach 3 pass for another run; the root only counts it (the Initial pass), the buttons still get it.
    val taps = remember { mutableIntStateOf(0) }
    Box(
        Modifier.fillMaxSize().background(page).semantics { testTagsAsResourceId = true }.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) { if (awaitPointerEvent(PointerEventPass.Initial).changes.any { it.changedToDown() }) taps.intValue++ }
            }
        },
    ) {
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())) {
            TopBar(flow, onClose = close)
            androidx.compose.runtime.CompositionLocalProvider(LocalHumoVoice provides voice) {
                LessonCard(flow, theme == LessonTheme.Fly, motion, humo, onFinish = onFinish)
            }
        }
        if (theme == LessonTheme.Fly && motion && atEnd) MachPass(taps, flow.skillRight == flow.skill.size)
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

/** What Humo feels on each screen (player.js `react`): glad and a hop on right, thinking on wrong, cheering on the bare screens. */
@Composable
private fun HumoMoods(flow: LessonFlow, humo: HumoState, voice: HumoVoice) {
    val screen = flow.screen
    val perfect = flow.skillRight == flow.skill.size
    LaunchedEffect(flow.phase, screen, flow.inSkill) {
        when {
            screen is Screen.End -> { humo.face = HumoFace.Happy; humo.play(HumoMove.Celebrate) }
            screen is Screen.SkillSplash -> { humo.face = HumoFace.Cheer; humo.play(HumoMove.Jump); if (humo.alive) { delay(2200); humo.face = HumoFace.Rest } }
            screen is Screen.Score && perfect -> { humo.face = HumoFace.Laugh; humo.play(HumoMove.Celebrate); if (humo.alive) { delay(2600); humo.face = HumoFace.Rest } }
            screen is Screen.Score -> { humo.face = HumoFace.Proud; humo.play(HumoMove.Nod); if (humo.alive) { delay(2200); humo.face = HumoFace.Rest } }
            flow.phase == Phase.Correct && !flow.inSkill -> { humo.face = HumoFace.Happy; humo.play(HumoMove.Jump); if (humo.alive) { delay(1800); humo.face = HumoFace.Rest } }
            flow.phase == Phase.Wrong && !flow.inSkill -> { humo.face = HumoFace.Thinking; if (humo.alive) { delay(2600); humo.face = HumoFace.Rest } }
            else -> humo.face = HumoFace.Rest
        }
    }
    // Humo says every line he shows: his mouth moves for about 45 ms a letter (0.7-2.6 s), then he is glad after a right answer, else at rest.
    val last = flow.chat.lines.lastOrNull()
    LaunchedEffect(last) {
        if (last == null || last.mine || last.typing || !humo.alive) return@LaunchedEffect
        humo.talking = true
        // A bundled recording sets how long the mouth moves; a line without one gets the letter estimate and stays silent.
        val spoken = voice.on && !flow.inSkill && voice.say(last.text)
        if (!spoken) delay((last.text.length * 45L).coerceIn(700L, 2600L))
        humo.talking = false
        humo.face = if (flow.phase == Phase.Correct && !flow.inSkill) HumoFace.Happy else HumoFace.Rest
    }
    LaunchedEffect(flow.lookOn) {
        humo.lookSource = { flow.humoLook }
        humo.tracking = flow.lookOn
    }
}

/** Close, back, the progress bar, the skill check's three dots and the sparks (play.html `.top`, 64 dp tall, slots reserved). */
@Composable
private fun TopBar(flow: LessonFlow, onClose: () -> Unit) {
    val p by animateFloatAsState(flow.progress, tween(400, easing = BrilliantEase), label = "progress")
    Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton("Exit lesson", onClose, tint = Chrome.Close) { c ->
            drawLine(c, Offset(size.width * 0.3f, size.height * 0.3f), Offset(size.width * 0.7f, size.height * 0.7f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(c, Offset(size.width * 0.7f, size.height * 0.3f), Offset(size.width * 0.3f, size.height * 0.7f), 2.dp.toPx(), StrokeCap.Round)
        }
        Spacer(Modifier.width(4.dp))
        // The back arrow keeps its slot on page 1 too, so the bar never moves.
        Box(Modifier.size(36.dp)) {
            if (flow.canGoBack && !flow.inSkill) IconButton("Previous page", { LessonSounds.play(Cue.Tick); flow.back() }, tint = Chrome.Close) { c ->
                val y = size.height / 2f
                drawLine(c, Offset(size.width * 0.25f, y), Offset(size.width * 0.78f, y), 2.dp.toPx(), StrokeCap.Round)
                drawLine(c, Offset(size.width * 0.25f, y), Offset(size.width * 0.48f, y - size.height * 0.22f), 2.dp.toPx(), StrokeCap.Round)
                drawLine(c, Offset(size.width * 0.25f, y), Offset(size.width * 0.48f, y + size.height * 0.22f), 2.dp.toPx(), StrokeCap.Round)
            }
        }
        Spacer(Modifier.width(8.dp))
        if (flow.inSkill) Spacer(Modifier.weight(1f)) else Box(
            Modifier.weight(1f).height(10.dp).clip(CircleShape).background(Chrome.Track)
                .semantics { contentDescription = "Lesson progress ${(flow.progress * 100).toInt()} %" },
        ) {
            Box(Modifier.fillMaxWidth(p).height(10.dp).clip(CircleShape).background(Chrome.Fill))
        }
        Spacer(Modifier.width(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            flow.skill.indices.forEach { j ->
                val r = flow.skillResults[j]
                val now = (flow.screen as? Screen.Skill)?.j == j
                Box(Modifier.size(9.dp).clip(CircleShape).background(when { r == true -> Chrome.Fill; r == false -> Chrome.DotWrong; now -> Chrome.DotNow; else -> Chrome.Track }))
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(Modifier.padding(end = 6.dp).semantics(mergeDescendants = true) { contentDescription = "${flow.sparks} sparks" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            Text("${flow.sparks}", fontFamily = LessonType.Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Chrome.Ink)
            Spacer(Modifier.width(6.dp))
            Spark(if (flow.sparks > 0) Chrome.Green else Chrome.StarOff, 20.dp)
            Spacer(Modifier.width(6.dp))
            Bolt()
        }
    }
}

private val SparkPath: Path by lazy { PathParser().parsePathString("M12 1.5l2.4 7.4 7.6 3.1-7.6 3.1L12 22.5l-2.4-7.4L2 12l7.6-3.1z").toPath() }
private val BoltPath: Path by lazy { PathParser().parsePathString("M10.5 0L0 12.6h6.2L4.6 22 16 8.7H9.6z").toPath() }
private val FlagPath: Path by lazy { PathParser().parsePathString("M5 2h2v20H5zM8 3h11l-2.5 4.5L19 12H8z").toPath() }

/** The four-point spark of the sparks counter: green once earned, grey before (play.html `#star`). */
@Composable
private fun Spark(color: Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) { withTransform({ scale(this.size.width / 24f, this.size.height / 24f, Offset.Zero) }) { drawPath(SparkPath, color) } }
}

/** The lime bolt next to the sparks (play.html `.bolt`, 16 x 22). */
@Composable
private fun Bolt() {
    Canvas(Modifier.size(width = 14.dp, height = 20.dp)) { withTransform({ scale(this.size.width / 16f, this.size.height / 22f, Offset.Zero) }) { drawPath(BoltPath, Chrome.Bolt) } }
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

/** The lesson card (play.html `.card`): the page, its verdict border and glow; the bare screens have none. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.LessonCard(flow: LessonFlow, fly: Boolean, motion: Boolean, humo: HumoState, onFinish: (Int) -> Unit) {
    val bare = flow.screen.let { it is Screen.SkillSplash || it is Screen.Score || it is Screen.End }
    val verdictOn = !bare
    val border by animateColorAsState(
        when {
            bare -> Color.Transparent
            !verdictOn -> Chrome.Line
            flow.phase == Phase.Correct -> Chrome.Green
            flow.phase == Phase.Wrong && flow.screen is Screen.Review -> Chrome.Teal
            flow.phase == Phase.Wrong -> Chrome.Yellow
            else -> Chrome.Line
        },
        tween(250), label = "border",
    )
    val glowOn = verdictOn && flow.phase != Phase.Idle
    Box(
        Modifier.weight(1f).fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .drawBehind {
                if (glowOn) drawRoundRect(border.copy(alpha = if (flow.phase == Phase.Correct) 0.12f else 0.16f), Offset(-4.dp.toPx(), -4.dp.toPx()), androidx.compose.ui.geometry.Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), CornerRadius(22.dp.toPx()))
            }
            .clip(RoundedCornerShape(18.dp)).background(if (bare) Color.Transparent else Color.White).border(1.dp, border, RoundedCornerShape(18.dp)),
    ) {
        val slide = with(LocalDensity.current) { 28.dp.roundToPx() }
        AnimatedContent(
            targetState = flow.screen,
            transitionSpec = {
                (fadeIn(tween(if (motion) 320 else 0, easing = BrilliantEase)) + slideInHorizontally(tween(if (motion) 320 else 0, easing = BrilliantEase)) { slide })
                    .togetherWith(fadeOut(tween(0)))
            },
            label = "screen",
        ) { screen ->
            when (screen) {
                is Screen.Page, is Screen.Skill, is Screen.Review -> PageView(flow, fly, motion, humo)
                Screen.SkillSplash -> SkillSplash(flow, humo)
                Screen.Score -> ScoreView(flow, humo)
                Screen.End -> EndView(flow, humo)
            }
        }
    }
}

private data class BtnSpec(val label: String, val kind: Kind, val enabled: Boolean = true, val weight: Float = 1f, val onClick: () -> Unit)
private enum class Kind { Dark, Gray, Outline, Teal, Yellow, Green }

/** Reads a prompt aloud with the phone's own voice (Android TextToSpeech), made on the first tap and released on exit. */
@Composable
private fun rememberReader(): (String) -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val engine = remember { arrayOfNulls<android.speech.tts.TextToSpeech>(1) }
    val ready = remember { booleanArrayOf(false) }
    val waiting = remember { arrayOfNulls<String>(1) }
    DisposableEffect(Unit) { onDispose { engine[0]?.shutdown(); engine[0] = null } }
    return { text ->
        val tts = engine[0]
        if (tts == null) {
            waiting[0] = text
            engine[0] = android.speech.tts.TextToSpeech(context.applicationContext) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    engine[0]?.language = java.util.Locale.US
                    ready[0] = true
                    waiting[0]?.let { engine[0]?.speak(it, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "prompt") }
                }
            }
        } else if (ready[0]) tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "prompt")
    }
}

/** One page: the prompt and its board at the top, the tools top right, Humo and the buttons in one row at the foot, what Humo says above it. */
@Composable
private fun PageView(flow: LessonFlow, fly: Boolean, motion: Boolean, humo: HumoState) {
    val page = flow.page ?: return
    val haptics = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val read = rememberReader()

    // Check: on a tile board the machines fly to where the numbers sent them first, and only then is the answer graded
    // (Brilliant's own tick or cross); in the skill check the same flight comes before the next item.
    val deck = remember(flow.screen) { FlightDeck() }
    val scope = rememberCoroutineScope()
    fun check() {
        scope.launch {
            flow.flying = true
            try { deck.fly() } finally { flow.flying = false }
            val ok = flow.check()
            if (ok) { LessonSounds.play(Cue.Correct); haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
            else { LessonSounds.play(Cue.Incorrect); haptics.performHapticFeedback(HapticFeedbackType.Reject) }
        }
    }

    val review = flow.screen is Screen.Review
    // The state shows first; Humo's line follows: 0.5 s after a right answer, 0.65 s after a wrong one (Brilliant's frame study).
    LaunchedEffect(flow.verdict, flow.phase) {
        if (flow.inSkill || flow.verdict == null || flow.bubble != null) return@LaunchedEffect
        delay(if (flow.phase == Phase.Correct) 500 else 650)
        flow.showVerdict()
    }
    val buttons: List<BtnSpec> = when {
        flow.inSkill && flow.answered -> listOf(
            BtnSpec(if ((flow.screen as Screen.Skill).j == flow.skill.size - 1) "Finish" else "Continue", Kind.Dark) { LessonSounds.play(Cue.Tick); flow.nextSkill() },
        )
        flow.inSkill -> listOf(
            BtnSpec("I don't know", Kind.Outline, enabled = !flow.flying, weight = 0.55f) { LessonSounds.play(Cue.Tick); flow.dontKnow() },
            BtnSpec("Check", Kind.Dark, enabled = flow.canCheck) { check() },
        )
        page.kind == PageKind.Explainer || page.kind == PageKind.Recap -> listOf(BtnSpec("Continue", Kind.Dark) { LessonSounds.play(Cue.Tick); flow.next() })
        flow.phase == Phase.Correct -> listOf(
            BtnSpec("Why?", Kind.Gray, weight = 0.42f) { LessonSounds.play(Cue.Tick); flow.chat.openWith(HumoChatState.Request.Why) },
            BtnSpec("Continue", Kind.Green) { LessonSounds.play(Cue.Tick); flow.next() },
        )
        flow.phase == Phase.Wrong -> listOf(
            BtnSpec("Get help", Kind.Gray, weight = 0.55f) { LessonSounds.play(Cue.Tick); flow.chat.openWith(HumoChatState.Request.WhyWrong) },
            BtnSpec("Try again", if (review) Kind.Teal else Kind.Yellow) { LessonSounds.play(Cue.Tick); flow.tryAgain() },
        )
        else -> listOf(BtnSpec("Check", Kind.Dark, enabled = flow.canCheck) { check() })
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val boardW = (maxWidth - 32.dp).coerceAtMost(420.dp)
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(horizontal = 16.dp).padding(top = 64.dp, bottom = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Prompt(if (fly) Words.machine(page.prompt) else page.prompt, bold = page.skill, modifier = Modifier.width(boardW))
                    Spacer(Modifier.height(24.dp))
                    when (page.kind) {
                        PageKind.Drag -> DragBoard(page, flow, fly, motion, boardW)
                        PageKind.Explainer -> FigureBoard(page, fly, boardW)
                        PageKind.Translate, PageKind.Recap -> WorkspaceBoard(page, flow, fly, motion, boardW, deck)
                    }
                    if (page.graded && !page.skill) StartOver(flow, Modifier.width(boardW).alpha(if (page.kind == PageKind.Translate && flow.answered) 0f else 1f))
                }
            }
            FootRow(humo, buttons, motion, key = "${flow.screen}-${flow.phase}-${flow.answered}", onAsk = { flow.chat.open = true }, skill = flow.inSkill, verdict = flow.verdict.takeIf { flow.inSkill }, verdictOk = flow.phase.let { if (it == Phase.Idle) null else it == Phase.Correct })
        }
        // The tools (play.html `.tools`): the "+ 15" chip after a first right answer, report a problem, read aloud.
        Row(Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GainChip(flow.gain)
            IconButton("Report a problem", { reportPage(context, flow) }, tint = Chrome.Icon) { c ->
                withTransform({ scale(size.width / 24f, size.height / 24f, Offset.Zero) }) { drawPath(FlagPath, c) }
            }
            val voice = LocalHumoVoice.current
            if (voice != null) IconButton(if (voice.on) "Humo's voice: on" else "Humo's voice: off", { voice.toggle() }, tint = if (voice.on) Chrome.Icon else Chrome.Icon.copy(alpha = 0.55f)) { c ->
                val w = size.width; val h = size.height
                drawPath(Path().apply { moveTo(w * 0.17f, h * 0.38f); lineTo(w * 0.33f, h * 0.38f); lineTo(w * 0.54f, h * 0.2f); lineTo(w * 0.54f, h * 0.8f); lineTo(w * 0.33f, h * 0.62f); lineTo(w * 0.17f, h * 0.62f); close() }, c)
                if (voice.on) {
                    drawArc(c, -45f, 90f, false, topLeft = Offset(w * 0.46f, h * 0.3f), size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.4f), style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(c, -45f, 90f, false, topLeft = Offset(w * 0.46f, h * 0.17f), size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.66f), style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
                } else drawLine(c, Offset(w * 0.12f, h * 0.12f), Offset(w * 0.88f, h * 0.88f), 2.dp.toPx(), StrokeCap.Round)
            }
            IconButton("Read aloud", { read((if (fly) Words.machine(page.prompt) else page.prompt).replace("*", "")) }, tint = Chrome.Icon) { c ->
                val h = size.height
                drawPath(Path().apply { moveTo(size.width * 0.12f, h * 0.38f); lineTo(size.width * 0.3f, h * 0.38f); lineTo(size.width * 0.52f, h * 0.18f); lineTo(size.width * 0.52f, h * 0.82f); lineTo(size.width * 0.3f, h * 0.62f); lineTo(size.width * 0.12f, h * 0.62f); close() }, c)
                drawArc(c, -50f, 100f, false, topLeft = Offset(size.width * 0.42f, h * 0.3f), size = androidx.compose.ui.geometry.Size(size.width * 0.3f, h * 0.4f), style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
                drawArc(c, -50f, 100f, false, topLeft = Offset(size.width * 0.42f, h * 0.16f), size = androidx.compose.ui.geometry.Size(size.width * 0.5f, h * 0.68f), style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        // What Humo says: it floats over the board's foot, just above the buttons (play.html `.bubble`, left 12, bottom 74).
        Bubble((if (flow.phase == Phase.Idle) (flow.bubble ?: flow.coach) else flow.bubble).takeUnless { flow.chat.open || flow.inSkill }, flow.phase, flow.inSkill, Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 74.dp))
        HumoChat(flow, page, fly, read, onTryAgain = { LessonSounds.play(Cue.Tick); flow.tryAgain() }, onContinue = { if (flow.phase == Phase.Correct) flow.next() }, Modifier.align(Alignment.BottomCenter).padding(8.dp))
    }
}

/** Report a problem: a local log line per tap (lesson, page, time), and Humo says thanks in the bubble. */
private fun reportPage(context: android.content.Context, flow: LessonFlow) {
    val prefs = context.getSharedPreferences("humo-flags", android.content.Context.MODE_PRIVATE)
    val line = "${flow.lesson.code}|${flow.screen}|${System.currentTimeMillis()}"
    prefs.edit().putStringSet("flags", (prefs.getStringSet("flags", emptySet()).orEmpty() + line)).apply()
    flow.say("Thanks. We will look at this page.")
}

/** The foot: Humo (58 dp) and the buttons in one row; each new set of buttons rises 6 dp and fades in over 200 ms. */
@Composable
private fun FootRow(humo: HumoState, buttons: List<BtnSpec>, motion: Boolean, key: String, onAsk: (() -> Unit)?, skill: Boolean, verdict: String?, verdictOk: Boolean?) {
    Row(Modifier.fillMaxWidth().padding(start = if (skill) 14.dp else 4.dp, end = 10.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // The skill check has no Humo (play.html body.skillmode .buddy): the verdict stands in the corner instead.
        if (!skill) {
            Humo(humo, Modifier.size(58.dp))
            if (onAsk != null) AskButton(onAsk)
        } else if (verdict != null) SkillVerdict(verdict, verdictOk)
        ButtonsRow(buttons, motion, key, Modifier.weight(1f))
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
        style = TextStyle(fontFamily = LessonType.Figtree, fontSize = if (bold) 18.sp else 16.sp, lineHeight = if (bold) 28.8.sp else 25.6.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = Chrome.Ink),
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
            Text("Start over", fontFamily = LessonType.Figtree, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = c)
        }
    }
}

/** What Humo says (play.html `.bubble`): praise on a green bubble, a nudge on a yellow one, a plain remark on a pale one. */
@Composable
private fun Bubble(text: String?, phase: Phase, inSkill: Boolean, modifier: Modifier) {
    var last by remember { mutableStateOf("") }
    if (text != null) last = text
    val face = when { inSkill || phase == Phase.Idle -> Chrome.SayBubble; phase == Phase.Correct -> Chrome.GoodBubble; else -> Chrome.BadBubble }
    val edge = if (face == Chrome.SayBubble) Chrome.SayEdge else face
    AnimatedVisibility(text != null, modifier, enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 4 }, exit = fadeOut(tween(120))) {
        Text(
            last, Modifier.widthIn(max = 250.dp).clip(RoundedCornerShape(12.dp)).background(face).border(1.dp, edge, RoundedCornerShape(12.dp)).padding(horizontal = 17.dp, vertical = 12.dp),
            fontFamily = LessonType.Figtree, fontWeight = if (face == Chrome.SayBubble) FontWeight.Normal else FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.75.sp, color = Chrome.Ink,
            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/** "+ 15 star": the sparks a first right answer earns, at the card's top right until the page changes (play.html `.gain`). */
@Composable
private fun GainChip(shown: Boolean) {
    AnimatedVisibility(shown, enter = fadeIn(tween(160)) + scaleIn(tween(300, easing = EaseOutStrong), initialScale = 0.4f), exit = fadeOut(tween(120))) {
        Row(Modifier.height(30.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFDCF6E4)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("+ ${L11.SPARKS_PER_PAGE}", fontFamily = LessonType.Figtree, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF19A14A))
            Spacer(Modifier.width(4.dp))
            Spark(Color(0xFF19A14A), 14.dp)
        }
    }
}

/** The buttons: one primary, or a grey pill beside it; each new set rises 6 dp and fades in over 200 ms. */
@Composable
private fun ButtonsRow(buttons: List<BtnSpec>, motion: Boolean, key: String, modifier: Modifier = Modifier) {
    val rise = with(LocalDensity.current) { 6.dp.roundToPx() }
    AnimatedContent(
        targetState = key to buttons,
        contentKey = { it.first },
        modifier = modifier,
        transitionSpec = { (fadeIn(tween(if (motion) 200 else 0)) + slideInVertically(tween(if (motion) 200 else 0)) { rise }).togetherWith(fadeOut(tween(0))) },
        label = "buttons",
    ) { (_, list) ->
        Row(Modifier.fillMaxWidth().height(51.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            list.forEach { b -> LButton(b.label, b.kind, b.enabled, Modifier.weight(b.weight), b.onClick) }
        }
    }
}

/** The pill (play.html `#main`): 48 dp, a 3 dp darker edge under it; disabled = flat light grey with a lighter weight. */
@Composable
private fun LButton(label: String, kind: Kind, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val (face, edge, ink) = when {
        !enabled -> Triple(Chrome.Off, Chrome.Off, Chrome.OffText)
        kind == Kind.Dark -> Triple(Chrome.Dark, Chrome.DarkEdge, Color.White)
        kind == Kind.Teal -> Triple(Chrome.Teal, Chrome.TealEdge, Color.White)
        kind == Kind.Yellow -> Triple(Color(0xFFF7C51E), Color(0xFFC99A00), Color(0xFF1D1D1D))
        kind == Kind.Green -> Triple(Color(0xFF2BC556), Color(0xFF1F9E45), Color.White)
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
            Text(label, fontFamily = LessonType.Figtree, fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Normal, fontSize = 17.sp, color = ink, maxLines = 1)
        }
    }
}

/**
 * A bare screen (play.html `.center`): Humo beside the blue check medal, a big title, a line, stacked 290 dp pills. [big]
 * replaces the medal pair (the score's number sits under it).
 */
@Composable
private fun CenterScreen(humo: HumoState, big: (@Composable () -> Unit)?, title: String, line: String?, buttons: List<BtnSpec>, foot: BtnSpec? = null) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(width = 150.dp, height = 128.dp)) {
                Medal(Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 6.dp))
                Humo(humo, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-10).dp).size(104.dp))
            }
            if (big != null) { Spacer(Modifier.height(18.dp)); big() }
            Spacer(Modifier.height(14.dp))
            Text(title, fontFamily = LessonType.Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp, color = Chrome.Ink, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            if (line != null) {
                Spacer(Modifier.height(8.dp))
                Text(line, fontFamily = LessonType.Figtree, fontSize = 16.sp, color = Chrome.Muted, textAlign = TextAlign.Center)
            }
            if (buttons.isNotEmpty()) Spacer(Modifier.height(34.dp))
            buttons.forEachIndexed { i, b ->
                if (i > 0) Spacer(Modifier.height(14.dp))
                LButton(b.label, b.kind, b.enabled, Modifier.widthIn(max = 290.dp).fillMaxWidth()) { LessonSounds.play(Cue.Tick); b.onClick() }
            }
        }
        if (foot != null) Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) { LButton(foot.label, foot.kind, true, Modifier.fillMaxWidth()) { LessonSounds.play(Cue.Tick); foot.onClick() } }
    }
}

/** The skill check's corner verdict (play.html `.verdict`): our own drawn icon, then "Correct!" / "Not quite." in 800 19 sp. */
@Composable
private fun SkillVerdict(text: String, ok: Boolean?) {
    Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (ok != null) Canvas(Modifier.size(30.dp)) {
            val k = size.width / 34f
            if (ok) {
                drawPath(Path().apply { moveTo(5f * k, 30f * k); lineTo(12f * k, 11f * k); lineTo(24f * k, 23f * k); close() }, Color(0xFFF7C51E))
                drawPath(Path().apply { moveTo(5f * k, 30f * k); lineTo(12f * k, 11f * k); lineTo(16f * k, 15f * k); lineTo(10f * k, 30f * k); close() }, Color(0xFFE8A500))
                drawCircle(Color(0xFF3FCE69), 2.4f * k, Offset(24f * k, 7f * k)); drawCircle(Color(0xFF6170EC), 2f * k, Offset(30f * k, 15f * k))
                for (l in listOf(listOf(19f, 4f, 20f, 8f), listOf(27f, 22f, 31f, 23f), listOf(22f, 12f, 27f, 7f))) drawLine(Color(0xFFFF7A59), Offset(l[0] * k, l[1] * k), Offset(l[2] * k, l[3] * k), 2.2f * k, StrokeCap.Round)
            } else {
                drawCircle(Color(0xFFFBE8A4), 13f * k, center)
                drawLine(Color(0xFFC99A00), Offset(10f * k, 10f * k), Offset(20f * k, 20f * k), 3f * k, StrokeCap.Round)
                drawLine(Color(0xFFC99A00), Offset(20f * k, 10f * k), Offset(10f * k, 20f * k), 3f * k, StrokeCap.Round)
            }
        }
        Text(text, fontFamily = LessonType.Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Chrome.Ink)
    }
}

/** The skill check's blue medal with a white tick (play.html `.medal`), drawn, not anyone's art. */
@Composable
private fun Medal(modifier: Modifier) {
    Canvas(modifier.size(64.dp)) {
        drawCircle(Brush.radialGradient(0f to Color(0xFF8D9BFF), 0.7f to Chrome.Medal, center = Offset(size.width * 0.35f, size.height * 0.3f), radius = size.minDimension * 0.9f), size.minDimension / 2f)
        drawCircle(Color(0x47FFFFFF), size.minDimension / 2f - 2.dp.toPx(), style = Stroke(4.dp.toPx()))
        drawPath(Path().apply { moveTo(size.width * 0.3f, size.height * 0.52f); lineTo(size.width * 0.44f, size.height * 0.66f); lineTo(size.width * 0.71f, size.height * 0.36f) }, Color.White, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun SkillSplash(flow: LessonFlow, humo: HumoState) {
    LaunchedEffect(Unit) { LessonSounds.play(Cue.Start) }
    CenterScreen(humo, null, "Skill check", "Let's see if it all clicked.", emptyList(), foot = BtnSpec("Continue", Kind.Dark) { flow.startSkill() })
}

@Composable
private fun ScoreView(flow: LessonFlow, humo: HumoState) {
    val n = flow.skillRight
    val total = flow.skill.size
    LaunchedEffect(Unit) { LessonSounds.play(Cue.Start) }
    CenterScreen(
        humo,
        big = {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$n out of $total" }) {
                Text("$n", fontFamily = LessonType.Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 84.sp, lineHeight = 84.sp, color = Chrome.Ink)
                Text("/$total", fontFamily = LessonType.Figtree, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Chrome.StarOff, modifier = Modifier.offset(y = 6.dp))
            }
        },
        title = L11.scoreTitle(n, total),
        line = null,
        buttons = if (n == total) listOf(BtnSpec("Continue", Kind.Dark) { flow.finish() })
        else listOf(BtnSpec("Review with Humo", Kind.Dark) { flow.startReview() }, BtnSpec("Skip review", Kind.Outline) { flow.skipReview() }),
    )
}

@Composable
private fun EndView(flow: LessonFlow, humo: HumoState) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) { LessonSounds.play(Cue.Complete); haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
    CenterScreen(humo, null, "Lesson complete", "${flow.sparks} sparks earned.", listOf(BtnSpec("Restart lesson", Kind.Dark) { flow.restart() }))
}
