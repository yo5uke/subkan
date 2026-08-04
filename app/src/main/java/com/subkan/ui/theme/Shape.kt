package com.subkan.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 shape scale, with `large` raised to 24dp.
 *
 * Subscription rows use it, and at the size of a list card the stock 16dp reads as a rounded
 * rectangle rather than as the soft, tactile card the summary header is shaped like — the two sit
 * directly above each other, so they need to agree.
 */
internal val SubKanShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
