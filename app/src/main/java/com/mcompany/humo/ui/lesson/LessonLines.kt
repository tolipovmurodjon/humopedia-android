package com.mcompany.humo.ui.lesson

import kotlin.math.abs

/**
 * Every sentence Humo can say or show for a lesson, in one place. The tutor, the feedback panel and the worked-explanation
 * card read their text from here, and [all] lists the whole finite set, so the build can prove that each line has a bundled
 * Jessa clip (LessonVoiceCoverageTest) and scripts/bundle-voice.cjs knows what to fetch. No reply holds a number the learner
 * chose: the teal squares on the board carry those, so the set stays small.
 */
internal object LessonLines {
    const val SENSE = "\n\nDoes that make sense?"
    const val CHECK_FIRST = "Check first. Then I will show you why."
    const val YES = "Right adds to x, up adds to y. Ready for the next one?"
    const val SEE_IT = "Nice. Fix it and try again."
    const val STAY = "Sure. Take your time."
    const val REVIEW = "Ok, let's get into it."
    const val OPEN_FALLBACK = "Ok, let's go. What do you need?"
    const val QUIT = "Leaving so soon? Your spot is saved. Come back when you are ready."
    const val AGAIN = "Say it another way?"
    const val MIC_ASK = "I need to hear you. Tap Allow, please."
    const val MIC_DENIED = "No problem. You can type, or tap one of these."
    const val SKILL_YOURS = "This one is yours. Take your time, you can do it."

    fun plain(text: String, fly: Boolean): String = (if (fly) Words.machine(text) else text).replace("*", "")

    fun steps(n: Int, way: String): String = "${abs(n)} step${if (abs(n) == 1) "" else "s"} $way"

    /** How to count, never the answer (player.js `hintFor`). */
    fun hint(page: LessonPage, machine: String): String = when {
        page.skill -> SKILL_YOURS
        page.kind == PageKind.Drag -> "Count the steps right, then up. Then move it there."
        page.kind == PageKind.Translate -> "Pick one $machine and its ring. Count right, then up."
        else -> "Watch one $machine move. The others move the same."
    }

    fun why(page: LessonPage, fly: Boolean): String = page.explain?.let { plain(it, fly) + SENSE } ?: CHECK_FIRST

    /** After a wrong try: Humo marks the points with teal squares and asks what the learner notices. */
    fun look(page: LessonPage, machine: String): String =
        if (page.kind == PageKind.Translate) "Look at the teal squares. What do you notice?"
        else "Look at the teal squares. What do you notice?"

    fun showMe(page: LessonPage, machine: String): String =
        if (page.kind == PageKind.Translate) "Compare each $machine with its ring. Fix the difference, then try again."
        else "Compare the two squares. Move it the difference, then try again."

    /** The worked-explanation card (Show me how): the move written out with this page's own numbers. */
    fun how(page: LessonPage, machine: String): String {
        val m = page.move
        val first = page.points[0]
        return if (page.kind == PageKind.Drag) {
            "The $machine starts at (${first.x}, ${first.y}). Move it ${steps(m.x, if (m.x >= 0) "right" else "left")}: x goes from ${first.x} to ${first.x + m.x}. " +
                "Move it ${steps(m.y, if (m.y >= 0) "up" else "down")}: y goes from ${first.y} to ${first.y + m.y}. It lands on (${first.x + m.x}, ${first.y + m.y})."
        } else {
            "Each $machine moves ${steps(m.x, if (m.x >= 0) "right" else "left")} and ${steps(m.y, if (m.y >= 0) "up" else "down")}, so the translation is (${m.x}, ${m.y}). " +
                "For example, (${first.x}, ${first.y}) moves to (${first.x + m.x}, ${first.y + m.y})."
        }
    }

    /** The bold line on top of the card. */
    fun howTitle(page: LessonPage): String = "translate by (${page.move.x}, ${page.move.y})"

    /**
     * Humo's wrong-try lines (player.js `linePools`): each at most 8 words, casual, naming the real slip. [ex] / [ey] are how far
     * right / up the try is off (positive = too far). A learner who misses again meets a new line each time.
     */
    class Pools(val xOnly: List<String>, val yOnly: List<String>, val both: List<String>, val swap: List<String>, val generic: List<String>)

    fun linePools(ex: Int, ey: Int, machine: String): Pools {
        val xs = "${abs(ex)} ${if (ex > 0) "too far" else "short"}"
        val ys = "${abs(ey)} ${if (ey > 0) "too far" else "short"}"
        return Pools(
            xOnly = listOf("Up's good. Right is $xs.", "So close. Right is $xs.", "Half there! Right is $xs.", "Right's the slip: $xs.", "Up works. Count right again.", "Nice up. Right needs a recount."),
            yOnly = listOf("Right's good. Up is $ys.", "So close. Up is $ys.", "Half there! Up is $ys.", "Up's the slip: $ys.", "Right works. Count up again.", "Nice right. Up needs a recount."),
            both = listOf("Right $xs, up $ys.", "Both are off. Right first.", "Not yet. Count right, then up.", "Both slipped. Try one $machine first.", "Count the gap to one ring.", "Two to fix. Start with right."),
            swap = listOf("Right first, up second. Swap them.", "Right numbers, wrong order.", "Flip those two numbers.", "Order slip: right goes first."),
            generic = listOf("Not on the rings yet.", "Missed it. Look at the rings.", "Close! Try another pair.", "Off the mark. Count again.", "Rings still empty. Go again.", "Hmm, not quite. Recount.", "Still short. Try once more.", "Nope, not that one. Again?"),
        )
    }

    /** The pools for one real try: ([learner's move] - [answer]); a swapped pair gets the order lines first. */
    fun wrongPools(tried: Node, answer: Node, graded: Boolean, machine: String): List<List<String>> {
        val ex = tried.x - answer.x
        val ey = tried.y - answer.y
        val p = linePools(ex, ey, machine)
        val swapped = graded && tried.x == answer.y && tried.y == answer.x && ex != 0 && ey != 0
        return listOf(if (swapped) p.swap else if (ex != 0 && ey == 0) p.xOnly else if (ey != 0 && ex == 0) p.yOnly else p.both, p.generic)
    }

    fun demoRight(m: Node) = if (m.y != 0) "Right ${m.x}, then up ${m.y}." else "Right ${m.x}. Nothing up."
    fun youMoved(m: Node) = "You moved ${m.x} right, ${m.y} up."

    /** What the tutor can say about one page. */
    private fun tutor(page: LessonPage, fly: Boolean, machine: String): Set<String> = buildSet {
        add(hint(page, machine)); add(hint(page, machine) + SENSE)
        add(plain(page.prompt, fly))
        if (!page.skill) add(why(page, fly))
        if (page.graded) { add(look(page, machine)); add(showMe(page, machine)); add(how(page, machine)) }
    }

    /** Every line a learner can hear in the lesson (the skill check itself is silent: no Humo there). */
    fun all(lesson: LessonContent, praise: List<String>, fly: Boolean = true): Set<String> {
        val machine = if (fly) Words.MACHINE else "point"
        return buildSet {
            lesson.pages.forEach { p -> lesson.coach[p.id]?.let(::add) }
            addAll(praise)
            for (ex in -3..3) for (ey in -3..3) if (ex != 0 || ey != 0) linePools(ex, ey, machine).let { p ->
                addAll(if (ex != 0 && ey == 0) p.xOnly else if (ey != 0 && ex == 0) p.yOnly else p.both); addAll(p.swap); addAll(p.generic)
            }
            for (a in 0..5) for (b in 0..5) add(youMoved(Node(a, b)))
            (lesson.skill + lesson.fixit).forEach { add(demoRight(it.move)) }
            if (lesson === L11) listOf("fixLook2", "fixNone", "fixThen", "fixNew", "fixPass", "fixTitle").forEach { addAll(L11.fx(it)) }
            add(REVIEW); add(OPEN_FALLBACK)
            add(YES); add(SEE_IT); add(STAY); add(CHECK_FIRST); add(QUIT); add(AGAIN); add(MIC_ASK); add(MIC_DENIED)
            (lesson.pages + lesson.skill + lesson.fixit).forEach { addAll(tutor(it, fly, machine)) }
        }
    }
}
