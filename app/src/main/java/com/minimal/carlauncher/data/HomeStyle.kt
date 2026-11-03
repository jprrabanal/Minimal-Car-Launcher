package com.minimal.carlauncher.data

/** Which home screen the launcher shows. */
enum class HomeStyle(val label: String, val description: String) {
    /** Big clock, date and the bottom bar. Nothing else. */
    MINIMAL("Minimal", "Clock, date and the bottom bar only"),

    /** Clock, music controls, weather, GPS speed and quick buttons. */
    DASHBOARD("Dashboard", "Music, weather, speed and quick buttons");

    companion object {
        val DEFAULT = MINIMAL

        fun fromName(name: String?): HomeStyle =
            values().firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** Size of the Dashboard's bottom bar buttons, for easier tapping while driving. */
enum class DockSize(val label: String, val scale: Float) {
    NORMAL("Normal", 1.0f),
    LARGE("Large", 1.2f),
    EXTRA_LARGE("Extra Large", 1.4f);

    companion object {
        val DEFAULT = LARGE

        fun fromName(name: String?): DockSize =
            values().firstOrNull { it.name == name } ?: DEFAULT
    }
}
