package com.subkan.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SubscriptionSortTest {

    private val netflix = subscription(
        id = "netflix",
        name = "Netflix",
        nextPaymentDate = LocalDate.of(2026, 9, 1),
        createdAt = 300,
    )
    private val amazon = subscription(
        id = "amazon",
        name = "amazon prime",
        nextPaymentDate = LocalDate.of(2026, 8, 5),
        createdAt = 100,
    )
    private val spotify = subscription(
        id = "spotify",
        name = "Spotify",
        nextPaymentDate = LocalDate.of(2026, 8, 20),
        createdAt = 200,
    )

    private val all = listOf(netflix, amazon, spotify)

    private val today = LocalDate.of(2026, 8, 1)

    @Test
    fun `registered order follows creation time, not insertion into the list`() {
        val sorted = all.sorted(SubscriptionSort.Registered, ascending = true, today = today)
        assertEquals(listOf("amazon", "spotify", "netflix"), sorted.map { it.id })
    }

    @Test
    fun `name order ignores case, so a lowercase entry does not sort last`() {
        val sorted = all.sorted(SubscriptionSort.Name, ascending = true, today = today)
        assertEquals(listOf("amazon", "netflix", "spotify"), sorted.map { it.id })
    }

    @Test
    fun `payment date order puts the next charge first`() {
        val sorted = all.sorted(SubscriptionSort.PaymentDate, ascending = true, today = today)
        assertEquals(listOf("amazon", "spotify", "netflix"), sorted.map { it.id })
    }

    @Test
    fun `payment date order uses the next occurrence, not the stored anchor`() {
        // Stored anchors are all in 2025, so sorting on them would freeze the order as a, b, c.
        // Rolled forward from 2026-08-01 the next charges are the 20th, the 1st and the 5th.
        val stale = listOf(
            subscription(id = "a", nextPaymentDate = LocalDate.of(2025, 1, 20)),
            subscription(id = "b", nextPaymentDate = LocalDate.of(2025, 2, 1)),
            subscription(id = "c", nextPaymentDate = LocalDate.of(2025, 3, 5)),
        )

        val sorted = stale.sorted(SubscriptionSort.PaymentDate, ascending = true, today = today)

        assertEquals(listOf("b", "c", "a"), sorted.map { it.id })
    }

    @Test
    fun `descending is the ascending list reversed`() {
        val ascending = all.sorted(SubscriptionSort.Name, ascending = true, today = today)
        val descending = all.sorted(SubscriptionSort.Name, ascending = false, today = today)
        assertEquals(ascending.reversed().map { it.id }, descending.map { it.id })
    }

    @Test
    fun `an unknown stored sort falls back to registered rather than throwing`() {
        assertEquals(SubscriptionSort.Registered, SubscriptionSort.fromNameOrDefault("Whatever"))
        assertEquals(SubscriptionSort.Registered, SubscriptionSort.fromNameOrDefault(null))
        assertEquals(SubscriptionSort.Name, SubscriptionSort.fromNameOrDefault("Name"))
    }
}
