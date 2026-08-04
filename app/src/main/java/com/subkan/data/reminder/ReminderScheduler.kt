package com.subkan.data.reminder

import com.subkan.core.model.ReminderKind

/**
 * Books the two daily reminder alarms.
 *
 * Deliberately *two* alarms — one per [ReminderKind] — rather than one per subscription. The alarm
 * carries no payload; when it fires it asks the database what is due. That means adding, editing or
 * deleting a subscription needs no rescheduling at all, and several subscriptions falling on the
 * same day produce one notification instead of five.
 */
interface ReminderScheduler {

    /** Books each enabled reminder for its next occurrence and cancels the disabled ones. */
    suspend fun rescheduleAll()

    fun cancel(kind: ReminderKind)
}
