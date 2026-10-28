package com.mcompany.humo.data.course

/** One level of a course: a stepping stone on the path. Lesson content is English (Boss, 2026-10-28). */
data class Level(val n: Int, val title: String, val lessons: Int, val firstLessonId: String, val firstLessonTitle: String)

data class Course(val id: String, val number: Int, val title: String, val levels: List<Level>)

/**
 * Course 15 "Coordinate Transformations" (id `trans`), names copied from
 * D:\Humopedia\docs\curriculum-v0.5\curriculum-v0.5.json (tracks[0].courses[14]); the server will deal courses later.
 */
val CoordinateTransformations = Course(
    id = "trans",
    number = 15,
    title = "Coordinate Transformations",
    levels = listOf(
        Level(1, "Translations", 6, "trans-L1.1", "Translating Points"),
        Level(2, "Translations in Coordinates", 4, "trans-L2.1", "Coordinate-wise Translation"),
        Level(3, "Rotations", 4, "trans-L3.1", "Rotation Around the Origin"),
        Level(4, "Rotations in Coordinates", 4, "trans-L4.1", "Rotating by 180°"),
        Level(5, "Rotations and Translations", 4, "trans-L5.1", "Rotating before Translating"),
        Level(6, "Rotating to Translate", 5, "trans-L6.1", "Translation by Rotation"),
        Level(7, "Reflections", 6, "trans-L7.1", "Reflecting Across Axes"),
        Level(8, "Reflections in Coordinates", 5, "trans-L8.1", "Reflecting Coordinates"),
        Level(9, "Combining Reflections", 5, "trans-L9.1", "Reflecting to Translate"),
        Level(10, "Congruence and Symmetry", 6, "trans-L10.1", "Congruence"),
        Level(11, "Dilation", 3, "trans-L11.1", "Dilating Points"),
        Level(12, "Dilation and Translation", 3, "trans-L12.1", "Dilating Around a Point"),
        Level(13, "Similarity", 3, "trans-L13.1", "Dilations and Congruence"),
        Level(14, "Stretching", 4, "trans-L14.1", "Stretching in One Direction"),
    ),
)

/** "Lesson 3.1 · Rotation Around the Origin": the first lesson of a level, as the lesson screen names it. */
fun Level.firstLessonLabel(): String = "Lesson ${firstLessonId.substringAfter("-L")} · $firstLessonTitle"
