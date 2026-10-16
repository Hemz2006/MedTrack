package com.medtrack.data

/** Follow the phone's dark mode, or always light (green and white) or always dark. */
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/**
 * The full-screen alarm's background and the colour of its text and buttons (ARGB). Each pair keeps the text
 * readable at a glance: dark text on amber, white text on the darker colours.
 */
enum class AlarmColor(val label: String, val background: Long, val content: Long) {
    AMBER("Amber", 0xFFF4A23C, 0xFF0E1824),
    GREEN("Green", 0xFF187A4C, 0xFFFFFFFF),
    BLUE("Blue", 0xFF2563C9, 0xFFFFFFFF),
    RED("Red", 0xFFC8372D, 0xFFFFFFFF),
    PURPLE("Purple", 0xFF6D4AC4, 0xFFFFFFFF),
    NIGHT("Night", 0xFF0E1824, 0xFFE3EAF2),
}

/** The alarm's text and button colour (ARGB); [AUTO] uses the most readable one for the alarm colour. */
enum class AlarmTextColor(val label: String, val argb: Long?) {
    AUTO("Automatic", null),
    WHITE("White", 0xFFFFFFFF),
    BLACK("Black", 0xFF000000),
    YELLOW("Yellow", 0xFFFFE066),
    NAVY("Navy", 0xFF0E1824),
}

/**
 * How the app looks: the colour theme, and two accessibility sizes. [textScale] enlarges or shrinks text on top
 * of the phone's own font size; [sizeScale] does the same for buttons, icons, photos and spacing.
 */
data class UiSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val textScale: Float = 1f,
    val sizeScale: Float = 1f,
    val alarmColor: AlarmColor = AlarmColor.AMBER,
    val alarmTextColor: AlarmTextColor = AlarmTextColor.AUTO,
) {
    /** The alarm's text colour as ARGB: the chosen one, or the alarm colour's own readable pairing. */
    val alarmContent: Long get() = alarmTextColor.argb ?: alarmColor.content

    companion object {
        val TEXT_SCALES = listOf(0.85f, 1f, 1.15f, 1.3f, 1.5f)
        val SIZE_SCALES = listOf(0.9f, 1f, 1.1f, 1.2f, 1.35f)
    }
}
