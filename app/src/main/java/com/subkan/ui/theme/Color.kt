package com.subkan.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/*
 * SubKan's baseline palette.
 *
 * The app's seed colour is #6750A4 — which is exactly the seed Material 3's own baseline scheme is
 * generated from. So rather than hand-copying sixty tonal values into this file and letting them
 * drift, the schemes are Compose's defaults, which *are* that palette.
 *
 * This scheme is the fallback. On Android 12+ the user can switch on dynamic colour and the system
 * palette takes over — see `SubKanTheme`.
 *
 * If the brand colour ever moves off #6750A4, this is where it stops being a one-line file: build
 * the tonal palette properly (as TasKan's Color.kt does) instead of nudging individual roles.
 */

internal val SubKanLightColorScheme = lightColorScheme()

internal val SubKanDarkColorScheme = darkColorScheme()
