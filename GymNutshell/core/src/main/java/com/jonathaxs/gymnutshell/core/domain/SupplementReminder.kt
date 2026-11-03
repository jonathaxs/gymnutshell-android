package com.jonathaxs.gymnutshell.core.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Horário de um lembrete (hora e minuto, 24h) — porte de ReminderTime (iOS).
 */
class ReminderTime(hour: Int, minute: Int) : Comparable<ReminderTime> {

    val hour: Int = hour.coerceIn(0, 23)
    val minute: Int = minute.coerceIn(0, 59)

    val id: Int get() = this.hour * 60 + this.minute

    /** Texto estável "HH:mm" usado nas preferências e no backup. */
    val storageString: String
        get() = String.format(Locale.US, "%02d:%02d", this.hour, this.minute)

    fun toStorageString(): String = storageString

    fun toLocalTime(): LocalTime = LocalTime.of(this.hour, this.minute)

    /** Data/hora deste lembrete no dia informado. */
    fun atDate(day: LocalDate): LocalDateTime = day.atTime(this.hour, this.minute)

    override fun compareTo(other: ReminderTime): Int = id.compareTo(other.id)

    override fun equals(other: Any?): Boolean =
        other is ReminderTime && hour == other.hour && minute == other.minute

    override fun hashCode(): Int = id

    override fun toString(): String = storageString

    /** Atalho de horário mostrado como chip ao editar os lembretes. */
    enum class Preset(val titleKey: String, val time: ReminderTime) {
        Morning("notifications.supplement.preset.morning", ReminderTime(8, 0)),
        Lunch("notifications.supplement.preset.lunch", ReminderTime(12, 0)),
        Afternoon("notifications.supplement.preset.afternoon", ReminderTime(17, 0)),
        Night("notifications.supplement.preset.night", ReminderTime(20, 0)),
        Bedtime("notifications.supplement.preset.bedtime", ReminderTime(22, 30)),
    }

    companion object {
        /**
         * Manhã, almoço, fim da tarde, noite e antes de dormir.
         * Getter de propósito: guardar em campo cria um ciclo de inicialização com o enum
         * (Preset cria ReminderTime, que inicia o companion antes de Preset terminar) e a lista fica nula.
         */
        val presets: List<Preset> get() = Preset.entries

        /** Converte uma string "HH:mm" em [ReminderTime], ou `null` se for inválida. */
        fun parse(storageString: String): ReminderTime? {
            val parts = storageString.split(":")
            if (parts.size != 2) return null
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            if (h !in 0..23 || m !in 0..59) return null
            return ReminderTime(hour = h, minute = m)
        }

        operator fun invoke(storageString: String): ReminderTime? = parse(storageString)
    }
}

/**
 * Uma meta check-in com seus horários, pronta pra virar avisos.
 */
data class SupplementReminderEntry(
    val id: String,
    val emoji: String,
    val name: String,
    val times: List<ReminderTime>,
    val doneToday: Boolean,
)

/**
 * Aviso a agendar: um horário de um dia, com uma ou várias metas (quando agrupado).
 */
data class PlannedSupplementReminder(
    val identifier: String,
    val fireDate: LocalDateTime,
    val entryIds: List<String>,
    val title: String,
    val body: String,
)

/**
 * Planejador puro dos lembretes por horário de suplementos — porte de SupplementReminderPlanner (iOS).
 */
object SupplementReminderPlanner {

    /** Prefixo dos identifiers dos avisos de suplemento. */
    const val IDENTIFIER_PREFIX = "notif.supplement."

    /** Dias agendados à frente no plano. */
    const val DAYS_AHEAD = 3

    /** Teto de avisos planejados. */
    const val MAX_PENDING = 30

    private val stampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMddHHmm", Locale.US)

    /**
     * Monta os avisos dos próximos dias. Pula horários que já passaram e, hoje,
     * as metas já feitas. Agrupado: um aviso por horário com todas as metas dele.
     * [showEmoji] põe o emoji de cada meta (e o ⏰ do agrupado) no texto do aviso.
     */
    fun plan(
        entries: List<SupplementReminderEntry>,
        now: LocalDateTime = LocalDateTime.now(),
        grouped: Boolean,
        showEmoji: Boolean = false,
        days: Int = DAYS_AHEAD,
        bodyFormat: (String) -> String = { "Hora de tomar $it." },
        groupTitle: String = "Hora do check-in",
    ): List<PlannedSupplementReminder> {
        val today = now.toLocalDate()

        // (data/hora do aviso, meta) de todos os dias, já sem passados e sem feitos hoje.
        val slots = mutableListOf<Pair<LocalDateTime, SupplementReminderEntry>>()
        for (offset in 0 until maxOf(days, 0)) {
            val day = today.plusDays(offset.toLong())
            for (entry in entries) {
                if (offset == 0 && entry.doneToday) continue
                for (time in entry.times) {
                    val date = time.atDate(day)
                    if (date.isAfter(now)) {
                        slots.add(date to entry)
                    }
                }
            }
        }

        val result = if (grouped) {
            slots.groupBy({ it.first }, { it.second }).map { (date, groupEntries) ->
                val sortedGroup = groupEntries.sortedBy { it.name }
                reminder(sortedGroup, date, bodyFormat, groupTitle, showEmoji)
            }
        } else {
            slots.map { (date, entry) ->
                reminder(listOf(entry), date, bodyFormat, groupTitle, showEmoji)
            }
        }

        return result
            .sortedWith(compareBy<PlannedSupplementReminder> { it.fireDate }.thenBy { it.identifier })
            .take(MAX_PENDING)
    }

    private fun reminder(
        group: List<SupplementReminderEntry>,
        date: LocalDateTime,
        bodyFormat: (String) -> String,
        groupTitle: String,
        showEmoji: Boolean,
    ): PlannedSupplementReminder {
        val stamp = date.format(stampFormatter)
        fun label(entry: SupplementReminderEntry) =
            if (showEmoji && entry.emoji.isNotEmpty()) "${entry.emoji} ${entry.name}" else entry.name
        if (group.size == 1) {
            val entry = group.first()
            return PlannedSupplementReminder(
                identifier = "$IDENTIFIER_PREFIX$stamp.${entry.id}",
                fireDate = date,
                entryIds = listOf(entry.id),
                title = label(entry),
                body = bodyFormat(entry.name),
            )
        }
        val names = group.joinToString(", ") { label(it) }
        return PlannedSupplementReminder(
            identifier = "$IDENTIFIER_PREFIX$stamp.group",
            fireDate = date,
            entryIds = group.map { it.id },
            title = if (showEmoji) "⏰ $groupTitle" else groupTitle,
            body = names,
        )
    }
}
