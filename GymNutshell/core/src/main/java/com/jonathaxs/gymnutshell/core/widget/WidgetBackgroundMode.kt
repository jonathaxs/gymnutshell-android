package com.jonathaxs.gymnutshell.core.widget

/**
 * Modo de fundo dos widgets escolhido pelo usuário — porte de WidgetBackgroundMode (iOS).
 * `rawValue` persiste no DataStore. Default é System (igual iOS): widgets saem com o fundo padrão do sistema.
 */
enum class WidgetBackgroundMode(val rawValue: String) {
    System("system"),   // Fundo padrão do sistema (claro/escuro)
    Accent("accent"),   // Cor de destaque do app
    Custom("custom");   // Cor personalizada escolhida pelo usuário

    companion object {
        val Default = System
        fun fromRaw(raw: String?): WidgetBackgroundMode = entries.firstOrNull { it.rawValue == raw } ?: Default
    }
}
