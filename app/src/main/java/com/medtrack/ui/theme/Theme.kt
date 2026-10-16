package com.medtrack.ui.theme

import com.medtrack.data.ThemeMode
import com.medtrack.container
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.SideEffect
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

/** Small corners for label blocks; fully rounded capsules are reserved for doses. */
val LabelShape = RoundedCornerShape(6.dp)

/**
 * The app's colours and sizes, from the user's settings: light (green and white), dark, or following the phone,
 * and the accessibility text and button sizes. Sizes work by scaling the screen density, so every dp (buttons,
 * icons, spacing) and every sp (text) follows; the text setting is kept independent of the button setting.
 */
@Composable
fun MedTrackTheme(content: @Composable () -> Unit) {
    val settings by LocalContext.current.container.uiSettings.collectAsState()
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val med = if (dark) DarkMedColors else LightMedColors
    val scheme = if (dark) darkScheme(med) else lightScheme(med)
    val base = LocalDensity.current
    val density = Density(base.density * settings.sizeScale, base.fontScale * settings.textScale / settings.sizeScale)

    // Status- and navigation-bar icons dark on the light theme, light on the dark one.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }
    CompositionLocalProvider(LocalMedColors provides med, LocalDensity provides density) {
        MaterialTheme(colorScheme = scheme, typography = MedTrackTypography, content = content)
    }
}
