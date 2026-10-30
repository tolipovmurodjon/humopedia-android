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
    data object Retry : Screen
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
    var whyOpen by mutableStateOf(false)
    /** Lesson pages answered right at least once (sparks are paid once per page). */
    private val solved = mutableSetOf<Int>()
    private var praiseAt = 0
    private var nudgeAt = 0
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
        return phase == Phase.Idle && !flying && when (p.kind) {
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
            is Screen.Skill -> { skillResults[s.j] = ok; skillAnswers[s.j] = slots }
            else -> {
                phase = if (ok) Phase.Correct else Phase.Wrong
                verdict = if (ok) L11.praise[praiseAt++ % L11.praise.size] else L11.nudge[nudgeAt++ % L11.nudge.size]
                if (ok && s is Screen.Page && solved.add(s.i)) { sparks += L11.SPARKS_PER_PAGE; sparkBurst++ }
            }
        }
        return ok
    }

    fun tryAgain() { phase = Phase.Idle; verdict = null }

    fun dontKnow() { val s = screen as? Screen.Skill ?: return; skillResults[s.j] = false; skillAnswers[s.j] = slots }

    /** After a skill item's Check (or "I don't know"): the next item, or the score. */
    fun nextSkill() {
        val s = screen as? Screen.Skill ?: return
        if (s.j + 1 < skill.size) show(Screen.Skill(s.j + 1)) else show(Screen.Score)
    }

    /** Continue on a lesson page / review item. */
    fun next() {
        when (val s = screen) {
            is Screen.Page -> show(if (s.i + 1 < pages.size) Screen.Page(s.i + 1) else Screen.SkillSplash)
            is Screen.Review -> show(if (s.k + 1 < reviewQueue.size) Screen.Review(s.k + 1) else Screen.Retry)
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

    fun skipReview() = show(Screen.Retry)
    fun finish() = show(Screen.End)

    /** Back to page [i] (tests and the end card's replay). */
    fun goTo(s: Screen) = show(s)

    private fun show(s: Screen) {
        screen = s
        flying = false
        phase = Phase.Idle
        verdict = null
        whyOpen = false
        dragAt = null
        slots = page?.let { Slots.start(it) } ?: Slots()
        if (s is Screen.Review) {
            // The missed item comes back as the learner left it, in its wrong colours, until Try again.
            val j = reviewQueue[s.k]
            slots = (skillAnswers[j] ?: Slots.start(skill[j])).copy(active = null).let { if (it.empty) Slots.start(skill[j]) else it }
            phase = Phase.Wrong
        }
    }
}
