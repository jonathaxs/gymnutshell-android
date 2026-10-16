package com.jonathaxs.gymnutshell.core.domain

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Converte peso, altura e água entre métrico e imperial — porte de UnitConverter (iOS).
 * O app sempre guarda os valores internamente em kg, cm e ml.
 *
 * Onde o iOS devolvia tuplas nomeadas (stones:lbs / feet:inches), aqui usamos Pair:
 * `first` = stones/feet, `second` = lbs/inches (dá pra desestruturar: `val (s, l) = ...`).
 */
object UnitConverter {

    // MARK: Peso (US: libras)
    fun kgToLbs(kg: Double): Double = kg * 2.20462
    fun lbsToKg(lbs: Double): Double = lbs / 2.20462

    // MARK: Peso (UK: stones + libras)
    fun kgToStoneLbs(kg: Double): Pair<Int, Int> {
        val totalLbs = kg * 2.20462
        val stones = (totalLbs / 14).toInt()
        val remainingLbs = totalLbs.roundToInt() - stones * 14
        return stones to remainingLbs
    }

    fun stoneLbsToKg(stones: Int, lbs: Int): Double = (stones * 14 + lbs) / 2.20462

    // MARK: Água

    /** No Reino Unido o "fl oz" é a onça britânica (28,4131 ml); no resto, a americana (29,5735 ml). */
    fun usesImperialFlOz(locale: Locale = Locale.getDefault()): Boolean =
        locale.country.equals("GB", ignoreCase = true)

    val mlPerFlOz: Double get() = mlPerFlOz()
    fun mlPerFlOz(locale: Locale = Locale.getDefault()): Double =
        if (usesImperialFlOz(locale)) 28.4131 else 29.5735

    /** Passo padrão da água em fl oz: 8 (um copo americano) ou 10 (meio pint britânico). */
    val flOzWaterStep: Int get() = flOzWaterStep()
    fun flOzWaterStep(locale: Locale = Locale.getDefault()): Int =
        if (usesImperialFlOz(locale)) 10 else 8

    fun mlToFlOz(ml: Double, locale: Locale = Locale.getDefault()): Double =
        ml / mlPerFlOz(locale)

    fun flOzToMl(flOz: Double, locale: Locale = Locale.getDefault()): Double =
        flOz * mlPerFlOz(locale)

    // MARK: Altura
    fun cmToFeetAndInches(cm: Int): Pair<Int, Int> {
        val totalInches = (cm / 2.54).roundToInt()
        return (totalInches / 12) to (totalInches % 12)
    }

    fun feetAndInchesToCm(feet: Int, inches: Int): Int =
        ((feet * 12 + inches) * 2.54).roundToInt()
}

