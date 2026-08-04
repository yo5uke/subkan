package com.subkan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * The Material 3 type scale, with CJK-friendly line-height alignment.
 *
 * The platform font is deliberate. The Flutter build shipped Inter and Outfit through
 * `google_fonts`; on Android the system font is what renders Japanese, Latin and emoji correctly
 * together without bundling anything, and its metrics are already tuned for accessibility scaling.
 * The distinctive look of the amounts is carried by [AmountLarge] / [AmountMedium] below — weight
 * and letter spacing rather than a typeface.
 */
private val Default = Typography()

/**
 * Trim-both alignment stops Japanese service names sitting off-centre in a list row — CJK glyphs
 * are taller than Latin ones, so the default first-line padding shows.
 */
private val CjkFriendlyLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun TextStyle.cjkFriendly(): TextStyle = copy(lineHeightStyle = CjkFriendlyLineHeight)

internal val SubKanTypography = Typography(
    displayLarge = Default.displayLarge.cjkFriendly(),
    displayMedium = Default.displayMedium.cjkFriendly(),
    displaySmall = Default.displaySmall.cjkFriendly(),
    headlineLarge = Default.headlineLarge.cjkFriendly(),
    headlineMedium = Default.headlineMedium.cjkFriendly(),
    headlineSmall = Default.headlineSmall.cjkFriendly(),
    titleLarge = Default.titleLarge.copy(fontWeight = FontWeight.SemiBold).cjkFriendly(),
    titleMedium = Default.titleMedium.cjkFriendly(),
    titleSmall = Default.titleSmall.cjkFriendly(),
    bodyLarge = Default.bodyLarge.cjkFriendly(),
    bodyMedium = Default.bodyMedium.cjkFriendly(),
    bodySmall = Default.bodySmall.cjkFriendly(),
    labelLarge = Default.labelLarge.cjkFriendly(),
    labelMedium = Default.labelMedium.cjkFriendly(),
    labelSmall = Default.labelSmall.cjkFriendly(),
)

/**
 * The summary header's total. Heavy and slightly tightened, because this one number is the reason
 * the user opened the app.
 *
 * No fixed height anywhere near it — at a 2x font scale this wraps, and that is correct.
 */
internal val AmountLarge: TextStyle = Default.displaySmall.copy(
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = (-0.5).sp,
).cjkFriendly()

/** The per-row price. Same treatment, list-row size. */
internal val AmountMedium: TextStyle = Default.titleLarge.copy(
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.25).sp,
).cjkFriendly()
