package com.jonathaxs.gymnutshell.core.ml

import com.jonathaxs.gymnutshell.core.data.DailyRecord
import java.time.LocalDate

/**
 * Um dia do histórico, só com os campos usados pelo modelo de "dia de risco" e pelas médias.
 * Fica separado do [DailyRecord] pra dar pra testar sem banco de dados.
 */
data class TrainingDay(
    val date: LocalDate,
    val water: Int,
    val protein: Int,
    val carbs: Int,
    val goodFat: Int,
    val fiber: Int,
    val sleep: Int,
    val didWorkout: Boolean,
    val didCardio: Boolean,
    val workoutRestDay: Boolean,
    val cardioRestDay: Boolean,
    val percent: Int,
    val points: Int,
)

/** Converte um [DailyRecord] salvo no Room em [TrainingDay]. */
fun DailyRecord.toTrainingDay(): TrainingDay = TrainingDay(
    date = LocalDate.ofEpochDay(date),
    water = water,
    protein = protein,
    carbs = carbs,
    goodFat = goodFat,
    fiber = fiber,
    sleep = sleep,
    didWorkout = didWorkout,
    didCardio = didCardio,
    workoutRestDay = workoutRestDay,
    cardioRestDay = cardioRestDay,
    percent = percent,
    points = points,
)
