package com.mcompany.humo.ui.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The bar's icons (Boss 2026-10-28: icons only, Instagram style): Lucide shapes (ISC License, Copyright Lucide Icons and
 * Contributors; paths read from lucide-static 0.460.0 on 2026-10-28): house, map, swords, trophy, user, chevron-left.
 * Unselected = outline; selected = the same shape filled (an even-odd hole keeps the house's door). A 2.2 px round stroke reads a little softer than Lucide's 2 px.
 */
object TabIcons {
    private const val HOUSE = "M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"
    private const val DOOR = "M10 12h4a1 1 0 0 1 1 1v8h-6v-8a1 1 0 0 1 1-1z"
    private const val MAP = "M14.106 5.553a2 2 0 0 0 1.788 0l3.659-1.83A1 1 0 0 1 21 4.619v12.764a1 1 0 0 1-.553.894l-4.553 2.277a2 2 0 0 1-1.788 0l-4.212-2.106a2 2 0 0 0-1.788 0l-3.659 1.83A1 1 0 0 1 3 19.381V6.618a1 1 0 0 1 .553-.894l4.553-2.277a2 2 0 0 1 1.788 0z"
    private val SWORDS = arrayOf(
        "M14.5 17.5 3 6V3h3l11.5 11.5", "m13 19 6-6", "m16 16 4 4", "m19 21 2-2",
        "M14.5 6.5 18 3h3v3l-3.5 3.5", "m5 14 4 4", "m7 17-3 3", "m3 19 2 2",
    )
    private const val CUP = "M18 2H6v7a6 6 0 0 0 12 0V2Z"
    private val TROPHY_REST = arrayOf(
        "M6 9H4.5a2.5 2.5 0 0 1 0-5H6", "M18 9h1.5a2.5 2.5 0 0 0 0-5H18", "M4 22h16",
        "M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22", "M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22",
    )
    private const val HEAD = "M8 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0"
    private const val BODY = "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2z"

    val Home = icon("house", Stroke(HOUSE), Stroke("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"))
    val HomeFilled = icon("house-filled", Filled("$HOUSE $DOOR", evenOdd = true))
    val Worlds = icon("map", Stroke(MAP), Stroke("M15 5.764v15"), Stroke("M9 3.236v15"))
    val WorldsFilled = icon("map-filled", Filled(MAP))
    val Battle = icon("swords", *SWORDS.map { Stroke(it) }.toTypedArray())
    val BattleFilled = icon("swords-bold", *SWORDS.map { Stroke(it, width = 3f) }.toTypedArray())
    val Leaderboard = icon("trophy", Stroke(CUP), *TROPHY_REST.map { Stroke(it) }.toTypedArray())
    val LeaderboardFilled = icon("trophy-filled", Filled(CUP), *TROPHY_REST.map { Stroke(it) }.toTypedArray())
    val Me = icon("user", Stroke(BODY.removeSuffix("z")), Stroke(HEAD))
    val MeFilled = icon("user-filled", Filled(BODY), Filled(HEAD))
    val ChevronLeft = icon("chevron-left", Stroke("m15 18-6-6 6-6"))

    private sealed class Part(val d: String)
    private class Stroke(d: String, val width: Float = 2.2f) : Part(d)
    private class Filled(d: String, val evenOdd: Boolean = false) : Part(d)

    private fun icon(name: String, vararg parts: Part): ImageVector {
        val b = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        for (p in parts) {
            b.addPath(
                pathData = PathParser().parsePathString(p.d).toNodes(),
                pathFillType = if (p is Filled && p.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
                fill = if (p is Filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = if (p is Stroke) p.width else 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }
}
