---
name: m3-design-review
description: Reviews SubKan's Jetpack Compose screens against Material 3 guidelines and the project's own UI rules — colour roles, typography, touch targets, insets, dark theme, dynamic colour, Japanese text and accessibility. Use after building or changing a screen, when asked whether the UI looks right, or before a release. Read-only review that reports findings and does not edit files.
---

# Material 3 design review for SubKan

## Goal

Review Compose UI code for Material 3 correctness and report findings. **Do not edit files** — the
output is a report the user acts on.

## What to check against

Material 3 (m3.material.io) plus the project's own rules in `.agents/rules/compose-ui.md`. When a
guideline's specifics matter — exact tokens, component anatomy, minimum sizes — read the relevant page
on `m3.material.io` or `developer.android.com/develop/ui/compose` instead of reciting it from memory.
M3 has changed substantially across releases and stale specifics are worse than none.

## Instructions

Read the screens under review, then work through these checks. They are the ones that find real
problems in this app.

### Colour

- Any `Color(0xFF…)` outside `ui/theme/` — breaks dark theme and dynamic colour. Nothing checks this
  automatically, so search for it:
  `rg "Color\(\s*0x[0-9A-Fa-f]{6,8}" app/src/main/java/com/subkan/ui --glob "!**/theme/**"`.
- Container/on-container pairs mixed across roles. The summary header's
  `primaryContainer → secondaryContainer` gradient under `onPrimaryContainer` text is the one
  sanctioned case; anything else that mixes them is a finding.
- Semantic misuse: an imminent charge that is not `error`, secondary text that is not
  `onSurfaceVariant`, a primary action that is not `primary`.
- The two deliberate exceptions — `cardColor()` for a user-chosen card colour and
  `serviceAccentColors()` for the fallback logo tile — are *not* findings. Check instead that they are
  never a background under body text, and that the card colour appears only as an accent.
- Anything that would become illegible when dynamic colour supplies an unexpected hue.

### Typography and layout

- Type styles come from `MaterialTheme.typography`, or the project's `AmountLarge` / `AmountMedium`
  for money. Ad-hoc `TextStyle` in a screen file is a finding.
- Fixed `height` on text containers — clips at large font scales. Amounts must be free to wrap.
- Touch targets below 48dp.
- Spacing that is not a multiple of 4dp.

### Japanese text

- Labels that fit in English but overflow in Japanese. Every string here is Japanese, so check the
  longest one each component can receive. Card names and service names are user-supplied and
  unbounded.
- `maxLines` + `overflow` present wherever a card name, service name or the 「〈カード名〉の月額」
  summary label is rendered.
- CJK line-height: bare `Text` in a dense row without the project's type scale sits off-centre.
- The 「約」 marker must not be concatenated into an amount string in the UI; it belongs in
  `AmountText`.

### Structure

- Stable `key`s on every `LazyColumn` item.
- Edge-to-edge insets: screens own their top bar and FAB, the bottom tab row needs
  `navigationBarsPadding()`, and sheets with a text field need `imePadding()`.
- Empty states for every list that can be empty — including a card tab whose card has no
  subscriptions.
- Destructive actions are confirmed *and* undoable. Deleting a card from its tab removes its
  subscriptions, so it must state the count and be undoable; deleting from the management screen
  keeps them and must say so.
- The summary header reads the pager's fractional offset, not `currentPage`.
- Dialogs and sheets use `inheritedListItemColors()` for any `ListItem`.

### Accessibility

- `contentDescription` on every `Icon`/`IconButton`, or an explicit `null` justified by adjacent text.
- Gesture-only affordances have a non-gesture equivalent. Card reordering must keep its up/down buttons
  alongside the drag handle.
- Interactive elements are reachable and labelled for TalkBack.

## Report format

Group findings by file, most severe first. For each: the file and line, what is wrong, why it matters
*for a user* (not "violates M3"), and the concrete fix.

Separate "this is broken" from "this could be better", and say which is which. A screen with three
real accessibility bugs and eleven spacing nitpicks should not read as fourteen equal problems.

If a screen is fine, say so plainly and briefly. Do not manufacture findings to fill a report.

## Constraints

- Read-only: never modify files while running this skill.
- Report reasons in terms of user impact, and keep report text in Japanese when replying to the user;
  file paths and code identifiers stay as written.
