package com.jonathaxs.gymnutshell.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakBonusCalculatorTest {

    private fun makeLevels(start: LocalDate, numbers: List<Int>): Map<LocalDate, DailyAchievement> {
        val map = mutableMapOf<LocalDate, DailyAchievement>()
        numbers.forEachIndexed { idx, n ->
            val day = start.plusDays(idx.toLong())
            map[day] = DailyAchievement.entries.first { it.number == n }
        }
        return map
    }

    @Test
    fun `pontos do dia sao o numero do nivel`() {
        assertEquals(listOf(1, 2, 3, 4, 5), DailyAchievement.entries.map { it.points })
    }

    @Test
    fun `semana nivel 4 ou 5 da oito pontos`() {
        // 2026-11-02 é segunda; 2026-11-08 é domingo.
        val levels = makeLevels(LocalDate.of(2026, 11, 2), listOf(4, 5, 4, 5, 5, 4, 5))
        val bonuses = StreakBonusCalculator.bonuses(levels)
        assertEquals(1, bonuses.size)
        assertEquals(StreakBonusKind.WeeklyLevel4, bonuses.first().kind)
        assertEquals(8, bonuses.first().points)
        assertEquals(LocalDate.of(2026, 11, 8), bonuses.first().anchorDate)
    }

    @Test
    fun `semana so nivel 5 da so o bonus maior`() {
        val levels = makeLevels(LocalDate.of(2026, 11, 2), List(7) { 5 })
        val bonuses = StreakBonusCalculator.bonuses(levels)
        assertEquals(listOf(StreakBonusKind.WeeklyLevel5), bonuses.map { it.kind })
        assertEquals(12, bonuses.first().points)
    }

    @Test
    fun `semana com nivel 3 ou dia faltando nao da bonus`() {
        val withThree = makeLevels(LocalDate.of(2026, 11, 2), listOf(5, 5, 3, 5, 5, 5, 5))
        assertTrue(StreakBonusCalculator.bonuses(withThree).isEmpty())

        val missingWednesday = makeLevels(LocalDate.of(2026, 11, 2), List(7) { 5 }).toMutableMap()
        missingWednesday.remove(LocalDate.of(2026, 11, 4))
        assertTrue(StreakBonusCalculator.bonuses(missingWednesday).isEmpty())

        // 7 dias seguidos de domingo a sábado não fecham uma semana segunda..domingo.
        val sunToSat = makeLevels(LocalDate.of(2026, 11, 1), List(7) { 5 })
        assertTrue(StreakBonusCalculator.bonuses(sunToSat).isEmpty())
    }

    @Test
    fun `mes inteiro da bonus mensal alem dos semanais`() {
        // Fevereiro de 2027: 28 dias, começa numa segunda (2027-02-01) e termina num domingo (2027-02-28).
        val levels = makeLevels(LocalDate.of(2027, 2, 1), List(28) { 5 })
        val bonuses = StreakBonusCalculator.bonuses(levels)
        assertEquals(1, bonuses.count { it.kind == StreakBonusKind.MonthlyLevel5 })
        assertEquals(4, bonuses.count { it.kind == StreakBonusKind.WeeklyLevel5 })
        // No dia 28/02 caem o mensal e o semanal; o mensal vem primeiro.
        assertEquals(StreakBonusKind.MonthlyLevel5, bonuses.first().kind)
        assertEquals(33 + 4 * 12, bonuses.sumOf { it.points })

        // Se um único dia do mês foi nível 4, o bônus mensal cai pra 20 pts.
        val mixed = levels.toMutableMap()
        mixed[LocalDate.of(2027, 2, 10)] = DailyAchievement.Level4
        val mixedBonuses = StreakBonusCalculator.bonuses(mixed)
        assertEquals(1, mixedBonuses.count { it.kind == StreakBonusKind.MonthlyLevel4 })
        assertEquals(1, mixedBonuses.count { it.kind == StreakBonusKind.WeeklyLevel4 })
    }

    @Test
    fun `id do bonus eh estavel`() {
        val bonus = EarnedStreakBonus(
            kind = StreakBonusKind.WeeklyLevel5,
            anchorDate = LocalDate.of(2026, 11, 8),
        )
        assertEquals("weekly.level5.2026-11-08", bonus.id)
    }
}
