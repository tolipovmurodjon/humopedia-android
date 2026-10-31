package com.mcompany.humo.ui.lesson

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

/** The nearest grid node to a point given in cells (x right, y up), kept on the board: Brilliant's snap. */
fun snap(cx: Float, cy: Float, xMax: Int, yMax: Int): Node = Node(cx.roundToInt().coerceIn(0, xMax), cy.roundToInt().coerceIn(0, yMax))

/**
 * The two slots of "translate by ( , )" and which one the tray fills next (Brilliant's workspace, spec §5): the first
 * open slot starts active; a tile fills the active slot and the next empty one turns active; once both are full nothing
 * is active and the tray hides. Tapping a full slot makes it active again (its tile stays until another replaces it).
 * A [fixed] slot is filled in by Brilliant (L1.2's "( , 0)") and never changes.
 */
data class Slots(val a: Int? = null, val b: Int? = null, val active: Int? = 0, val fixed: Set<Int> = emptySet()) {
    val full: Boolean get() = a != null && b != null
    /** Nothing the learner placed is left. */
    val empty: Boolean get() = (0..1).all { it in fixed || this[it] == null }
    val trayShown: Boolean get() = active != null
    val move: Node? get() = if (full) Node(a!!, b!!) else null
    operator fun get(i: Int): Int? = if (i == 0) a else b

    fun place(v: Int): Slots {
        val i = active ?: return this
        val next = if (i == 0) copy(a = v) else copy(b = v)
        val nextActive = listOf(0, 1).firstOrNull { it != i && it !in fixed && next[it] == null }
        return next.copy(active = nextActive)
    }

    fun select(i: Int): Slots = if (i in fixed) this else copy(active = i)

    companion object {
        /** A page's slots before any tile: Brilliant's given values in, the first open slot active. */
        fun start(p: LessonPage): Slots {
            val fixed = setOfNotNull(if (p.fixedX != null) 0 else null, if (p.fixedY != null) 1 else null)
            return Slots(p.fixedX, p.fixedY, (0..1).firstOrNull { it !in fixed }, fixed)
        }
    }
}

enum class Phase { Idle, Correct, Wrong }

sealed interface Screen {
    data class Page(val i: Int) : Screen
    data object SkillSplash : Screen
    data class Skill(val j: Int) : Screen
    data object Score : Screen
    data class Review(val k: Int) : Screen
    data object End : Screen
}

/**
 * L1.1's player as plain state (Brilliant's lesson player, brilliant.html flows): pages P1-P7, the skill check with no
 * verdict per item, the score, review, retry and the end card. Grading, sparks, praise order and the tile flow live
 * here so the JVM tests check them without a screen. Values are snapshot state, so the screen redraws on change.
 */
class LessonFlow(val lesson: LessonContent = L11) {
    val pages: List<LessonPage> = lesson.pages
    val skill: List<LessonPage> = lesson.skill
    var screen by mutableStateOf<Screen>(Screen.Page(0)); private set
    var phase by mutableStateOf(Phase.Idle); private set
    /** The drag page's point (a grid node); null = still at its start. */
    var dragAt by mutableStateOf<Node?>(null); private set
    var slots by mutableStateOf(Slots.start(lesson.pages[0])); private set
    var sparks by mutableIntStateOf(0); private set
    /** "+15" shows once per page, on its first right answer. */
    var sparkBurst by mutableIntStateOf(0); private set
    var verdict by mutableStateOf<String?>(null); private set
    /** Humo's bubble after a verdict: empty while the state shows (wrong 0.65 s, right 0.5 s), then the line (player.js `later`). */
    var bubble by mutableStateOf<String?>(null); private set
    /** The "+ 15" chip at the card's top right: shown after this page's first right answer, until the page changes. */
    var gain by mutableStateOf(false); private set
    /** Where the flying machine is as Humo sees it (-1..1 each way), written by the boards while it flies. */
    var humoLook: androidx.compose.ui.geometry.Offset? = null; private set
    /** A machine is in the air: Humo's eyes follow [humoLook] every frame. */
    var lookOn by mutableStateOf(false); private set
    fun look(at: androidx.compose.ui.geometry.Offset?) { humoLook = at; if ((at != null) != lookOn) lookOn = at != null }
    /** What the machines are called in words ("SR-71" on the fly layer); set by the player. */
    var machineName = "point"
    /** Humo's one conversation for the whole lesson (the chat sheet and the bubble share it). */
    val chat = HumoChatState()
    /** The worked-explanation card under the board ("Show me how"), open until the page changes or Try again. */
    var showHow by mutableStateOf(false)
    /** Teal squares Humo draws round points on the board when he looks at a wrong try with the learner. */
    var marks by mutableStateOf<List<Node>>(emptyList())
    /** Humo's teacher line for this page (shown in the bubble until a verdict replaces it). */
    val coach: String? get() = page?.id?.let { lesson.coach[it] }
    /** Lesson pages answered right at least once (sparks are paid once per page). */
    private val solved = mutableSetOf<Int>()
    /** The web player picks the praise by the page's place in the whole flow (pages, splash, skill items); so do we. */
    private fun praiseIndex(s: Screen): Int = when (s) {
        is Screen.Page -> s.i
        is Screen.Review -> pages.size + 1 + reviewQueue[s.k]
        else -> 0
    }
    /** null = not answered yet; per skill item. */
    val skillResults = mutableStateListOf<Boolean?>().apply { repeat(skill.size) { add(null) } }
    private val skillAnswers = arrayOfNulls<Slots>(skill.size)
    private var reviewQueue = emptyList<Int>()

    val page: LessonPage? get() = when (val s = screen) {
        is Screen.Page -> pages[s.i]
        is Screen.Skill -> skill[s.j]
        is Screen.Review -> skill[reviewQueue[s.k]]
        else -> null
    }

    /** 0..1 along the lesson: page i of 7 is i/6; the skill check and after are full. */
    val progress: Float get() = when (val s = screen) {
        is Screen.Page -> s.i / (pages.size - 1f)
        else -> 1f
    }

    val inSkill: Boolean get() = screen is Screen.Skill
    val canGoBack: Boolean get() = (screen as? Screen.Page)?.let { it.i > 0 } ?: false
    /** Checked: a verdict shows (lesson pages, review) or the skill item was answered (no verdict there). */
    val answered: Boolean get() = phase != Phase.Idle || screen is Screen.Skill && skillResults[(screen as Screen.Skill).j] != null
    /** The machines are flying to where the numbers sent them; the board waits for them before it grades (Check). */
    var flying by mutableStateOf(false)
    val locked: Boolean get() = answered || flying

    /** Check is live once the board changed (Brilliant greys it out on an untouched board). */
    val canCheck: Boolean get() {
        val p = page ?: return false
        return phase == Phase.Idle && !flying && !answered && when (p.kind) {
            PageKind.Drag -> dragAt != null && dragAt != p.points[0]
            PageKind.Translate -> slots.full
            else -> false
        }
    }
    val canStartOver: Boolean get() = phase == Phase.Idle && !flying && when (page?.kind) {
        PageKind.Drag -> dragAt != null && dragAt != page!!.points[0]
        PageKind.Translate -> !slots.empty
        else -> false
    }

    /** The translation the board shows now (points glide by it). */
    val shownMove: Node? get() = when (page?.kind) {
        PageKind.Translate -> slots.move
        PageKind.Recap, PageKind.Explainer -> page!!.move
        else -> null
    }

    // ---- input ----
    fun dropAt(n: Node) { if (!locked) dragAt = n }
    fun tapTile(v: Int) { if (!locked) slots = slots.place(v) }
    fun tapSlot(i: Int) { if (!locked) slots = slots.select(i) }
    fun startOver() { if (!locked) { dragAt = null; slots = page?.let { Slots.start(it) } ?: Slots() } }

    /** Is the board right? Brilliant grades the landing: the dragged node, or the slots' translation. */
    fun isRight(p: LessonPage = page!!): Boolean = when (p.kind) {
        PageKind.Drag -> dragAt == p.points[0] + p.move
        PageKind.Translate -> slots.move == p.move
        else -> true
    }

    /** Returns true when right. A skill item never shows a verdict: it is recorded and the screen moves on. */
    fun check(): Boolean {
        val p = page ?: return false
        val ok = isRight(p)
        when (val s = screen) {
            is Screen.Skill -> {
                // The skill check keeps the page tint and shows a corner verdict instead of Humo's bubble (player.js graded).
                skillResults[s.j] = ok; skillAnswers[s.j] = slots
                phase = if (ok) Phase.Correct else Phase.Wrong
                verdict = if (ok) "Correct!" else "Not quite."
                if (ok) { sparks += L11.SPARKS_PER_PAGE; sparkBurst++; gain = true }
            }
            else -> {
                phase = if (ok) Phase.Correct else Phase.Wrong
                verdict = when {
                    ok -> L11.praise[praiseIndex(s) % L11.praise.size]
                    p.kind == PageKind.Translate -> "Not all the ${machineName}s landed on their targets, lowkey close though."
                    else -> "The $machineName isn't in the right spot yet, no stress."
                }
                if (ok && s is Screen.Page && solved.add(s.i)) { sparks += L11.SPARKS_PER_PAGE; sparkBurst++; gain = true }
            }
        }
        return ok
    }

    /** Humo says [text] in the bubble (a plain remark: no verdict colours). */
    fun say(text: String) { verdict = text; bubble = text; chat.hear(text) }

    /** The verdict's line reaches the bubble (and the chat, and the voice) once its delay is over. */
    fun showVerdict() { val v = verdict ?: return; if (bubble == null) { bubble = v; chat.hear(v) } }

    /** Try again clears the verdict, Humo's marks and (on a translate page) both numbers, so the machines fly home. */
    fun tryAgain() {
        phase = Phase.Idle; verdict = null; bubble = null; marks = emptyList(); showHow = false
        val p = page
        if (p?.kind == PageKind.Translate) slots = Slots.start(p)
    }

    /** "I don't know": counts as missed and the machines fly the right move (no shame, no tint). */
    fun dontKnow() {
        val s = screen as? Screen.Skill ?: return
        val p = skill[s.j]
        skillResults[s.j] = false
        slots = Slots(p.move.x, p.move.y, null, slots.fixed)
        skillAnswers[s.j] = slots
        verdict = "Here's how it goes."
    }

    /** After a skill item's Check (or "I don't know"): the next item, or the score. */
    fun nextSkill() {
        val s = screen as? Screen.Skill ?: return
        if (s.j + 1 < skill.size) show(Screen.Skill(s.j + 1)) else show(Screen.Score)
    }

    /** Continue on a lesson page / review item. */
    fun next() {
        when (val s = screen) {
            is Screen.Page -> show(if (s.i + 1 < pages.size) Screen.Page(s.i + 1) else Screen.SkillSplash)
            is Screen.Review -> show(if (s.k + 1 < reviewQueue.size) Screen.Review(s.k + 1) else Screen.End)
            else -> Unit
        }
    }

    fun back() { val s = screen as? Screen.Page ?: return; if (s.i > 0) show(Screen.Page(s.i - 1)) }

    fun startSkill() { for (i in skillResults.indices) skillResults[i] = null; show(Screen.Skill(0)) }
    fun beginSkillSplash() = show(Screen.SkillSplash)

    val skillRight: Int get() = skillResults.count { it == true }

    fun startReview() {
        reviewQueue = skillResults.indices.filter { skillResults[it] != true }
        if (reviewQueue.isEmpty()) show(Screen.End) else show(Screen.Review(0))
    }

    fun skipReview() = show(Screen.End)

    /** "Restart lesson" on the end card: back to page 1 with nothing earned. */
    fun restart() { sparks = 0; solved.clear(); for (i in skillResults.indices) skillResults[i] = null; show(Screen.Page(0)) }
    fun finish() = show(Screen.End)

    /** Back to page [i] (tests and the end card's replay). */
    fun goTo(s: Screen) = show(s)

    private fun show(s: Screen) {
        screen = s
        flying = false
        phase = Phase.Idle
        verdict = null
        bubble = null
        gain = false
        showHow = false
        marks = emptyList()
        dragAt = null
        slots = page?.let { Slots.start(it) } ?: Slots()
        if (s is Screen.Review) {
            // The missed item comes back as the learner left it, in its wrong colours, until Try again.
            val j = reviewQueue[s.k]
            slots = (skillAnswers[j] ?: Slots.start(skill[j])).copy(active = null).let { if (it.empty) Slots.start(skill[j]) else it }
            phase = Phase.Wrong
            verdict = LessonLines.REVIEW
        }
    }
}
