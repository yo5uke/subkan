package com.subkan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.subkan.data.local.dao.PaymentCardDao
import com.subkan.data.local.dao.SubscriptionDao
import com.subkan.data.local.entity.PaymentCardEntity
import com.subkan.data.local.entity.SubscriptionEntity

@Database(
    entities = [PaymentCardEntity::class, SubscriptionEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class SubKanDatabase : RoomDatabase() {
    abstract fun paymentCardDao(): PaymentCardDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        const val NAME = "subkan.db"
    }
}

/**
 * Makes `price` nullable and adds `is_estimated`, for costs that vary month to month — 光熱費 and
 * the like, registered by name alone or with a rough figure.
 *
 * `price` has to change from `REAL NOT NULL` to `REAL`, and SQLite cannot alter a column's
 * nullability. Hence the create-copy-drop-rename rebuild rather than a pair of `ADD COLUMN`s.
 *
 * The foreign key is the part worth re-reading: `card_id` must come back as `ON DELETE SET NULL`.
 * Recreating it as `CASCADE`, or dropping it, would silently turn deleting a card from the
 * management screen into a delete of every subscription filed under it.
 *
 * Existing rows keep their amount and get `is_estimated = 0` — everything entered before this
 * feature existed was a definite figure.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `subscriptions_new` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `price` REAL,
                `is_estimated` INTEGER NOT NULL DEFAULT 0,
                `currency` TEXT NOT NULL,
                `billing_cycle` TEXT NOT NULL,
                `next_payment_epoch_day` INTEGER NOT NULL,
                `card_id` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`card_id`) REFERENCES `payment_cards`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )

        connection.execSQL(
            """
            INSERT INTO `subscriptions_new` (
                `id`, `name`, `price`, `is_estimated`, `currency`, `billing_cycle`,
                `next_payment_epoch_day`, `card_id`, `created_at`, `updated_at`
            )
            SELECT `id`, `name`, `price`, 0, `currency`, `billing_cycle`,
                   `next_payment_epoch_day`, `card_id`, `created_at`, `updated_at`
            FROM `subscriptions`
            """.trimIndent(),
        )

        connection.execSQL("DROP TABLE `subscriptions`")
        connection.execSQL("ALTER TABLE `subscriptions_new` RENAME TO `subscriptions`")

        // Indices do not survive the rename either.
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_subscriptions_card_id` " +
                "ON `subscriptions` (`card_id`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_subscriptions_next_payment_epoch_day` " +
                "ON `subscriptions` (`next_payment_epoch_day`)",
        )
    }
}
