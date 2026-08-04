package com.subkan.data.repository

import com.subkan.core.model.PaymentCard
import com.subkan.core.model.Subscription
import kotlinx.coroutines.flow.Flow

/**
 * Everything needed to put a deleted card back exactly as it was.
 *
 * Both deletion paths are undoable from a snackbar, and they remove different things, so the undo
 * payload carries both: [detachedSubscriptionIds] are rows that survived and only lost their
 * `card_id`, while [removedSubscriptions] were deleted outright and have to be re-inserted.
 */
data class DeletedCard(
    val card: PaymentCard,
    val detachedSubscriptionIds: List<String> = emptyList(),
    val removedSubscriptions: List<Subscription> = emptyList(),
)

interface PaymentCardRepository {

    fun observeAll(): Flow<List<PaymentCard>>

    /** Inserts the starter cards on a fresh install. A no-op once any card exists. */
    suspend fun seedDefaultsIfEmpty()

    suspend fun create(name: String, colorHex: String): String

    suspend fun update(id: String, name: String, colorHex: String)

    /** Rewrites `sort_order` for every card so the tab row matches [orderedIds]. */
    suspend fun reorder(orderedIds: List<String>)

    suspend fun countSubscriptions(cardId: String): Int

    /**
     * Deletes the card only. Its subscriptions stay, with no card — the list shows them as
     * 「不明なカード」. This is what the card management screen does.
     */
    suspend fun deleteKeepingSubscriptions(id: String): DeletedCard?

    /**
     * Deletes the card *and* every subscription filed under it. This is what deleting from the
     * card's own tab does, which is why that path confirms with the count first.
     */
    suspend fun deleteWithSubscriptions(id: String): DeletedCard?

    suspend fun restore(deleted: DeletedCard)
}
