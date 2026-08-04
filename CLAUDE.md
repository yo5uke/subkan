# SubKan

Android subscription tracker. The name is 「サブスク管理」 shortened. Single Gradle module, Kotlin +
Jetpack Compose + Material 3, fully offline (Room), no backend.

## What makes this app different

Two ideas drive almost every design decision:

1. **The 月額合計 is the product.** Everything else — the list, the cards, the sort order — exists to
   make one number trustworthy. A yearly plan contributes `price / 12`, so plans on different cycles
   are directly comparable.
2. **Cards are a lens, not a hierarchy.** 「すべて」 plus one tab per payment card are filters over a
   single list, which is why they are a `HorizontalPager` and not a nav graph. A subscription with no
   card is still a subscription.

If a change makes either of those harder, it is probably the wrong change.

## Commands

Run from the repo root. `JAVA_HOME` must point at a JDK 17+ — Android Studio's bundled one works:
`C:\Program Files\Android\Android Studio\jbr`.

| Task | Command |
| --- | --- |
| Compile | `./gradlew :app:compileDebugKotlin` |
| Unit tests | `./gradlew :app:testDebugUnitTest` |
| Debug APK | `./gradlew :app:assembleDebug` |
| Install on a device | `./gradlew :app:installDebug` |
| Everything | `./gradlew :app:assembleDebug :app:testDebugUnitTest` |

A clean build takes ~2–3 minutes; incremental compiles are a few seconds. Prefer
`compileDebugKotlin` + `testDebugUnitTest` while iterating and save `assembleDebug` for when the APK
is actually needed.

## Layout

```
app/src/main/java/com/subkan/
├── core/model/      Domain types + pure logic (totals, sorting, countdown buckets). No Android imports.
├── core/time/       AppClock — the seam that makes 「あと N 日」 testable.
├── data/local/      Room: entities, DAOs, database. Entities own the `*_snake_case` column names.
├── data/repository/ Repository interfaces + Offline* implementations. UI never touches a DAO.
├── data/preferences/DataStore-backed settings.
├── data/icon/       Service name → logo URL. Pure, and deliberately allowed to answer "no URL".
├── data/reminder/   AlarmManager scheduling, the notification itself, and its two receivers.
├── data/di/         Hilt modules (DatabaseModule, RepositoryModule).
└── ui/
    ├── theme/       M3 colour scheme, type scale, shapes, card + service accent colours.
    ├── components/  Subscription row, service icon, empty state, time picker dialog.
    ├── permissions/ The notification permission handshake.
    ├── home/        The subscription list — tabs, pager, summary header, sort menu.
    ├── editor/      Subscription editor sheet, card editor dialog, card action sheets, reorder list.
    ├── cards/       Card management screen.
    └── settings/    Settings screen.
```

`app/schemas/` holds exported Room schema JSON and is committed — it is what makes migrations
reviewable.

## Conventions

- **Data flows one way.** DAO → Repository (maps entity → domain) → ViewModel (`StateFlow<UiState>`)
  → Composable. Composables receive state and emit callbacks; they never inject a repository.
- **Domain models are Android-free.** Anything in `core/model` must be unit-testable on the JVM with
  no Robolectric. That is deliberate — it is where the money and date logic lives.
- **Deleting a card means two different things, and both are real.** From a card's *tab*, delete
  takes its subscriptions with it (the dialog says how many). From the *management screen*, delete
  keeps them and they render as 「不明なカード」. `ON DELETE SET NULL` is what makes the second one
  work; the first is done explicitly in `OfflinePaymentCardRepository`. Both are undoable, so both
  return a `DeletedCard` carrying enough to reverse themselves.
- **A null `cardId` is a normal state, not corruption.** Anything rendering a subscription must cope
  with it.
- **The summary header tracks the pager's *fractional* page**, not `currentPage`. That is what makes
  the total follow a swipe in real time instead of snapping when the page settles — it was a
  reported bug once already (v1.0.2). Do not "simplify" it back.
- **One-off events use a `Channel`, not state.** Snackbars and their undo actions are `eventFlow`, so
  they fire once instead of replaying on rotation.
- **There are exactly two reminder alarms, not one per subscription.** Each carries only its
  `ReminderKind`; what is *due* is looked up when it fires. That is why editing a subscription never
  touches the alarms, and why five charges on the same day are one notification. Whatever changes a
  reminder setting must call `ReminderScheduler.rescheduleAll()` afterwards — both *whether* and
  *when* a reminder fires are decided at scheduling time.
- **The stored payment date is an anchor; everything else derives from it.** `nextPaymentDate` is
  never advanced. Reminders, the countdown badge and 「支払日順」 all go through
  `nextChargeDate` / `nextOccurrenceOnOrAfter`, which rolls the anchor forward by the billing cycle.
  Reading the anchor directly gets you a subscription frozen in the past — that was a real bug in
  both the badge and the sort. Nothing is written back, which is what keeps 「毎月5日」 saying the
  5th even after a February clamps it to the 28th.
- **Money is `Double?` and stays `Double?`.** A yearly price divided by twelve is genuinely
  fractional; rounding happens once, at format time. Currencies are never added together — totals
  are listed per currency, JPY first. **Null means no amount was entered** (光熱費 registered by
  name alone) and must never be treated as zero: it contributes nothing to a total, and a currency
  represented only by such rows does not appear at all.
- **An estimate has to say it is one.** `isEstimated` marks a rough figure, rendered 「約¥4,000」.
  Every amount the UI shows goes through `ui/util/formatAmount` so the prefix cannot be applied in
  one place and forgotten in another; `ReminderNotifier` keeps a deliberate copy for the
  non-Compose side. A total inherits 「約」 from any estimate folded into it.
- **Experimental Compose opt-ins are centralised** in `app/build.gradle.kts` (`freeCompilerArgs`)
  rather than scattered `@OptIn` annotations.

## Toolchain notes

AGP 9 supplies Kotlin itself ("built-in Kotlin"), so **`org.jetbrains.kotlin.android` is not
applied** and the `kotlin { compilerOptions { … } }` block sits at the top level of
`app/build.gradle.kts`, not inside `android { }`. The `kotlin` version in the catalog only versions
the Compose compiler plugin.

`compileSdk`/`targetSdk` are 37 because androidx.lifecycle 2.11 and androidx.hilt 1.4 require it.
Robolectric lags the platform, so JVM tests are pinned to SDK 35 in
`app/src/test/resources/robolectric.properties`.

Versions live in `gradle/libs.versions.toml` — never hard-code a version in a build script.

## Language

The app's UI is Japanese only, so Japanese lives in `res/values/strings.xml` — the *default* folder,
not `values-ja/`. That is what makes the app read the same on a device set to any language. There is
no second locale to keep in sync; if one is ever added, this file becomes English and the Japanese
moves to `values-ja/`.

Code, comments, commit messages and everything under `.claude/` are in English. `README.md`,
`CHANGELOG.md` and `docs/` are in Japanese — they explain decisions to the project's owner, who
reads Japanese. Conversation with the user is in Japanese.

## Further reading

- `.claude/rules/` — layer-specific rules, applied automatically by path (Compose UI, Room, Android
  resources, reminders).
- `docs/architecture.md` — why each library was chosen, and the data model in detail.
- `docs/migration-from-flutter.md` — what the Flutter build did and where each piece landed.
- `docs/roadmap.md` — what is deliberately not built yet.

## Other notes

- Do not include Claude's signature when committing.
- Do not commit or push unless asked.
