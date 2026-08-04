package com.subkan.data.repository

import com.subkan.core.model.BillingCycle
import com.subkan.core.model.Currency
import com.subkan.core.model.Subscription
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** The editable fields of a subscription — everything the editor sheet collects. */
data class SubscriptionDraft(
    val name: String,
    /** Null when the user registered a name without an amount. */
    val price: Double?,
    val isEstimated: Boolean = false,
    val currency: Currency,
    val billingCycle: BillingCycle,
    val nextPaymentDate: LocalDate,
    val cardId: String?,
)

interface SubscriptionRepository {

    fun observeAll(): Flow<List<Subscription>>

    suspend fun create(draft: SubscriptionDraft): String

    suspend fun update(id: String, draft: SubscriptionDraft)

    suspend fun delete(id: String)

    /** Re-inserts a deleted subscription unchanged, keeping its original `createdAt`. */
    suspend fun restore(subscription: Subscription)
}
