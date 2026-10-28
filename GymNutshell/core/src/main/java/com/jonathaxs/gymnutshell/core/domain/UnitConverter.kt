package com.jonathaxs.gymnutshell.core.domain

import java.util.Locale

/**
 * Converte volume de água entre métrico (ml) e imperial (fl oz) — porte de UnitConverter (iOS).
 * O app sempre guarda os valores internamente em ml.
 */
object UnitConverter {

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
}

