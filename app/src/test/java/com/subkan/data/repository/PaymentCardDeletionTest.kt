package com.subkan.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.subkan.core.model.AmountKind
import com.subkan.core.model.BillingCycle
import com.subkan.core.model.amountKind
import com.subkan.core.model.Currency
import com.subkan.core.time.AppClock
import com.subkan.data.local.SubKanDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The two ways a card can be deleted differ in what happens to its subscriptions, and both are
 * undoable. That combination is the easiest thing in this app to get quietly wrong, so it is
 * tested against a real SQLite database rather than a mocked DAO — the `ON DELETE SET NULL` that
 * makes one of them work is a property of the schema, not of the Kotlin.
 */
@RunWith(RobolectricTestRunner::class)
class PaymentCardDeletionTest {

    private lateinit var database: SubKanDatabase
    private lateinit var cardRepository: OfflinePaymentCardRepository
    private lateinit var subscriptionRepository: OfflineSubscriptionRepository

    private val clock = object : AppClock {
        override fun nowMillis(): Long = 1_000L
        override fun today(): LocalDate = LocalDate.of(2026, 8, 2)
        override fun now(): LocalDateTime = LocalDateTime.of(2026, 8, 2, 9, 0)
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SubKanDatabase::class.java,
        ).allowMainThreadQueries().build()

        cardRepository = OfflinePaymentCardRepository(
            database = database,
            cardDao = database.paymentCardDao(),
            subscriptionDao = database.subscriptionDao(),
            clock = clock,
        )
        subscriptionRepository = OfflineSubscriptionRepository(
            subscriptionDao = database.subscriptionDao(),
            clock = clock,
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `seeding is a one-time operation`() = runTest {
        cardRepository.seedDefaultsIfEmpty()
        val afterFirst = cardRepository.observeAll().first()

        cardRepository.seedDefaultsIfEmpty()
        val afterSecond = cardRepository.observeAll().first()

        assertEquals(7, afterFirst.size)
        assertEquals(afterFirst, afterSecond)
    }

    @Test
    fun `deleting from the management screen keeps the subscriptions and unlinks them`() = runTest {
        val cardId = cardRepository.create("楽天カード", "BF0000")
        val subscriptionId = subscriptionRepository.create(draft(cardId))

        val deleted = cardRepository.deleteKeepingSubscriptions(cardId)

        assertEquals(listOf(subscriptionId), deleted?.detachedSubscriptionIds)
        val remaining = subscriptionRepository.observeAll().first()
        assertEquals(1, remaining.size)
        // Null, not dangling: the row is intact and simply names no card.
        assertNull(remaining.single().cardId)
    }

    @Test
    fun `deleting from the card's tab removes the subscriptions too`() = runTest {
        val cardId = cardRepository.create("楽天カード", "BF0000")
        subscriptionRepository.create(draft(cardId))
        subscriptionRepository.create(draft(cardId, name = "Spotify"))

        val deleted = cardRepository.deleteWithSubscriptions(cardId)

        assertEquals(2, deleted?.removedSubscriptions?.size)
        assertEquals(emptyList<Any>(), subscriptionRepository.observeAll().first())
    }

    @Test
    fun `undoing a management-screen delete re-links the subscriptions`() = runTest {
        val cardId = cardRepository.create("楽天カード", "BF0000")
        subscriptionRepository.create(draft(cardId))

        val deleted = cardRepository.deleteKeepingSubscriptions(cardId)!!
        cardRepository.restore(deleted)

        assertEquals(cardId, subscriptionRepository.observeAll().first().single().cardId)
        assertEquals(1, cardRepository.observeAll().first().size)
    }

    @Test
    fun `undoing a tab delete brings the subscriptions back unchanged`() = runTest {
        val cardId = cardRepository.create("楽天カード", "BF0000")
        subscriptionRepository.create(draft(cardId, name = "Netflix"))

        val deleted = cardRepository.deleteWithSubscriptions(cardId)!!
        cardRepository.restore(deleted)

        val restored = subscriptionRepository.observeAll().first().single()
        assertEquals("Netflix", restored.name)
        assertEquals(cardId, restored.cardId)
        assertEquals(1_480.0, restored.price!!, 0.001)
    }

    @Test
    fun `reordering rewrites every card's position`() = runTest {
        val first = cardRepository.create("A", "BF0000")
        val second = cardRepository.create("B", "004D40")
        val third = cardRepository.create("C", "1976D2")

        cardRepository.reorder(listOf(third, first, second))

        assertEquals(
            listOf("C", "A", "B"),
            cardRepository.observeAll().first().map { it.name },
        )
    }

    @Test
    fun `editing a subscription leaves its registration order alone`() = runTest {
        val cardId = cardRepository.create("楽天カード", "BF0000")
        val id = subscriptionRepository.create(draft(cardId))
        val createdAt = subscriptionRepository.observeAll().first().single().createdAt

        subscriptionRepository.update(id, draft(cardId, name = "Renamed"))

        val updated = subscriptionRepository.observeAll().first().single()
        assertEquals("Renamed", updated.name)
        assertEquals(createdAt, updated.createdAt)
    }

    @Test
    fun `an amountless subscription round-trips through delete and undo`() = runTest {
        val cardId = cardRepository.create("楽天カード", "BF0000")
        subscriptionRepository.create(draft(cardId, name = "電気代").copy(price = null))

        val deleted = cardRepository.deleteWithSubscriptions(cardId)!!
        cardRepository.restore(deleted)

        val restored = subscriptionRepository.observeAll().first().single()
        assertEquals("電気代", restored.name)
        assertNull("a missing amount must not come back as zero", restored.price)
    }

    @Test
    fun `the estimate flag survives a round trip`() = runTest {
        val cardId = cardRepository.create("楽天カード", "BF0000")
        subscriptionRepository.create(
            draft(cardId, name = "ガス代").copy(price = 4_000.0, isEstimated = true),
        )

        val stored = subscriptionRepository.observeAll().first().single()
        assertEquals(true, stored.isEstimated)
        assertEquals(AmountKind.Estimated, stored.amountKind)
    }

    private fun draft(cardId: String?, name: String = "Netflix") = SubscriptionDraft(
        name = name,
        price = 1_480.0,
        currency = Currency.JPY,
        billingCycle = BillingCycle.Monthly,
        nextPaymentDate = LocalDate.of(2026, 8, 15),
        cardId = cardId,
    )
}
