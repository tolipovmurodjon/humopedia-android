package com.mcompany.humo.ui.path

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.withTransform
import com.mcompany.humo.ui.glass.Glass
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The map's low-poly art, drawn in code (PHASE-PATH-V2: "flat facets, 2-3 tones per part, soft contact shadow", one
 * light from the top-left, colour theory #6). Every function takes dp and a centre in px; nothing allocates a bitmap.
 */
internal object LowPoly {
    /** Three tones per surface, light → shade (colour theory #7: shade by hue, never by black). */
    class Tones(val light: Color, val mid: Color, val shade: Color)

    class IslandLook(val top: Tones, val side: Tones)

    val DoneLook = IslandLook(Tones(Color(0xFFA6E6B0), Color(0xFF7FCE91), Color(0xFF5DB275)), Tones(Color(0xFFC09166), Color(0xFF9C7149), Color(0xFF7E5938)))
    val CurrentLook = IslandLook(Tones(Color(0xFFFFE597), Color(0xFFFFCD52), Color(0xFFF0AE2E)), Tones(Color(0xFFE39A3B), Color(0xFFC27626), Color(0xFF9E5B1A)))
    val LockedLook = IslandLook(Tones(Color(0xFFDDE3EB), Color(0xFFC6CFDA), Color(0xFFAEB8C6)), Tones(Color(0xFF9AA5B6), Color(0xFF828EA2), Color(0xFF6C788D)))

    /** The contact shadow's hue: deep teal (colour theory #14: shadows take a hue). */
    private val ShadowTeal = Color(0xFF1F5E57)

    /** Light direction (y down): from the top-left. A facet facing it is lit. */
    private const val LIGHT = (225.0 * PI / 180.0).toFloat()

    private fun pick(t: Tones, facing: Float): Color {
        val lit = (cos(facing - LIGHT) + 1f) / 2f
        return when {
            lit > 0.66f -> t.light
            lit > 0.33f -> t.mid
            else -> t.shade
        }
    }

    /**
     * An octagonal island seen from above at a tilt (Boss 2026-10-29: "islands look squashed"): a faceted top whose
     * half height is 0.62 [rx], a side band 0.42 [rx] deep (25 % of the island's height), a lit rim where top meets side
     * so the facets read crisp, and a soft hue shadow under it. [rx] is the half width in px.
     */
    fun DrawScope.island(c: Offset, rx: Float, look: IslandLook) {
        val ry = rx * TOP_RATIO
        val depth = rx * SIDE_RATIO
        drawOval(ShadowTeal.copy(alpha = 0.20f), topLeft = Offset(c.x - rx * 1.06f, c.y + depth - ry * 0.45f), size = Size(rx * 2.12f, ry * 1.3f))
        val v = (0 until 8).map { k ->
            val a = (k * 45f + 22.5f) * PI.toFloat() / 180f
            Offset(c.x + rx * cos(a), c.y + ry * sin(a))
        }
        // The side band: the front half of the rim pushed down, each face lit by how much it turns to the left.
        val front = (0 until 8).filter { k -> v[k].y + v[(k + 1) % 8].y > 2 * c.y }
        for (k in front) {
            val a = v[k]; val b = v[(k + 1) % 8]
            val mid = (a.x + b.x) / 2f - c.x
            val tone = if (mid < -rx * 0.35f) look.side.light else if (mid > rx * 0.35f) look.side.shade else look.side.mid
            drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(b.x, b.y + depth); lineTo(a.x, a.y + depth); close() }, tone)
        }
        // The top: eight facets meeting a ridge point nudged toward the light.
        val ridge = Offset(c.x - rx * 0.10f, c.y - ry * 0.12f)
        for (k in 0 until 8) {
            val a = v[k]; val b = v[(k + 1) % 8]
            val facing = kotlin.math.atan2((a.y + b.y) / 2f - c.y, (a.x + b.x) / 2f - c.x)
            drawPath(Path().apply { moveTo(ridge.x, ridge.y); lineTo(a.x, a.y); lineTo(b.x, b.y); close() }, pick(look.top, facing))
        }
        // The lit rim along the front edge of the top: one crisp line where the light catches the corner.
        for (k in front) drawLine(look.top.light, v[k], v[(k + 1) % 8], rx * 0.035f, StrokeCap.Round)
    }

    /** The island's shape: top half height and side depth, as fractions of its half width. */
    const val TOP_RATIO = 0.62f
    const val SIDE_RATIO = 0.42f

    /** The "done" mark: a white disc with a mint tick, on the island's back-right rim. */
    fun DrawScope.doneBadge(c: Offset, r: Float) {
        drawCircle(ShadowTeal.copy(alpha = 0.18f), r, c + Offset(0f, r * 0.18f))
        drawCircle(Color.White, r, c)
        val tick = Path().apply {
            moveTo(c.x - r * 0.45f, c.y + r * 0.02f); lineTo(c.x - r * 0.1f, c.y + r * 0.36f); lineTo(c.x + r * 0.48f, c.y - r * 0.3f)
        }
        drawPath(tick, Glass.Primary, style = Stroke(r * 0.26f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    /** A four-point sparkle, [s] its half size, [a] its opacity: the "you did this" twinkle on a done tick. */
    fun DrawScope.sparkle(c: Offset, s: Float, a: Float) {
        val w = s * 0.26f
        drawPath(Path().apply {
            moveTo(c.x, c.y - s); lineTo(c.x + w, c.y - w); lineTo(c.x + s, c.y); lineTo(c.x + w, c.y + w)
            lineTo(c.x, c.y + s); lineTo(c.x - w, c.y + w); lineTo(c.x - s, c.y); lineTo(c.x - w, c.y - w); close()
        }, Color(0xFFFFD45C), alpha = a)
        drawCircle(Color.White, s * 0.16f, c, alpha = a)
    }

    /** The current level's flag: a pole and a faceted mint pennant (three tones) that waves, planted on the island's ridge. */
    fun DrawScope.flag(base: Offset, h: Float, wave: Float) {
        val top = Offset(base.x, base.y - h)
        drawLine(Color(0xFF6B4A2E), base, top, strokeWidth = h * 0.07f, cap = StrokeCap.Round)
        // [wave] -1 … 1: the tip swings one way while the fold dips the other, a ripple running down the pennant.
        val tip = Offset(top.x + h * (0.6f + 0.03f * wave), top.y + h * 0.2f + wave * h * 0.08f)
        val low = Offset(top.x, top.y + h * 0.42f)
        val fold = Offset(top.x + h * 0.3f, top.y + h * 0.2f - wave * h * 0.05f)
        drawPath(Path().apply { moveTo(top.x, top.y); lineTo(tip.x, tip.y); lineTo(fold.x, fold.y); close() }, Color(0xFF66DDA8))
        drawPath(Path().apply { moveTo(fold.x, fold.y); lineTo(tip.x, tip.y); lineTo(low.x, low.y); close() }, Color(0xFF3DC98C))
        drawPath(Path().apply { moveTo(top.x, top.y); lineTo(fold.x, fold.y); lineTo(low.x, low.y); close() }, Color(0xFF23A06C))
    }

    /** A small padlock in slate for a locked island (a picture, not a word). */
    fun DrawScope.lock(c: Offset, s: Float) {
        val ink = Color(0xFF5D6880)
        drawArc(ink, 180f, 180f, false, topLeft = Offset(c.x - s * 0.3f, c.y - s * 0.62f), size = Size(s * 0.6f, s * 0.62f), style = Stroke(s * 0.13f, cap = StrokeCap.Round))
        drawLine(ink, Offset(c.x - s * 0.3f, c.y - s * 0.31f), Offset(c.x - s * 0.3f, c.y - s * 0.05f), s * 0.13f, StrokeCap.Round)
        drawLine(ink, Offset(c.x + s * 0.3f, c.y - s * 0.31f), Offset(c.x + s * 0.3f, c.y - s * 0.05f), s * 0.13f, StrokeCap.Round)
        drawRoundRect(ink, topLeft = Offset(c.x - s * 0.45f, c.y - s * 0.08f), size = Size(s * 0.9f, s * 0.62f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.14f))
        drawCircle(Color(0xFFDDE3EB), s * 0.09f, Offset(c.x, c.y + s * 0.2f))
    }

    /** One flagstone: an irregular hexagon, lit half and shaded half, on a thin darker rim (its thickness). */
    fun DrawScope.flagstone(c: Offset, r: Float, seed: Int, dim: Float) {
        val pts = (0 until 6).map { k ->
            val a = (k * 60f + seed * 23 % 60) * PI.toFloat() / 180f
            val rr = r * (0.82f + ((seed * 7 + k * 13) % 5) * 0.06f)
            Offset(c.x + rr * cos(a), c.y + rr * sin(a) * 0.72f)
        }
        val rim = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y + r * 0.28f) else lineTo(p.x, p.y + r * 0.28f) }; close() }
        drawPath(rim, Color(0xFFA88C5E).copy(alpha = dim))
        val face = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }; close() }
        drawPath(face, Color(0xFFCDB68A).copy(alpha = dim))
        // The half facing the light, from the top-left.
        val lit = Path().apply { moveTo(pts[2].x, pts[2].y); lineTo(pts[3].x, pts[3].y); lineTo(pts[4].x, pts[4].y); lineTo(pts[5].x, pts[5].y); close() }
        drawPath(lit, Color(0xFFE9DAB6).copy(alpha = dim))
    }

    /** A wooden footbridge across the river, along the trail's [angle]: deck planks, two rails. */
    fun DrawScope.bridge(c: Offset, angle: Float, len: Float, wide: Float) {
        withTransform({ translate(c.x, c.y); rotateRad(angle, Offset.Zero) }) {
            drawRoundRect(ShadowTeal.copy(alpha = 0.22f), Offset(-len / 2f + wide * 0.1f, -wide / 2f + wide * 0.32f), Size(len, wide), androidx.compose.ui.geometry.CornerRadius(wide * 0.2f))
            drawRect(Color(0xFF9C6E45), Offset(-len / 2f, -wide / 2f + wide * 0.16f), Size(len, wide))
            drawRect(Color(0xFFD7A36C), Offset(-len / 2f, -wide / 2f), Size(len, wide))
            val planks = 7
            for (p in 1 until planks) {
                val x = -len / 2f + len * p / planks
                drawLine(Color(0xFFB0804F), Offset(x, -wide / 2f), Offset(x, wide / 2f), strokeWidth = wide * 0.07f)
            }
            drawLine(Color(0xFF7E5938), Offset(-len / 2f, -wide / 2f), Offset(len / 2f, -wide / 2f), strokeWidth = wide * 0.16f, cap = StrokeCap.Round)
            drawLine(Color(0xFF8F6640), Offset(-len / 2f, wide / 2f), Offset(len / 2f, wide / 2f), strokeWidth = wide * 0.16f, cap = StrokeCap.Round)
        }
    }

    /** A low-poly pine: three stacked cones, each split into a lit and a shaded half, on a small trunk. */
    fun DrawScope.pine(base: Offset, h: Float) {
        drawOval(ShadowTeal.copy(alpha = 0.18f), Offset(base.x - h * 0.34f, base.y - h * 0.06f), Size(h * 0.7f, h * 0.16f))
        drawRect(Color(0xFF8F6A48), Offset(base.x - h * 0.05f, base.y - h * 0.18f), Size(h * 0.1f, h * 0.18f))
        for (k in 0 until 3) {
            val w = h * (0.36f - k * 0.08f)
            val bottom = base.y - h * (0.14f + k * 0.24f)
            val apex = Offset(base.x, bottom - h * 0.42f)
            drawPath(Path().apply { moveTo(apex.x, apex.y); lineTo(base.x - w, bottom); lineTo(base.x, bottom + h * 0.04f); close() }, Color(0xFF79D195))
            drawPath(Path().apply { moveTo(apex.x, apex.y); lineTo(base.x, bottom + h * 0.04f); lineTo(base.x + w, bottom); close() }, Color(0xFF3F9E6B))
        }
    }

    /** A low-poly bush: a faceted dome in three greens. */
    fun DrawScope.bush(base: Offset, s: Float) {
        drawOval(ShadowTeal.copy(alpha = 0.18f), Offset(base.x - s * 0.55f, base.y - s * 0.08f), Size(s * 1.1f, s * 0.2f))
        val top = Offset(base.x - s * 0.08f, base.y - s * 0.62f)
        val l = Offset(base.x - s * 0.5f, base.y); val r = Offset(base.x + s * 0.5f, base.y)
        val ml = Offset(base.x - s * 0.42f, base.y - s * 0.36f); val mr = Offset(base.x + s * 0.4f, base.y - s * 0.4f)
        val mid = Offset(base.x + s * 0.05f, base.y - s * 0.12f)
        drawPath(Path().apply { moveTo(l.x, l.y); lineTo(ml.x, ml.y); lineTo(mid.x, mid.y); close() }, Color(0xFF6CC488))
        drawPath(Path().apply { moveTo(ml.x, ml.y); lineTo(top.x, top.y); lineTo(mid.x, mid.y); close() }, Color(0xFF8FDDA4))
        drawPath(Path().apply { moveTo(top.x, top.y); lineTo(mr.x, mr.y); lineTo(mid.x, mid.y); close() }, Color(0xFF5DB878))
        drawPath(Path().apply { moveTo(mr.x, mr.y); lineTo(r.x, r.y); lineTo(mid.x, mid.y); close() }, Color(0xFF3F9E6B))
        drawPath(Path().apply { moveTo(l.x, l.y); lineTo(mid.x, mid.y); lineTo(r.x, r.y); close() }, Color(0xFF4FA972))
    }

    /**
     * A low-poly cloud around (0, 0) (checker 2026-10-29: "stickers", not sky): one of three silhouettes (3, 4 or 5
     * puffs) picked by [seed], each puff a five-sided dome with two lit facets on its upper-left, over a chamfered
     * blue-grey underside narrower than the puffs ([underside] off for a bank row hidden behind the one in front).
     * [far] rows are a touch bluer. [w] is its width in px.
     */
    fun cloudShape(w: Float, seed: Int, far: Boolean = false, underside: Boolean = true): List<Pair<Path, Color>> = buildList {
        // Rows further back are a touch bluer, so the bank has depth without any blur.
        val lit = if (far) Color(0xFFF7FBFD) else Color(0xFFFFFFFF)
        val soft = if (far) Color(0xFFEAF2F6) else Color(0xFFF6FAFC)
        val body = if (far) Color(0xFFDCE8EF) else Color(0xFFE6EFF4)
        val shade = if (far) Color(0xFFB0C5D4) else Color(0xFFB8CCDA)
        if (underside) add(Path().apply {
            moveTo(-w * 0.44f, -w * 0.02f); lineTo(w * 0.44f, -w * 0.02f); lineTo(w * 0.4f, w * 0.05f)
            lineTo(w * 0.3f, w * 0.085f); lineTo(-w * 0.3f, w * 0.085f); lineTo(-w * 0.4f, w * 0.05f); close()
        } to shade)
        val puffs = when (seed % 3) {
            0 -> listOf(-0.24f to 0.24f, 0.06f to 0.32f, 0.32f to 0.2f)
            1 -> listOf(-0.27f to 0.2f, -0.04f to 0.29f, 0.2f to 0.24f, 0.36f to 0.15f)
            else -> listOf(-0.36f to 0.14f, -0.18f to 0.22f, 0.04f to 0.3f, 0.24f to 0.22f, 0.4f to 0.13f)
        }
        puffs.forEachIndexed { k, (dx, r0) ->
            val r = w * r0 * (0.92f + ((seed + k * 5) % 4) * 0.05f)
            val pc = Offset(w * dx, 0f)
            val n = 5
            // Uneven angles so every dome has its own faceted silhouette.
            val v = (0..n).map { i ->
                val a = PI.toFloat() + i * PI.toFloat() / n + if (i in 1 until n) ((seed + k + i) % 3 - 1) * 0.08f else 0f
                Offset(pc.x + r * cos(a), pc.y + r * 0.95f * sin(a))
            }
            add(Path().apply { moveTo(v[0].x, v[0].y); for (i in 1..n) lineTo(v[i].x, v[i].y); close() } to body)
            val ridge = Offset(pc.x - r * 0.08f, pc.y - r * 0.18f)
            // Two facets that face the light (top-left), the brighter one on the left shoulder.
            add(Path().apply { moveTo(ridge.x, ridge.y); lineTo(v[0].x, v[0].y); lineTo(v[1].x, v[1].y); lineTo(v[2].x, v[2].y); close() } to lit)
            add(Path().apply { moveTo(ridge.x, ridge.y); lineTo(v[2].x, v[2].y); lineTo(v[3].x, v[3].y); close() } to soft)
        }
    }
}
