package com.jonathaxs.gymnutshell.core.domain

/**
 * Modo de aparência do app (Dispositivo, Claro, Escuro) — porte de AppearanceMode (iOS).
 */
enum class AppearanceMode(val rawValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        const val STORAGE_KEY = "app.appearanceMode"
        val Default = SYSTEM

        fun fromRaw(raw: String?): AppearanceMode =
            entries.firstOrNull { it.rawValue == raw } ?: Default

        fun fromRawOrNull(raw: String?): AppearanceMode? =
            entries.firstOrNull { it.rawValue == raw }
    }
}
