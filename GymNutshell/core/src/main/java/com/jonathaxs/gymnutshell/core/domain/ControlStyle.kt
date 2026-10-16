package com.jonathaxs.gymnutshell.core.domain

/**
 * Estilo global dos controles de registro das metas (slider ou stepper −/+).
 * Porte de ControlStyle (iOS).
 */
enum class ControlStyle(val rawValue: String) {
    Slider("slider"),
    Stepper("stepper");

    companion object {
        const val STORAGE_KEY = "app.controlStyle"
        val Default = Slider

        fun fromRaw(raw: String?): ControlStyle =
            entries.firstOrNull { it.rawValue == raw } ?: Default

        fun fromRawOrNull(raw: String?): ControlStyle? =
            entries.firstOrNull { it.rawValue == raw }
    }
}
