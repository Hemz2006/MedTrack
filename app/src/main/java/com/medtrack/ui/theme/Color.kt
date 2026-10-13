package com.medtrack.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The dispensing-label palette: label paper, prescription ink, blister foil and warning-sticker amber. */
@Immutable
data class MedTrackColors(
    val paper: Color,
    val paperRaised: Color,
    val ink: Color,
    val inkMuted: Color,
    val foil: Color,
    val amber: Color,
    /** Text and icons on an amber surface: white fails contrast there, so it is dark in both modes. */
    val onAmber: Color,
    val missed: Color,
    val taken: Color,
)

val LightMedColors = MedTrackColors(
    paper = Color(0xFFF7F9FB),
    paperRaised = Color(0xFFFFFFFF),
    ink = Color(0xFF16324F),
    inkMuted = Color(0xFF4E6378),
    foil = Color(0xFFC9D2DB),
    amber = Color(0xFFE07F00),
    onAmber = Color(0xFF16324F),
    missed = Color(0xFFB8342C),
    taken = Color(0xFF2E7A55),
)

val DarkMedColors = MedTrackColors(
    paper = Color(0xFF0E1824),
    paperRaised = Color(0xFF16222F),
    ink = Color(0xFFE3EAF2),
    inkMuted = Color(0xFF9FB0C2),
    foil = Color(0xFF2A3A4C),
    amber = Color(0xFFF4A23C),
    onAmber = Color(0xFF0E1824),
    missed = Color(0xFFF07167),
    taken = Color(0xFF5CC28E),
)

internal val LocalMedColors = staticCompositionLocalOf { LightMedColors }

/** The MedTrack tokens that Material3's ColorScheme has no slot for. */
val MaterialTheme.medColors: MedTrackColors
    @Composable @ReadOnlyComposable get() = LocalMedColors.current

internal fun lightScheme(m: MedTrackColors): ColorScheme = lightColorScheme(
    primary = m.ink,
    onPrimary = m.paper,
    primaryContainer = Color(0xFFDCE6F0),
    onPrimaryContainer = m.ink,
    inversePrimary = Color(0xFFF4A23C),
    secondary = m.inkMuted,
    onSecondary = m.paper,
    secondaryContainer = Color(0xFFDCE4EC),
    onSecondaryContainer = m.ink,
    tertiary = m.amber,
    onTertiary = m.onAmber,
    tertiaryContainer = Color(0xFFFFE2BF),
    onTertiaryContainer = m.ink,
    background = m.paper,
    onBackground = m.ink,
    surface = m.paper,
    onSurface = m.ink,
    surfaceVariant = Color(0xFFE6EBF0),
    onSurfaceVariant = m.inkMuted,
    surfaceTint = Color.Transparent,
    inverseSurface = m.ink,
    inverseOnSurface = m.paper,
    error = m.missed,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBE3E1),
    onErrorContainer = Color(0xFF7A1F19),
    outline = m.foil,
    outlineVariant = m.foil,
    scrim = Color(0xFF000000),
    surfaceBright = m.paperRaised,
    surfaceDim = Color(0xFFE6EBF0),
    surfaceContainerLowest = m.paperRaised,
    surfaceContainerLow = m.paperRaised,
    surfaceContainer = m.paperRaised,
    surfaceContainerHigh = Color(0xFFEEF2F6),
    surfaceContainerHighest = Color(0xFFE6EBF0),
)

internal fun darkScheme(m: MedTrackColors): ColorScheme = darkColorScheme(
    primary = m.ink,
    onPrimary = m.paper,
    primaryContainer = Color(0xFF243444),
    onPrimaryContainer = m.ink,
    inversePrimary = Color(0xFF16324F),
    secondary = m.inkMuted,
    onSecondary = m.paper,
    secondaryContainer = Color(0xFF2A3A4C),
    onSecondaryContainer = m.ink,
    tertiary = m.amber,
    onTertiary = m.onAmber,
    tertiaryContainer = Color(0xFF4A3414),
    onTertiaryContainer = m.ink,
    background = m.paper,
    onBackground = m.ink,
    surface = m.paper,
    onSurface = m.ink,
    surfaceVariant = Color(0xFF243444),
    onSurfaceVariant = m.inkMuted,
    surfaceTint = Color.Transparent,
    inverseSurface = m.ink,
    inverseOnSurface = m.paper,
    error = m.missed,
    onError = m.paper,
    errorContainer = Color(0xFF4A1A17),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = m.foil,
    outlineVariant = m.foil,
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF243444),
    surfaceDim = m.paper,
    surfaceContainerLowest = Color(0xFF0B141E),
    surfaceContainerLow = m.paperRaised,
    surfaceContainer = m.paperRaised,
    surfaceContainerHigh = Color(0xFF1D2B3A),
    surfaceContainerHighest = Color(0xFF243444),
)
