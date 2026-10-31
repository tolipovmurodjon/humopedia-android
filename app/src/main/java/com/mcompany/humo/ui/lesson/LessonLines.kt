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
    const val CHECK_FIRST = "Check your answer first. Then I can show you why it works."
    const val YES = "Great, that is the whole idea: right adds to x, up adds to y.\n\nReady for the next one?"
    const val SEE_IT = "Great. Fix it and try again."
    const val STAY = "Sure. Take your time."
    const val REVIEW = "Ok, let's get into it."
    const val OPEN_FALLBACK = "Ok, let's go. What do you need?"
    const val QUIT = "Wait, you're so close! Stay a little longer?"
    const val AGAIN = "Say it another way?"
    const val MIC_ASK = "I need your microphone to hear you. Tap Allow, please."
    const val MIC_DENIED = "No problem. You can type, or tap one of these."
    const val SKILL_YOURS = "This one is yours. Take your time, you can do it."

    fun plain(text: String, fly: Boolean): String = (if (fly) Words.machine(text) else text).replace("*", "")

    fun steps(n: Int, way: String): String = "${abs(n)} step${if (abs(n) == 1) "" else "s"} $way"

    /** How to count, never the answer (player.js `hintFor`). */
    fun hint(page: LessonPage, machine: String): String = when {
        page.skill -> SKILL_YOURS
        page.kind == PageKind.Drag -> "Start at the $machine. Count the steps to the right along the grid first, then count the steps up. Then move it there."
        page.kind == PageKind.Translate -> "Pick one $machine and its empty circle. Count the steps right, then the steps up. Every $machine moves the same way."
        else -> "Watch one $machine: it moves some steps right and some steps up. The other one moves exactly the same."
    }

    fun why(page: LessonPage, fly: Boolean): String = page.explain?.let { plain(it, fly) + SENSE } ?: CHECK_FIRST

    /** After a wrong try: Humo marks the points with teal squares and asks what the learner notices. */
    fun look(page: LessonPage, machine: String): String =
        if (page.kind == PageKind.Translate) "Let's look together. The teal squares show where your ${machine}s landed and where the targets are. What do you notice?"
        else "Let's look together. The teal squares show where your $machine started and where it is now. Count the steps between them. What do you notice?"

    fun showMe(page: LessonPage, machine: String): String =
        if (page.kind == PageKind.Translate) "Check each $machine against its ring. Count the steps right, then the steps up, and change your numbers by exactly the difference. Then try again."
        else "Compare the two teal squares with the task. Count the steps right, then the steps up, and move your $machine by exactly that much. Then try again."

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

    fun wrongTranslate(machine: String) = "Not all the ${machine}s landed on their targets, lowkey close though."
    fun wrongDrag(machine: String) = "The $machine isn't in the right spot yet, no stress."

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
            add(wrongTranslate(machine)); add(wrongDrag(machine)); add(REVIEW); add(OPEN_FALLBACK)
            add(YES); add(SEE_IT); add(STAY); add(CHECK_FIRST); add(QUIT); add(AGAIN); add(MIC_ASK); add(MIC_DENIED)
            (lesson.pages + lesson.skill).forEach { addAll(tutor(it, fly, machine)) }
        }
    }
}
