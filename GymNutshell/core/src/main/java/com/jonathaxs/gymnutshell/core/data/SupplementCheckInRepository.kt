package com.jonathaxs.gymnutshell.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.jonathaxs.gymnutshell.core.domain.CategoryKind
import com.jonathaxs.gymnutshell.core.domain.DefaultGoals
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * Marca metas check-in como feitas a partir da ação "Tomei" da notificação — porte de SupplementCheckIn (iOS).
 * Se a Hoje já está no dia do aviso, grava direto no [IntakeRepository]. Se o app ainda não virou o dia,
 * guarda como pendente e a Hoje aplica logo depois de fechar o dia anterior.
 */
class SupplementCheckInRepository(private val context: Context) {

    private val intakeRepository = IntakeRepository(context)
    private val goalConfigRepository = GoalConfigRepository(context)
    private val customGoalRepository = CustomGoalRepository(context)

    private val pendingDayKey = longPreferencesKey("notifications.supplements.pendingDay")
    private val pendingIdsKey = stringSetPreferencesKey("notifications.supplements.pendingIds")

    suspend fun markDone(
        ids: List<String>,
        reminderEpochDay: Long,
        nowEpochDay: Long = LocalDate.now().toEpochDay(),
    ) {
        if (ids.isEmpty()) return
        val lastActiveDay = intakeRepository.lastActiveDay()
        if (lastActiveDay == reminderEpochDay) {
            apply(ids)
        } else if (reminderEpochDay == nowEpochDay) {
            context.appPreferences.edit { prefs ->
                val existing = if (prefs[pendingDayKey] == reminderEpochDay) {
                    prefs[pendingIdsKey] ?: emptySet()
                } else {
                    emptySet()
                }
                prefs[pendingDayKey] = reminderEpochDay
                prefs[pendingIdsKey] = existing + ids
            }
        }
        // Aviso de um dia que já passou: ignora, não marca o dia errado.
    }

    /** Chamado pela Hoje depois de virar o dia: aplica o que ficou pendente pra hoje e limpa o resto. */
    suspend fun applyPending(todayEpochDay: Long = LocalDate.now().toEpochDay()) {
        val prefs = context.appPreferences.data.first()
        val pendingDay = prefs[pendingDayKey]
        val pendingIds = prefs[pendingIdsKey]
        if (pendingDay == null && pendingIds.isNullOrEmpty()) return

        context.appPreferences.edit {
            it.remove(pendingDayKey)
            it.remove(pendingIdsKey)
        }

        if (intakeRepository.lastActiveDay() == todayEpochDay &&
            pendingDay == todayEpochDay &&
            !pendingIds.isNullOrEmpty()
        ) {
            apply(pendingIds.toList())
        }
    }

    suspend fun pendingIds(epochDay: Long): Set<String> {
        val prefs = context.appPreferences.data.first()
        return if (prefs[pendingDayKey] == epochDay) {
            prefs[pendingIdsKey] ?: emptySet()
        } else {
            emptySet()
        }
    }

    // Grava o valor de "Feito" (a dose da meta), igual ao seletor da Hoje.
    private suspend fun apply(ids: List<String>) {
        val creatineGoal =
            goalConfigRepository.valueOverrides.first()[CREATINE_STORAGE_KEY] ?: DefaultGoals.CREATINE
        val customGoals = customGoalRepository.all()
        for (id in ids) {
            if (id == NotificationPreferencesRepository.CREATINE_REMINDER_ID) {
                intakeRepository.setIntake(
                    CREATINE_STORAGE_KEY,
                    CategoryKind.checkInValue(done = true, goal = creatineGoal),
                )
            } else {
                val goal = customGoals.firstOrNull {
                    NotificationPreferencesRepository.customReminderId(it.id) == id
                }
                if (goal != null) {
                    intakeRepository.setIntake(
                        goal.intakeKey,
                        CategoryKind.checkInValue(done = true, goal = goal.target),
                    )
                }
            }
        }
    }

    companion object {
        const val CREATINE_STORAGE_KEY = "tracking.creatine"
    }
}
