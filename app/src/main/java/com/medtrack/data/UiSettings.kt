package com.medtrack.data

/** Follow the phone's dark mode, or always light (green and white) or always dark. */
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/**
 * How the app looks: the colour theme, and two accessibility sizes. [textScale] enlarges or shrinks text on top
 * of the phone's own font size; [sizeScale] does the same for buttons, icons, photos and spacing.
 */
data class UiSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val textScale: Float = 1f,
    val sizeScale: Float = 1f,
) {
    companion object {
        val TEXT_SCALES = listOf(0.85f, 1f, 1.15f, 1.3f, 1.5f)
        val SIZE_SCALES = listOf(0.9f, 1f, 1.1f, 1.2f, 1.35f)
    }
}
