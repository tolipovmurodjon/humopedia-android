package com.mcompany.humo.ui.lesson

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatLine(val mine: Boolean, val text: String, val typing: Boolean = false)

/**
 * What the tutor sheet knows: the one line Humo is saying now and the chips on offer. The sheet starts clean every time it
 * opens (no history). [lines] is the lesson's log of what Humo said, which drives his voice outside the sheet.
 */
@Stable
class HumoChatState {
    val lines = mutableStateListOf<ChatLine>()
    var open by mutableStateOf(false)
    internal var chips by mutableStateOf(Chips.More)
    internal var busy by mutableStateOf(false)
    /** A question the player asks for the learner as the sheet opens (Get help). */
    internal var request by mutableStateOf<Request?>(null)
    /** The one line on show in the sheet, and a counter so the same line said twice types again. */
    internal var current by mutableStateOf("")
    internal var serial by mutableIntStateOf(0)

    fun hear(text: String) { lines.add(ChatLine(false, text)) }
    internal fun say(text: String) { current = text; serial++ }

    internal enum class Chips { More, Sense, Notice, Retry, Next }
    internal enum class Request { WhyWrong }

    /** Opens the sheet with the learner's question already asked. */
    internal fun openWith(r: Request) { chips = Chips.More; request = r; open = true }
    internal fun openClean() { chips = Chips.More; request = null; current = ""; busy = false; open = true }
}

/**
 * The tutor (Duolingo's bottom sheet): a grabber, Humo big on the left with an outlined speech bubble whose text types in
 * while his mouth moves and Jessa's voice plays, one row of chips that scrolls sideways, a field to type a question, and
 * one DONE button. One answer at a time. Replies that have no clip never exist: every line is in [LessonLines].
 */
@Composable
internal fun TutorSheet(
    flow: LessonFlow, page: LessonPage, fly: Boolean, humo: HumoState, voice: HumoVoice,
    onTryAgain: () -> Unit, onContinue: () -> Unit, bottomInset: Dp, modifier: Modifier,
) {
    val chat = flow.chat
    val scope = rememberCoroutineScope()
    val machine = if (fly) Words.MACHINE else "point"
    var shown by remember { mutableIntStateOf(0) }

    fun ask(then: HumoChatState.Chips, answer: () -> String) {
        if (chat.busy) return
        chat.busy = true
        scope.launch {
            if (humo.alive) delay(250)
            chat.say(answer())
            chat.chips = then
            chat.busy = false
        }
    }
    fun marked(): String {
        if (page.kind == PageKind.Translate) {
            flow.marks = page.points.map { it + (flow.slots.move ?: Node(0, 0)) } + page.targets
        } else {
            val start = page.points[0]
            flow.marks = listOf(start, flow.dragAt ?: start)
        }
        return LessonLines.look(page, machine)
    }
    fun close() { chat.open = false }

    // The sheet is composed only while it is open, so every opening starts here, clean.
    LaunchedEffect(Unit) {
        val r = chat.request
        chat.request = null
        if (r == HumoChatState.Request.WhyWrong) { chat.say(marked()); chat.chips = HumoChatState.Chips.Notice }
        else { chat.chips = HumoChatState.Chips.More; chat.say(flow.coach ?: LessonLines.OPEN_FALLBACK) }
    }
    // Letter by letter, the mouth moving and the voice playing; without motion (screenshot tests) the line is there at once.
    LaunchedEffect(chat.serial) {
        val text = chat.current
        if (text.isEmpty()) return@LaunchedEffect
        if (!humo.alive) { shown = text.length; return@LaunchedEffect }
        shown = 0
        humo.talking = true
        val clip = launch { if (voice.on) voice.say(text) }
        while (shown < text.length) { delay(26); shown++ }
        clip.join()
        humo.talking = false
    }
    DisposableEffect(Unit) {
        onDispose { voice.stop(); humo.talking = false; flow.marks = emptyList(); chat.current = "" }
    }

    fun typed(q: String) {
        when (TutorIntent.match(q)) {
            TutorIntent.Hint -> ask(HumoChatState.Chips.Sense) { LessonLines.hint(page, machine) }
            TutorIntent.Why -> if (!page.skill) ask(if (flow.phase != Phase.Idle) HumoChatState.Chips.Sense else HumoChatState.Chips.More) { LessonLines.why(page, fly) } else ask(HumoChatState.Chips.Sense) { LessonLines.hint(page, machine) }
            TutorIntent.Look -> ask(HumoChatState.Chips.Notice) { marked() }
            TutorIntent.ShowMe -> ask(HumoChatState.Chips.Retry) { LessonLines.showMe(page, machine) }
            TutorIntent.Read -> ask(HumoChatState.Chips.More) { LessonLines.plain(page.prompt, fly) }
            TutorIntent.SeeIt -> ask(HumoChatState.Chips.Retry) { LessonLines.SEE_IT }
            TutorIntent.Repeat -> chat.say(chat.current)
            TutorIntent.Yes -> ask(HumoChatState.Chips.Next) { LessonLines.YES }
            TutorIntent.No -> ask(HumoChatState.Chips.Sense) { LessonLines.hint(page, machine) + LessonLines.SENSE }
            TutorIntent.Unknown -> ask(HumoChatState.Chips.More) { LessonLines.AGAIN }
        }
    }

    // Talk to Humo: listens only after a tap; the live words fill the field, the finished ones are answered like typed ones.
    val context = LocalContext.current
    val mic = remember { TutorMic(context) }
    var question by remember { mutableStateOf("") }
    fun listen() {
        mic.start(onPartial = { question = it }, onFinal = { heard -> question = ""; if (heard.isBlank()) chat.say(LessonLines.AGAIN) else typed(heard) })
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listen() else chat.say(LessonLines.MIC_DENIED)
    }
    DisposableEffect(mic) { onDispose { mic.cancel() } }
    fun micTapped() {
        when {
            mic.listening -> mic.stop()
            !mic.available -> chat.say(LessonLines.MIC_DENIED)
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> listen()
            else -> { chat.say(LessonLines.MIC_ASK); permission.launch(Manifest.permission.RECORD_AUDIO) }
        }
    }

    Column(
        modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp + bottomInset).testTag("tutor-sheet").chromeGlass(28.dp)
            .padding(top = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFD9D9D9)))
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 20.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            val squeeze = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 700
            Humo(humo, Modifier.size(if (squeeze) 68.dp else 92.dp))
            Spacer(Modifier.width(14.dp))
            Speech(chat.current.take(shown.coerceAtMost(chat.current.length)), Modifier.weight(1f), compact = squeeze)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when (chat.chips) {
                HumoChatState.Chips.Sense -> {
                    Chip("Yes") { ask(HumoChatState.Chips.Next) { LessonLines.YES } }
                    Chip("No") { ask(HumoChatState.Chips.Sense) { LessonLines.hint(page, machine) + LessonLines.SENSE } }
                }
                HumoChatState.Chips.Notice -> {
                    Chip("I see it") { ask(HumoChatState.Chips.Retry) { LessonLines.SEE_IT } }
                    Chip("Show me") { ask(HumoChatState.Chips.Retry) { LessonLines.showMe(page, machine) } }
                }
                HumoChatState.Chips.Retry -> Chip("Try again") { close(); onTryAgain() }
                HumoChatState.Chips.Next -> {
                    Chip("Continue lesson") { close(); onContinue() }
                    Chip("Stay here") { ask(HumoChatState.Chips.More) { LessonLines.STAY } }
                }
                HumoChatState.Chips.More -> {
                    Chip("Give me a hint") { ask(HumoChatState.Chips.Sense) { LessonLines.hint(page, machine) } }
                    if (!page.skill) Chip("Why does it work?") { ask(if (flow.phase != Phase.Idle) HumoChatState.Chips.Sense else HumoChatState.Chips.More) { LessonLines.why(page, fly) } }
                    Chip("Read the task to me") { ask(HumoChatState.Chips.More) { LessonLines.plain(page.prompt, fly) } }
                }
            }
        }
        AskPill(question, { question = it }, mic.listening, mic.level, Modifier.padding(horizontal = 16.dp), onMic = ::micTapped, onCancel = { mic.cancel(); question = "" }, onSend = { q -> if (mic.listening) mic.stop() else { question = ""; typed(q) } })
        Spacer(Modifier.height(12.dp))
        Chunky("DONE", Kind.Primary, true, Modifier.fillMaxWidth().padding(horizontal = 20.dp), onClick = ::close)
    }
}

/** Humo's line in an outlined bubble with a tail pointing at him (the tail covers the border where it joins). */
@Composable
internal fun Speech(text: String, modifier: Modifier, compact: Boolean = false) {
    val line = Color(0xFFE5E5E5)
    Box(
        modifier.heightIn(min = if (compact) 68.dp else 104.dp)
            .clip(RoundedCornerShape(20.dp)).background(Color.White).border(2.dp, line, RoundedCornerShape(20.dp))
            .drawWithContent {
                drawContent()
                val y = size.height * 0.62f
                val w = 12.dp.toPx()
                val h = 11.dp.toPx()
                val tail = Path().apply { moveTo(0f, y - h); lineTo(-w, y); lineTo(0f, y + h) }
                drawPath(Path().apply { moveTo(0f, y - h); lineTo(-w, y); lineTo(0f, y + h); close() }, Color.White)
                drawPath(tail, line, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                drawLine(Color.White, Offset(0f, y - h + 1.dp.toPx()), Offset(0f, y + h - 1.dp.toPx()), 3.dp.toPx())
            }
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .semantics { contentDescription = "Humo says" },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, fontFamily = LessonType.Figtree, fontSize = 17.sp, lineHeight = 25.sp, color = Color(0xFF3C3C3C))
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Chunky(label, Kind.Secondary, true, Modifier, caps = false, onClick = onClick)
}

/**
 * Brilliant's input pill (Boss: "the target"): a soft grey capsule. Listening: a round X (cancel), a dotted live waveform that
 * swells with the child's voice (or the words as they come), a round send arrow. Not listening: a round mic, the field, the
 * send arrow. Speak freely: the words pick the nearest answer Humo has (his clips are a finite set).
 */
@Composable
private fun AskPill(value: String, onValue: (String) -> Unit, listening: Boolean, level: Float, modifier: Modifier, onMic: () -> Unit, onCancel: () -> Unit, onSend: (String) -> Unit) {
    fun send() { val t = value.trim(); if (t.isNotEmpty() || listening) onSend(t) }
    val ink = Color(0xFF3C3C3C)
    Row(modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(Color(0xFFF1F1F1)).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(27.dp)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(19.dp)).background(if (listening) Color(0xFFD9D9D9) else Color(0xFFE6F6EE))
                .clickable(role = Role.Button, onClickLabel = if (listening) "Cancel" else "Talk to Humo", onClick = if (listening) onCancel else onMic)
                .semantics { contentDescription = if (listening) "Cancel listening" else "Talk to Humo" }.testTag(if (listening) "pill-cancel" else "pill-mic"),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(22.dp)) {
                val u = size.width / 22f
                if (listening) {
                    drawLine(ink, Offset(5f * u, 5f * u), Offset(17f * u, 17f * u), 2.6f * u, StrokeCap.Round)
                    drawLine(ink, Offset(17f * u, 5f * u), Offset(5f * u, 17f * u), 2.6f * u, StrokeCap.Round)
                } else {
                    val c = Color(0xFF23A06C)
                    drawRoundRect(c, Offset(8f * u, 2f * u), androidx.compose.ui.geometry.Size(6f * u, 11f * u), androidx.compose.ui.geometry.CornerRadius(3f * u))
                    drawArc(c, 0f, 180f, false, Offset(5f * u, 5f * u), androidx.compose.ui.geometry.Size(12f * u, 11f * u), style = Stroke(1.8f * u, cap = StrokeCap.Round))
                    drawLine(c, Offset(11f * u, 16f * u), Offset(11f * u, 20f * u), 1.8f * u, StrokeCap.Round)
                }
            }
        }
        Box(Modifier.weight(1f).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            if (listening && value.isEmpty()) Dots(level) else if (listening) Text(value, fontFamily = LessonType.Figtree, fontSize = 16.sp, color = ink, maxLines = 1)
            else BasicTextField(
                value, onValue, Modifier.fillMaxWidth().testTag("tutor-input"), singleLine = true,
                textStyle = TextStyle(fontFamily = LessonType.Figtree, fontSize = 16.sp, color = ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }),
                decorationBox = { inner -> Box(contentAlignment = Alignment.CenterStart) { if (value.isEmpty()) Text("Ask Humo something", fontFamily = LessonType.Figtree, fontSize = 16.sp, color = Color(0xFFAFAFAF)); inner() } },
            )
        }
        val live = listening || value.isNotBlank()
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(19.dp)).background(if (live) Color(0xFF3DC98C) else Color(0xFFE0E0E0)).clickable(role = Role.Button, onClickLabel = "Send", onClick = ::send)
                .semantics { contentDescription = "Send question" }.testTag("pill-send"),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(18.dp)) {
                val u = size.width / 18f
                val c = if (live) Color.White else Color(0xFFAFAFAF)
                drawLine(c, Offset(9f * u, 15f * u), Offset(9f * u, 3.5f * u), 2.4f * u, StrokeCap.Round)
                drawLine(c, Offset(9f * u, 3.5f * u), Offset(4f * u, 8.5f * u), 2.4f * u, StrokeCap.Round)
                drawLine(c, Offset(9f * u, 3.5f * u), Offset(14f * u, 8.5f * u), 2.4f * u, StrokeCap.Round)
            }
        }
    }
}

/** The pill drawn on its own, for the screenshot test (the recogniser cannot run on a CI emulator). */
@Composable
internal fun AskPillPreview(listening: Boolean, level: Float) = AskPill("", {}, listening, level, Modifier, {}, {}, {})

/** The dotted live waveform: 18 dots that swell one after the other with how loud the child is. */
@Composable
private fun Dots(level: Float) {
    val t by rememberInfiniteTransition(label = "dots").animateFloat(0f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "phase")
    Canvas(Modifier.fillMaxWidth().height(24.dp).semantics { contentDescription = "Listening" }) {
        val n = 18
        val gap = size.width / n
        for (i in 0 until n) {
            val wave = (kotlin.math.sin((i / n.toFloat() - t) * 2f * Math.PI.toFloat()) + 1f) / 2f
            val r = 2.2.dp.toPx() + (1.0f + 4.5.dp.toPx()) * level * wave
            drawCircle(Color(0xFF8A8F96), r, Offset(gap * (i + 0.5f), size.height / 2f))
        }
    }
}
