package com.subkan.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MonthlyTotalsTest {

    @Test
    fun `monthly plan contributes its full price`() {
        val sub = subscription(price = 980.0, cycle = BillingCycle.Monthly)
        assertEquals(980.0, sub.monthlyAmount!!, 0.001)
    }

    @Test
    fun `yearly plan contributes a twelfth`() {
        val sub = subscription(price = 12_000.0, cycle = BillingCycle.Yearly)
        assertEquals(1_000.0, sub.monthlyAmount!!, 0.001)
    }

    @Test
    fun `a subscription with no amount contributes nothing`() {
        assertNull(subscription(price = null).monthlyAmount)
        assertEquals(AmountKind.Unset, subscription(price = null).amountKind)
    }

    @Test
    fun `totals are grouped per currency, never summed across them`() {
        val totals = monthlyTotals(
            listOf(
                subscription(price = 1_000.0, currency = Currency.JPY),
                subscription(price = 500.0, currency = Currency.JPY),
                subscription(price = 10.0, currency = Currency.USD),
            ),
        )

        assertEquals(2, totals.size)
        assertEquals(Currency.JPY, totals[0].currency)
        assertEquals(1_500.0, totals[0].amount, 0.001)
        assertEquals(Currency.USD, totals[1].currency)
        assertEquals(10.0, totals[1].amount, 0.001)
    }

    @Test
    fun `JPY sorts first, everything else alphabetically`() {
        val totals = monthlyTotals(
            listOf(
                subscription(price = 5.0, currency = Currency.USD),
                subscription(price = 5.0, currency = Currency.EUR),
                subscription(price = 5.0, currency = Currency.JPY),
            ),
        )

        assertEquals(listOf(Currency.JPY, Currency.EUR, Currency.USD), totals.map { it.currency })
    }

    @Test
    fun `mixed cycles are comparable in one total`() {
        val totals = monthlyTotals(
            listOf(
                subscription(price = 1_200.0, cycle = BillingCycle.Yearly),
                subscription(price = 900.0, cycle = BillingCycle.Monthly),
            ),
        )

        assertEquals(1_000.0, totals.single().amount, 0.001)
    }

    @Test
    fun `no subscriptions means no totals rather than a zero row`() {
        assertEquals(emptyList<CurrencyTotal>(), monthlyTotals(emptyList()))
    }

    @Test
    fun `amountless subscriptions are skipped, not counted as zero`() {
        val totals = monthlyTotals(
            listOf(
                subscription(price = 1_000.0),
                subscription(price = null),
            ),
        )

        assertEquals(1_000.0, totals.single().amount, 0.001)
    }

    @Test
    fun `a currency represented only by amountless entries does not appear`() {
        val totals = monthlyTotals(
            listOf(
                subscription(price = 1_000.0, currency = Currency.JPY),
                subscription(price = null, currency = Currency.USD),
            ),
        )

        assertEquals(listOf(Currency.JPY), totals.map { it.currency })
    }

    @Test
    fun `one estimate makes the whole currency total an estimate`() {
        val totals = monthlyTotals(
            listOf(
                subscription(price = 1_000.0),
                subscription(price = 5_000.0, isEstimated = true),
            ),
        )

        // The header says 約¥6,000 — claiming ¥6,000 exactly would be a precision it does not have.
        assertEquals(true, totals.single().isEstimated)
        assertEquals(6_000.0, totals.single().amount, 0.001)
    }

    @Test
    fun `a total built only from confirmed amounts is not an estimate`() {
        val totals = monthlyTotals(listOf(subscription(price = 1_000.0)))
        assertEquals(false, totals.single().isEstimated)
    }

    @Test
    fun `formatting groups thousands and drops the minor unit`() {
        assertEquals("¥1,480", Currency.JPY.format(1_480.0))
        assertEquals("$10", Currency.USD.format(9.99))
        assertEquals("€1,000", Currency.EUR.format(1_000.0))
    }

    @Test
    fun `Japanese notation puts the unit after the number`() {
        assertEquals("1,480円", Currency.JPY.format(1_480.0, AmountNotation.Japanese))
        assertEquals("20ドル", Currency.USD.format(20.0, AmountNotation.Japanese))
        assertEquals("50ユーロ", Currency.EUR.format(50.0, AmountNotation.Japanese))
    }

    @Test
    fun `symbol notation is the default`() {
        assertEquals(
            Currency.JPY.format(1_480.0),
            Currency.JPY.format(1_480.0, AmountNotation.Symbol),
        )
        assertEquals(AmountNotation.Symbol, AmountNotation.fromNameOrDefault(null))
        assertEquals(AmountNotation.Symbol, AmountNotation.fromNameOrDefault("Nonsense"))
        assertEquals(AmountNotation.Japanese, AmountNotation.fromNameOrDefault("Japanese"))
    }
}

internal fun subscription(
    id: String = "s1",
    name: String = "Netflix",
    price: Double? = 1_000.0,
    isEstimated: Boolean = false,
    currency: Currency = Currency.JPY,
    cycle: BillingCycle = BillingCycle.Monthly,
    nextPaymentDate: LocalDate = LocalDate.of(2026, 8, 15),
    cardId: String? = "c1",
    createdAt: Long = 0L,
): Subscription = Subscription(
    id = id,
    name = name,
    price = price,
    nextPaymentDate = nextPaymentDate,
    cardId = cardId,
    isEstimated = isEstimated,
    currency = currency,
    billingCycle = cycle,
    createdAt = createdAt,
)
