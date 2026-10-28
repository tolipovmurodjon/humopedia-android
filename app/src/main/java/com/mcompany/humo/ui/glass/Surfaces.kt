package com.mcompany.humo.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import kotlin.random.Random

/** The page's colour clouds, captured on their own layer so glass panes can bend them (a pane never bends itself). */
val LocalPageBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * The page behind everything (colour theory t10 "aurora in Humopedia mint", PAGEL's AuroraBackground): near-white
 * #F3FBF7 lit by four mint clouds of different hues, one light from the top-left, 2.5 % grain against banding. Two
 * clouds sit under the glass bar so it always has colour to bend. Built once per size change, free while scrolling.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val page = rememberLayerBackdrop()
    Box(modifier) {
        Box(
            Modifier.matchParentSize()
                .layerBackdrop(page)
                .background(Glass.Page)
                .drawWithCache {
                    val grain = ShaderBrush(ImageShader(Grain.bitmap, TileMode.Repeated, TileMode.Repeated))
                    fun glow(x: Float, y: Float, r: Float, c: Color) = Brush.radialGradient(
                        listOf(c, c.copy(alpha = c.alpha * 0.45f), Color.Transparent),
                        center = Offset(size.width * x, size.height * y),
                        radius = size.maxDimension * r,
                    )
                    val clouds = listOf(
                        glow(0.04f, 0.02f, 0.40f, Glass.Mint1.copy(alpha = 0.70f)),
                        glow(0.95f, 0.10f, 0.30f, Glass.TealCloud.copy(alpha = 0.45f)),
                        glow(0.96f, 0.97f, 0.42f, Glass.Mint2.copy(alpha = 0.55f)),
                        glow(0.08f, 0.99f, 0.34f, Glass.Brand.copy(alpha = 0.38f)),
                    )
                    onDrawBehind {
                        clouds.forEach { drawRect(it) }
                        drawRect(grain, alpha = 0.025f)
                    }
                },
        )
        val scope = this
        CompositionLocalProvider(LocalPageBackdrop provides page) { scope.content() }
    }
}

/** 128 px of random grey dots, made once per process (colour theory #5: 2-3 % grain stops banding). */
private object Grain {
    val bitmap: ImageBitmap by lazy {
        val n = 128
        val r = Random(7)
        val px = IntArray(n * n) { val v = r.nextInt(256); (0xFF shl 24) or (v shl 16) or (v shl 8) or v }
        android.graphics.Bitmap.createBitmap(px, n, n, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}

/**
 * A glass pane (PAGEL's Pane): it bends the page's clouds, carries a mint glow in the top-left corner and a
 * white-to-mint rim; text stays ink-dark on it. Off a glass page (tests, previews) it falls back to a flat pane.
 */
@Composable
fun Pane(modifier: Modifier = Modifier, radius: Dp = 24.dp, content: @Composable BoxScope.() -> Unit) {
    val page = LocalPageBackdrop.current
    val shape = RoundedCornerShape(radius)
    val surface = if (page == null || !GlassStage.on) {
        Modifier.clip(shape).background(Color.White.copy(alpha = 0.7f)).drawBehind { paneSurface() }
    } else {
        Modifier.drawBackdrop(
            backdrop = page,
            shape = { RoundedRectangle(radius) },
            effects = { vibrancy(); lens(10f.dp.toPx(), 18f.dp.toPx()) },
            highlight = { Highlight.Default },
            shadow = { Shadow(alpha = 0.45f) },
            onDrawSurface = { paneSurface() },
        )
    }
    Box(modifier.then(surface).border(1.25.dp, PaneRim, shape), content = content)
}

private fun DrawScope.paneSurface() {
    drawRect(Color.White.copy(alpha = 0.42f))
    drawRect(Brush.radialGradient(listOf(Glass.Mint2.copy(alpha = 0.26f), Color.Transparent), center = Offset.Zero, radius = size.minDimension * 0.9f))
}

/** The rim light from the top-left (colour theory t03): bright white, fading, a mint band, bright again. */
val PaneRim: Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val c = Offset(size.width / 2f, size.height / 2f)
        return SweepGradientShader(
            c,
            listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.25f), Glass.Mint2.copy(alpha = 0.55f), Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.95f)),
            listOf(0f, 0.22f, 0.45f, 0.70f, 1f),
        ).apply { setLocalMatrix(android.graphics.Matrix().apply { setRotate(225f, c.x, c.y) }) }
    }
}

/**
 * The screen's edges as an alpha mask on its content (PAGEL RULES "Layout" 4): clear under the status bar, fully drawn
 * at [top]; from [bottom] above the foot the content fades to 35 % toward the glass bar, so the glass bends the fade too.
 */
fun Modifier.edgeFade(top: Dp, bottom: Dp?): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val t = top.toPx().coerceAtLeast(1f)
        val topMask = Brush.verticalGradient(0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color.Black, startY = 0f, endY = t)
        val start = bottom?.let { (size.height - it.toPx()).coerceAtLeast(0f) }
        val foot = start?.let { Brush.verticalGradient(0f to Color.Black, 1f to Color.Black.copy(alpha = 0.35f), startY = it, endY = size.height) }
        onDrawWithContent {
            drawContent()
            drawRect(topMask, size = Size(size.width, t), blendMode = BlendMode.DstIn)
            if (start != null && foot != null) {
                drawRect(foot, topLeft = Offset(0f, start), size = Size(size.width, size.height - start), blendMode = BlendMode.DstIn)
            }
        }
    }
