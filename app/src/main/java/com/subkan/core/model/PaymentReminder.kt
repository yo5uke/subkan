package com.subkan.core.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** The two reminders a payment can raise. Each is enabled and timed independently. */
enum class ReminderKind {
    /** The evening before — 「明日は◯◯の支払日です」. */
    DayBefore,

    /** The morning of — 「今日は◯◯の支払日です」. */
    OnDay,
}

data class NotificationSettings(
    val notifyDayBefore: Boolean = true,
    val dayBeforeTime: LocalTime = LocalTime.of(20, 0),
    val notifyOnDay: Boolean = true,
    val onDayTime: LocalTime = LocalTime.of(8, 0),
) {
    fun isEnabled(kind: ReminderKind): Boolean = when (kind) {
        ReminderKind.DayBefore -> notifyDayBefore
        ReminderKind.OnDay -> notifyOnDay
    }

    fun timeOf(kind: ReminderKind): LocalTime = when (kind) {
        ReminderKind.DayBefore -> dayBeforeTime
        ReminderKind.OnDay -> onDayTime
    }

    val anyEnabled: Boolean get() = notifyDayBefore || notifyOnDay
}

/**
 * The next date this subscription is actually charged, on or after [from].
 *
 * `nextPaymentDate` is a *stored anchor*, not a running value — nothing in the app advances it once
 * the date passes. Reminders therefore derive the real next occurrence by rolling the anchor
 * forward by the billing cycle. Without this, every subscription would notify once and then never
 * again, because its stored date would sit permanently in the past.
 *
 * Nothing is written back. The stored anchor stays put, which is what keeps 「毎月5日」 saying the
 * 5th forever.
 *
 * Short months clamp rather than overflow: an anchor on the 31st is charged on the 30th in
 * November and on the 28th in February, and then returns to the 31st. Rolling with
 * `plusMonths` alone would permanently drag the anchor back to the 28th after one February.
 */
fun Subscription.nextOccurrenceOnOrAfter(from: LocalDate): LocalDate {
    if (nextPaymentDate >= from) return nextPaymentDate

    val anchorDay = nextPaymentDate.dayOfMonth
    return when (billingCycle) {
        BillingCycle.Monthly -> {
            val elapsed = ChronoUnit.MONTHS.between(
                YearMonth.from(nextPaymentDate),
                YearMonth.from(from),
            )
            val candidate = monthlyOccurrence(nextPaymentDate, elapsed, anchorDay)
            // The anchor day may already have passed within `from`'s own month.
            if (candidate >= from) {
                candidate
            } else {
                monthlyOccurrence(nextPaymentDate, elapsed + 1, anchorDay)
            }
        }

        BillingCycle.Yearly -> {
            val elapsed = ChronoUnit.YEARS.between(
                nextPaymentDate.withDayOfMonth(1),
                from.withDayOfMonth(1),
            )
            val candidate = yearlyOccurrence(nextPaymentDate, elapsed)
            if (candidate >= from) candidate else yearlyOccurrence(nextPaymentDate, elapsed + 1)
        }
    }
}

private fun monthlyOccurrence(anchor: LocalDate, monthsLater: Long, anchorDay: Int): LocalDate {
    val month = YearMonth.from(anchor).plusMonths(monthsLater)
    return month.atDay(minOf(anchorDay, month.lengthOfMonth()))
}

/** 29 February clamps to the 28th in common years, the same way short months do. */
private fun yearlyOccurrence(anchor: LocalDate, yearsLater: Long): LocalDate {
    val year = anchor.year + yearsLater.toInt()
    val month = YearMonth.of(year, anchor.month)
    return month.atDay(minOf(anchor.dayOfMonth, month.lengthOfMonth()))
}

/** The subscriptions charged on [date], accounting for recurrence. */
fun List<Subscription>.dueOn(date: LocalDate): List<Subscription> =
    filter { it.nextOccurrenceOnOrAfter(date) == date }

/**
 * The next moment a daily reminder set for [time] should fire, strictly after [now].
 *
 * Strictly: rescheduling immediately after a reminder fires must land on tomorrow, not re-fire the
 * one that just went off.
 */
fun nextDailyTrigger(now: LocalDateTime, time: LocalTime): LocalDateTime {
    val today = now.toLocalDate().atTime(time)
    return if (today > now) today else today.plusDays(1)
}

/**
 * Reminder times are persisted as minute-of-day, for the same reason payment dates are persisted as
 * day numbers: 20:00 is a wall-clock time the user chose, not an instant. Storing it this way keeps
 * it at 20:00 across a timezone change.
 */
fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

fun localTimeFromMinuteOfDay(minuteOfDay: Int): LocalTime =
    LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

/**
 * The date a reminder of [kind] firing at [firedOn] is talking about.
 *
 * The evening reminder is about tomorrow; the morning one is about today.
 */
fun ReminderKind.targetDate(firedOn: LocalDate): LocalDate = when (this) {
    ReminderKind.DayBefore -> firedOn.plusDays(1)
    ReminderKind.OnDay -> firedOn
}
