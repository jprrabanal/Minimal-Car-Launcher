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
