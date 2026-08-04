---
name: room-migration
description: Safely change the SubKan Room schema — add/rename/drop a column, add an entity or index — with a version bump, a tested Migration, and a regenerated schema JSON. Use whenever an @Entity in data/local/entity changes, before writing any UI that depends on the new shape.
---

# Changing the SubKan database schema

The database lives on the user's device and holds the only copy of what the user entered. There is
no server to restore from. Every schema change is therefore a migration, and the migration is
tested.

## The rule that matters most

**Never add `fallbackToDestructiveMigration()`.** It makes the crash go away by deleting everything
the user recorded. If a migration is failing, fix the migration.

## 1. Change the entity

Edit the `@Entity` in `data/local/entity/`. New columns must be nullable or have a Kotlin default —
existing rows have no value for them.

Update `toDomain()` / `toEntity()` in the same file, and the domain type in `core/model` if the new
field is user-visible.

## 2. Bump the version

In `data/local/SubKanDatabase.kt`, increment `version`. Go up by exactly one.

## 3. Write the migration

Add it next to the database, and register it on the builder in `data/di/DataModule.kt`:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE subscriptions ADD COLUMN note TEXT")
    }
}

// DataModule.kt
Room.databaseBuilder(context, SubKanDatabase::class.java, SubKanDatabase.NAME)
    .addMigrations(MIGRATION_1_2)
    .build()
```

The database currently has no migrations at all — version 1 is the first shipped Kotlin schema. The
`addMigrations(...)` call therefore does not exist yet; adding the first migration means adding it.

SQLite cannot rename or drop a column portably, and it cannot change a column's type at all. For
anything beyond `ADD COLUMN` or `CREATE INDEX`, use the create-copy-drop-rename dance:

```kotlin
connection.execSQL("CREATE TABLE subscriptions_new (...)")   // the new shape, with its indices
connection.execSQL("INSERT INTO subscriptions_new (...) SELECT ... FROM subscriptions")
connection.execSQL("DROP TABLE subscriptions")
connection.execSQL("ALTER TABLE subscriptions_new RENAME TO subscriptions")
```

Recreate every index and foreign key on the new table — they do not survive the rename. **The
foreign key is load-bearing here**: `subscriptions.card_id` is `ON DELETE SET NULL`, and that is the
only reason deleting a card from the management screen keeps its subscriptions. Recreating it as
`CASCADE`, or omitting it, silently turns a safe delete into a destructive one. Re-read the entity's
`foreignKeys` block against what the migration creates.

## 4. Regenerate and commit the schema JSON

```
./gradlew :app:compileDebugKotlin
```

This writes `app/schemas/com.subkan.data.local.SubKanDatabase/<version>.json`. Commit it. Diffing
those files against the migration is how a reviewer — human or otherwise — confirms the two agree.

## 5. Test the migration

Room's `MigrationTestHelper` (already on the test classpath via `room-testing`) opens a database at
the old version, runs the migration, and validates the result against the exported schema:

```kotlin
@get:Rule
val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    SubKanDatabase::class.java,
)

@Test
fun migrate1To2_keepsExistingSubscriptions() {
    helper.createDatabase(TEST_DB, 1).apply {
        execSQL("INSERT INTO subscriptions (id, name, price, ...) VALUES ('s1', 'Netflix', 1480.0, ...)")
        close()
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
    db.query("SELECT name FROM subscriptions WHERE id = 's1'").use {
        assertTrue(it.moveToFirst())
        assertEquals("Netflix", it.getString(0))
    }
}
```

Assert on the *data*, not just that the migration ran. `runMigrationsAndValidate` checks the shape;
only your query checks that the rows survived.

## 6. Check the invariants still hold

After the migration, re-run the data-layer tests — the two delete paths and the `SET NULL` behaviour
are the first things a table rewrite breaks:

```
./gradlew :app:testDebugUnitTest
```
