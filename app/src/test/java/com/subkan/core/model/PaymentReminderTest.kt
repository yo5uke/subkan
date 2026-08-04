package com.subkan.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PaymentReminderTest {

    // --- Recurrence -----------------------------------------------------------------------------

    @Test
    fun `a future payment date is already the next occurrence`() {
        val sub = subscription(nextPaymentDate = LocalDate.of(2026, 9, 5))
        assertEquals(
            LocalDate.of(2026, 9, 5),
            sub.nextOccurrenceOnOrAfter(LocalDate.of(2026, 8, 2)),
        )
    }

    @Test
    fun `today counts as the next occurrence, not a missed one`() {
        val today = LocalDate.of(2026, 8, 5)
        val sub = subscription(nextPaymentDate = today)
        assertEquals(today, sub.nextOccurrenceOnOrAfter(today))
    }

    @Test
    fun `a past monthly date rolls forward to the same day of a later month`() {
        val sub = subscription(nextPaymentDate = LocalDate.of(2026, 1, 5))
        assertEquals(
            LocalDate.of(2026, 9, 5),
            sub.nextOccurrenceOnOrAfter(LocalDate.of(2026, 8, 20)),
        )
    }

    @Test
    fun `rolling forward stays within the current month when the day has not passed`() {
        val sub = subscription(nextPaymentDate = LocalDate.of(2026, 1, 20))
        assertEquals(
            LocalDate.of(2026, 8, 20),
            sub.nextOccurrenceOnOrAfter(LocalDate.of(2026, 8, 2)),
        )
    }

    @Test
    fun `a 31st anchor clamps in short months and then returns to the 31st`() {
        val sub = subscription(nextPaymentDate = LocalDate.of(2026, 1, 31))

        // February 2026 has 28 days.
        assertEquals(
            LocalDate.of(2026, 2, 28),
            sub.nextOccurrenceOnOrAfter(LocalDate.of(2026, 2, 1)),
        )
        // April has 30 — and crucially the anchor has not been dragged back to the 28th.
        assertEquals(
            LocalDate.of(2026, 4, 30),
            sub.nextOccurrenceOnOrAfter(LocalDate.of(2026, 4, 1)),
        )
        assertEquals(
            LocalDate.of(2026, 5, 31),
            sub.nextOccurrenceOnOrAfter(LocalDate.of(2026, 5, 1)),
        )
    }

    @Test
    fun `a past yearly date rolls forward a whole year at a time`() {
        val sub = subscription(
            nextPaymentDate = LocalDate.of(2024, 3, 10),
            cycle = BillingCycle.Yearly,
        )
        assertEquals(
            LocalDate.of(2027, 3, 10),
            sub.nextOccurrenceOnOrAfter(LocalDate.of(2026, 8, 2)),
        )
    }

    @Test
    fun `a 29 February yearly anchor clamps in common years`() {
        val sub = subscription(
            nextPaymentDate = LocalDate.of(2024, 2, 29),
            cycle = BillingCycle.Yearly,
        )
        assertEquals(
            LocalDate.of(2027, 2, 28),
            sub.nextOccurrenceOnOrAfter(LocalDate.of(2026, 8, 2)),
        )
        // 2028 is a leap year, so the anchor is honoured again.
        assertEquals(
            LocalDate.of(2028, 2, 29),
            sub.nextOccurrenceOnOrAfter(LocalDate.of(2027, 3, 1)),
        )
    }

    // --- Selecting what to notify about ---------------------------------------------------------

    @Test
    fun `dueOn picks up recurring subscriptions whose stored date is long past`() {
        val netflix = subscription(id = "netflix", nextPaymentDate = LocalDate.of(2025, 3, 5))
        val spotify = subscription(id = "spotify", nextPaymentDate = LocalDate.of(2025, 3, 6))

        val due = listOf(netflix, spotify).dueOn(LocalDate.of(2026, 9, 5))

        assertEquals(listOf("netflix"), due.map { it.id })
    }

    @Test
    fun `dueOn returns every subscription sharing a date`() {
        val a = subscription(id = "a", nextPaymentDate = LocalDate.of(2026, 8, 15))
        val b = subscription(id = "b", nextPaymentDate = LocalDate.of(2026, 8, 15))
        val c = subscription(id = "c", nextPaymentDate = LocalDate.of(2026, 8, 16))

        val due = listOf(a, b, c).dueOn(LocalDate.of(2026, 8, 15))

        assertEquals(listOf("a", "b"), due.map { it.id })
    }

    @Test
    fun `a yearly plan does not fire on the monthly anniversary of its date`() {
        val sub = subscription(
            nextPaymentDate = LocalDate.of(2026, 3, 10),
            cycle = BillingCycle.Yearly,
        )
        assertEquals(emptyList<Subscription>(), listOf(sub).dueOn(LocalDate.of(2026, 4, 10)))
        assertEquals(listOf(sub), listOf(sub).dueOn(LocalDate.of(2026, 3, 10)))
    }

    // --- Scheduling -----------------------------------------------------------------------------

    @Test
    fun `a time still ahead today fires today`() {
        assertEquals(
            LocalDateTime.of(2026, 8, 2, 20, 0),
            nextDailyTrigger(LocalDateTime.of(2026, 8, 2, 9, 30), LocalTime.of(20, 0)),
        )
    }

    @Test
    fun `a time already past today fires tomorrow`() {
        assertEquals(
            LocalDateTime.of(2026, 8, 3, 8, 0),
            nextDailyTrigger(LocalDateTime.of(2026, 8, 2, 9, 30), LocalTime.of(8, 0)),
        )
    }

    @Test
    fun `rescheduling at the exact moment a reminder fired lands on tomorrow`() {
        // Otherwise the alarm that just went off would immediately re-book itself for now.
        assertEquals(
            LocalDateTime.of(2026, 8, 3, 20, 0),
            nextDailyTrigger(LocalDateTime.of(2026, 8, 2, 20, 0), LocalTime.of(20, 0)),
        )
    }

    @Test
    fun `the evening reminder is about tomorrow and the morning one about today`() {
        val firedOn = LocalDate.of(2026, 8, 2)
        assertEquals(LocalDate.of(2026, 8, 3), ReminderKind.DayBefore.targetDate(firedOn))
        assertEquals(LocalDate.of(2026, 8, 2), ReminderKind.OnDay.targetDate(firedOn))
    }

    @Test
    fun `a wall-clock time round-trips through minute-of-day`() {
        listOf(
            LocalTime.of(0, 0),
            LocalTime.of(8, 0),
            LocalTime.of(20, 0),
            LocalTime.of(21, 45),
            LocalTime.of(23, 59),
        ).forEach { time ->
            assertEquals(time, localTimeFromMinuteOfDay(time.toMinuteOfDay()))
        }

        assertEquals(0, LocalTime.of(0, 0).toMinuteOfDay())
        assertEquals(1_200, LocalTime.of(20, 0).toMinuteOfDay())
        assertEquals(1_439, LocalTime.of(23, 59).toMinuteOfDay())
    }

    @Test
    fun `each reminder kind carries its own switch and time`() {
        val settings = NotificationSettings(
            notifyDayBefore = true,
            dayBeforeTime = LocalTime.of(21, 30),
            notifyOnDay = false,
            onDayTime = LocalTime.of(7, 15),
        )

        assertEquals(true, settings.isEnabled(ReminderKind.DayBefore))
        assertEquals(false, settings.isEnabled(ReminderKind.OnDay))
        assertEquals(LocalTime.of(21, 30), settings.timeOf(ReminderKind.DayBefore))
        assertEquals(LocalTime.of(7, 15), settings.timeOf(ReminderKind.OnDay))
        assertEquals(true, settings.anyEnabled)
        assertEquals(false, NotificationSettings(false, notifyOnDay = false).anyEnabled)
    }
}
