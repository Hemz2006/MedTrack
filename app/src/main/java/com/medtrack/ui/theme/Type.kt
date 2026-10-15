package com.medtrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.medtrack.R

/**
 * Newsreader, a free (OFL) contemporary serif chosen as a stand-in for Audela: sharp serifs, built for
 * reading on screens. Static 400/500 (text optical size) and 700 (heading optical size) cut from the variable
 * font; licence in assets/licenses.
 */
val Newsreader = FontFamily(
    Font(R.font.newsreader_regular, FontWeight.Normal),
    Font(R.font.newsreader_medium, FontWeight.Medium),
    Font(R.font.newsreader_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Newsreader, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp)

/** Compact scale: display 28 / title 20 / heading 16 medium / body 14 / label 13. */
val MedTrackTypography = Typography(
    displayLarge = style(28, 34, FontWeight.Bold),
    displayMedium = style(28, 34, FontWeight.Bold),
    displaySmall = style(28, 34, FontWeight.Bold),
    headlineLarge = style(20, 26, FontWeight.Bold),
    headlineMedium = style(20, 26, FontWeight.Bold),
    headlineSmall = style(20, 26, FontWeight.Bold),
    titleLarge = style(20, 26, FontWeight.Bold),
    titleMedium = style(16, 22, FontWeight.Medium),
    titleSmall = style(14, 20, FontWeight.Medium),
    bodyLarge = style(14, 20),
    bodyMedium = style(14, 20),
    bodySmall = style(13, 18),
    labelLarge = style(13, 18, FontWeight.Medium),
    labelMedium = style(13, 18),
    labelSmall = style(12, 16),
)

/** Tabular figures, so times line up in a column. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
