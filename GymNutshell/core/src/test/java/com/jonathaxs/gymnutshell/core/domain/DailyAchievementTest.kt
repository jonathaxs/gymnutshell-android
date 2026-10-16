package com.jonathaxs.gymnutshell.core.domain

import com.jonathaxs.gymnutshell.core.data.DailyRecord
import com.jonathaxs.gymnutshell.core.data.normalizeAchievement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyAchievementTest {

    @Test
    fun `from progress mapeia os tiers nos mesmos limites do iOS`() {
        assertEquals(DailyAchievement.Level1, DailyAchievement.from(0.0))
        assertEquals(DailyAchievement.Level1, DailyAchievement.from(0.20))
        assertEquals(DailyAchievement.Level1, DailyAchievement.from(0.209)) // floors para 20%
        assertEquals(DailyAchievement.Level2, DailyAchievement.from(0.21))
        assertEquals(DailyAchievement.Level2, DailyAchievement.from(0.40))
        assertEquals(DailyAchievement.Level2, DailyAchievement.from(0.409)) // floors para 40%
        assertEquals(DailyAchievement.Level3, DailyAchievement.from(0.41))
        assertEquals(DailyAchievement.Level3, DailyAchievement.from(0.60))
        assertEquals(DailyAchievement.Level3, DailyAchievement.from(0.609)) // floors para 60%
        assertEquals(DailyAchievement.Level4, DailyAchievement.from(0.61))
        assertEquals(DailyAchievement.Level4, DailyAchievement.from(0.80))
        assertEquals(DailyAchievement.Level4, DailyAchievement.from(0.809)) // floors para 80%
        assertEquals(DailyAchievement.Level5, DailyAchievement.from(0.81))
        assertEquals(DailyAchievement.Level5, DailyAchievement.from(1.0))
    }

    @Test
    fun `pontos de cada tier batem com o iOS`() {
        assertEquals(0, DailyAchievement.Level1.points)
        assertEquals(25, DailyAchievement.Level2.points)
        assertEquals(50, DailyAchievement.Level3.points)
        assertEquals(75, DailyAchievement.Level4.points)
        assertEquals(100, DailyAchievement.Level5.points)
    }

    @Test
    fun `faixas dos cinco niveis`() {
        assertEquals(DailyAchievement.Level1, DailyAchievement.fromPercent(0))
        assertEquals(DailyAchievement.Level1, DailyAchievement.fromPercent(20))
        assertEquals(DailyAchievement.Level2, DailyAchievement.fromPercent(21))
        assertEquals(DailyAchievement.Level2, DailyAchievement.fromPercent(40))
        assertEquals(DailyAchievement.Level3, DailyAchievement.fromPercent(41))
        assertEquals(DailyAchievement.Level3, DailyAchievement.fromPercent(60))
        assertEquals(DailyAchievement.Level4, DailyAchievement.fromPercent(61))
        assertEquals(DailyAchievement.Level4, DailyAchievement.fromPercent(80))
        assertEquals(DailyAchievement.Level5, DailyAchievement.fromPercent(81))
        assertEquals(DailyAchievement.Level5, DailyAchievement.fromPercent(100))
        assertEquals(DailyAchievement.Level5, DailyAchievement.from(1.0))

        assertEquals("21 – 40%", DailyAchievement.Level2.percentRange)
        assertNull(DailyAchievement.Level5.next)
        assertEquals(DailyAchievement.Level2, DailyAchievement.Level1.next)
        assertEquals(DailyAchievement.Level5, DailyAchievement.Level4.next)
    }

    @Test
    fun `registro antigo reclassificado pela porcentagem mantem o tema`() {
        // Dia antigo com 70%: era nível 3, nas faixas novas é nível 4 do mesmo tema (Dog).
        val record = DailyRecord(
            date = 100L,
            water = 0,
            protein = 0,
            carbs = 0,
            goodFat = 0,
            fiber = 0,
            sleep = 0,
            percent = 70,
            achievementTitle = "",
            achievementEmoji = AppTheme.Dog.emoji(DailyAchievement.Level3),
            points = 60,
        )
        val normalized = record.normalizeAchievement()
        assertEquals(AppTheme.Dog.emoji(DailyAchievement.Level4), normalized.achievementEmoji)
        assertEquals(DailyAchievement.Level4.points, normalized.points)
    }
}
