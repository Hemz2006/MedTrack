package com.medtrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

/** Small corners for label blocks; fully rounded capsules are reserved for doses. */
val LabelShape = RoundedCornerShape(6.dp)

/** Fixed dispensing-label colours (no wallpaper colour), following the system light/dark setting. */
@Composable
fun MedTrackTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val med = if (dark) DarkMedColors else LightMedColors
    val scheme = if (dark) darkScheme(med) else lightScheme(med)
    CompositionLocalProvider(LocalMedColors provides med) {
        MaterialTheme(colorScheme = scheme, typography = MedTrackTypography, content = content)
    }
}
