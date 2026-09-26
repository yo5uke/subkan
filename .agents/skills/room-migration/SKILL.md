---
name: room-migration
description: Safely changes the SubKan Room database schema — adds, renames or drops a column, adds an entity or index — with a version bump, a tested Migration and regenerated schema JSON. Use whenever an @Entity in data/local/entity changes, before writing any UI that depends on the new shape.
---

# Changing the SubKan database schema

## Goal

Change the schema without any user losing data. The database lives on the user's device and holds the
only copy of what they entered; there is no server to restore from. Every schema change is therefore a
migration, and every migration is tested.

## Current state

- `SubKanDatabase.version` is **2**.
- `MIGRATION_1_2` lives in `data/local/SubKanDatabase.kt` and is registered with
  `.addMigrations(MIGRATION_1_2)` in `data/di/DataModule.kt`. A new migration is **added to that call**
  (`.addMigrations(MIGRATION_1_2, MIGRATION_2_3)`), not a replacement for it.
- `app/schemas/com.subkan.data.local.SubKanDatabase/` holds `1.json` and `2.json`.

## Instructions

### 1. Change the entity

Edit the `@Entity` in `data/local/entity/`. New columns must be nullable or have a Kotlin default —
existing rows have no value for them.

Update `toDomain()` / `toEntity()` in the same file, and the domain type in `core/model` if the new
field is user-visible.

### 2. Bump the version

In `data/local/SubKanDatabase.kt`, increment `version` by exactly one.

### 3. Write the migration

Add it beside `MIGRATION_1_2` and register it in `DataModule.kt`. Room 2.8 migrations take a
`SQLiteConnection`:

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE subscriptions ADD COLUMN note TEXT")
    }
}
```

SQLite cannot rename or drop a column portably, and cannot change a column's type or nullability at
all. For anything beyond `ADD COLUMN` or `CREATE INDEX`, use create-copy-drop-rename — `MIGRATION_1_2`
is the worked example:

```kotlin
connection.execSQL("CREATE TABLE subscriptions_new (...)")   // the new shape
connection.execSQL("INSERT INTO subscriptions_new (...) SELECT ... FROM subscriptions")
connection.execSQL("DROP TABLE subscriptions")
connection.execSQL("ALTER TABLE subscriptions_new RENAME TO subscriptions")
// then recreate every index
```

Recreate every index and foreign key — they do not survive the rename. **The foreign key is
load-bearing**: `subscriptions.card_id` is `ON DELETE SET NULL`, and that is the only reason deleting a
card from the management screen keeps its subscriptions. Recreating it as `CASCADE`, or omitting it,
silently turns a safe delete into a destructive one. Re-read the entity's `foreignKeys` block against
what the migration creates. The indices to restore are `index_subscriptions_card_id` and
`index_subscriptions_next_payment_epoch_day`.

### 4. Regenerate and commit the schema JSON

```
./gradlew :app:compileDebugKotlin
```

This writes `app/schemas/com.subkan.data.local.SubKanDatabase/<version>.json`. Never hand-edit it.
Diffing it against the migration is how a reviewer confirms the two agree. (Commit only if the user
has asked you to commit.)

### 5. Test the migration

Add tests to `app/src/test/java/com/subkan/data/local/MigrationTest.kt`. It runs on the JVM under
Robolectric, using Room 2.8's **driver-based** `MigrationTestHelper` — copy the existing rule setup
rather than the older `(instrumentation, databaseClass)` overload, which compiles but then rejects the
resolved path:

```kotlin
@get:Rule
val helper = MigrationTestHelper(
    instrumentation = InstrumentationRegistry.getInstrumentation(),
    file = InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(TEST_DB),
    driver = AndroidSQLiteDriver(),
    databaseClass = SubKanDatabase::class,
)

@Test
fun migrate2To3_keepsExistingSubscriptions() {
    helper.createDatabase(2).use { connection ->
        connection.execSQL("INSERT INTO subscriptions (...) VALUES ('s1', 'Netflix', 1480.0, ...)")
    }
    helper.runMigrationsAndValidate(3, listOf(MIGRATION_2_3)).use { connection ->
        connection.prepare("SELECT name FROM subscriptions WHERE id = 's1'").use { statement ->
            assertTrue(statement.step())
            assertEquals("Netflix", statement.getText(0))
        }
    }
}
```

Assert on the *data*, not just that the migration ran: `runMigrationsAndValidate` checks the shape,
only your query checks that the rows survived. For a table rebuild, also assert that the foreign key is
still `ON DELETE SET NULL` and that both indices exist.

### 6. Re-run the invariants

```
./gradlew :app:testDebugUnitTest
```

The two card-delete paths and the `SET NULL` behaviour (`PaymentCardDeletionTest`) are the first things
a table rewrite breaks.

## Constraints

- **Never add `fallbackToDestructiveMigration()`.** It makes a crash go away by deleting everything
  the user recorded. If a migration fails, fix the migration.
- Never skip a version number or edit a migration that has already shipped.
- Never hand-edit files under `app/schemas/`.
