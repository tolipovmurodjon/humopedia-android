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
    /** "Almost! Let's fix one.": a skill-check miss is never a fail screen, Humo starts the fix-it round (player.js `fixIntro`). */
    data object FixIntro : Screen
    /** Humo replays the missed problem on the board. */
    data object Demo : Screen
    /** One of the two fresh problems of a fix-it round. */
    data class Fix(val k: Int) : Screen
    data object End : Screen
    /** END LESSON with nowhere to go: a calm goodbye, never "Lesson complete" or sparks. */
    data object Bye : Screen
}

/** A bracket Humo draws on the board: a short label beside a run of cells, from [from] to [to] (cells), one mark at a time. */
data class Brace(val from: Node, val to: Node, val label: String)

/**
 * L1.1's player as plain state (Brilliant's lesson player, brilliant.html flows): pages P1-P7, the skill check as a mastery
 * gate (3 of 3 on the first try passes; any miss or Show me goes to Humo's fix-it round and loops until two fresh problems
 * are right), the end card. Grading, sparks, line pools and the tile flow live here so the JVM tests check them without a
 * screen. Values are snapshot state, so the screen redraws on change.
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
    /** Short labels Humo draws on the board while he counts ("4 steps" right, then "up"); Koji-minimal. */
    var braces by mutableStateOf<List<Brace>>(emptyList())
    /** Humo's teacher line for this page (shown in the bubble until a verdict replaces it). */
    val coach: String? get() = page?.id?.let { lesson.coach[it] }
    /** Lesson pages answered right at least once (sparks are paid once per page). */
    private val solved = mutableSetOf<Int>()
    /** Lines said so far: no pool line is said twice in one lesson run until its pool is used up (player.js `fresh`). */
    private val said = mutableSetOf<String>()
    /** The first line of the first pool that has an unsaid one; when all are said the last pool starts over. */
    fun fresh(vararg pools: List<String>): String {
        for (pool in pools) { val a = pool.filter { it !in said }; if (a.isNotEmpty()) { said += a[0]; return a[0] } }
        val last = pools.last()
        said.removeAll(last.toSet()); said += last[0]
        return last[0]
    }
    /** null = not answered yet; per skill item. */
    val skillResults = mutableStateListOf<Boolean?>().apply { repeat(skill.size) { add(null) } }
    private val skillAnswers = arrayOfNulls<Slots>(skill.size)
    /** What the learner tried on a missed problem (page id -> the move in the slots), so Humo can show it back. */
    private val mine = mutableMapOf<String, Node?>()
    /** The problem Humo replays now, and what the learner did on it. */
    var demoPage by mutableStateOf<LessonPage?>(null); private set
    var demoMine by mutableStateOf<Node?>(null); private set
    /** Humo is still replaying: the board is his, Continue waits. */
    var demoBusy by mutableStateOf(false)
    private var fixPair = emptyList<LessonPage>()
    private var fixN = 0
    /** The right / wrong of the two fix-it problems of the round in play. */
    val fixResults = mutableStateListOf<Boolean>()
    /** The skill check was passed by way of fix-it: Humo bounces and says one warm line, no jet pass. */
    var small by mutableStateOf(false); private set
    var endLine by mutableStateOf<String?>(null); private set

    val page: LessonPage? get() = when (val s = screen) {
        is Screen.Page -> pages[s.i]
        is Screen.Skill -> skill[s.j]
        is Screen.Demo -> demoPage
        is Screen.Fix -> fixPair.getOrNull(s.k)
        else -> null
    }

    /** 0..1 along the lesson: page i of 7 is i/6; the skill check and after are full. */
    val progress: Float get() = when (val s = screen) {
        is Screen.Page -> s.i / (pages.size - 1f)
        else -> 1f
    }

    val inSkill: Boolean get() = screen is Screen.Skill
    val inFix: Boolean get() = screen is Screen.Fix
    val canGoBack: Boolean get() = (screen as? Screen.Page)?.let { it.i > 0 } ?: false
    /** Checked: a verdict shows (lesson pages, fix-it) or the skill item was answered (no verdict there). */
    val answered: Boolean get() = phase != Phase.Idle || screen is Screen.Skill && skillResults[(screen as Screen.Skill).j] != null
    /** The machines are flying to where the numbers sent them; the board waits for them before it grades (Check). */
    var flying by mutableStateOf(false)
    val locked: Boolean get() = answered || flying || demoBusy || screen is Screen.Demo

    /** Check is live once the board changed (Brilliant greys it out on an untouched board). */
    val canCheck: Boolean get() {
        val p = page ?: return false
        return phase == Phase.Idle && !flying && !answered && !locked && when (p.kind) {
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
    /** The keypad's undo: the number in the slot just filled goes, and that slot is active again. */
    fun undo() {
        if (locked) return
        val s = slots
        val last = listOf(1, 0).firstOrNull { it !in s.fixed && s[it] != null } ?: return
        slots = (if (last == 0) s.copy(a = null) else s.copy(b = null)).copy(active = last)
    }
    /** Humo sets the numbers himself while he replays a problem (null = empty slots). */
    fun demoSet(move: Node?) { slots = if (move == null) Slots.start(page ?: return) else Slots(move.x, move.y, null, slots.fixed) }

    /** Is the board right? Brilliant grades the landing: the dragged node, or the slots' translation. */
    fun isRight(p: LessonPage = page!!): Boolean = when (p.kind) {
        PageKind.Drag -> dragAt == p.points[0] + p.move
        PageKind.Translate -> slots.move == p.move
        else -> true
    }

    /** Humo's line for a wrong try: the slip it names is the real one, and it is a new line each time (player.js `wrongLine`). */
    private fun wrongLine(p: LessonPage): String {
        val tried = when (p.kind) {
            PageKind.Drag -> (dragAt ?: p.points[0]) - p.points[0]
            else -> slots.move ?: Node(0, 0)
        }
        return fresh(*LessonLines.wrongPools(tried, p.move, p.kind == PageKind.Translate, machineName).toTypedArray())
    }

    /** Returns true when right. A skill item never shows a verdict: it is recorded and the screen moves on. */
    fun check(): Boolean {
        val p = page ?: return false
        val ok = isRight(p)
        when (val s = screen) {
            is Screen.Skill -> {
                // The skill check keeps the page tint and shows a corner verdict instead of Humo's bubble (player.js graded).
                skillResults[s.j] = ok; skillAnswers[s.j] = slots
                if (!ok) mine[p.id] = slots.move
                phase = if (ok) Phase.Correct else Phase.Wrong
                verdict = if (ok) "Correct!" else "Incorrect"
                if (ok) { sparks += L11.SPARKS_PER_PAGE; sparkBurst++; gain = true }
            }
            is Screen.Fix -> {
                fixResults += ok
                if (!ok) mine[p.id] = slots.move
                phase = if (ok) Phase.Correct else Phase.Wrong
                verdict = if (ok) fresh(L11.praise) else wrongLine(p)
                if (ok) { sparks += L11.SPARKS_PER_PAGE; sparkBurst++; gain = true }
            }
            else -> {
                phase = if (ok) Phase.Correct else Phase.Wrong
                verdict = if (ok) fresh(L11.praise) else wrongLine(p)
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
        phase = Phase.Idle; verdict = null; bubble = null; marks = emptyList(); braces = emptyList(); showHow = false
        val p = page
        if (p?.kind == PageKind.Translate) slots = Slots.start(p)
    }

    /** "Show me" in the skill check: counts as missed and goes on to Humo's fix-it round (no shame, no tint). */
    fun dontKnow() {
        val s = screen as? Screen.Skill ?: return
        val p = skill[s.j]
        skillResults[s.j] = false
        mine[p.id] = null
        slots = Slots(p.move.x, p.move.y, null, slots.fixed)
        skillAnswers[s.j] = slots
        verdict = "Here's how it goes."
    }

    /** After a skill item's Check (or Show me): the next item, then the score (a clean 3 of 3) or the fix-it round. */
    fun nextSkill() {
        val s = screen as? Screen.Skill ?: return
        when {
            s.j + 1 < skill.size -> show(Screen.Skill(s.j + 1))
            skillResults.all { it == true } -> show(Screen.Score)
            else -> show(Screen.FixIntro)
        }
    }

    /** Continue on a lesson page / fix-it problem / Humo's replay. */
    fun next() {
        when (val s = screen) {
            is Screen.Page -> show(if (s.i + 1 < pages.size) Screen.Page(s.i + 1) else Screen.SkillSplash)
            is Screen.Demo -> show(Screen.Fix(0))
            is Screen.Fix -> if (s.k == 0) show(Screen.Fix(1)) else fixDecide()
            else -> Unit
        }
    }

    fun back() { val s = screen as? Screen.Page ?: return; if (s.i > 0) show(Screen.Page(s.i - 1)) }

    fun startSkill() { for (i in skillResults.indices) skillResults[i] = null; mine.clear(); show(Screen.Skill(0)) }
    fun beginSkillSplash() = show(Screen.SkillSplash)

    val skillRight: Int get() = skillResults.count { it == true }

    /** "Fix it with Humo": he replays a missed problem, then serves two fresh ones from the pool, in order, wrapping. */
    fun fixStart(missed: LessonPage = skill[skillResults.indexOfFirst { it != true }.coerceAtLeast(0)]) {
        val pool = lesson.fixit
        fixPair = List(2) { pool[fixN++ % pool.size] }
        fixResults.clear()
        demoPage = missed.copy(skill = false)
        demoMine = mine[missed.id]
        show(Screen.Demo)
    }

    /** Both right = passed (Humo bounces, no jet); a miss replays that problem and serves a new pair. */
    private fun fixDecide() {
        if (fixResults.all { it }) { small = true; endLine = fresh(L11.fx("fixPass")); show(Screen.End) }
        else fixStart(if (fixResults[0]) fixPair[1] else fixPair[0])
    }

    /** "Restart lesson" on the end card: back to page 1 with nothing earned. */
    fun restart() {
        sparks = 0; solved.clear(); said.clear(); small = false; endLine = null; fixN = 0; mine.clear()
        for (i in skillResults.indices) skillResults[i] = null
        show(Screen.Page(0))
    }
    fun finish() = show(Screen.End)
    /** END LESSON with no screen to go back to. */
    fun bye() { before = screen; show(Screen.Bye) }
    private var before: Screen? = null
    /** "Back to lesson" on the goodbye: the page the learner left, fresh. */
    fun resume() = show(before?.takeIf { it !is Screen.Bye } ?: Screen.Page(0))

    /** Back to page [i] (tests and the end card's replay). */
    fun goTo(s: Screen) = show(s)

    private fun show(s: Screen) {
        screen = s
        flying = false
        demoBusy = false
        phase = Phase.Idle
        verdict = null
        bubble = null
        gain = false
        showHow = false
        marks = emptyList()
        braces = emptyList()
        dragAt = null
        slots = page?.let { Slots.start(it) } ?: Slots()
    }
}
