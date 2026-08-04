package com.subkan.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The countdown badge is the one piece of this app that depends on what day it is, so the
 * boundaries get explicit tests rather than a spot check.
 *
 * Everything here is measured against the *next* charge, which is why there is no "already paid"
 * case: a past-dated subscription rolls forward to its next occurrence instead of getting stuck.
 */
class PaymentStatusTest {

    private val today = LocalDate.of(2026, 8, 2)

    @Test
    fun `today is its own bucket`() {
        assertEquals(PaymentStatus.Today, statusOn(today))
    }

    @Test
    fun `up to three days away counts as soon`() {
        assertEquals(PaymentStatus.Soon, statusOn(today.plusDays(1)))
        assertEquals(PaymentStatus.Soon, statusOn(today.plusDays(3)))
    }

    @Test
    fun `the fourth day is where soon stops`() {
        assertEquals(PaymentStatus.Later, statusOn(today.plusDays(4)))
    }

    @Test
    fun `days until payment counts calendar days`() {
        assertEquals(30L, subscription(nextPaymentDate = today.plusDays(30)).daysUntilPayment(today))
    }

    @Test
    fun `a past date counts down to next month, not backwards`() {
        // The badge used to read 「済」 here forever, while the reminder already knew about the
        // September charge.
        val sub = subscription(nextPaymentDate = LocalDate.of(2026, 7, 20))

        assertEquals(LocalDate.of(2026, 8, 20), sub.nextChargeDate(today))
        assertEquals(18L, sub.daysUntilPayment(today))
        assertEquals(PaymentStatus.Later, sub.paymentStatus(today))
    }

    @Test
    fun `a past date whose anniversary is today reads as today`() {
        val sub = subscription(nextPaymentDate = LocalDate.of(2026, 5, 2))
        assertEquals(PaymentStatus.Today, sub.paymentStatus(today))
    }

    @Test
    fun `the countdown never goes negative`() {
        listOf(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 2),
            LocalDate.of(2026, 12, 31),
        ).forEach { date ->
            val days = subscription(nextPaymentDate = date).daysUntilPayment(today)
            assertEquals("$date should not count backwards", true, days >= 0)
        }
    }

    private fun statusOn(date: LocalDate): PaymentStatus =
        subscription(nextPaymentDate = date).paymentStatus(today)
}
