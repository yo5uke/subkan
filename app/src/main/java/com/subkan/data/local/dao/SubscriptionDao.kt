package com.subkan.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.subkan.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    /**
     * Unordered on purpose — the sort the user picked is applied in `core/model` where it can be
     * unit-tested, and re-sorting a list this size costs nothing.
     */
    @Query("SELECT * FROM subscriptions")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE card_id = :cardId")
    suspend fun getByCardId(cardId: String): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: String): SubscriptionEntity?

    @Upsert
    suspend fun upsert(subscription: SubscriptionEntity)

    @Upsert
    suspend fun upsertAll(subscriptions: List<SubscriptionEntity>)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM subscriptions WHERE card_id = :cardId")
    suspend fun deleteByCardId(cardId: String)

    @Query("UPDATE subscriptions SET card_id = :cardId, updated_at = :updatedAt WHERE id IN (:ids)")
    suspend fun reassignCard(ids: List<String>, cardId: String?, updatedAt: Long)
}
