package com.jonathaxs.gymnutshell.core.domain

import kotlin.math.floor

/**
 * Níveis de conquista diária — porte de DailyAchievement (iOS/GymNutshellCore).
 * Define a lógica dos 5 níveis de conquista diária (faixas de 20%, emoji, pontos).
 */
enum class DailyAchievement {
    Level1,
    Level2,
    Level3,
    Level4,
    Level5;

    /** Número do nível (1..5). */
    val number: Int get() = ordinal + 1

    /** % mínima pra chegar neste nível. */
    val minPercent: Int
        get() = when (this) {
            Level1 -> 0
            Level2 -> 21
            Level3 -> 41
            Level4 -> 61
            Level5 -> 81
        }

    /** % máxima deste nível. */
    val maxPercent: Int
        get() = when (this) {
            Level5 -> 100
            else -> entries[ordinal + 1].minPercent - 1
        }

    /** Faixa pra exibição, ex.: "21 – 40%". */
    val percentRange: String get() = "$minPercent – $maxPercent%"

    /** Próximo nível (null no nível 5). */
    val next: DailyAchievement?
        get() = if (ordinal + 1 < entries.size) entries[ordinal + 1] else null

    /** Pontos que o tier vale — valores idênticos ao iOS (0/25/50/75/100). */
    val points: Int
        get() = when (this) {
            Level1 -> 0
            Level2 -> 25
            Level3 -> 50
            Level4 -> 75
            Level5 -> 100
        }

    /**
     * Emoji do tier no tema padrão Gato (cat) — espelha o DailyAchievement.swift do iOS.
     */
    val emoji: String
        get() = when (this) {
            Level1 -> "🐱"
            Level2 -> "🐈"
            Level3 -> "🐆"
            Level4 -> "🦁"
            Level5 -> "🐅"
        }

    companion object {
        /**
         * Converte um progresso normalizado (0..1) no tier correspondente.
         * Usa a % inteira arredondada para baixo.
         */
        fun from(progress: Double): DailyAchievement =
            fromPercent(floor(progress * 100).toInt())

        /**
         * Faixas de 20%, as mesmas cores do anel: 0–20, 21–40, 41–60, 61–80 e 81–100.
         */
        fun fromPercent(percent: Int): DailyAchievement = when {
            percent <= 20 -> Level1
            percent <= 40 -> Level2
            percent <= 60 -> Level3
            percent <= 80 -> Level4
            else -> Level5
        }

        /**
         * Converte um emoji armazenado de volta pro tier DailyAchievement.
         * Nenhum emoji se repete entre temas/níveis.
         */
        fun from(emoji: String): DailyAchievement {
            for (theme in AppTheme.entries) {
                for (tier in entries) {
                    if (theme.emoji(tier) == emoji) {
                        return tier
                    }
                }
            }
            return Level1
        }
    }
}
