package com.subkan.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.subkan.core.model.PaymentCard

@Entity(tableName = "payment_cards")
data class PaymentCardEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    /** Six hex digits, no `#` and no alpha — e.g. `BF0000`. */
    @ColumnInfo(name = "color_hex") val colorHex: String,
    /** Position in the tab row. Rewritten for every card whenever one is dragged. */
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun PaymentCardEntity.toDomain(): PaymentCard = PaymentCard(
    id = id,
    name = name,
    colorHex = colorHex,
    sortOrder = sortOrder,
)
