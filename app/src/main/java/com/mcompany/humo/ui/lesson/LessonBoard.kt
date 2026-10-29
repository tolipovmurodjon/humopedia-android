package com.mcompany.humo.ui.lesson

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mcompany.humo.ui.path.Jet
import com.mcompany.humo.ui.path.Jet.jet
import com.mcompany.humo.ui.path.Jet.jetShadow
import kotlin.math.atan2
import kotlin.math.hypot

/** Brilliant's board colours, as measured (docs/qa/l11-spec.md §3-6). */
internal object BoardColors {
    val Axis = Color(0xFF000000)
    val Grid = Color(0x1A000000)
    val NodeDot = Color(0xFFCCCCCC)
    val Start = Color(0xFF808080)
    val Blue = Color(0xFF375CE3)
    val WorkBlue = Color(0xFF456DFF)
    val WorkHalo = Color(0xFFECF0FF)
    val Ghost = Color(0xFFDAE2FF)
    val GhostLine = Color(0xFFABBDFF)
    val Yellow = Color(0xFFD7A613)
    val YellowHalo = Color(0xFFFDF3D3)
    val Green = Color(0xFF15B441)
    val GreyGhost = Color(0xFFE5E5E5)
    val Ring = Color(0xFFCCCCCC)
    val Orange = Color(0xFFE77A16)
    val Label = Color(0xFF000000)
}

/** One board laid out at [s] phone px per Brilliant px: grid nodes to px and back. */
internal class BoardGeo(val spec: GraphSpec, val s: Float) {
    val cell: Float get() = spec.cell * s
    fun px(n: Node): Offset = Offset((spec.originX + n.x * spec.cell) * s, (spec.originY - n.y * spec.cell) * s)
    fun px(x: Float, y: Float): Offset = Offset((spec.originX + x * spec.cell) * s, (spec.originY - y * spec.cell) * s)
    /** A px point as grid cells (x right, y up). */
    fun cells(o: Offset): Pair<Float, Float> = Pair((o.x / s - spec.originX) / spec.cell, (spec.originY - o.y / s) / spec.cell)
    fun snapPx(o: Offset): Node { val (x, y) = cells(o); return snap(x, y, spec.xMax, spec.yMax) }
}

/** The axes, grid, ticks and labels of a Brilliant graph (spec §3: axes 1.5 px with arrow heads, grid 10 % black). */
internal fun DrawScope.graph(g: BoardGeo, text: TextMeasurer, dragging: Boolean = false) {
    val sp = g.spec
    val s = g.s
    val o = g.px(Node(0, 0))
    val gx = sp.xMax + 0.5f
    val gy = sp.yMax + 0.5f
    for (i in 1..sp.xMax) drawLine(BoardColors.Grid, g.px(i.toFloat(), 0f), g.px(i.toFloat(), gy), 1f * s)
    for (j in 1..sp.yMax) drawLine(BoardColors.Grid, g.px(0f, j.toFloat()), g.px(gx, j.toFloat()), 1f * s)
    if (dragging) for (i in 0..sp.xMax) for (j in 0..sp.yMax) drawCircle(BoardColors.NodeDot, 2f * s, g.px(Node(i, j)))
    val ax = g.px(sp.xMax + 0.78f, 0f)
    val ay = g.px(0f, sp.yMax + 0.78f)
    drawLine(BoardColors.Axis, o, ax, 1.5f * s)
    drawLine(BoardColors.Axis, o, ay, 1.5f * s)
    drawPath(Path().apply { moveTo(ax.x + 1f * s, ax.y); lineTo(ax.x - 10.5f * s, ax.y - 5f * s); lineTo(ax.x - 10.5f * s, ax.y + 5f * s); close() }, BoardColors.Axis)
    drawPath(Path().apply { moveTo(ay.x, ay.y - 1f * s); lineTo(ay.x - 5f * s, ay.y + 10.5f * s); lineTo(ay.x + 5f * s, ay.y + 10.5f * s); close() }, BoardColors.Axis)
    val style = TextStyle(fontSize = (16f * s / density).sp / fontScale, color = BoardColors.Label)
    for (i in 1..sp.xMax) {
        val p = g.px(i.toFloat(), 0f)
        drawLine(BoardColors.Axis, Offset(p.x, p.y - 5f * s), Offset(p.x, p.y + 5f * s), 1.5f * s)
        if (sp.labelEvery > 0 && i % sp.labelEvery == 0) label(text, "$i", Offset(p.x, p.y + 18f * s), style)
    }
    for (j in 1..sp.yMax) {
        val p = g.px(0f, j.toFloat())
        drawLine(BoardColors.Axis, Offset(p.x - 5f * s, p.y), Offset(p.x + 5f * s, p.y), 1.5f * s)
        if (sp.labelEvery > 0 && j % sp.labelEvery == 0) label(text, "$j", Offset(p.x - 18f * s, p.y), style)
    }
    if (sp.names) {
        val bold = style.copy(fontWeight = FontWeight.Bold)
        label(text, "x", Offset(ax.x - 2f * s, ax.y + 18f * s), bold)
        label(text, "y", Offset(ay.x - 18f * s, ay.y + 2f * s), bold)
    }
}

internal fun DrawScope.label(text: TextMeasurer, s: String, centre: Offset, style: TextStyle) {
    val m = text.measure(s, style)
    drawText(m, topLeft = Offset(centre.x - m.size.width / 2f, centre.y - m.size.height / 2f))
}

/** The 16 px tick or cross badge Brilliant shows above a checked point. */
internal fun DrawScope.badge(c: Offset, ok: Boolean, s: Float) {
    val h = 8f * s
    drawRoundRect(if (ok) BoardColors.Green else BoardColors.Yellow, Offset(c.x - h, c.y - h), Size(2 * h, 2 * h), CornerRadius(3f * s))
    val st = Stroke(2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (ok) drawPath(Path().apply { moveTo(c.x - 4f * s, c.y + 0.2f * s); lineTo(c.x - 1.2f * s, c.y + 3f * s); lineTo(c.x + 4.2f * s, c.y - 3f * s) }, Color.White, style = st)
    else {
        drawLine(Color.White, Offset(c.x - 3.4f * s, c.y - 3.4f * s), Offset(c.x + 3.4f * s, c.y + 3.4f * s), 2f * s, StrokeCap.Round)
        drawLine(Color.White, Offset(c.x + 3.4f * s, c.y - 3.4f * s), Offset(c.x - 3.4f * s, c.y + 3.4f * s), 2f * s, StrokeCap.Round)
    }
}

/** Brilliant's drag handle: a soft 64 px shadow, a white 40 px halo at .9 and the dot (10 px, 30 px while dragged). */
internal fun DrawScope.dragHandle(c: Offset, dot: Color, dotR: Float, s: Float, halo: Boolean) {
    if (halo) {
        drawCircle(Brush.radialGradient(0.55f to Color(0x24000000), 1f to Color.Transparent, center = c, radius = 32f * s), 32f * s, c)
        drawCircle(Color.White.copy(alpha = 0.9f), 20f * s, c)
    }
    drawCircle(dot, dotR, c)
}

/** A workspace point (12 px on a 20 px halo), its colour by the verdict. */
internal fun DrawScope.workPoint(c: Offset, phase: Phase, s: Float) {
    when (phase) {
        Phase.Idle -> { drawCircle(BoardColors.WorkHalo, 10f * s, c); drawCircle(BoardColors.WorkBlue, 6f * s, c) }
        Phase.Wrong -> { drawCircle(BoardColors.YellowHalo, 10f * s, c); drawCircle(BoardColors.Yellow, 6f * s, c) }
        Phase.Correct -> drawCircle(BoardColors.Green, 6f * s, c)
    }
}

/** A target: an 18 px ring, 2 px #CCC, faintly filled. */
internal fun DrawScope.target(c: Offset, s: Float) {
    drawCircle(Color(0x06000000), 9f * s, c)
    drawCircle(BoardColors.Ring, 8f * s, c, style = Stroke(2f * s))
}

/** Where a point began, once it has moved: a pale ghost joined to the point by a 2 px line at .7. */
internal fun DrawScope.ghost(from: Offset, to: Offset, phase: Phase, s: Float) {
    val grey = phase != Phase.Idle
    drawLine((if (grey) BoardColors.Ring else BoardColors.GhostLine).copy(alpha = 0.7f), from, to, 2f * s)
    drawCircle(if (grey) BoardColors.GreyGhost else BoardColors.Ghost, 6f * s, from)
    drawCircle(if (grey) BoardColors.Ring else BoardColors.GhostLine, 5.5f * s, from, style = Stroke(1f * s))
}

/**
 * P3's figure (spec §4): two points moved by (3, 1); each start dot grey, the end dot coloured, the coloured segment
 * between them, the grey legs a then b and their dimension markers. On the fly layer the end dots are jets.
 */
internal fun DrawScope.explainer(g: BoardGeo, page: LessonPage, text: TextMeasurer, frames: List<ImageBitmap>?) {
    graph(g, text)
    val s = g.s
    val legStyle = TextStyle(fontSize = (16f * s / density).sp / fontScale, color = Color(0xFF808080), fontWeight = FontWeight.Medium)
    page.points.forEachIndexed { i, start ->
        val end = start + page.move
        val a = g.px(start)
        val b = g.px(end)
        val corner = g.px(end.x.toFloat(), start.y.toFloat())
        val colour = if (i == 0) BoardColors.Blue else BoardColors.Orange
        drawLine(Color(0xFF808080), a, corner, 2f * s)
        drawLine(Color(0xFF808080), corner, b, 2f * s)
        drawLine(colour, a, b, 2f * s)
        // dimension markers: 16 px below the a leg and right of the b leg, 8 px end ticks, the letter in a white gap
        val ya = a.y + 16f * s
        drawLine(Color(0xFF808080), Offset(a.x, ya), Offset(corner.x, ya), 1.5f * s)
        drawLine(Color(0xFF808080), Offset(a.x, ya - 4f * s), Offset(a.x, ya + 4f * s), 1.5f * s)
        drawLine(Color(0xFF808080), Offset(corner.x, ya - 4f * s), Offset(corner.x, ya + 4f * s), 1.5f * s)
        val xb = corner.x + 16f * s
        drawLine(Color(0xFF808080), Offset(xb, b.y), Offset(xb, corner.y), 1.5f * s)
        drawLine(Color(0xFF808080), Offset(xb - 4f * s, b.y), Offset(xb + 4f * s, b.y), 1.5f * s)
        drawLine(Color(0xFF808080), Offset(xb - 4f * s, corner.y), Offset(xb + 4f * s, corner.y), 1.5f * s)
        val ma = Offset((a.x + corner.x) / 2f, ya)
        drawRect(Color.White, Offset(ma.x - 9f * s, ma.y - 9f * s), Size(18f * s, 18f * s))
        label(text, "a", ma, legStyle)
        val mb = Offset(xb, (b.y + corner.y) / 2f)
        drawRect(Color.White, Offset(mb.x - 9f * s, mb.y - 9f * s), Size(18f * s, 18f * s))
        label(text, "b", mb, legStyle)
        drawCircle(BoardColors.Start, 5f * s, a)
        if (frames == null) drawCircle(colour, 5f * s, b)
        else jetAt(frames, b, atan2(b.y - a.y, b.x - a.x), g.cell * 0.9f, flames = 0f, t = 0f, motion = false)
    }
    drawCircle(BoardColors.Axis, 5f * s, g.px(Node(0, 0)))
}

/** The SR-71 as the point: [len] its nose-to-tail length in px; the sprite frame is 1/0.857 of that. */
internal fun DrawScope.jetAt(frames: List<ImageBitmap>, c: Offset, heading: Float, len: Float, flames: Float, t: Float, motion: Boolean, shadow: Boolean = true) {
    val frame = len / 0.857f
    if (shadow) jetShadow(frames, c, frame, heading, 0f, 0f, 0.2f, len / 60f)
    jet(frames, c, frame, heading, 0f, 0f, t, motion, flames)
}

/** The armed jet (tap-then-tap): a dashed blue ring around it, turning slowly. */
internal fun DrawScope.armedRing(c: Offset, r: Float, turn: Float, s: Float) {
    rotate(turn, c) {
        drawCircle(Color(0xFF375CE3), r, c, style = Stroke(2f * s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * s, 5f * s))))
    }
}

/** A smoke puff list: each (position, age 0..1). */
internal fun DrawScope.puffs(list: List<Pair<Offset, Float>>, cell: Float) {
    for ((p, age) in list) drawCircle(Color(0xFF787C84), cell * 0.16f * (0.5f + 1.7f * age), p, alpha = 0.32f * (1f - age))
}

internal fun dist(a: Offset, b: Offset) = hypot(a.x - b.x, a.y - b.y)

internal val JetFrames get() = Jet.Frames
