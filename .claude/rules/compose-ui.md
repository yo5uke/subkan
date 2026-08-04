---
paths:
  - "app/src/main/java/com/subkan/ui/**"
---

# Compose & Material 3 conventions

## Colour

Every colour comes from `MaterialTheme.colorScheme`. A hard-coded `Color(0xFF…)` inside a `ui/` file
outside `ui/theme/` is a bug: the app supports light theme, dark theme, and dynamic colour from the
wallpaper on Android 12+, and a literal breaks at least one. A `PostToolUse` hook checks this.

Two things in `ui/theme/` are deliberately *not* scheme colours, and both are documented where they
are defined:

- `cardColor(colorHex)` — the user picked that colour for that card, so it must not shift with the
  theme. It is only ever an accent (a dot, a 6dp rule down a row edge), never a background under
  text.
- `serviceAccentColors(...)` — the lettered fallback tile stands in for a brand logo; its job is to
  be distinctive and stable, not to blend in.

Otherwise use the semantic role, not the one that happens to look right — `error` for an imminent
charge, `primary` for the main action, `onSurfaceVariant` for secondary text. Container/on-container
colours are contrast-checked as a *pair*. The one place the app mixes them — the summary header's
`primaryContainer → secondaryContainer` gradient under `onPrimaryContainer` text — is safe only
because M3 puts every `*Container` role at the same tone. Do not generalise from it.

## State

Screens take `UiState` from a ViewModel via `collectAsStateWithLifecycle()` and emit callbacks
upward. A composable that injects a repository, or calls `viewModelScope`, is in the wrong layer.

Local UI concerns — which sheet is open, which editor target is selected — stay in the composable as
`remember { mutableStateOf(...) }`. Anything that must survive process death uses `rememberSaveable`.

## The tab row and pager

The card tabs are a filter over one list, so they are a `HorizontalPager` plus
`PrimaryScrollableTabRow` — not navigation. Two consequences:

- The summary header reads `pagerState.currentPage + pagerState.currentPageOffsetFraction` and
  rounds. Using `currentPage` alone leaves the total stale mid-swipe.
- Long-press on a card tab opens its action sheet. That gesture lives on the *label* inside `Tab`,
  not on the `Tab` itself, so it does not fight the tab's own selection handling.

## Surfaces inside surfaces

`ListItemDefaults.colors()` paints `surface`. That is correct on a screen, but a dialog is
`surfaceContainerHigh` and a bottom sheet is `surfaceContainerLow` — so the default turns a list
inside either into a lighter block floating in the container, which reads as plain white in the
light theme.

Any `ListItem` placed in a dialog or a sheet takes `inheritedListItemColors()` so it inherits what
it was placed on. The same caution applies to any component with its own container colour: check
what it is sitting on before accepting the default.

## Amounts

Never concatenate the 「約」 marker into an amount string in the UI. `"約" + "¥4,000"` puts a CJK
glyph into an otherwise Latin run, font fallback resolves a Japanese face with taller ascent and
descent, and that one row renders taller than 「$20」 next to it. It looks like a spacing bug and is
not one.

`ui/components/AmountText` is the only place that renders an amount with its marker: the marker is a
separate, smaller `Text`, so it cannot drag the line box. `ui/util/formatAmountValue` deliberately
stops at the number.

`ReminderNotifier` does concatenate, and that is fine — notification text has no layout and nothing
beside it to line up with. It is also the reason there are two 「約」 string resources: a bare marker
for the UI and joined forms for the notification.

Amounts follow `AmountNotation`: `¥1,480` by default, `1,480円` when the user switches to Japanese
notation. Anything rendering money takes the setting rather than assuming the symbol form.

## Lists

`LazyColumn` items always pass a stable `key`. Without it, editing one subscription re-animates
unrelated rows.

## Text and accessibility

- No string literals in composables — everything goes through `stringResource`.
- Every `Icon` and `IconButton` either has a real `contentDescription` or an explicit `null` because
  an adjacent `Text` already says it.
- Do not set fixed `height` on anything containing text; large font scales must not clip. Amounts in
  particular are allowed to wrap.
- Any gesture-only affordance needs a non-gesture equivalent. Card reordering is drag-to-move *and*
  explicit up/down buttons for exactly this reason.

## Insets

The app is edge-to-edge. Each screen's `Scaffold` owns its top bar and FAB. The bottom tab row adds
`.navigationBarsPadding()`, and any sheet with a text field also adds `.imePadding()`.

## Experimental APIs

Opt-ins are declared centrally in `app/build.gradle.kts`. If a new API needs one, add it there
rather than sprinkling `@OptIn` through screen files — and prefer a stable API where one exists.
