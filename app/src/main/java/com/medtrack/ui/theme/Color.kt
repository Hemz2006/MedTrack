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

/** Light mode: green and white. Deep-green ink on white (about 12:1), muted green text about 6:1. */
val LightMedColors = MedTrackColors(
    paper = Color(0xFFFFFFFF),
    paperRaised = Color(0xFFF2F8F5),
    ink = Color(0xFF0F3D2E),
    inkMuted = Color(0xFF4A6A5C),
    foil = Color(0xFFD3E6DC),
    amber = Color(0xFFE07F00),
    onAmber = Color(0xFF0F3D2E),
    missed = Color(0xFFB8342C),
    taken = Color(0xFF157A4F),
)

/** Dark mode: the app icon's green-black tile, with mint as the accent (the icon's capsule). */
val DarkMedColors = MedTrackColors(
    paper = Color(0xFF0E171A),
    paperRaised = Color(0xFF162321),
    ink = Color(0xFFE4EEEA),
    inkMuted = Color(0xFF9DB3AA),
    foil = Color(0xFF2B3C37),
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
    primaryContainer = Color(0xFFCDEBDD),
    onPrimaryContainer = m.ink,
    inversePrimary = Color(0xFFF4A23C),
    secondary = m.inkMuted,
    onSecondary = m.paper,
    secondaryContainer = Color(0xFFDCEFE5),
    onSecondaryContainer = m.ink,
    tertiary = m.amber,
    onTertiary = m.onAmber,
    tertiaryContainer = Color(0xFFFFE2BF),
    onTertiaryContainer = m.ink,
    background = m.paper,
    onBackground = m.ink,
    surface = m.paper,
    onSurface = m.ink,
    surfaceVariant = Color(0xFFE5F1EB),
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
    surfaceDim = Color(0xFFE5F1EB),
    surfaceContainerLowest = m.paper,
    surfaceContainerLow = m.paperRaised,
    surfaceContainer = m.paperRaised,
    surfaceContainerHigh = Color(0xFFEAF4EF),
    surfaceContainerHighest = Color(0xFFE5F1EB),
)

internal fun darkScheme(m: MedTrackColors): ColorScheme = darkColorScheme(
    // Mint for actions and selection, matching the icon; dark text on it (about 10:1).
    primary = Color(0xFF6ED3A3),
    onPrimary = m.paper,
    primaryContainer = Color(0xFF1E3B31),
    onPrimaryContainer = m.ink,
    inversePrimary = Color(0xFF157A4F),
    secondary = m.inkMuted,
    onSecondary = m.paper,
    secondaryContainer = Color(0xFF25352F),
    onSecondaryContainer = m.ink,
    tertiary = m.amber,
    onTertiary = m.onAmber,
    tertiaryContainer = Color(0xFF4A3414),
    onTertiaryContainer = m.ink,
    background = m.paper,
    onBackground = m.ink,
    surface = m.paper,
    onSurface = m.ink,
    surfaceVariant = Color(0xFF22322D),
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
    surfaceBright = Color(0xFF22322D),
    surfaceDim = m.paper,
    surfaceContainerLowest = Color(0xFF0A1214),
    surfaceContainerLow = m.paperRaised,
    surfaceContainer = m.paperRaised,
    surfaceContainerHigh = Color(0xFF1C2B28),
    surfaceContainerHighest = Color(0xFF22322D),
)
