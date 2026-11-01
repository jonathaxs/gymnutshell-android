package com.jonathaxs.gymnutshell.core.domain

import com.jonathaxs.gymnutshell.core.backup.BackupCodec
import com.jonathaxs.gymnutshell.core.backup.BackupPayload
import com.jonathaxs.gymnutshell.core.backup.GoalsSnapshot
import com.jonathaxs.gymnutshell.core.backup.PreferencesSnapshot
import com.jonathaxs.gymnutshell.core.backup.ProfileSnapshot
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testes do planejador e dos horários de lembrete dos suplementos — porte de GymNutshellCoreTests (iOS). */
class SupplementReminderTest {

    private fun dataFixa(dia: Int, hora: Int, minuto: Int): LocalDateTime =
        LocalDateTime.of(2026, 11, dia, hora, minuto)

    private val manha = ReminderTime(hour = 8, minute = 0)
    private val noite = ReminderTime(hour = 22, minute = 30)

    @Test
    fun lembretePulaHorariosQueJaPassaram() {
        val creatina = SupplementReminderEntry(
            id = "creatine",
            emoji = "🧪",
            name = "Creatina",
            times = listOf(manha, noite),
            doneToday = false,
        )
        val plano = SupplementReminderPlanner.plan(
            entries = listOf(creatina),
            now = dataFixa(1, 10, 0),
            grouped = false,
        )
        // Hoje só às 22:30; amanhã e depois, os dois horários.
        assertEquals(5, plano.size)
        assertEquals(dataFixa(1, 22, 30), plano.first().fireDate)
        assertTrue(plano.all { it.fireDate.isAfter(dataFixa(1, 10, 0)) })
    }

    @Test
    fun lembretePulaHojeQuandoJaFeito() {
        val creatina = SupplementReminderEntry(
            id = "creatine",
            emoji = "🧪",
            name = "Creatina",
            times = listOf(noite),
            doneToday = true,
        )
        val plano = SupplementReminderPlanner.plan(
            entries = listOf(creatina),
            now = dataFixa(1, 10, 0),
            grouped = false,
        )
        assertEquals(
            listOf(dataFixa(2, 22, 30), dataFixa(3, 22, 30)),
            plano.map { it.fireDate },
        )
    }

    @Test
    fun lembreteUmAvisoPorSuplementoPorPadrao() {
        val a = SupplementReminderEntry(id = "a", emoji = "🧪", name = "Creatina", times = listOf(manha), doneToday = false)
        val b = SupplementReminderEntry(id = "b", emoji = "🐟", name = "Ômega 3", times = listOf(manha), doneToday = false)
        val plano = SupplementReminderPlanner.plan(
            entries = listOf(a, b),
            now = dataFixa(1, 6, 0),
            grouped = false,
            days = 1,
        )
        assertEquals(2, plano.size)
        assertEquals(2, plano.map { it.identifier }.toSet().size)
    }

    @Test
    fun lembreteAgrupadoJuntaMesmoHorario() {
        val a = SupplementReminderEntry(id = "a", emoji = "🧪", name = "Creatina", times = listOf(manha, noite), doneToday = false)
        val b = SupplementReminderEntry(id = "b", emoji = "🐟", name = "Ômega 3", times = listOf(manha), doneToday = false)
        val plano = SupplementReminderPlanner.plan(
            entries = listOf(a, b),
            now = dataFixa(1, 6, 0),
            grouped = true,
            days = 1,
        )
        assertEquals(2, plano.size)
        assertEquals(listOf("a", "b"), plano[0].entryIds)
        assertEquals("🧪 Creatina, 🐟 Ômega 3", plano[0].body)
        assertEquals(listOf("a"), plano[1].entryIds)
    }

    @Test
    fun lembreteRespeitaOTeto() {
        val horarios = (0 until 24).map { ReminderTime(hour = it, minute = 0) }
        val a = SupplementReminderEntry(id = "a", emoji = "🧪", name = "A", times = horarios, doneToday = false)
        val plano = SupplementReminderPlanner.plan(
            entries = listOf(a),
            now = dataFixa(1, 0, 30),
            grouped = false,
        )
        assertEquals(SupplementReminderPlanner.MAX_PENDING, plano.size)
        assertEquals(plano.map { it.fireDate }.sorted(), plano.map { it.fireDate })
    }

    @Test
    fun horarioDoLembreteIdaEVoltaNoBackup() {
        assertEquals(ReminderTime(hour = 8, minute = 5), ReminderTime.parse("08:05"))
        assertEquals("22:30", ReminderTime(hour = 22, minute = 30).storageString)
        assertNull(ReminderTime.parse("25:00"))

        val payload = BackupPayload(
            version = BackupCodec.CURRENT_VERSION,
            exportedAt = "2026-11-01T10:00:00Z",
            profile = ProfileSnapshot(
                name = "Jonathas",
                height = 0,
                weight = 0.0,
                age = 0,
                sex = "male",
                userGoal = "maintenance",
            ),
            goals = GoalsSnapshot(sleep = 7, water = 3000, protein = 160, carbs = 250, goodFat = 70, fiber = 30),
            goalsOrder = emptyList(),
            customGoals = emptyList(),
            dailyRecords = emptyList(),
            preferences = PreferencesSnapshot(
                notificationTimes = mapOf("creatine" to listOf("08:00", "22:30"), "custom.3" to listOf("12:00")),
                groupSupplements = true,
            ),
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(payload))
        assertEquals(
            mapOf("creatine" to listOf("08:00", "22:30"), "custom.3" to listOf("12:00")),
            decoded.preferences?.notificationTimes,
        )
        assertEquals(true, decoded.preferences?.groupSupplements)
    }

    @Test
    fun atalhosNaoFicamNulosQuandoOEnumIniciaPrimeiro() {
        // Acessar o enum antes da classe reproduz o ciclo de inicialização que deixava a lista nula.
        assertEquals(ReminderTime(8, 0), ReminderTime.Preset.Morning.time)
        assertEquals(5, ReminderTime.presets.size)
        assertEquals(ReminderTime(22, 30), ReminderTime.presets.last().time)
    }
}
