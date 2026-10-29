package com.mcompany.humo.ui.lesson

/** A grid node (whole numbers), x right, y up, as the lesson's maths writes it. */
data class Node(val x: Int, val y: Int) {
    operator fun plus(o: Node) = Node(x + o.x, y + o.y)
    operator fun minus(o: Node) = Node(x - o.x, y - o.y)
    override fun toString() = "($x, $y)"
}

enum class PageKind { Drag, Explainer, Translate, Recap }

/**
 * One board's geometry in Brilliant's own px at a 350 px wide widget (docs/qa/l11-spec.md §3-6): the box, the origin
 * inside it, one cell, the axis range, which ticks are labelled and whether the axes are named x and y. Every number
 * scales with the widget's width on the phone.
 */
data class GraphSpec(
    val boxW: Float, val boxH: Float, val originX: Float, val originY: Float, val cell: Float,
    val xMax: Int, val yMax: Int, val labelEvery: Int, val names: Boolean,
)

/**
 * One page of the lesson, as Brilliant's L1.1 has it (lessons/L1.1/lesson.js + the boards measured on 2026-10-29):
 * [points] where the points start, [move] the translation that is the answer, [targets] where they must land.
 */
data class LessonPage(
    val id: String,
    val kind: PageKind,
    val prompt: String,
    val graph: GraphSpec,
    val points: List<Node>,
    val move: Node,
    val explain: String? = null,
    val skill: Boolean = false,
) {
    val targets: List<Node> get() = points.map { it + move }
    val graded: Boolean get() = kind == PageKind.Drag || kind == PageKind.Translate
}

/** L1.1 Translating Points, page by page: same texts, numbers, order and answers as Brilliant (clean English). */
object L11 {
    const val TITLE = "Translating Points"
    const val CODE = "L1.1"

    /** "Graph" (P1, P2): 0..6 both ways, every tick labelled, no axis names; cell 42.67 px. */
    private val Graph = GraphSpec(350f, 370f, 48f, 308f, 42.67f, 6, 6, 1, false)
    /** P3's figure: 0..6, no labels, cell 47 px. */
    private val Figure = GraphSpec(350f, 366f, 22f, 334f, 47f, 6, 6, 0, false)
    /** The transformation workspace's graph area: x 0..7, y 0..5, labels every 2, axes named; cell 37.15 px. */
    private val Work = GraphSpec(346f, 256f, 44f, 216f, 37.15f, 7, 5, 2, true)
    /** P7's graph: x 0..8, y 0..6, cell 32.5 px. */
    private val Recap = GraphSpec(346f, 265f, 44f, 225f, 32.5f, 8, 6, 2, true)

    /** *a* is maths italic; digits in a sentence are maths too (KaTeX on Brilliant). */
    private const val MOVE = "Move the point 3 steps to the right and 1 up."
    private const val TRANSLATE = "Translate the points to the targets."
    private const val SKILL = "Translate the points to the target."

    val pages = listOf(
        LessonPage("P1", PageKind.Drag, MOVE, Graph, listOf(Node(2, 1)), Node(3, 1),
            "The point starts at (2, 1). After moving it 3 steps right and 1 step up, it will be at (5, 2)."),
        LessonPage("P2", PageKind.Drag, MOVE, Graph, listOf(Node(1, 3)), Node(3, 1),
            "The point starts at (1, 3). After moving it 3 steps right and 1 step up, it will be at (4, 4)."),
        LessonPage("P3", PageKind.Explainer,
            "Moving points is described as a translation. A translation by (*a*, *b*) moves every point on the plane *a* steps right and *b* steps up.",
            Figure, listOf(Node(2, 1), Node(1, 3)), Node(3, 1)),
        LessonPage("P4", PageKind.Translate, TRANSLATE, Work, listOf(Node(1, 1), Node(1, 2)), Node(2, 2),
            "A translation by (2, 2) will move the points to the targets."),
        LessonPage("P5", PageKind.Translate, TRANSLATE, Work, listOf(Node(3, 1), Node(1, 2), Node(1, 3)), Node(3, 1),
            "A translation by (3, 1) will move the points 3 steps right and 1 step up, landing on the targets."),
        LessonPage("P6", PageKind.Translate, TRANSLATE, Work, listOf(Node(1, 1), Node(1, 2), Node(3, 1), Node(3, 2)), Node(2, 3),
            "A translation by (2, 3) will move the points 2 steps right and 3 steps up, landing on the targets."),
        LessonPage("P7", PageKind.Recap, "A translation moves any number of points in the same way.", Recap,
            listOf(Node(2, 1), Node(1, 2), Node(5, 1), Node(4, 3), Node(3, 1), Node(1, 4)), Node(3, 2)),
    )

    /** The skill check: Brilliant's own three boards (measured), no verdict per item. */
    val skill = listOf(
        LessonPage("SC1", PageKind.Translate, SKILL, Work, listOf(Node(1, 1), Node(3, 1)), Node(3, 2),
            "Each point needs to move 3 steps right and 2 steps up, so the translation is (3, 2).", skill = true),
        LessonPage("SC2", PageKind.Translate, SKILL, Work, listOf(Node(1, 1), Node(3, 1)), Node(0, 3),
            "Each point needs to move 0 steps right and 3 steps up, so the translation is (0, 3).", skill = true),
        LessonPage("SC3", PageKind.Translate, SKILL, Work, listOf(Node(1, 1), Node(3, 1)), Node(2, 4),
            "Each point needs to move 2 steps right and 4 steps up, so the translation is (2, 4).", skill = true),
    )

    /** The tile tray: 0 to 5 on every tile page. */
    val tiles = (0..5).toList()

    val praise = listOf("Good!", "Nice!", "Nicely done.", "Yep.", "Correct.", "Boom.", "Perfect.", "Exactly.")
    val nudge = listOf("Hmm, think about that again.", "Not quite. What’s another way to look at it?")

    /** Sparks for the first right answer on each lesson page (Brilliant: 15 each, 75 for L1.1). */
    const val SPARKS_PER_PAGE = 15

    fun scoreTitle(right: Int, total: Int): String = when {
        right == total -> "Perfect!"
        right == total - 1 -> "So close!"
        total == 3 && right == 1 -> "That was tricky!"
        right > 0 -> "Good effort!"
        else -> "Keep practicing!"
    }
}
