package com.subkan.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.subkan.data.local.entity.PaymentCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentCardDao {

    /** Tab order. `id` breaks ties so a reorder that leaves duplicates never flickers. */
    @Query("SELECT * FROM payment_cards ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flow<List<PaymentCardEntity>>

    @Query("SELECT * FROM payment_cards ORDER BY sort_order ASC, id ASC")
    suspend fun getAll(): List<PaymentCardEntity>

    @Query("SELECT * FROM payment_cards WHERE id = :id")
    suspend fun getById(id: String): PaymentCardEntity?

    @Query("SELECT COUNT(*) FROM payment_cards")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sort_order) + 1, 0) FROM payment_cards")
    suspend fun nextSortOrder(): Int

    @Upsert
    suspend fun upsert(card: PaymentCardEntity)

    @Upsert
    suspend fun upsertAll(cards: List<PaymentCardEntity>)

    @Query("UPDATE payment_cards SET sort_order = :sortOrder, updated_at = :updatedAt WHERE id = :id")
    suspend fun setSortOrder(id: String, sortOrder: Int, updatedAt: Long)

    @Query("DELETE FROM payment_cards WHERE id = :id")
    suspend fun deleteById(id: String)
}
