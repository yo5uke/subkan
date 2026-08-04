package com.subkan.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.subkan.core.model.ReminderKind
import com.subkan.core.model.dueOn
import com.subkan.core.model.targetDate
import com.subkan.core.time.AppClock
import com.subkan.data.preferences.SettingsRepository
import com.subkan.data.repository.SubscriptionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires one daily reminder.
 *
 * The alarm carries only which [ReminderKind] it is; what is *due* is looked up here, at fire time.
 * That is why editing a subscription never needs to touch the alarms.
 *
 * Rescheduling happens at the end, unconditionally — an alarm that fires without booking the next
 * one stops the whole feature after a single day.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var subscriptionRepository: SubscriptionRepository

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var notifier: ReminderNotifier

    @Inject lateinit var scheduler: ReminderScheduler

    @Inject lateinit var clock: AppClock

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { name -> ReminderKind.entries.firstOrNull { it.name == name } }
            ?: return

        // onReceive must return promptly, so the work is handed to a coroutine and the broadcast is
        // kept alive with goAsync() until it finishes.
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val settings = settingsRepository.settings.first()
                if (settings.notifications.isEnabled(kind)) {
                    val due = subscriptionRepository.observeAll().first()
                        .dueOn(kind.targetDate(clock.today()))
                    notifier.notifyDue(kind, due, settings.showEstimatePrefix, settings.amountNotation)
                }
                scheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.subkan.action.FIRE_REMINDER"
        const val EXTRA_KIND = "kind"
    }
}
