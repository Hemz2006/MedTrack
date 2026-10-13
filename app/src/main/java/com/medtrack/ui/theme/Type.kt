package com.medtrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.medtrack.R

/** Atkinson Hyperlegible Next: static 400/500/700 instances cut from the OFL variable font (see assets/licenses). */
val Atkinson = FontFamily(
    Font(R.font.atkinson_hyperlegible_next_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_next_medium, FontWeight.Medium),
    Font(R.font.atkinson_hyperlegible_next_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Atkinson, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp)

/** Scale: display 32 / title 22 / heading 17 medium / body 15 / label 13. Every slot uses the one family. */
val MedTrackTypography = Typography(
    displayLarge = style(32, 40, FontWeight.Bold),
    displayMedium = style(32, 40, FontWeight.Bold),
    displaySmall = style(32, 40, FontWeight.Bold),
    headlineLarge = style(22, 28, FontWeight.Bold),
    headlineMedium = style(22, 28, FontWeight.Bold),
    headlineSmall = style(22, 28, FontWeight.Bold),
    titleLarge = style(22, 28, FontWeight.Bold),
    titleMedium = style(17, 24, FontWeight.Medium),
    titleSmall = style(15, 22, FontWeight.Medium),
    bodyLarge = style(15, 22),
    bodyMedium = style(15, 22),
    bodySmall = style(13, 18),
    labelLarge = style(13, 18, FontWeight.Medium),
    labelMedium = style(13, 18),
    labelSmall = style(13, 18),
)

/** Tabular figures, so times line up in a column. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
