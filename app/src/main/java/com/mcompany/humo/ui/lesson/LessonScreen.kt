package com.mcompany.humo.ui.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mcompany.humo.ui.path.rememberSystemMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.mcompany.humo.ui.glass.AuroraBackground

/**
 * The lesson chrome's colours: ours (Humo mint, our yellow), with Duolingo's mobile patterns copied for the layout and the
 * spacing only (Boss, 2026-10-31).
 */
internal object Chrome {
    val Ink = Color(0xFF3C3C3C)
    val Line = Color(0xFFE5E5E5)
    val Track = Color(0xFFE5E5E5)
    val Mint = Color(0xFF3DC98C)
    val MintEdge = Color(0xFF23A06C)
    val MintInk = Color(0xFF1C7E69)
    val Yellow = Color(0xFFF7C51E)
    val YellowEdge = Color(0xFFC99A00)
    val YellowInk = Color(0xFF7A5A00)
    val PanelGood = Color(0xFFD9F5E6)
    val PanelBad = Color(0xFFFFF0B8)
    val OffFace = Color(0xFFE5E5E5)
    val OffText = Color(0xFFAFAFAF)
    val Muted = Color(0xFF6B7079)
    val Close = Color(0xFF9A9A9A)
    val Medal = Color(0xFF6170EC)
    val Scrim = Color(0x66000000)
}

enum class LessonTheme { Plain, Fly }

internal enum class Kind { Primary, Secondary, Warn }

/**
 * A Brilliant lesson (L1.1 "Translating Points", L1.2 "Negative Translations"), rebuilt natively in Boss's picked mobile design:
 * Humo at the top saying the task in his bubble, a large graph, "translate by ( , )" with a big keypad low for the thumb, liquid
 * glass on the chrome (bottom bar, mint and amber panels, Humo's sheet, the quit sheet), the progress track with the jet on its
 * tip. The skill check is a mastery gate: a clean 3 of 3 earns the Mach 3 pass, anything else goes to Humo's fix-it round.
 * On [LessonTheme.Fly] every point is Boss's SR-71. [onFinish] gets the sparks earned; [onExit] leaves without finishing.
 * [humoAlive] runs Humo's springs, blinks and the word-by-word bubble (off in the screenshot tests, which want a still pose).
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
    val atBye = flow.screen is Screen.Bye
    val close: () -> Unit = { if (atEnd) onFinish(flow.sparks) else if (atBye) onExit() else { leaving = true } }
    BackHandler(enabled = !leaving) { close() }
    BackHandler(enabled = leaving) { leaving = false }
    BackHandler(enabled = flow.chat.open && !leaving) { flow.chat.open = false }
    val humo = rememberHumo(humoAlive)
    flow.machineName = if (theme == LessonTheme.Fly) Words.MACHINE else "point"
    val voice = rememberHumoVoice()
    LaunchedEffect(flow.screen) { voice.stop() }
    HumoMoods(flow, humo, voice)
    val bare = flow.screen.let { it is Screen.SkillSplash || it is Screen.Score || it is Screen.End || it is Screen.FixIntro || it is Screen.Bye }
    val bottomInset = padding.calculateBottomPadding()
    // A tap anywhere asks the Mach 3 pass for another run; the root only counts it (the Initial pass), the buttons still get it.
    val taps = remember { mutableIntStateOf(0) }
    AuroraBackground(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) { if (awaitPointerEvent(PointerEventPass.Initial).changes.any { it.changedToDown() }) taps.intValue++ }
                }
            },
        ) {
            Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding(), bottom = if (bare) bottomInset else 0.dp)) {
                if (!atBye) Box {
                    TopBar(flow, theme == LessonTheme.Fly, onClose = close)
                    // the tutor dims the page, but never while Humo draws on the board: then the board must be seen as it is
                    if (flow.chat.open && flow.marks.isEmpty() && !bare) Box(Modifier.matchParentSize().background(Chrome.Scrim))
                }
                LessonBody(flow, theme == LessonTheme.Fly, motion, humo, voice, bottomInset, onExit)
            }
            if (theme == LessonTheme.Fly && motion && atEnd && !flow.small) MachPass(taps, flow.skillRight == flow.skill.size)
            if (leaving) QuitSheet(humo, voice, bottomInset, onKeep = { leaving = false }, onEnd = { leaving = false; flow.bye() })
        }
    }
}

/**
 * The close cross asks first (Duolingo's quit sheet): the page dims, Humo looks up sadly (never crying) and says one gentle line in his
 * voice on a glass sheet, KEEP LEARNING is the big button and END LESSON a quiet text. END LESSON ends on a calm "See you soon".
 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.QuitSheet(humo: HumoState, voice: HumoVoice, bottomInset: Dp, onKeep: () -> Unit, onEnd: () -> Unit) {
    DisposableEffect(Unit) {
        humo.face = HumoFace.Sad
        onDispose { humo.face = HumoFace.Rest; humo.talking = false; voice.stop() }
    }
    LaunchedEffect(Unit) {
        if (!humo.alive || !voice.on) return@LaunchedEffect
        humo.talking = true
        voice.say(LessonLines.QUIT)
        humo.talking = false
        humo.face = HumoFace.Sad
    }
    Box(Modifier.matchParentSize().background(Chrome.Scrim).pointerInput(Unit) { detectTapGestures { } })
    Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp + bottomInset).testTag("quit-sheet")
            .chromeGlass(28.dp).padding(top = 18.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Humo(humo, Modifier.size(104.dp))
            Spacer(Modifier.width(12.dp))
            Speech(LessonLines.QUIT, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Chunky("Keep learning", Kind.Primary, true, Modifier.fillMaxWidth().padding(horizontal = 18.dp), onClick = onKeep)
        Text(
            "END LESSON", Modifier.clickable(role = Role.Button, onClick = onEnd).padding(horizontal = 24.dp, vertical = 14.dp),
            fontFamily = LessonType.Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 0.8.sp, color = Chrome.Muted,
        )
    }
}

/** What Humo feels on each screen (player.js `react`): glad and a hop on right, thinking on wrong, cheering on the bare screens. */
@Composable
private fun HumoMoods(flow: LessonFlow, humo: HumoState, voice: HumoVoice) {
    val screen = flow.screen
    val perfect = flow.skillRight == flow.skill.size
    LaunchedEffect(flow.phase, screen, flow.inSkill) {
        humo.pose = when {
            screen is Screen.Bye -> HumoPose.Wave
            screen is Screen.Demo || flow.showHow -> HumoPose.Point
            else -> HumoPose.None
        }
        when {
            screen is Screen.End && flow.small -> { humo.face = HumoFace.Happy; humo.play(HumoMove.Jump) }
            screen is Screen.End -> { humo.face = HumoFace.Happy; humo.play(HumoMove.Celebrate) }
            screen is Screen.Bye -> { humo.face = HumoFace.Happy; humo.play(HumoMove.Nod) }
            screen is Screen.FixIntro -> { humo.face = HumoFace.Happy; humo.play(HumoMove.Nod) }
            screen is Screen.SkillSplash -> { humo.face = HumoFace.Cheer; humo.play(HumoMove.Jump); if (humo.alive) { delay(2200); humo.face = HumoFace.Rest } }
            screen is Screen.Score && perfect -> { humo.face = HumoFace.Laugh; humo.play(HumoMove.Celebrate); if (humo.alive) { delay(2600); humo.face = HumoFace.Rest } }
            screen is Screen.Score -> { humo.face = HumoFace.Proud; humo.play(HumoMove.Nod); if (humo.alive) { delay(2200); humo.face = HumoFace.Rest } }
            flow.phase == Phase.Correct && !flow.inSkill -> { humo.face = HumoFace.Happy; humo.play(HumoMove.Jump); if (humo.alive) { delay(1800); humo.face = HumoFace.Rest } }
            flow.phase == Phase.Wrong && !flow.inSkill -> { humo.face = HumoFace.Thinking; if (humo.alive) { delay(2600); humo.face = HumoFace.Rest } }
            else -> humo.face = HumoFace.Rest
        }
    }
    // Humo says every line he shows: his mouth moves while his clip plays; a line without a clip stays silent (never a
    // system voice), and the mouth then moves for about 45 ms a letter. The tutor sheet speaks its own lines.
    val last = flow.chat.lines.lastOrNull()
    LaunchedEffect(last) {
        if (last == null || last.mine || last.typing || !humo.alive || flow.chat.open) return@LaunchedEffect
        humo.talking = true
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

/** The top bar: a close cross and the glass progress track with the jet on its tip. Nothing else (Boss: no XP while learning). */
@Composable
private fun TopBar(flow: LessonFlow, fly: Boolean, onClose: () -> Unit) {
    val p by animateFloatAsState(flow.progress, tween(400, easing = BrilliantEase), label = "progress")
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Exit lesson", onClick = onClose).semantics { contentDescription = "Exit lesson" },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(22.dp)) {
                drawLine(Chrome.Close, Offset(0f, 0f), Offset(size.width, size.height), 3.dp.toPx(), StrokeCap.Round)
                drawLine(Chrome.Close, Offset(size.width, 0f), Offset(0f, size.height), 3.dp.toPx(), StrokeCap.Round)
            }
        }
        Spacer(Modifier.width(14.dp))
        ProgressTrack(p, fly, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
    }
}

/** The page below the top bar: a lesson page with its bottom bar, or a bare screen. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.LessonBody(
    flow: LessonFlow, fly: Boolean, motion: Boolean, humo: HumoState, voice: HumoVoice, bottomInset: Dp, onExit: () -> Unit,
) {
    val slide = with(LocalDensity.current) { 28.dp.roundToPx() }
    Box(Modifier.weight(1f).fillMaxWidth()) {
        AnimatedContent(
            targetState = flow.screen,
            transitionSpec = {
                (fadeIn(tween(if (motion) 320 else 0, easing = BrilliantEase)) + slideInHorizontally(tween(if (motion) 320 else 0, easing = BrilliantEase)) { slide })
                    .togetherWith(fadeOut(tween(0)))
            },
            label = "screen",
        ) { screen ->
            when (screen) {
                is Screen.Page, is Screen.Skill, is Screen.Demo, is Screen.Fix -> PageView(flow, fly, motion, humo, voice, bottomInset)
                Screen.SkillSplash -> SkillSplash(flow, humo)
                Screen.Score -> ScoreView(flow, humo)
                Screen.FixIntro -> FixIntroView(flow, humo)
                Screen.End -> EndView(flow, humo)
                Screen.Bye -> ByeView(flow, humo, onExit)
            }
        }
    }
}

private data class BtnSpec(val label: String, val kind: Kind, val enabled: Boolean = true, val onClick: () -> Unit)

/**
 * Duolingo's button: solid, rounded 16 dp, bold capitals with letter spacing, a 4 dp darker edge under it that the face
 * presses into while held. Disabled is flat light grey with a lighter label. [caps] false is for the tutor's chips.
 */
@Composable
internal fun Chunky(
    label: String, kind: Kind, enabled: Boolean, modifier: Modifier = Modifier, caps: Boolean = true,
    leading: (@Composable () -> Unit)? = null, onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(16.dp)
    val (face, edge, ink) = when {
        !enabled -> Triple(Chrome.OffFace, Chrome.OffFace, Chrome.OffText)
        kind == Kind.Primary -> Triple(Chrome.Mint, Chrome.MintEdge, Color.White)
        kind == Kind.Warn -> Triple(Chrome.Yellow, Chrome.YellowEdge, Color(0xFF4A3600))
        else -> Triple(Color.White, Chrome.Line, Chrome.MintInk)
    }
    // propagateMinConstraints: a button given an exact width (a weight, fillMaxWidth) fills it; otherwise it wraps its label.
    Box(modifier.height(52.dp).clickable(interaction, null, enabled = enabled, role = Role.Button, onClick = onClick), propagateMinConstraints = true) {
        if (enabled) Box(Modifier.matchParentSize().padding(top = 4.dp).clip(shape).background(edge))
        Box(
            Modifier.height(48.dp).offset(y = if (!enabled || pressed) 4.dp else 0.dp).clip(shape).background(face)
                .then(if (kind == Kind.Secondary && enabled) Modifier.border(2.dp, edge, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            // The label shrinks until it fits its button (a narrow phone, a big font): never cut, never wrapped.
            var fit by remember(label) { mutableFloatStateOf(1f) }
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                leading?.invoke()
                Text(
                    if (caps) label.uppercase() else label, fontFamily = LessonType.Figtree, fontWeight = if (caps) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = (if (caps) 15.sp else 17.sp) * fit, letterSpacing = if (caps) 0.8.sp else 0.sp, color = ink, maxLines = 1, softWrap = false,
                    onTextLayout = { if (it.didOverflowWidth && fit > 0.45f) fit *= 0.92f },
                )
            }
        }
    }
}

/** Lays its content out at natural height, centred in [avail]; while the tutor is up ([fit]) it shrinks to fit above the sheet. */
@Composable
private fun FitColumn(fit: Boolean, avail: Int, content: @Composable () -> Unit) {
    Layout(content, Modifier.fillMaxWidth()) { measurables, c ->
        val p = measurables.first().measure(Constraints(maxWidth = c.maxWidth, maxHeight = Constraints.Infinity))
        val k = if (fit && p.height > avail && p.height > 0) avail / p.height.toFloat() else 1f
        val h = if (fit) avail else maxOf(p.height, avail)
        layout(c.maxWidth, h) {
            // a little above the middle: the title and the board read as one block, the gap goes under the board
            p.placeWithLayer((c.maxWidth - p.width) / 2, ((h - p.height * k) * 0.42f).toInt()) {
                scaleX = k; scaleY = k; transformOrigin = TransformOrigin(0.5f, 0f)
            }
        }
    }
}

/** Humo's replay of a missed problem (player.js `demo`): he rings the start, counts right then up with short labels on the graph, shows what the learner did, then flies the right move. */
private suspend fun runDemo(flow: LessonFlow, motion: Boolean) {
    val p = flow.page ?: return
    flow.demoBusy = true
    suspend fun nap(ms: Long) { if (motion) delay(ms) }
    suspend fun say(t: String) { flow.say(t); nap((t.split(' ').size * 380L + 500L).coerceAtLeast(1300L)) }
    val s0 = p.points[0]
    val m = p.move
    flow.marks = listOf(s0)
    say(flow.fresh(L11.fx("fixLook2")))
    flow.braces = listOf(Brace(s0, Node(s0.x + m.x, s0.y), LessonLines.steps(m.x, "right").substringBeforeLast(' ')))
    nap(700)
    if (m.y != 0) flow.braces = flow.braces + Brace(Node(s0.x + m.x, s0.y), Node(s0.x + m.x, s0.y + m.y), if (m.y > 0) "up" else "down")
    say(LessonLines.demoRight(m))
    val mine = flow.demoMine
    if (mine != null) {
        flow.demoSet(mine); nap(1500)
        flow.marks = listOf(s0, s0 + mine)
        say(LessonLines.youMoved(mine))
        flow.demoSet(m)
    } else {
        say(flow.fresh(L11.fx("fixNone")))
        flow.demoSet(m)
    }
    nap(1500)
    flow.marks = emptyList()
    say(flow.fresh(L11.fx("fixThen")))
    flow.demoBusy = false
}

/**
 * One page, top to bottom: Humo with the task in his bubble, the board (large) with "translate by ( , )", the big keypad low
 * for the thumb, and over the bottom of the page the glass layer, which is one of: the Check bar, the mint or amber verdict
 * panel, the worked explanation's CONTINUE LESSON, or Humo's sheet (which takes the place of the keypad and the bar). The
 * skill check has the task bubble but no tutor; Humo's fix-it replay (Demo) is his own and the board is not touchable.
 */
@Composable
private fun PageView(flow: LessonFlow, fly: Boolean, motion: Boolean, humo: HumoState, voice: HumoVoice, bottomInset: Dp) {
    val page = flow.page ?: return
    val haptics = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val machine = if (fly) Words.MACHINE else "point"
    var reporting by remember { mutableStateOf(false) }
    val demo = flow.screen is Screen.Demo
    val fix = flow.screen is Screen.Fix

    // Check: on a keypad page the machines fly to where the numbers sent them first, and only then is the answer graded
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
    // Humo reads the task itself when a page opens (spoken text = written text); the teacher line waits in the tutor sheet.
    LaunchedEffect(flow.screen) {
        flow.chat.open = false
        if (demo) runDemo(flow, motion) else if (!flow.inSkill) flow.chat.hear(LessonLines.plain(page.prompt, fly))
    }
    // The panel shows the verdict at once; Humo's voice follows: 0.5 s after a right answer, 0.65 s after a wrong one (Brilliant's frame study).
    LaunchedEffect(flow.verdict, flow.phase) {
        if (flow.inSkill || flow.verdict == null || flow.bubble != null) return@LaunchedEffect
        delay(if (flow.phase == Phase.Correct) 500 else 650)
        flow.showVerdict()
    }
    // Show me how: Humo reads the worked line while the card is open.
    LaunchedEffect(flow.showHow) {
        if (!flow.showHow || !humo.alive) return@LaunchedEffect
        humo.talking = true
        if (voice.on) voice.say(LessonLines.how(page, machine))
        humo.talking = false
    }
    val tutorOpen = flow.chat.open && !flow.inSkill && !demo
    val barKey = when {
        tutorOpen -> "tutor"
        flow.showHow -> "how"
        demo -> "demo-${flow.demoBusy}"
        flow.inSkill && flow.answered -> "skill-${flow.verdict}"
        flow.inSkill -> "skill"
        page.kind == PageKind.Explainer || page.kind == PageKind.Recap -> "go"
        flow.phase == Phase.Correct -> if (fix) "fixright" else "right"
        flow.phase == Phase.Wrong -> if (fix) "fixwrong" else "wrong"
        else -> "check-${flow.canCheck}-${flow.flying}"
    }
    val keypad = page.kind == PageKind.Translate && !tutorOpen
    val negatives = page.tiles.any { it < 0 }
    val promptText = if (fly) Words.machine(page.prompt) else page.prompt
    val headerText = if (demo) flow.bubble ?: LessonLines.plain(L11.fx("fixLook").first(), fly) else promptText
    val askHumo: (() -> Unit)? = if (flow.inSkill || demo) null else { { LessonSounds.play(Cue.Tick); flow.chat.openClean() } }
    // the room under the page for the glass bar, so the verdict panel (drawn over it) never moves the board
    val screenH = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val compact = screenH < 700
    val keyHeight = when { screenH < 640 -> 34.dp; screenH < 760 -> 40.dp; else -> 46.dp }
    // a drag page has no keypad under the board, so the verdict panel (taller than the bar) would sit on the graph: keep its room
    val panelRoom = if (page.kind == PageKind.Drag) 80.dp + 80.dp * (LocalDensity.current.fontScale - 1f).coerceAtLeast(0f) else 0.dp
    val dockRoom = (if (compact) 78.dp else 84.dp) + bottomInset + panelRoom

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Humo moves into his sheet while it is open: the board gets the room his header held
            if (!tutorOpen) HumoHeader(headerText, humo, askHumo, compact = compact)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val boardW = (maxWidth - 24.dp).coerceAtMost(460.dp)
                    val avail = with(LocalDensity.current) { maxHeight.roundToPx() }
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        FitColumn(true, avail) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                // room above the board for a jet that sits on the top row (the scroll box would clip it)
                                Spacer(Modifier.height(18.dp))
                                when (page.kind) {
                                    PageKind.Drag -> DragBoard(page, flow, fly, motion, boardW)
                                    PageKind.Explainer -> FigureBoard(page, fly, boardW)
                                    PageKind.Translate, PageKind.Recap -> WorkspaceBoard(page, flow, fly, motion, boardW, deck)
                                }
                                if (flow.showHow) {
                                    Spacer(Modifier.height(16.dp))
                                    HowCard(page, machine, Modifier.width(boardW))
                                }
                            }
                        }
                    }
                }
                if (tutorOpen && flow.marks.isEmpty()) Box(Modifier.matchParentSize().background(Chrome.Scrim).pointerInput(Unit) { detectTapGestures { } })
            }
            if (keypad) {
                Keypad(flow, negatives, keyHeight, Modifier.padding(bottom = if (compact) 4.dp else 8.dp)) { LessonSounds.play(Cue.Tick); check() }
            }
            if (tutorOpen) {
                TutorSheet(
                    flow, page, fly, humo, voice, bottomInset = bottomInset, modifier = Modifier,
                    onTryAgain = { LessonSounds.play(Cue.Tick); flow.tryAgain() }, onContinue = { if (flow.phase == Phase.Correct) flow.next() },
                )
            } else Spacer(Modifier.height(dockRoom))
        }
        if (!tutorOpen) Box(Modifier.align(Alignment.BottomCenter)) {
            AnimatedContent(
                targetState = barKey,
                transitionSpec = { (slideInVertically(tween(if (motion) 220 else 0, easing = BrilliantEase)) { it / 2 } + fadeIn(tween(if (motion) 160 else 0))).togetherWith(fadeOut(tween(0))) },
                label = "bottom",
            ) { key ->
                when {
                    key == "how" -> GlassDock(bottomInset) { Chunky("Continue lesson", Kind.Primary, true, Modifier.weight(1f)) { LessonSounds.play(Cue.Continue); flow.next() } }
                    key.startsWith("demo") -> GlassDock(bottomInset) { Chunky("Continue", Kind.Primary, !flow.demoBusy, Modifier.weight(1f)) { LessonSounds.play(Cue.Continue); flow.next() } }
                    key.startsWith("skill-") -> {
                        val j = (flow.screen as Screen.Skill).j
                        val ok = flow.skillResults[j] == true
                        val last = j == flow.skill.size - 1
                        GlassPanel(ok, flow.verdict.orEmpty(), bottomInset, onFlag = { reporting = true }) {
                            Chunky(if (last) "Finish" else "Continue", if (ok) Kind.Primary else Kind.Warn, true, Modifier.weight(1f)) { LessonSounds.play(Cue.Continue); flow.nextSkill() }
                        }
                    }
                    key == "skill" -> GlassDock(bottomInset) {
                        Chunky("Show me", Kind.Secondary, !flow.flying, Modifier.weight(1f)) { LessonSounds.play(Cue.Skip); flow.dontKnow() }
                        Chunky("Check", Kind.Primary, flow.canCheck, Modifier.weight(1f)) { check() }
                    }
                    key == "go" -> GlassDock(bottomInset) { Chunky("Continue", Kind.Primary, true, Modifier.weight(1f)) { LessonSounds.play(Cue.Continue); flow.next() } }
                    key == "right" -> GlassPanel(true, flow.verdict.orEmpty(), bottomInset, onFlag = { reporting = true }) {
                        Chunky("Show me how", Kind.Secondary, true, Modifier.weight(1f)) { LessonSounds.play(Cue.Tick); flow.showHow = true }
                        Chunky("Continue", Kind.Primary, true, Modifier.weight(1f)) { LessonSounds.play(Cue.Continue); flow.next() }
                    }
                    key == "fixright" -> GlassPanel(true, flow.verdict.orEmpty(), bottomInset, onFlag = null) {
                        Chunky("Continue", Kind.Primary, true, Modifier.weight(1f)) { LessonSounds.play(Cue.Continue); flow.next() }
                    }
                    key == "fixwrong" -> GlassPanel(false, flow.verdict.orEmpty(), bottomInset, onFlag = null) {
                        Chunky("Continue", Kind.Warn, true, Modifier.weight(1f)) { LessonSounds.play(Cue.Continue); flow.next() }
                    }
                    key == "wrong" -> GlassPanel(false, flow.verdict ?: LessonLines.REVIEW, bottomInset, onFlag = { reporting = true }) {
                        Chunky("Get help", Kind.Secondary, true, Modifier.weight(1f)) { LessonSounds.play(Cue.Tick); flow.chat.openWith(HumoChatState.Request.WhyWrong) }
                        Chunky("Try again", Kind.Warn, true, Modifier.weight(1f)) { LessonSounds.play(Cue.Tick); flow.tryAgain() }
                    }
                    else -> GlassDock(bottomInset) { Chunky("Check", Kind.Primary, flow.canCheck, Modifier.weight(1f)) { check() } }
                }
            }
        }
    }
    if (reporting) ReportDialog(onPick = { reason -> reportPage(context, flow, reason); reporting = false }, onDismiss = { reporting = false })
}

@Composable
private fun ReportDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report this page") },
        text = {
            Column {
                for (r in listOf("The answer is wrong", "It is confusing", "It looks broken")) TextButton(onClick = { onPick(r) }) { Text(r) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Report a page: a local log line per tap (lesson, page, reason, time). */
private fun reportPage(context: android.content.Context, flow: LessonFlow, reason: String) {
    val prefs = context.getSharedPreferences("humo-flags", android.content.Context.MODE_PRIVATE)
    val line = "${flow.lesson.code}|${flow.screen}|$reason|${System.currentTimeMillis()}"
    prefs.edit().putStringSet("flags", (prefs.getStringSet("flags", emptySet()).orEmpty() + line)).apply()
}

/** Show me how: the move written out with this page's numbers, under the board (Duolingo's explanation card). */
@Composable
private fun HowCard(page: LessonPage, machine: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).border(2.dp, Chrome.Line, RoundedCornerShape(16.dp)).padding(16.dp).testTag("how-card")) {
        Text(LessonLines.howTitle(page), fontFamily = LessonType.Figtree, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Chrome.MintInk)
        Spacer(Modifier.height(8.dp))
        Text(LessonLines.how(page, machine), fontFamily = LessonType.Figtree, fontSize = 16.sp, lineHeight = 24.sp, color = Chrome.Ink)
    }
}

/**
 * A bare screen: Humo (big, alone) with an optional medal, a big title, a line, stacked pills; [big] sits under Humo (the score's
 * number, the sparks earned).
 */
@Composable
private fun CenterScreen(humo: HumoState, big: (@Composable () -> Unit)?, title: String, line: String?, buttons: List<BtnSpec>, foot: BtnSpec? = null, medal: Boolean = true) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (medal) Box(Modifier.size(width = 150.dp, height = 128.dp)) {
                Medal(Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 6.dp))
                Humo(humo, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-10).dp).size(104.dp))
            } else Humo(humo, Modifier.size(132.dp))
            if (big != null) { Spacer(Modifier.height(18.dp)); big() }
            Spacer(Modifier.height(14.dp))
            Text(title, fontFamily = LessonType.Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp, color = Chrome.Ink, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            if (line != null) {
                Spacer(Modifier.height(8.dp))
                Text(line, fontFamily = LessonType.Figtree, fontSize = 17.sp, color = Chrome.Muted, textAlign = TextAlign.Center)
            }
            if (buttons.isNotEmpty()) Spacer(Modifier.height(34.dp))
            buttons.forEachIndexed { i, b ->
                if (i > 0) Spacer(Modifier.height(14.dp))
                Chunky(b.label, b.kind, b.enabled, Modifier.widthIn(max = 320.dp).fillMaxWidth()) { LessonSounds.play(Cue.Tick); b.onClick() }
            }
        }
        if (foot != null) Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
            GlassDock(0.dp) { Chunky(foot.label, foot.kind, true, Modifier.weight(1f)) { LessonSounds.play(Cue.Tick); foot.onClick() } }
        }
    }
}

/** The skill check's blue medal with a white tick, drawn, not anyone's art. */
@Composable
private fun Medal(modifier: Modifier) {
    Canvas(modifier.size(64.dp)) {
        drawCircle(Brush.radialGradient(0f to Color(0xFF8D9BFF), 0.7f to Chrome.Medal, center = Offset(size.width * 0.35f, size.height * 0.3f), radius = size.minDimension * 0.9f), size.minDimension / 2f)
        drawCircle(Color(0x47FFFFFF), size.minDimension / 2f - 2.dp.toPx(), style = Stroke(4.dp.toPx()))
        drawPath(Path().apply { moveTo(size.width * 0.3f, size.height * 0.52f); lineTo(size.width * 0.44f, size.height * 0.66f); lineTo(size.width * 0.71f, size.height * 0.36f) }, Color.White, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** The gate (Boss: one clean line, no small "Three jets..." dots): "Show what you know". */
@Composable
private fun SkillSplash(flow: LessonFlow, humo: HumoState) {
    LaunchedEffect(Unit) { LessonSounds.play(Cue.Start) }
    CenterScreen(humo, null, L11.fx("gate").first(), null, emptyList(), foot = BtnSpec("Continue", Kind.Primary) { flow.startSkill() }, medal = false)
}

/** Almost! Let's fix one. A miss is never a fail screen: Humo starts the fix-it round. */
@Composable
private fun FixIntroView(flow: LessonFlow, humo: HumoState) {
    val voice = rememberHumoVoice()
    LaunchedEffect(Unit) {
        if (humo.alive && voice.on) { humo.talking = true; voice.say(L11.fx("fixTitle").first()); humo.talking = false }
    }
    CenterScreen(humo, null, L11.fx("fixTitle").first(), null, emptyList(), foot = BtnSpec(L11.fx("fixGo").first(), Kind.Primary) { flow.fixStart() }, medal = false)
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
                Text("/$total", fontFamily = LessonType.Figtree, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Chrome.OffText, modifier = Modifier.offset(y = 6.dp))
            }
        },
        title = L11.scoreTitle(n, total), line = null,
        buttons = listOf(BtnSpec("Continue", Kind.Primary) { flow.finish() }),
    )
}

/** The end: the one place the sparks show (given once, after the skill check). Through fix-it Humo bounces and says one warm line (no jet pass). */
@Composable
private fun EndView(flow: LessonFlow, humo: HumoState) {
    val haptics = LocalHapticFeedback.current
    val voice = rememberHumoVoice()
    LaunchedEffect(Unit) {
        LessonSounds.play(Cue.Complete); haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        val line = flow.endLine
        if (line != null && humo.alive && voice.on) { humo.talking = true; voice.say(line); humo.talking = false }
    }
    CenterScreen(
        humo,
        big = { Text("+${flow.sparks}", fontFamily = LessonType.Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 64.sp, lineHeight = 64.sp, color = Chrome.MintInk) },
        title = "Lesson complete", line = flow.endLine ?: "${flow.sparks} sparks earned.", buttons = listOf(BtnSpec("Restart lesson", Kind.Secondary) { flow.restart() }),
    )
}

/** END LESSON with nowhere to go: a calm goodbye, never "Lesson complete" or sparks (player.js `bye`). */
@Composable
private fun ByeView(flow: LessonFlow, humo: HumoState, onExit: () -> Unit) {
    CenterScreen(
        humo, null, "See you soon", "Your spot is saved.",
        listOf(BtnSpec("Done", Kind.Primary) { onExit() }, BtnSpec("Back to lesson", Kind.Secondary) { flow.resume() }), medal = false,
    )
}
