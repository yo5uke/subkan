package com.subkan.data.repository

import com.subkan.core.model.Subscription
import com.subkan.core.time.AppClock
import com.subkan.data.local.dao.SubscriptionDao
import com.subkan.data.local.entity.SubscriptionEntity
import com.subkan.data.local.entity.toDomain
import com.subkan.data.local.entity.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineSubscriptionRepository @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val clock: AppClock,
) : SubscriptionRepository {

    override fun observeAll(): Flow<List<Subscription>> =
        subscriptionDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun create(draft: SubscriptionDraft): String {
        val now = clock.nowMillis()
        val id = UUID.randomUUID().toString()
        subscriptionDao.upsert(
            SubscriptionEntity(
                id = id,
                name = draft.name.trim(),
                price = draft.price,
                isEstimated = draft.isEstimated,
                currency = draft.currency.code,
                billingCycle = draft.billingCycle.name,
                nextPaymentEpochDay = draft.nextPaymentDate.toEpochDay(),
                cardId = draft.cardId,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    override suspend fun update(id: String, draft: SubscriptionDraft) {
        val existing = subscriptionDao.getById(id) ?: return
        subscriptionDao.upsert(
            existing.copy(
                name = draft.name.trim(),
                price = draft.price,
                isEstimated = draft.isEstimated,
                currency = draft.currency.code,
                billingCycle = draft.billingCycle.name,
                nextPaymentEpochDay = draft.nextPaymentDate.toEpochDay(),
                cardId = draft.cardId,
                // `createdAt` is deliberately untouched: 「登録日順」 must not reshuffle because a
                // price was corrected.
                updatedAt = clock.nowMillis(),
            ),
        )
    }

    override suspend fun delete(id: String) {
        subscriptionDao.deleteById(id)
    }

    override suspend fun restore(subscription: Subscription) {
        subscriptionDao.upsert(subscription.toEntity(clock.nowMillis()))
    }
}
