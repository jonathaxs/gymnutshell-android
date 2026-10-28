package com.jonathaxs.gymnutshell.ui.welcome

import androidx.annotation.StringRes
import com.jonathaxs.gymnutshell.R

/**
 * Etapas do onboarding do Welcome 2.0 — porte de WelcomeStep (iOS).
 * Ordem: início → sobre você → objetivo → medidas (opcional) → metas → controles → tema → tudo pronto.
 */
enum class WelcomeStep(
    val emoji: String,
    @StringRes val titleRes: Int,
) {
    Start("🐿️", R.string.welcome_start_title),
    About("👤", R.string.welcome_step_about_title),
    Goal("🎯", R.string.welcome_goal_title),
    Units("📏", R.string.welcome_step_units_title),
    Summary("💪", R.string.welcome_summary_title),
    ControlStyle("🎛️", R.string.welcome_step_control_title),
    Theme("🎭", R.string.welcome_step_theme_title),
    Ready("🚀", R.string.welcome_step_ready_title);

    companion object {
        fun visible(includeUnits: Boolean): List<WelcomeStep> =
            entries.filter { it != Units || includeUnits }
    }
}
