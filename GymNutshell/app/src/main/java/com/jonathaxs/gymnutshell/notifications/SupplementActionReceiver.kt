package com.jonathaxs.gymnutshell.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.jonathaxs.gymnutshell.core.data.SupplementCheckInRepository
import com.jonathaxs.gymnutshell.wear.PhoneWearSync
import com.jonathaxs.gymnutshell.widget.GymWidgets
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Recebe o toque na ação "Tomei" da notificação de suplemento, marca a meta como feita,
 * cancela a notificação e reagenda os próximos avisos.
 */
class SupplementActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val ids = intent.getStringArrayExtra(EXTRA_SUPPLEMENT_IDS)?.toList().orEmpty()
        val reminderEpochDay = intent.getLongExtra(EXTRA_REMINDER_EPOCH_DAY, LocalDate.now().toEpochDay())
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (notifId >= 0) {
            NotificationManagerCompat.from(context).cancel(notifId)
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                SupplementCheckInRepository(appContext).markDone(
                    ids = ids,
                    reminderEpochDay = reminderEpochDay,
                )
                NotificationScheduler(appContext).rescheduleSupplementReminders()
                GymWidgets.update(appContext)
                PhoneWearSync.push(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_SUPPLEMENT_IDS = "gn.notif.supplementIds"
        const val EXTRA_REMINDER_EPOCH_DAY = "gn.notif.supplementEpochDay"
        const val EXTRA_NOTIFICATION_ID = "gn.notif.notificationId"
    }
}
