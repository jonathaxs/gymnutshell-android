package com.jonathaxs.gymnutshell.core.domain

/**
 * Modo de visualização das metas na Today (lista ou grade com balão).
 * Porte de TodayGoalsViewMode (iOS).
 */
enum class TodayGoalsViewMode(val rawValue: String) {
    List("list"),
    Grid("grid");

    companion object {
        const val STORAGE_KEY = "today.goals.viewMode"
        val Default = List

        fun fromRaw(raw: String?): TodayGoalsViewMode =
            entries.firstOrNull { it.rawValue == raw } ?: Default
    }
}
