package com.mcompany.humo.ui.lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What Humo says to a hint request: how to count, never the answer (player.js `hintFor`). */
internal fun hintFor(page: LessonPage, machine: String): String = when {
    page.skill -> "This one is yours. Take your time, you can do it."
    page.kind == PageKind.Drag -> "Start at the $machine. Count the steps to the right along the grid first, then count the steps up. Then move it there."
    page.kind == PageKind.Translate -> "Pick one $machine and its empty circle. Count the steps right, then the steps up. Every $machine moves the same way."
    else -> "Watch one $machine: it moves some steps right and some steps up. The other one moves exactly the same."
}

class ChatLine(val mine: Boolean, val text: String, val typing: Boolean = false)

/** The conversation Humo has with the learner over a whole lesson (Brilliant's Koji): every line, the open sheet, the chips on offer. */
@Stable
class HumoChatState {
    val lines = mutableStateListOf<ChatLine>()
    var open by mutableStateOf(false)
    internal var chips by mutableStateOf(Chips.More)
    internal var busy by mutableStateOf(false)
    /** A question the player asks for the learner as the sheet opens (Why? / Get help). */
    internal var request by mutableStateOf<Request?>(null)

    fun hear(text: String) { lines.add(ChatLine(false, text)) }

    internal enum class Chips { More, Sense, Notice, Retry, Next }
    internal enum class Request { Why, WhyWrong }

    /** Opens the sheet with the learner's question already asked. */
    fun openWith(r: Request) { chips = Chips.More; open = true; request = r }
}

/** The chat button beside Humo (play.html `.ask`, 44 dp on a phone): a round pill with a speech-bubble glyph. */
@Composable
internal fun AskButton(onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFEEF1EE))
            .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = "Ask Humo" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val k = size.width / 24f
            drawPath(Path().apply { moveTo(4f * k, 4f * k); lineTo(20f * k, 4f * k); quadraticTo(22f * k, 4f * k, 22f * k, 6f * k); lineTo(22f * k, 16f * k); quadraticTo(22f * k, 18f * k, 20f * k, 18f * k); lineTo(9f * k, 18f * k); lineTo(4f * k, 22f * k); lineTo(4f * k, 18f * k); quadraticTo(2f * k, 18f * k, 2f * k, 16f * k); lineTo(2f * k, 6f * k); quadraticTo(2f * k, 4f * k, 4f * k, 4f * k); close() }, Color(0xFF8A8F8A))
            drawRect(Color(0xFFEEF1EE), Offset(7f * k, 9f * k), androidx.compose.ui.geometry.Size(10f * k, 2f * k))
            drawRect(Color(0xFFEEF1EE), Offset(7f * k, 13f * k), androidx.compose.ui.geometry.Size(7f * k, 2f * k))
        }
    }
}

private fun listOfPoints(ps: List<Node>): String {
    val a = ps.map { "(${it.x}, ${it.y})" }
    return if (a.size < 2) a.firstOrNull().orEmpty() else a.dropLast(1).joinToString(", ") + " and " + a.last()
}

private fun steps(n: Int, way: String) = "${kotlin.math.abs(n)} step${if (kotlin.math.abs(n) == 1) "" else "s"} $way"

/**
 * The chat with Humo (player.js chat): a bottom sheet over the page. His lines on the left (#eef3f8), the learner's on the
 * right (#2b2b2b), the last three bright and older ones faded, a typing pause, and chips that depend on where the talk is:
 * a hint (how to count, never the answer) / why it works / read the task; after a wrong try "I see it" / "Show me"; after an
 * explanation "Yes" / "No". [onTryAgain] is the Try again chip; [onContinue] the Continue lesson chip.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HumoChat(flow: LessonFlow, page: LessonPage, fly: Boolean, read: (String) -> Unit, onTryAgain: () -> Unit, onContinue: () -> Unit, modifier: Modifier) {
    val chat = flow.chat
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val machine = if (fly) Words.MACHINE else "point"
    fun plain(t: String) = (if (fly) Words.machine(t) else t).replace("*", "")
    val promptText = plain(page.prompt)

    fun why(): String {
        val explain = page.explain
        return if (flow.phase != Phase.Idle && explain != null) plain(explain) + "\n\nDoes that make sense?" else "Check your answer first. Then I can show you why it works."
    }

    /** Where the machines landed and where the targets are; the points get teal squares on the board. */
    fun lookTogether(): String {
        if (page.kind == PageKind.Translate) {
            val at = page.points.map { it + (flow.slots.move ?: Node(0, 0)) }
            flow.marks = at + page.targets
            return "Let's look together. Your ${machine}s landed at ${listOfPoints(at)}. The targets are at ${listOfPoints(page.targets)}. What do you notice?"
        }
        val start = page.points[0]
        val at = flow.dragAt ?: start
        flow.marks = listOf(start, at)
        return "Let's look together. Your $machine started at ${listOfPoints(listOf(start))} and is now at ${listOfPoints(listOf(at))}: that is ${steps(at.x - start.x, "right")} and ${steps(at.y - start.y, "up")}. What do you notice?"
    }

    fun showMe(): String {
        val at = (flow.slots.move?.let { page.points[0] + it }) ?: flow.dragAt ?: page.points[0]
        val goal = page.targets[0]
        val dx = at.x - goal.x
        val dy = at.y - goal.y
        val parts = listOfNotNull(
            if (dx != 0) steps(dx, if (dx > 0) "too far right" else "too far left") else null,
            if (dy != 0) steps(dy, if (dy > 0) "too high" else "too low") else null,
        ).joinToString(" and ")
        return "It landed $parts. So change your move by exactly that much, then try again."
    }

    fun ask(q: String, then: HumoChatState.Chips = HumoChatState.Chips.More, answer: () -> String) {
        if (chat.busy) return
        chat.busy = true
        chat.lines.add(ChatLine(true, q))
        val dots = ChatLine(false, "•••", typing = true)
        chat.lines.add(dots)
        scope.launch {
            delay(650)
            chat.lines.remove(dots)
            chat.lines.add(ChatLine(false, answer()))
            chat.chips = then
            chat.busy = false
        }
    }

    LaunchedEffect(chat.request) {
        when (chat.request) {
            HumoChatState.Request.Why -> ask("Why is this the answer?", HumoChatState.Chips.Sense) { why() }
            HumoChatState.Request.WhyWrong -> ask("Why was my answer wrong?", HumoChatState.Chips.Notice) { lookTogether() }
            null -> Unit
        }
        chat.request = null
    }
    LaunchedEffect(chat.lines.size, chat.open) { if (chat.open) scroll.animateScrollTo(scroll.maxValue) }

    AnimatedVisibility(chat.open, modifier, enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 5 }, exit = fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 5 }) {
        Column(
            Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x24111111), spotColor = Color(0x24111111))
                .clip(RoundedCornerShape(18.dp)).background(Color.White).border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(18.dp)).heightIn(max = 480.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Humo", fontFamily = LessonType.Figtree, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF111111), modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button) { chat.open = false }.semantics { contentDescription = "Close chat" }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(16.dp)) {
                        drawLine(Color(0xFF5A5F68), Offset(0f, 0f), Offset(size.width, size.height), 2.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(Color(0xFF5A5F68), Offset(size.width, 0f), Offset(0f, size.height), 2.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(scroll).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val shown = chat.lines.takeLast(14)
                shown.forEachIndexed { i, m ->
                    Box(Modifier.fillMaxWidth().alpha(if (i < shown.size - 3) 0.38f else 1f), contentAlignment = if (m.mine) Alignment.CenterEnd else Alignment.CenterStart) {
                        Text(
                            m.text,
                            Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(12.dp)).background(if (m.mine) Color(0xFF2B2B2B) else Color(0xFFEEF3F8)).padding(horizontal = 14.dp, vertical = 10.dp),
                            fontFamily = LessonType.Figtree, fontSize = 15.sp, lineHeight = 21.75.sp, color = if (m.mine) Color.White else if (m.typing) Color(0xFF8A8F98) else Color(0xFF111111),
                        )
                    }
                }
            }
            FlowRow(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (chat.chips) {
                    HumoChatState.Chips.Sense -> {
                        Chip("Yes") { ask("Yes", HumoChatState.Chips.Next) { "Great, that is the whole idea: right adds to x, up adds to y.\n\nReady for the next one?" } }
                        Chip("No") { ask("No", HumoChatState.Chips.Sense) { hintFor(page, machine) + "\n\nDoes that make sense?" } }
                    }
                    HumoChatState.Chips.Notice -> {
                        Chip("I see it") { ask("I see it", HumoChatState.Chips.Retry) { "Great. Fix it and try again." } }
                        Chip("Show me") { ask("Show me", HumoChatState.Chips.Retry) { showMe() } }
                    }
                    HumoChatState.Chips.Retry -> Chip("Try again") { chat.open = false; onTryAgain() }
                    HumoChatState.Chips.Next -> {
                        Chip("Continue lesson") { chat.open = false; onContinue() }
                        Chip("Stay here") { ask("Stay here") { "Sure. Take your time." } }
                    }
                    HumoChatState.Chips.More -> {
                        Chip("Give me a hint") { ask("Can I get a hint?", HumoChatState.Chips.Sense) { hintFor(page, machine) } }
                        if (!page.skill) Chip("Why does it work?") { ask("Why is this the answer?", if (flow.phase != Phase.Idle) HumoChatState.Chips.Sense else HumoChatState.Chips.More) { why() } }
                        Chip("Read the task to me") { chat.lines.add(ChatLine(true, "Read the task to me")); read(promptText) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Text(
        label,
        Modifier.clip(RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFDCDCDC), RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        fontFamily = LessonType.Figtree, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF111111),
    )
}
