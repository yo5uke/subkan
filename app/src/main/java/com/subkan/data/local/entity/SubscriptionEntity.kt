package com.subkan.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.subkan.core.model.BillingCycle
import com.subkan.core.model.Currency
import com.subkan.core.model.Subscription
import java.time.LocalDate

/**
 * One recurring charge.
 *
 * The next payment date is stored as [nextPaymentEpochDay] — a day number, not an instant. The
 * user picked a calendar date; flying to another timezone must not move 「毎月5日」 to the 4th.
 *
 * `card_id` is `ON DELETE SET NULL`, which is what makes deleting a card from the management
 * screen keep its subscriptions (they render as 「不明なカード」). Deleting a card from its tab
 * removes the subscriptions too, but that is done explicitly in the repository — see
 * `OfflinePaymentCardRepository`.
 */
@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = PaymentCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("card_id"), Index("next_payment_epoch_day")],
)
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    /**
     * Null when no amount has been entered — 光熱費 registered by name alone. Nullable rather than
     * a 0.0 sentinel so "free" and "not known yet" cannot be confused, and so no code path can
     * accidentally render an amount that was never entered.
     */
    @ColumnInfo(name = "price") val price: Double?,
    /** The entered amount is a rough figure, shown with a 「約」 prefix. */
    @ColumnInfo(name = "is_estimated", defaultValue = "0") val isEstimated: Boolean = false,
    @ColumnInfo(name = "currency") val currency: String,
    @ColumnInfo(name = "billing_cycle") val billingCycle: String,
    @ColumnInfo(name = "next_payment_epoch_day") val nextPaymentEpochDay: Long,
    /** Null when the card this was filed under has been deleted. */
    @ColumnInfo(name = "card_id") val cardId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    id = id,
    name = name,
    price = price,
    isEstimated = isEstimated,
    nextPaymentDate = LocalDate.ofEpochDay(nextPaymentEpochDay),
    cardId = cardId,
    currency = Currency.fromCodeOrDefault(currency),
    billingCycle = BillingCycle.fromNameOrDefault(billingCycle),
    createdAt = createdAt,
)

fun Subscription.toEntity(updatedAt: Long): SubscriptionEntity = SubscriptionEntity(
    id = id,
    name = name,
    price = price,
    isEstimated = isEstimated,
    currency = currency.code,
    billingCycle = billingCycle.name,
    nextPaymentEpochDay = nextPaymentDate.toEpochDay(),
    cardId = cardId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
