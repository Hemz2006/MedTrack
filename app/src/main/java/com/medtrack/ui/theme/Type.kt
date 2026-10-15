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

/** Scale: display 34 / title 23 / heading 18 medium / body 16 / label 14. A serif needs a touch more size than a sans. */
val MedTrackTypography = Typography(
    displayLarge = style(34, 42, FontWeight.Bold),
    displayMedium = style(34, 42, FontWeight.Bold),
    displaySmall = style(34, 42, FontWeight.Bold),
    headlineLarge = style(23, 30, FontWeight.Bold),
    headlineMedium = style(23, 30, FontWeight.Bold),
    headlineSmall = style(23, 30, FontWeight.Bold),
    titleLarge = style(23, 30, FontWeight.Bold),
    titleMedium = style(18, 25, FontWeight.Medium),
    titleSmall = style(16, 23, FontWeight.Medium),
    bodyLarge = style(16, 24),
    bodyMedium = style(16, 24),
    bodySmall = style(14, 20),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(14, 20),
    labelSmall = style(14, 20),
)

/** Tabular figures, so times line up in a column. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
