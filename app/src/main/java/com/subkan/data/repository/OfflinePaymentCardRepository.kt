package com.subkan.data.repository

import androidx.room.withTransaction
import com.subkan.core.model.PaymentCard
import com.subkan.core.model.defaultPaymentCards
import com.subkan.core.time.AppClock
import com.subkan.data.local.SubKanDatabase
import com.subkan.data.local.dao.PaymentCardDao
import com.subkan.data.local.dao.SubscriptionDao
import com.subkan.data.local.entity.PaymentCardEntity
import com.subkan.data.local.entity.toDomain
import com.subkan.data.local.entity.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflinePaymentCardRepository @Inject constructor(
    private val database: SubKanDatabase,
    private val cardDao: PaymentCardDao,
    private val subscriptionDao: SubscriptionDao,
    private val clock: AppClock,
) : PaymentCardRepository {

    override fun observeAll(): Flow<List<PaymentCard>> =
        cardDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun seedDefaultsIfEmpty() {
        if (cardDao.count() > 0) return
        val now = clock.nowMillis()
        cardDao.upsertAll(
            defaultPaymentCards.map { card ->
                PaymentCardEntity(
                    id = card.id,
                    name = card.name,
                    colorHex = card.colorHex,
                    sortOrder = card.sortOrder,
                    createdAt = now,
                    updatedAt = now,
                )
            },
        )
    }

    override suspend fun create(name: String, colorHex: String): String {
        val now = clock.nowMillis()
        val id = UUID.randomUUID().toString()
        cardDao.upsert(
            PaymentCardEntity(
                id = id,
                name = name.trim(),
                colorHex = colorHex,
                sortOrder = cardDao.nextSortOrder(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    override suspend fun update(id: String, name: String, colorHex: String) {
        val existing = cardDao.getById(id) ?: return
        cardDao.upsert(
            existing.copy(
                name = name.trim(),
                colorHex = colorHex,
                updatedAt = clock.nowMillis(),
            ),
        )
    }

    override suspend fun reorder(orderedIds: List<String>) {
        val now = clock.nowMillis()
        database.withTransaction {
            orderedIds.forEachIndexed { index, id -> cardDao.setSortOrder(id, index, now) }
        }
    }

    override suspend fun countSubscriptions(cardId: String): Int =
        subscriptionDao.getByCardId(cardId).size

    override suspend fun deleteKeepingSubscriptions(id: String): DeletedCard? {
        val card = cardDao.getById(id) ?: return null
        // Read the links before the delete: `ON DELETE SET NULL` clears them, and afterwards there
        // is nothing left to say which subscriptions used to point here.
        val detachedIds = subscriptionDao.getByCardId(id).map { it.id }
        cardDao.deleteById(id)
        return DeletedCard(card = card.toDomain(), detachedSubscriptionIds = detachedIds)
    }

    override suspend fun deleteWithSubscriptions(id: String): DeletedCard? {
        val card = cardDao.getById(id) ?: return null
        val removed = subscriptionDao.getByCardId(id).map { it.toDomain() }
        database.withTransaction {
            subscriptionDao.deleteByCardId(id)
            cardDao.deleteById(id)
        }
        return DeletedCard(card = card.toDomain(), removedSubscriptions = removed)
    }

    override suspend fun restore(deleted: DeletedCard) {
        val now = clock.nowMillis()
        database.withTransaction {
            cardDao.upsert(
                PaymentCardEntity(
                    id = deleted.card.id,
                    name = deleted.card.name,
                    colorHex = deleted.card.colorHex,
                    sortOrder = deleted.card.sortOrder,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            // Order matters: the card has to exist again before anything can point at it, or the
            // foreign key rejects both the re-link and the re-insert.
            if (deleted.detachedSubscriptionIds.isNotEmpty()) {
                subscriptionDao.reassignCard(deleted.detachedSubscriptionIds, deleted.card.id, now)
            }
            if (deleted.removedSubscriptions.isNotEmpty()) {
                subscriptionDao.upsertAll(deleted.removedSubscriptions.map { it.toEntity(now) })
            }
        }
    }
}
