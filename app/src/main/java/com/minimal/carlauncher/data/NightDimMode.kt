package com.minimal.carlauncher.data

/** When the launcher switches to its darker night colors. */
enum class NightDimMode(val label: String) {
    /** Never dim. */
    OFF("Off"),

    /** Follow the head unit's own day/night mode (on many units: the headlights). */
    AUTO("Auto"),

    /** Dim between two times of day that the user chooses. */
    SCHEDULE("Schedule"),

    /** Always dim. */
    ALWAYS("Always");

    companion object {
        val DEFAULT = SCHEDULE

        fun fromName(name: String?): NightDimMode =
            values().firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** How much darker Night Dimming makes the screen. */
enum class NightDimStrength(val label: String, val amount: Float) {
    LIGHT("Light", 0.25f),
    MEDIUM("Medium", 0.45f),
    STRONG("Strong", 0.6f),
    MAXIMUM("Maximum", 0.75f);

    companion object {
        /** Medium is the same strength as before this setting existed. */
        val DEFAULT = MEDIUM

        fun fromName(name: String?): NightDimStrength =
            values().firstOrNull { it.name == name } ?: DEFAULT
    }
}
