package com.subkan.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.subkan.core.model.ReminderKind
import com.subkan.core.model.nextDailyTrigger
import com.subkan.core.time.AppClock
import com.subkan.data.preferences.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val clock: AppClock,
) : ReminderScheduler {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override suspend fun rescheduleAll() {
        val settings = settingsRepository.settings.first().notifications

        ReminderKind.entries.forEach { kind ->
            if (settings.isEnabled(kind)) {
                schedule(kind, settings.timeOf(kind))
            } else {
                cancel(kind)
            }
        }
    }

    private fun schedule(kind: ReminderKind, time: java.time.LocalTime) {
        val alarms = alarmManager ?: return
        val triggerAt = nextDailyTrigger(clock.now(), time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // setAndAllowWhileIdle, not setExactAndAllowWhileIdle: an exact alarm would need the
        // SCHEDULE_EXACT_ALARM permission, which is a second system prompt and is policy-restricted
        // on Play. A payment reminder is fine landing a few minutes either side of 20:00, and this
        // variant still fires in Doze.
        alarms.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(kind, mutable = false),
        )
    }

    override fun cancel(kind: ReminderKind) {
        alarmManager?.cancel(pendingIntent(kind, mutable = false))
    }

    private fun pendingIntent(kind: ReminderKind, mutable: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_KIND, kind.name)
        }
        return PendingIntent.getBroadcast(
            context,
            // One stable request code per kind, so rebooking replaces the previous alarm rather
            // than stacking a second one on top of it.
            kind.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

internal val ReminderKind.requestCode: Int
    get() = when (this) {
        ReminderKind.DayBefore -> 1001
        ReminderKind.OnDay -> 1002
    }
