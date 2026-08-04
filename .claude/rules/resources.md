---
paths:
  - "app/src/main/res/**"
---

# Android resource rules

## Strings are Japanese, in the default folder

`res/values/strings.xml` is Japanese and it is the *default* — there is no `values-ja/`. That is
deliberate: the app is Japanese-only, and putting the strings in the default folder makes it read
the same on a device set to any language, which is what the Flutter build did with
`supportedLocales: [ja_JP]`.

Do not add a `values-ja/` folder "for correctness". It would only take effect on Japanese devices
and leave every other device with whatever ended up in `values/`.

If the app is ever localised, the move is: `values/` becomes English, the current Japanese moves to
`values-ja/`, and the format arguments below start to matter.

Positional format args (`%1$s`, `%1$d`) are used even though there is one locale — they cost nothing
and they are what a translation would need.

## Colours

`res/values/colors.xml` is only for the launch window and the adaptive icon. Every in-app colour
lives in `ui/theme/` so it can participate in the Material 3 scheme and dynamic colour.

## Icons

The launcher icon is an adaptive icon: `mipmap-anydpi-v26/ic_launcher.xml` plus
`drawable-*dpi/ic_launcher_foreground.png`. `minSdk` is 26, so no legacy square `mipmap-*/` PNGs are
needed — do not add them.

The source artwork is `art/logo.png` (1984×2114, purple mark on white). The density PNGs are
generated from it, so **edit the source and regenerate — never touch the density files by hand.**
Four things about that generation are deliberate:

- The crop that finds the mark thresholds at **>40 difference from white, not >12**. The source
  carries a near-white decorative wash in the bottom-right corner; a loose threshold pulls it into
  the bounding box, which pads the crop down and right and leaves the mark visibly up and left of
  centre once that crop is centred. After regenerating, check the ink margins are symmetric rather
  than trusting the eye.

- The foreground is **opaque white across the whole 108dp canvas**, not a transparent cut-out.
  The artwork is drawn on white, and keying that out leaves a fringe along every antialiased edge.
  `ic_launcher_background` is white to match, so the two layers agree and nothing shows through.
- The mark is scaled to **60% of the canvas**, not the full 72dp safe zone. It is taller than it is
  wide and its extremes are the two arrowheads, so sizing it to the safe zone puts them exactly on a
  circular mask's edge, where they read as clipped.
- The splash screen reuses the same drawable and therefore sets
  `windowSplashScreenIconBackgroundColor` to the same white. Without it the purple mark sits on the
  dark splash background in dark theme and all but vanishes.

There is no `monochrome` layer. The mark has interior detail — the calendar grid and the tick — that
a flat silhouette would fill in, so themed icons fall back to the full-colour one.

## Themes

The XML theme exists to bridge the launch window into Compose. Do not add app styling there; it will
not match dynamic colour and the Compose UI will override it anyway.
