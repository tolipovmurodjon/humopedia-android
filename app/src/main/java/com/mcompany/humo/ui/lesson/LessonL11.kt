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
    /** Negative ranges (L1.2 on): the board shows all four quadrants down to these. */
    val xMin: Int = 0, val yMin: Int = 0,
)

/**
 * One page of a lesson, as Brilliant has it (lessons/L1.x/lesson.js + the boards measured on 2026-10-29/30):
 * [points] where the points start, [move] the translation that is the answer, [targets] where they must land.
 * [fixedX] / [fixedY] are slots Brilliant fills in for the learner (L1.2: "translate by ( , 0)"); [tiles] the tray.
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
    val fixedX: Int? = null,
    val fixedY: Int? = null,
    val tiles: List<Int> = (0..5).toList(),
) {
    val targets: List<Node> get() = points.map { it + move }
    val graded: Boolean get() = kind == PageKind.Drag || kind == PageKind.Translate
}

/** One playable lesson: its pages, its 3-item skill check and its title. */
interface LessonContent {
    val code: String
    val title: String
    val pages: List<LessonPage>
    val skill: List<LessonPage>
}

/** L1.1 Translating Points, page by page: same texts, numbers, order and answers as Brilliant (clean English). */
object L11 : LessonContent {
    override val code = "1.1"
    override val title = "Translating Points"

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

    override val pages = listOf(
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
    override val skill = listOf(
        LessonPage("SC1", PageKind.Translate, SKILL, Work, listOf(Node(1, 1), Node(3, 1)), Node(3, 2),
            "Each point needs to move 3 steps right and 2 steps up, so the translation is (3, 2).", skill = true),
        LessonPage("SC2", PageKind.Translate, SKILL, Work, listOf(Node(1, 1), Node(3, 1)), Node(0, 3),
            "Each point needs to move 0 steps right and 3 steps up, so the translation is (0, 3).", skill = true),
        LessonPage("SC3", PageKind.Translate, SKILL, Work, listOf(Node(1, 1), Node(3, 1)), Node(2, 4),
            "Each point needs to move 2 steps right and 4 steps up, so the translation is (2, 4).", skill = true),
    )

    /** Brilliant's tutor lines and score titles are the same in every lesson (recorded runs); they live here. */
    val praise = listOf("Good!", "Nice!", "Nicely done.", "Yep.", "Correct.", "Boom.", "Perfect.", "Exactly.")
    val nudge = listOf("Hmm, think about that again.", "Not quite. What’s another way to look at it?")

    /** Sparks for the first right answer on each lesson page (Brilliant: 15 each, 75 for L1.1). */
    const val SPARKS_PER_PAGE = 15

    fun scoreTitle(right: Int, total: Int): String = when {
        right == total -> "Perfect!"
        right == total - 1 -> "Nice work!"
        total == 3 && right == 1 -> "That was tricky!"
        right > 0 -> "Good effort!"
        else -> "Keep practicing!"
    }
}

/**
 * L1.2 Negative Translations (Brilliant slug translating-left-down, v5), page by page: same texts, numbers, order and
 * answers; every board measured on Brilliant's own page at 390 px (lessons/ref390-l12.cjs, 2026-10-30). The tray holds
 * −5 to 5 and one slot of "translate by ( , )" is filled in by Brilliant on most pages.
 */
object L12 : LessonContent {
    override val code = "1.2"
    override val title = "Negative Translations"

    private val Neg = (-5..5).toList()
    private const val SEND = "Complete the translation to send the points to the targets."
    private const val COMPLETE = "Complete the translation."
    /** x −3..3, y −2..2, cell 43.67 (P1). */
    private val G1 = GraphSpec(346f, 228.7f, 173f, 117.35f, 43.67f, 3, 2, 2, true, xMin = -3, yMin = -2)
    /** x −4..2, y −2..2, cell 43.65 (P2, P3, SC3). */
    private val G2 = GraphSpec(346f, 228.7f, 216.7f, 117.35f, 43.65f, 2, 2, 2, true, xMin = -4, yMin = -2)
    /** x −4..5, y −3..2, cell 29.1 (P4). */
    private val G4 = GraphSpec(346f, 199.5f, 158.4f, 88.15f, 29.1f, 5, 2, 2, true, xMin = -4, yMin = -3)
    /** x −4..7, y −3..4, cell 23.8 (P5). */
    private val G5 = GraphSpec(346f, 220.7f, 137.3f, 125.25f, 23.8f, 7, 4, 2, true, xMin = -4, yMin = -3)
    /** x −5..5, y −3..3, cell 26.2 (P6, P7). */
    private val G6 = GraphSpec(346f, 211.2f, 173f, 108.6f, 26.2f, 5, 3, 2, true, xMin = -5, yMin = -3)
    /** x −5..5, y −4..2 (SC1). */
    private val S1 = GraphSpec(346f, 211.2f, 173f, 82.4f, 26.2f, 5, 2, 2, true, xMin = -5, yMin = -4)
    /** x −2..5, y −2..2, cell 37.4 (SC2). */
    private val S2 = GraphSpec(346f, 203.7f, 116.9f, 104.85f, 37.4f, 5, 2, 2, true, xMin = -2, yMin = -2)

    override val pages = listOf(
        LessonPage("P1", PageKind.Translate, SEND, G1, listOf(Node(-1, 1), Node(-1, -1)), Node(2, 0),
            "A translation by (2, 0) will move the points to the targets.", fixedY = 0, tiles = Neg),
        LessonPage("P2", PageKind.Translate, SEND, G2, listOf(Node(1, 1), Node(1, -1)), Node(-4, 0),
            "A translation by (−4, 0) will move the points to the targets.", fixedY = 0, tiles = Neg),
        LessonPage("P3", PageKind.Recap,
            "A translation by (*a*, 0) moves a point to the right if *a* is positive and to the left if *a* is negative.",
            G2, listOf(Node(1, -1), Node(1, 1)), Node(-4, 0)),
        LessonPage("P4", PageKind.Translate, COMPLETE, G4, listOf(Node(-1, 1), Node(2, -1)), Node(0, -1),
            "A translation by (0, −1) will move the points to the targets.", fixedX = 0, tiles = Neg),
        LessonPage("P5", PageKind.Translate, COMPLETE, G5, listOf(Node(-1, 0), Node(2, -2), Node(4, -1)), Node(0, 3),
            "A translation by (0, 3) will move the points to the targets.", fixedX = 0, tiles = Neg),
        LessonPage("P6", PageKind.Translate, COMPLETE, G6, listOf(Node(3, 2), Node(1, 1), Node(-1, 1), Node(-3, 2)), Node(0, -3),
            "A translation by (0, −3) will move the points to the targets.", fixedX = 0, tiles = Neg),
        LessonPage("P7", PageKind.Recap,
            "A translation by (0, *b*) moves a point up if *b* is positive and down if *b* is negative.",
            G6, listOf(Node(3, 2), Node(1, 1), Node(-1, 1), Node(-3, 2)), Node(0, -3)),
    )

    override val skill = listOf(
        LessonPage("SC1", PageKind.Translate, SEND, S1, listOf(Node(1, 1), Node(-1, -1)), Node(0, -2),
            "To move the points 2 steps down, we need to use the translation (0, −2).", skill = true, fixedX = 0, tiles = Neg),
        LessonPage("SC2", PageKind.Translate, SEND, S2, listOf(Node(1, 1), Node(-1, -1)), Node(3, 0),
            "To move the points 3 steps right, we need to use the translation (3, 0).", skill = true, fixedY = 0, tiles = Neg),
        LessonPage("SC3", PageKind.Translate, SEND, G2, listOf(Node(1, 1), Node(-1, -1)), Node(-2, 0),
            "To move the points 2 steps left, we need to use the translation (−2, 0).", skill = true, tiles = Neg),
    )
}

/** The built lessons by their course code. */
val Lessons: Map<String, LessonContent> = listOf(L11, L12).associateBy { it.code }

/** A whole number as Brilliant prints it: a true minus sign (−4), not a hyphen. */
fun signed(v: Int): String = if (v < 0) "−${-v}" else "$v"
