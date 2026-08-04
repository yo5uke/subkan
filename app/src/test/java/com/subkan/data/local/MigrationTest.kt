package com.subkan.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Migration 1 → 2 rebuilds the `subscriptions` table to make `price` nullable.
 *
 * A table rebuild is the migration most likely to be quietly wrong: the foreign key and both
 * indices have to be recreated by hand, and getting `ON DELETE SET NULL` wrong would turn the card
 * management screen's delete into a destructive one without anything failing.
 *
 * Uses Room 2.8's driver-based `MigrationTestHelper`. The older
 * `MigrationTestHelper(instrumentation, databaseClass)` overload still compiles, but its
 * `createDatabase(name, version)` resolves the name to a full path that the driver then refuses.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = InstrumentationRegistry.getInstrumentation().targetContext
            .getDatabasePath(TEST_DB),
        driver = AndroidSQLiteDriver(),
        databaseClass = SubKanDatabase::class,
    )

    @Test
    fun migrate1To2_keepsExistingSubscriptions() {
        helper.createDatabase(1).use { connection ->
            connection.execSQL(
                "INSERT INTO payment_cards (id, name, color_hex, sort_order, created_at, " +
                    "updated_at) VALUES ('c1', '楽天カード', 'BF0000', 0, 1, 1)",
            )
            connection.execSQL(
                "INSERT INTO subscriptions (id, name, price, currency, billing_cycle, " +
                    "next_payment_epoch_day, card_id, created_at, updated_at) " +
                    "VALUES ('s1', 'Netflix', 1480.0, 'JPY', 'Monthly', 20500, 'c1', 5, 5)",
            )
        }

        helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)).use { connection ->
            connection.prepare(
                "SELECT name, price, is_estimated, card_id, created_at FROM subscriptions",
            ).use { statement ->
                assertTrue(statement.step())
                assertEquals("Netflix", statement.getText(0))
                assertEquals(1480.0, statement.getDouble(1), 0.001)
                // Everything entered before this feature existed was a definite figure.
                assertEquals(0L, statement.getLong(2))
                assertEquals("c1", statement.getText(3))
                assertEquals(5L, statement.getLong(4))
            }
        }
    }

    @Test
    fun migrate1To2_keepsTheSetNullForeignKey() {
        helper.createDatabase(1).use { connection ->
            connection.execSQL(
                "INSERT INTO payment_cards (id, name, color_hex, sort_order, created_at, " +
                    "updated_at) VALUES ('c1', '楽天カード', 'BF0000', 0, 1, 1)",
            )
            connection.execSQL(
                "INSERT INTO subscriptions (id, name, price, currency, billing_cycle, " +
                    "next_payment_epoch_day, card_id, created_at, updated_at) " +
                    "VALUES ('s1', 'Netflix', 1480.0, 'JPY', 'Monthly', 20500, 'c1', 5, 5)",
            )
        }

        helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)).use { connection ->
            connection.execSQL("PRAGMA foreign_keys = ON")
            connection.execSQL("DELETE FROM payment_cards WHERE id = 'c1'")

            // Survives the card, with card_id cleared — not deleted along with it.
            connection.prepare("SELECT card_id FROM subscriptions WHERE id = 's1'").use { stmt ->
                assertTrue("the subscription should have survived its card", stmt.step())
                assertTrue("card_id should have been set to NULL", stmt.isNull(0))
            }
        }
    }

    @Test
    fun migrate1To2_allowsAnAmountlessSubscription() {
        helper.createDatabase(1).use { }

        helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)).use { connection ->
            connection.execSQL(
                "INSERT INTO subscriptions (id, name, price, is_estimated, currency, " +
                    "billing_cycle, next_payment_epoch_day, card_id, created_at, updated_at) " +
                    "VALUES ('s2', '電気代', NULL, 0, 'JPY', 'Monthly', 20500, NULL, 5, 5)",
            )

            connection.prepare("SELECT price FROM subscriptions WHERE id = 's2'").use { stmt ->
                assertTrue(stmt.step())
                assertTrue("price should be storable as NULL", stmt.isNull(0))
            }
        }
    }

    @Test
    fun migrate1To2_keepsBothIndices() {
        helper.createDatabase(1).use { }

        helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)).use { connection ->
            val indices = buildList {
                connection.prepare("PRAGMA index_list(`subscriptions`)").use { stmt ->
                    while (stmt.step()) add(stmt.getText(1))
                }
            }

            assertTrue(indices.contains("index_subscriptions_card_id"))
            assertTrue(indices.contains("index_subscriptions_next_payment_epoch_day"))
            assertFalse(
                "the temporary table should be gone",
                indices.any { it.contains("_new") },
            )
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
