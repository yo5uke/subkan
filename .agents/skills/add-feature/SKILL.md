---
name: add-feature
description: Guides adding a user-facing feature to the SubKan Android app end to end — domain model, Room schema, repository, ViewModel, Compose screen, strings and tests — in the order that keeps the build green. Use when asked to add or extend a feature such as a new field, filter, sort option, notification, export or widget. Not for one-line fixes.
---

# Adding a feature to SubKan

## Goal

Ship a feature outward from the domain, so each step compiles on its own and a mistake surfaces next
to the code that caused it rather than 200 lines later.

## Instructions

### 1. Decide whether the schema changes

If the feature stores anything new, it does. Stop and follow the **room-migration** skill first, then
come back — retrofitting a migration after the UI is written means redoing the entity, the DAO and the
tests.

If it stores nothing new (a filter, a sort, a display option), skip to step 3.

### 2. Domain model

Add or extend the type in `core/model`. Keep it Android-free: no `Context`, no `@Composable`, no
androidx imports. Pure logic — totals, sorting, date buckets — goes here too, as top-level functions,
because this is the layer that gets real unit tests.

Write those tests now, in `app/src/test/java/com/subkan/core/model/`. They run in about a second and
they are the cheapest place to discover the idea does not quite work. `MonthlyTotalsTest` shows the
shape; `subscription(...)` in that file is the shared fixture builder.

```
./gradlew :app:testDebugUnitTest
```

Anything involving "today" takes a `LocalDate` parameter rather than calling `LocalDate.now()`. That is
what `AppClock` is for, and why the countdown buckets are testable.

### 3. Data layer

- **DAO** — add queries to `data/local/dao/`. Return `Flow` for anything the UI observes. Use
  `IS :param` for nullable columns.
- **Entity mapping** — extend `toDomain()` / `toEntity()` in the same file as the entity.
- **Repository** — add the method to the interface in `data/repository/` first, then implement it in
  the `Offline*` class. The interface is what the ViewModel sees; keep it in domain types.

If the change touches deletion, cascading or `card_id`, add a case to `PaymentCardDeletionTest`. That
file exists because the two card-delete semantics are the easiest thing in this app to break silently.
Read the `room-data` rule before changing either.

### 4. ViewModel

Extend `HomeViewModel` / `CardsViewModel` / `SettingsViewModel`, or add one under the relevant `ui/`
package.

- Expose one `StateFlow<XxxUiState>` built with
  `combine(...).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`.
- Put one-shot things — snackbars, undo offers — on the `Channel`-backed event flow, not in the state.
  State replays on rotation; a snackbar should not.
- Anything destructive returns enough to undo itself, the way `DeletedCard` does.
- Actions are plain functions returning `Job` via `viewModelScope.launch { … }`.

### 5. Strings, then UI

Add strings to `res/values/strings.xml` **before** writing the composable, so it is written against
real resource ids. That file is Japanese and is the default locale — there is no `values-ja/`, and
adding one would be a bug (see the `resources` rule).

Then write the composable under the matching `ui/` package. Follow the existing screens: state in,
callbacks out, `MaterialTheme.colorScheme` for every colour, stable `key`s on every list item.

Reuse `components/SubscriptionRow`, `components/ServiceIcon` and `components/EmptyState` rather than
writing a new row — visual consistency across the list, the pager and the management screen is a
feature. Render amounts with `components/AmountText`.

There is no automatic hook here, so after writing UI code check for hard-coded colours yourself:

```
rg "Color\(\s*0x[0-9A-Fa-f]{6,8}" app/src/main/java/com/subkan/ui --glob "!**/theme/**"
```

Any hit is a bug — pick a scheme role.

### 6. Verify

```
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Then check by hand, because tests here do not catch these:

- Dark theme, and dynamic colour on (Settings → ダイナミックカラー).
- The tab row in both positions (Settings → タブバーの配置), including the bottom one with a
  gesture-navigation bar.
- Swiping between card tabs — the header total must move *with* the swipe, not after it.
- A subscription whose card has been deleted (「不明なカード」), and a mixed-currency total.
- Large font scale, if the feature adds text.

If the change touches reminders, also check what tests cannot: set a reminder a couple of minutes out,
confirm it arrives, and confirm the *next* one is still booked afterwards. Then revoke the notification
permission in system settings and come back — Settings should say so, and granting it again should
start reminders working without a restart. The `reminders` rule explains why each is a distinct
failure mode.

### 7. Write it down

If the feature changes an architectural rule, update `CLAUDE.md` and the matching file under
`.agents/rules/` (and `.claude/rules/`, which mirrors it for Claude Code). Add a `CHANGELOG.md` entry,
and if it lands something from `docs/roadmap.md`, tick it off there.

## Constraints

- Do not skip step 1: a stored field without a migration loses user data.
- Do not add a `values-ja/` folder or a hard-coded string in a composable.
- Do not commit or push unless asked.
