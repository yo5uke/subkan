package com.subkan

import android.app.Application
import com.subkan.data.reminder.ReminderNotifier
import com.subkan.data.reminder.ReminderScheduler
import com.subkan.data.repository.PaymentCardRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SubKanApplication : Application() {

    @Inject lateinit var paymentCardRepository: PaymentCardRepository

    @Inject lateinit var notifier: ReminderNotifier

    @Inject lateinit var scheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()

        // Created up front rather than on the first reminder, so the channel is already there for
        // the user to configure in system settings before anything has fired.
        notifier.ensureChannel()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            // A subscription cannot be saved without a card, so a fresh install that opened on an
            // empty card list would present a form the user cannot complete. Seeding here rather
            // than in a Room callback keeps it visible and testable; the list is a Flow, so the
            // tabs appear as soon as the insert lands.
            paymentCardRepository.seedDefaultsIfEmpty()

            // The safety net for what no broadcast tells us about — chiefly a force-stop, which
            // throws away every booked alarm and gives the app no chance to react until it is
            // next opened.
            scheduler.rescheduleAll()
        }
    }
}
