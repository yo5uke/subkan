package com.subkan.ui.components

import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * `ListItem` colours for a list that sits on something other than `surface`.
 *
 * `ListItemDefaults.colors()` paints a `surface` background. That is right on a screen, whose
 * background is also `surface` — but a dialog is `surfaceContainerHigh` and a bottom sheet is
 * `surfaceContainerLow`, so the default turns the rows into a visibly lighter block floating inside
 * the container. In the light theme that block reads as plain white.
 *
 * A transparent container makes the rows inherit whatever they were placed on, which is what a list
 * inside a dialog or a sheet should do.
 */
@Composable
fun inheritedListItemColors(): ListItemColors =
    ListItemDefaults.colors(containerColor = Color.Transparent)
