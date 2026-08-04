package com.subkan.core.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class BillingCycle {
    Monthly,
    Yearly,
    ;

    companion object {
        fun fromNameOrDefault(value: String?): BillingCycle =
            entries.firstOrNull { it.name == value } ?: Monthly
    }
}

/**
 * One recurring charge.
 *
 * [cardId] is nullable: deleting a card from the management screen leaves its subscriptions in
 * place with no card, which the list renders as 「不明なカード」. That is a normal state, not a
 * broken row — see `.claude/rules/room-data.md`.
 *
 * [price] is nullable for the same kind of reason. Not every recurring cost has a fixed amount —
 * 光熱費 can be registered by name alone, or with a rough figure marked [isEstimated]. Neither is
 * an incomplete record.
 */
data class Subscription(
    val id: String,
    val name: String,
    /** Null when no amount has been entered. Never 0.0 as a stand-in for "unknown". */
    val price: Double?,
    val nextPaymentDate: LocalDate,
    val cardId: String?,
    /** Whether [price] is a rough figure rather than a fixed amount. */
    val isEstimated: Boolean = false,
    val currency: Currency = Currency.JPY,
    val billingCycle: BillingCycle = BillingCycle.Monthly,
    val createdAt: Long = 0L,
)

/**
 * How much is known about what this costs.
 *
 * Three states rather than a nullable and a boolean at every call site — and it keeps the
 * meaningless fourth combination (no amount, but "estimated") from ever being representable in the
 * UI.
 */
enum class AmountKind {
    /** Registered by name only. */
    Unset,

    /** A rough figure — displayed with a 「約」 prefix. */
    Estimated,
    Confirmed,
}

val Subscription.amountKind: AmountKind
    get() = when {
        price == null -> AmountKind.Unset
        isEstimated -> AmountKind.Estimated
        else -> AmountKind.Confirmed
    }

/**
 * What this subscription costs per month, or null when no amount is known.
 *
 * A yearly plan contributes a twelfth of its price, which is what makes 「月額合計」 comparable
 * across a list mixing both cycles.
 */
val Subscription.monthlyAmount: Double?
    get() = price?.let {
        when (billingCycle) {
            BillingCycle.Monthly -> it
            BillingCycle.Yearly -> it / 12.0
        }
    }

/**
 * How close the next charge is, as three buckets rather than a raw day count.
 *
 * The UI needs a colour as well as a label, and colours belong to the theme — keeping the decision
 * here means `ui/` maps a bucket to a Material role instead of re-deriving the thresholds.
 *
 * There is no "already paid" bucket. Everything here is measured against the *next* charge, which
 * recurrence guarantees is today or later — see [nextChargeDate].
 */
enum class PaymentStatus {
    Today,

    /** Within three days — near enough that the user may want to act. */
    Soon,
    Later,
}

/**
 * The next date this is charged, counting from [today].
 *
 * The same derivation the reminders use, so the countdown badge and the notification can never
 * disagree about when the next payment is. `nextPaymentDate` on its own is an anchor that nothing
 * advances, so reading it directly would leave every past-dated subscription stuck.
 */
fun Subscription.nextChargeDate(today: LocalDate): LocalDate = nextOccurrenceOnOrAfter(today)

/** Days from [today] to the next charge. Never negative. */
fun Subscription.daysUntilPayment(today: LocalDate): Long =
    ChronoUnit.DAYS.between(today, nextChargeDate(today))

fun Subscription.paymentStatus(today: LocalDate): PaymentStatus {
    val days = daysUntilPayment(today)
    return when {
        days == 0L -> PaymentStatus.Today
        days <= 3 -> PaymentStatus.Soon
        else -> PaymentStatus.Later
    }
}
