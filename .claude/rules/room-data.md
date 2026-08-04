---
paths:
  - "app/src/main/java/com/subkan/data/**"
  - "app/schemas/**"
---

# Room & data layer rules

## Schema changes are versioned, never silent

The database ships on users' devices and holds the only copy of their subscriptions. Any change to
an `@Entity` — a new column, a renamed one, a changed type or a new index — requires **all** of:

1. Bumping `version` in `SubKanDatabase`.
2. A `Migration` object registered on the builder in `data/di/DataModule.kt`.
3. The regenerated JSON under `app/schemas/`, committed alongside the code.

Never resolve a schema mismatch with `fallbackToDestructiveMigration()`. It compiles, the app stops
crashing, and every user silently loses everything they entered.

Exported schema files under `app/schemas/` are append-only history. Edit the entity and let the
build regenerate them; never hand-edit a JSON file.

`MIGRATION_1_2` rebuilds the `subscriptions` table (SQLite cannot make a column nullable in place).
`MigrationTest` covers it, including that the foreign key came back as `ON DELETE SET NULL` and that
both indices survived — the two things a rebuild loses silently. Any future rebuild needs the same
checks. Use Room 2.8's driver-based `MigrationTestHelper` constructor, not the
`(instrumentation, databaseClass)` one, which compiles but then rejects the resolved path.

## The two card deletions

This is the invariant most likely to be broken by a well-meaning change.

- `subscriptions.card_id` is nullable with `ON DELETE SET NULL`. That is what makes
  `deleteKeepingSubscriptions` work — the card management screen deletes a card and its
  subscriptions survive with no card.
- `deleteWithSubscriptions` deletes the rows explicitly, in a transaction, *before* the card. This
  is the tab long-press path, and the UI confirms with a count first.

Do not "tidy" the foreign key to `CASCADE`. It would silently turn the management screen's delete
into the destructive one.

Both paths capture what they removed **before** removing it and return a `DeletedCard`. Once the
rows are gone the database cannot answer what they used to contain, and both deletions are undoable
from a snackbar.

## Dates and money

- The next payment date is `next_payment_epoch_day` — a day number, not an instant. The user picked
  a calendar date; a timezone change must not move 「毎月5日」 to the 4th. Convert through
  `ZoneOffset.UTC`, which is what `ui/util/DateFormatting.kt` does for the Material date picker.
- The stored date is an **anchor**, never advanced. Anything that needs "the next charge" derives it
  with `nextChargeDate` / `nextOccurrenceOnOrAfter`. Do not add a background job that rewrites the
  column when a payment passes: the anchor is what survives a month-end clamp, and rewriting it
  would drag a 31st permanently back to the 28th after one February.
- `price` is **nullable** `REAL`. Null means no amount was entered — a recurring cost the user only
  wants to track by name. Never substitute 0.0: it would silently join every total.
- `is_estimated` marks a rough amount. It is only meaningful alongside a non-null `price`; the
  editor will not set it otherwise.
- `created_at` is what 「登録日順」 sorts on, so `update` must never touch it. Correcting a price is
  not a re-registration.

## Queries

- Reads that feed the UI return `Flow`; one-shot reads inside a suspend function use a dedicated
  `suspend` query rather than collecting a `Flow`.
- `SubscriptionDao.observeAll()` is deliberately unordered. Sorting happens in `core/model` where it
  is unit-tested and where case-insensitive comparison works for Japanese, which
  `COLLATE NOCASE` does not extend to.
- Comparing against a nullable column uses `IS :param`, not `= :param`. `= NULL` is never true.

## Boundaries

Entities stay inside `data/local`. Repositories map entity → domain (`core/model`) and return domain
types only. If a `*Entity` type appears in a ViewModel signature, the mapping was skipped.

## Seeding

Default cards are inserted by `seedDefaultsIfEmpty()`, called once from `SubKanApplication`. It is
guarded on `count() == 0`, so a user who deletes every card does not get them back on next launch —
that is intended. Do not move this into a Room callback; it is visible and testable where it is.
