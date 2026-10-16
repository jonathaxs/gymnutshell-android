package com.jonathaxs.gymnutshell.core.domain

import java.util.Locale

/**
 * Sistema de medida — porte de MeasurementSystem (iOS).
 * Apenas Metric e Us (UK removido; "uk" legado vira Metric).
 * Afeta a unidade de exibição da água (ml ↔ fl oz); o armazenamento interno é sempre métrico (ml).
 */
enum class MeasurementSystem(val rawValue: String) {
    Metric("metric"),
    Us("us");

    companion object {
        const val STORAGE_KEY = "profile.measurementSystem"
        val Default = Metric

        fun fromRaw(raw: String?): MeasurementSystem = when (raw) {
            "us" -> Us
            "uk" -> Metric // 2.0: UK legado vira métrico
            else -> Metric
        }

        fun normalized(raw: String?): MeasurementSystem = fromRaw(raw)

        /**
         * Detecta o default pelo Locale do aparelho.
         * Padrão segue imperial só no sistema americano (US).
         */
        fun detectDefault(locale: Locale = Locale.getDefault()): MeasurementSystem {
            // EUA, Liberia e Mianmar usam o sistema americano.
            val country = locale.country.uppercase()
            return if (country == "US" || country == "LR" || country == "MM") Us else Metric
        }

        /**
         * Se o usuário precisa escolher unidades no Welcome.
         * Verdadeiro quando o sistema do país não é métrico por locale (US e UK veem a etapa).
         */
        fun needsChoice(locale: Locale = Locale.getDefault()): Boolean {
            val country = locale.country.uppercase()
            return country == "US" || country == "GB" || country == "LR" || country == "MM"
        }
    }
}

