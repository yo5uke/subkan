---
trigger: always_on
description: SubKan project overview — what the app is, the invariants that drive its design, build commands, layout, language and git rules.
---

# SubKan

Android subscription tracker. The name is 「サブスク管理」 shortened. Single Gradle module, Kotlin +
Jetpack Compose + Material 3, fully offline (Room), no backend.

## Two ideas drive the design

1. **The 月額合計 is the product.** The list, the cards and the sort order exist to make one number
   trustworthy. A yearly plan contributes `price / 12`, so plans on different cycles are directly
   comparable.
2. **Cards are a lens, not a hierarchy.** 「すべて」 plus one tab per payment card are filters over a
   single list — a `HorizontalPager`, not a nav graph. A subscription with no card is still a
   subscription.

If a change makes either of those harder, it is probably the wrong change.

## Commands

Run from the repo root. `JAVA_HOME` must point at a JDK 17+; Android Studio's bundled one works:
`C:\Program Files\Android\Android Studio\jbr`.

| Task | Command |
| --- | --- |
| Compile | `./gradlew :app:compileDebugKotlin` |
| Unit tests | `./gradlew :app:testDebugUnitTest` |
| Debug APK | `./gradlew :app:assembleDebug` |
| Install on a device | `./gradlew :app:installDebug` |

A clean build takes ~2–3 minutes; incremental compiles take seconds. While iterating, use
`compileDebugKotlin` + `testDebugUnitTest`, and build the APK only when it is actually needed.

## Layout

```
app/src/main/java/com/subkan/
├── core/model/      Domain types + pure logic (totals, sorting, countdown buckets). No Android imports.
├── core/time/       AppClock — the seam that makes 「あと N 日」 testable.
├── data/local/      Room: entities, DAOs, database. Entities own the `*_snake_case` column names.
├── data/repository/ Repository interfaces + Offline* implementations. UI never touches a DAO.
├── data/preferences/DataStore-backed settings.
├── data/icon/       Service name → logo URL. Pure, and allowed to answer "no URL".
├── data/reminder/   AlarmManager scheduling, the notification, and its two receivers.
├── data/di/         Hilt modules (DataModule).
└── ui/              theme/ components/ permissions/ home/ editor/ cards/ settings/ util/
```

`app/schemas/` holds exported Room schema JSON and is committed.

## Cross-cutting conventions

- **Data flows one way:** DAO → Repository (entity → domain) → ViewModel (`StateFlow<UiState>`) →
  Composable. Composables receive state and emit callbacks; they never inject a repository.
- **`core/model` is Android-free** and must be unit-testable on the JVM with no Robolectric. The
  money and date logic lives there.
- **Deleting a card means two different things.** From a card's *tab*, delete takes its subscriptions
  with it (the dialog says how many). From the *management screen*, delete keeps them and they
  render as 「不明なカード」. `ON DELETE SET NULL` makes the second work; the first is done
  explicitly in `OfflinePaymentCardRepository`. Both are undoable and return a `DeletedCard`.
- **A null `cardId` is a normal state, not corruption.** Anything rendering a subscription must cope.
- **The summary header tracks the pager's *fractional* page**, not `currentPage`, so the total follows
  a swipe in real time. It was a reported bug (v1.0.2). Do not "simplify" it back.
- **One-off events use a `Channel`, not state.** Snackbars and undo actions are an `eventFlow` so
  they do not replay on rotation.
- **Exactly two reminder alarms, not one per subscription.** Whatever changes a reminder setting must
  call `ReminderScheduler.rescheduleAll()` afterwards. Details load with the reminders rule.
- **The stored payment date is an anchor.** `nextPaymentDate` is never advanced. Reminders, the
  countdown badge and 「支払日順」 all derive through `nextChargeDate` / `nextOccurrenceOnOrAfter`.
  Reading the anchor directly gives a subscription frozen in the past.
- **Money is `Double?` and stays `Double?`.** Round once, at format time. Never add currencies
  together — list totals per currency, JPY first. **Null means no amount was entered**; it must never
  be treated as zero, and a currency represented only by null rows does not appear at all.
- **An estimate has to say it is one.** `isEstimated` renders 「約¥4,000」. Every amount shown goes
  through `ui/util/formatAmount`; `ReminderNotifier` keeps a deliberate copy for the non-Compose
  side. A total inherits 「約」 from any estimate folded into it.
- **Experimental Compose opt-ins are centralised** in `app/build.gradle.kts` (`freeCompilerArgs`),
  not scattered as `@OptIn`.

## Toolchain

AGP 9 supplies Kotlin itself ("built-in Kotlin"), so **`org.jetbrains.kotlin.android` is not
applied** and the `kotlin { compilerOptions { … } }` block sits at the top level of
`app/build.gradle.kts`, not inside `android { }`. The `kotlin` version in the catalog only versions
the Compose compiler plugin.

`compileSdk`/`targetSdk` are 37 (androidx.lifecycle 2.11 and androidx.hilt 1.4 require it).
Robolectric lags the platform, so JVM tests are pinned to SDK 35 in
`app/src/test/resources/robolectric.properties`.

Versions live in `gradle/libs.versions.toml` — never hard-code a version in a build script.

## Language

- The app's UI is Japanese only. Japanese strings live in `res/values/strings.xml` — the *default*
  folder, not `values-ja/`.
- Code, comments, commit messages and everything under `.claude/` and `.agents/` are in **English**.
- `README.md`, `CHANGELOG.md` and `docs/` are in **Japanese** (the project owner reads Japanese).
- Conversation with the user is in **Japanese**.

## Git

- **Do not commit or push unless asked.**
- Commit messages carry no AI signature and no `Co-Authored-By` line.

## Where the detail lives

- `.agents/rules/` — layer rules that load by file path (Compose UI, Room/data, reminders, resources).
- `.agents/skills/` — `add-feature`, `room-migration`, `m3-design-review`.
- `docs/architecture.md`, `docs/migration-from-flutter.md`, `docs/roadmap.md` (Japanese).
