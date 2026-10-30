package com.mcompany.humo.data.course

/**
 * One lesson of a course: one island on the path. [code] is Brilliant's "1.2", [unit] the level it belongs to, [n] its
 * place on the whole path (1-based). [built] lessons can be played; the rest show on the map and say they are coming.
 * Lesson content is English (Boss, 2026-10-28).
 */
data class Lesson(val n: Int, val code: String, val unit: Int, val unitTitle: String, val title: String, val built: Boolean)

data class Course(val id: String, val number: Int, val title: String, val lessons: List<Lesson>) {
    /** The last lesson of each unit but the final one: the trail crosses the river on a footbridge after each. */
    val unitEnds: List<Int> get() = lessons.zipWithNext().filter { (a, b) -> a.unit != b.unit }.map { it.first.n }
}

/** The units (Brilliant's levels) of course 15 and their lessons, as the course has them (trans15/, curriculum-v0.5). */
private val Units = listOf(
    "Translations" to listOf("Translating Points", "Negative Translations", "Translating Shapes", "Combining Translations", "Undoing Translations", "Level Check"),
    "Translations in Coordinates" to listOf("Coordinate-wise Translation", "Translating Shapes in x and y", "Fitting within a Region", "Level Check"),
    "Rotations" to listOf("Rotation Around the Origin", "Rotating Shapes", "Rotating Both Ways", "Composing Rotations"),
    "Rotations in Coordinates" to listOf("Rotating by 180°", "Rotating by 90°", "Rotating by 270°", "Rotating by Coordinates"),
    "Rotations and Translations" to listOf("Rotating before Translating", "Rotating Around a Point", "Rotating to Move", "Translating before Rotating"),
    "Rotating to Translate" to listOf("Translation by Rotation", "Gridline Translations", "Diagonal Translations", "Selecting Angles", "Limits of Rotation"),
    "Reflections" to listOf("Reflecting Across Axes", "Reflecting Across Lines", "The Line of Reflection", "Reflecting Shapes", "Reflection to a Region", "Reversing Reflection"),
    "Reflections in Coordinates" to listOf("Reflecting Coordinates", "Reflecting Vertices", "Reflecting Across y=x", "Reflecting Across y=-x", "Choosing the Coordinate Reflection"),
    "Combining Reflections" to listOf("Reflecting to Translate", "Reflecting to Rotate", "Reflecting to Rotate by 90°", "Rotation Around a Point", "90° Rotation Around a Point"),
    "Congruence and Symmetry" to listOf("Congruence", "Proving Congruence", "Reflectional Symmetry", "Rotational Symmetry", "Identifying Symmetry", "Symmetry and Congruence"),
    "Dilation" to listOf("Dilating Points", "Dilating Shapes", "Dilating and Translating"),
    "Dilation and Translation" to listOf("Dilating Around a Point", "Dilating Points to Targets", "Translating By Dilating"),
    "Similarity" to listOf("Dilations and Congruence", "Similarity", "Identifying Similarity"),
    "Stretching" to listOf("Stretching in One Direction", "Stretching in Two Directions", "Stretching Shapes", "Transforming Points Into Targets"),
)

/** The lessons that play natively so far (PHASE-LESSON-L11): 1.1 and 1.2. */
val BuiltLessons = setOf("1.1", "1.2")

/** Course 15 "Coordinate Transformations" (id `trans`): 14 units, 62 lessons; the server will deal courses later. */
val CoordinateTransformations = Course(
    id = "trans",
    number = 15,
    title = "Coordinate Transformations",
    lessons = Units.flatMapIndexed { u, (unitTitle, titles) -> titles.mapIndexed { k, t -> Triple(u + 1, unitTitle, k + 1 to t) } }
        .mapIndexed { i, (u, unitTitle, lesson) -> Lesson(i + 1, "$u.${lesson.first}", u, unitTitle, lesson.second, "$u.${lesson.first}" in BuiltLessons) },
)
