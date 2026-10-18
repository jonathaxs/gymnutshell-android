package com.jonathaxs.gymnutshell.ui.welcome

import androidx.annotation.StringRes
import com.jonathaxs.gymnutshell.R

/**
 * Opções de exercício favorito para o perfil — porte de FavoriteExercise (iOS).
 * Começa em "Não sei" (unknown) e é salvo em `profile.favoriteExercise`.
 */
enum class FavoriteExercise(
    val rawValue: String,
    @StringRes val labelRes: Int,
) {
    Unknown("unknown", R.string.favorite_exercise_unknown),
    Back("back", R.string.favorite_exercise_back),
    Arms("arms", R.string.favorite_exercise_arms),
    Chest("chest", R.string.favorite_exercise_chest),
    Legs("legs", R.string.favorite_exercise_legs),
    Hamstrings("hamstrings", R.string.favorite_exercise_hamstrings),
    Glutes("glutes", R.string.favorite_exercise_glutes),
    Shoulders("shoulders", R.string.favorite_exercise_shoulders),
    Biceps("biceps", R.string.favorite_exercise_biceps),
    Triceps("triceps", R.string.favorite_exercise_triceps),
    Abs("abs", R.string.favorite_exercise_abs),
    Traps("traps", R.string.favorite_exercise_traps),
    Forearms("forearms", R.string.favorite_exercise_forearms),
    Calves("calves", R.string.favorite_exercise_calves);

    companion object {
        fun fromRaw(raw: String?): FavoriteExercise =
            entries.firstOrNull { it.rawValue == raw } ?: Unknown
    }
}
