package com.subkan.core.model

import java.text.NumberFormat
import java.util.Locale

/**
 * The currencies a subscription can be priced in.
 *
 * Amounts are [Double] rather than a minor-unit [Long] because a yearly price divided by twelve is
 * genuinely fractional, and the app never adds money across currencies — it shows one total per
 * currency side by side. Rounding happens once, at format time.
 */
enum class Currency(val code: String, val symbol: String, val japaneseUnit: String) {
    JPY("JPY", "¥", "円"),
    USD("USD", "$", "ドル"),
    EUR("EUR", "€", "ユーロ"),
    ;

    companion object {
        fun fromCodeOrDefault(code: String?): Currency =
            entries.firstOrNull { it.code == code } ?: JPY
    }
}

/**
 * How an amount is written.
 *
 * [Symbol] is the default because it is the most compact, and compactness is what a dense list
 * needs. [Japanese] exists because 「約4,000円」 is how the amount would actually be said out loud
 * — the estimate marker sits naturally in front of it, where 「約 ¥4,000」 has to work around the
 * symbol also being a prefix.
 */
enum class AmountNotation {
    /** `¥1,480` / `$20` */
    Symbol,

    /** `1,480円` / `20ドル` */
    Japanese,
    ;

    companion object {
        fun fromNameOrDefault(value: String?): AmountNotation =
            entries.firstOrNull { it.name == value } ?: Symbol
    }
}

/**
 * Grouped, whole-unit rendering: `¥1,234` or `1,234円`.
 *
 * Every supported currency is shown with no decimal places, JPY because it has no minor unit and
 * the other two because a subscription list reads better in round numbers than in cents.
 */
fun Currency.format(
    amount: Double,
    notation: AmountNotation = AmountNotation.Symbol,
): String {
    val digits = integerFormat.format(Math.round(amount))
    return when (notation) {
        AmountNotation.Symbol -> symbol + digits
        AmountNotation.Japanese -> digits + japaneseUnit
    }
}

private val integerFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale.JAPAN)

/**
 * One currency's share of the monthly total.
 *
 * [isEstimated] is true when any subscription folded into [amount] was a rough figure, which is
 * what lets the header say 「約¥12,345」 rather than claiming a precision it does not have.
 */
data class CurrencyTotal(
    val currency: Currency,
    val amount: Double,
    val isEstimated: Boolean,
)

/**
 * Per-currency monthly totals, ordered for display.
 *
 * JPY sorts first because it is the default and the overwhelmingly common case for this app's
 * users; everything else follows alphabetically so the order is stable as subscriptions come and go.
 *
 * Subscriptions with no amount entered contribute nothing — they are not treated as zero, and a
 * currency represented only by such subscriptions does not appear at all.
 */
fun monthlyTotals(subscriptions: List<Subscription>): List<CurrencyTotal> =
    subscriptions
        .mapNotNull { sub -> sub.monthlyAmount?.let { sub to it } }
        .groupBy { (sub, _) -> sub.currency }
        .map { (currency, entries) ->
            CurrencyTotal(
                currency = currency,
                amount = entries.sumOf { (_, amount) -> amount },
                isEstimated = entries.any { (sub, _) -> sub.isEstimated },
            )
        }
        .sortedWith(
            compareBy(
                { if (it.currency == Currency.JPY) 0 else 1 },
                { it.currency.code },
            ),
        )
