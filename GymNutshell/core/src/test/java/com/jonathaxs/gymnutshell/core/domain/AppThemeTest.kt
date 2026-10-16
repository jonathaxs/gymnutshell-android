package com.jonathaxs.gymnutshell.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Trava o mapa tema × tier. São 19 temas × 5 níveis escritos à mão, então o erro provável é
 * copiar/colar (repetir uma chave ou pular um nível) — é isso que os testes de unicidade pegam.
 * O texto em si não é checado aqui: sem Robolectric o @StringRes é só o id.
 */
class AppThemeTest {

    @Test
    fun `cada tema e nivel tem seu proprio nome`() {
        val ids = AppTheme.entries.flatMap { theme ->
            DailyAchievement.entries.map { tier -> theme.tierNameRes(tier) }
        }
        assertEquals(19 * 5, ids.size)
        assertEquals("nome de tier repetido entre tema/nível", ids.size, ids.distinct().size)
    }

    /** Dentro de um tema, os 5 níveis têm que ser nomes diferentes (pega pick() mal preenchido). */
    @Test
    fun `niveis dentro de um tema tem nomes distintos`() {
        AppTheme.entries.forEach { theme ->
            val ids = DailyAchievement.entries.map { theme.tierNameRes(it) }
            assertEquals("tema $theme repete nome entre níveis", 5, ids.distinct().size)
        }
    }

    /** Espelha AppTheme.emoji(for:)/name(for:) do iOS: cada tema tem os 5 emojis distintos. */
    @Test
    fun `cada tema tem cinco preview emojis distintos`() {
        AppTheme.entries.forEach { theme ->
            assertEquals(5, theme.previewEmojis("male").size)
            assertEquals("tema $theme repete emoji entre níveis", 5, theme.previewEmojis("male").distinct().size)
        }
    }

    /** Sexo masculino sempre cai na base — tierNameRes(tier,"male") == tierNameRes(tier). */
    @Test
    fun `sexo masculino usa nomes base`() {
        AppTheme.entries.forEach { theme ->
            DailyAchievement.entries.forEach { tier ->
                assertEquals(theme.tierNameRes(tier), theme.tierNameRes(tier, "male"))
            }
        }
    }

    /** Onde existe forma feminina (ex.: Gym L1: Rooster→Hen), sex == "female" muda o @StringRes; "other" cai na base. */
    @Test
    fun `nome feminino difere onde existe e other usa base masculina`() {
        assertNotEquals(
            AppTheme.Gym.tierNameRes(DailyAchievement.Level1),
            AppTheme.Gym.tierNameRes(DailyAchievement.Level1, "female"),
        )
        // Na 2.0: qualquer sexo diferente de "female" (inclusive "other") usa a base masculina.
        assertEquals(
            AppTheme.Gym.tierNameRes(DailyAchievement.Level1, "male"),
            AppTheme.Gym.tierNameRes(DailyAchievement.Level1, "other"),
        )
    }

    /** Tema sem variante feminina (ex.: Champion) cai na base mesmo com sex != "male". */
    @Test
    fun `temas sem variante feminina caem na base`() {
        DailyAchievement.entries.forEach { tier ->
            assertEquals(
                AppTheme.Champion.tierNameRes(tier),
                AppTheme.Champion.tierNameRes(tier, "female"),
            )
        }
    }

    /** O emoji também tem variante feminina no Gym (Rooster 🐓 → Hen 🐔). */
    @Test
    fun `emoji feminino difere para gym`() {
        assertNotEquals(
            AppTheme.Gym.emoji(DailyAchievement.Level1),
            AppTheme.Gym.emoji(DailyAchievement.Level1, "female"),
        )
        assertEquals(
            AppTheme.Gym.emoji(DailyAchievement.Level1),
            AppTheme.Gym.emoji(DailyAchievement.Level1, "male"),
        )
    }

    @Test
    fun `tema padrao e gym`() {
        assertEquals(AppTheme.Gym, AppTheme.Default)
        assertEquals(AppTheme.Gym, AppTheme.fromRaw("desconhecido"))
        assertEquals(AppTheme.Cat, AppTheme.fromRaw("cat"))
    }

    /** O tema muda o nome do mesmo nível — é o ponto todo da feature. */
    @Test
    fun `tema muda o nome do nivel`() {
        assertNotEquals(
            AppTheme.Gym.tierNameRes(DailyAchievement.Level1),
            AppTheme.Cat.tierNameRes(DailyAchievement.Level1),
        )
    }

    /** O nível de um dia salvo é lido de volta pelo emoji, então nenhum emoji pode se repetir entre temas/níveis. */
    @Test
    fun `emojis dos niveis sao unicos entre todos os temas e niveis`() {
        val all = AppTheme.entries.flatMap { theme -> DailyAchievement.entries.map { theme.emoji(it) } }
        assertEquals(all.size, all.distinct().size)
        for (theme in AppTheme.entries) {
            for (tier in DailyAchievement.entries) {
                assertEquals(tier, DailyAchievement.from(theme.emoji(tier)))
            }
        }
    }
}
