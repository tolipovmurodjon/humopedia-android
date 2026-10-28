package com.mcompany.humo.ui.glass

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mcompany.humo.R

/**
 * Humopedia's mint, as tokens (brain card rules/colour-theory: brand mint set #A9F0CF #66DDA8 #3DC98C #23A06C; the
 * accessible mint line #1C7E69; ink #0F1630; t10 "aurora in Humopedia mint" ground #F3FBF7). Same names as PAGEL's
 * Glass object so the copied glass code reads the same.
 */
object Glass {
    /** Brand mint: fills and clouds, never text (2.0:1 on white). */
    val Brand = Color(0xFF3DC98C)
    /** The accessible mint line (colour theory #13): lines, rings, icons ≥ 3:1. */
    val Primary = Color(0xFF1C7E69)
    val PrimarySoft = Color(0xFFE3F4EC)
    /**
     * The selected tab's icon and word: #1C7E69 measured 3.55:1 on PAGEL's lit capsule (checker 2026-10-27), below the
     * 4.5:1 text rule, so the word uses the same hue one step deeper.
     */
    val PrimaryInk = Color(0xFF0B5C45)
    val Ink = Color(0xFF0F1630)
    val Muted = Color(0xFF4D5670)
    val Page = Color(0xFFF3FBF7)
    val Mint1 = Color(0xFFA9F0CF)
    val Mint2 = Color(0xFF66DDA8)
    /** oklch(0.86 0.08 185): the teal bridge cloud of t10. */
    val TealCloud = Color(0xFF9FE2D6)
    /** The river's water and its deep shade (deep teal, colour theory #7: darken by hue, never black). */
    val Water = Color(0xFF7FD3C4)
    val WaterDeep = Color(0xFF3FA79A)

    private fun variable(res: Int, weight: FontWeight) =
        Font(res, weight, variationSettings = FontVariation.Settings(weight, FontStyle.Normal))

    /** Bundled, not downloaded (PAGEL's faces): no first-launch font wait. */
    val Heading = FontFamily(variable(R.font.inter_tight, FontWeight.SemiBold), variable(R.font.inter_tight, FontWeight.Bold), variable(R.font.inter_tight, FontWeight.ExtraBold))
    val Body = FontFamily(variable(R.font.schibsted_grotesk, FontWeight.Normal), variable(R.font.schibsted_grotesk, FontWeight.Medium), variable(R.font.schibsted_grotesk, FontWeight.SemiBold), variable(R.font.schibsted_grotesk, FontWeight.Bold))
}

private val GlassTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Glass.Heading, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Glass.Heading, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontFamily = Glass.Body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = Glass.Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Glass.Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Glass.Body, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = Glass.Body, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),
)

private val GlassColors = lightColorScheme(
    primary = Glass.Primary, onPrimary = Color.White, primaryContainer = Glass.PrimarySoft, onPrimaryContainer = Glass.PrimaryInk,
    background = Glass.Page, onBackground = Glass.Ink, surface = Color.White, onSurface = Glass.Ink, onSurfaceVariant = Glass.Muted,
)

/** Light only: the aurora page is the brand; a phone in dark mode still gets ink on near-white (contrast holds). */
@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GlassColors, typography = GlassTypography, content = content)
}
