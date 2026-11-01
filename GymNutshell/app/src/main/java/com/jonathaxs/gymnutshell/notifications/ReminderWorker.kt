package com.jonathaxs.gymnutshell.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jonathaxs.gymnutshell.core.domain.NotificationKind
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Worker do WorkManager que posta um lembrete e reagenda o próximo (cadeia auto-renovável).
 * Porte do disparo de notificações de intervalo (iOS) e dos lembretes por horário de suplementos:
 * reavalia a cada disparo se a meta ainda está pendente e ligada antes de postar e encadear o próximo.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scheduler = NotificationScheduler(applicationContext)
        val notifier = GymNotifier(applicationContext)

        val kindRaw = inputData.getString(KEY_KIND)
        val customId = inputData.getLong(KEY_CUSTOM_ID, -1L)
        val supplementTargetRaw = inputData.getString(KEY_SUPPLEMENTS_TARGET)

        when {
            supplementTargetRaw != null -> {
                val target = runCatching { LocalDateTime.parse(supplementTargetRaw) }.getOrNull()
                    ?: LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
                if (target.toLocalDate() == LocalDate.now()) {
                    val due = scheduler.planSupplementReminders(
                        now = target.minusSeconds(1),
                        days = 1,
                    ).filter {
                        it.fireDate.truncatedTo(ChronoUnit.MINUTES) == target.truncatedTo(ChronoUnit.MINUTES)
                    }
                    for (item in due) {
                        notifier.postSupplementReminder(item)
                    }
                }
                scheduler.rescheduleSupplementReminders(after = target)
            }
            kindRaw != null -> {
                val kind = NotificationKind.fromRaw(kindRaw) ?: return Result.success()
                if (scheduler.shouldRemind(kind)) {
                    notifier.postReminder(kind)
                    scheduler.scheduleNext(kind) // encadeia o próximo
                }
            }
            customId >= 0 -> {
                if (scheduler.shouldRemindCustom(customId)) {
                    scheduler.customGoal(customId)?.let { notifier.postCustomReminder(it) }
                    scheduler.scheduleNextCustom(customId)
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val KEY_KIND = "kind"
        const val KEY_CUSTOM_ID = "customId"
        const val KEY_SUPPLEMENTS_TARGET = "supplementsTarget"
    }
}
