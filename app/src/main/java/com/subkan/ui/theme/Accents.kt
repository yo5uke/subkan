package com.subkan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.subkan.core.model.PaymentStatus
import com.subkan.core.model.ServiceAccent

/**
 * The colour a card's stored hex resolves to.
 *
 * This is user data, not a theme token — the whole point of the setting is that 楽天カード stays
 * red whatever the rest of the app is doing — so it deliberately bypasses the colour scheme. It is
 * only ever used as a small accent (a dot, a 6dp rule down the side of a row) against a themed
 * surface, never as a background behind text.
 */
fun cardColor(colorHex: String): Color =
    Color(colorHex.toLongOrNull(radix = 16)?.or(0xFF000000L)?.toInt() ?: FallbackCardColor)

private const val FallbackCardColor: Int = 0xFF607D8B.toInt()

/**
 * The countdown badge's colour, by bucket.
 *
 * Today and Soon share `error` because they share an urgency; the badge text (今日 / あと2日) is
 * what distinguishes them.
 */
@Composable
@ReadOnlyComposable
fun paymentStatusColor(status: PaymentStatus): Color = when (status) {
    PaymentStatus.Today, PaymentStatus.Soon -> MaterialTheme.colorScheme.error
    PaymentStatus.Later -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * The gradient pair and letter colour of a service's fallback tile.
 *
 * These are fixed rather than scheme-derived: the tile stands in for a brand logo, so its job is to
 * be *distinctive and stable*, not to blend into the surface. Eight tints, each with a matching
 * dark on-colour taken from the same tonal family, so contrast holds in either theme.
 */
fun serviceAccentColors(accent: ServiceAccent): Triple<Color, Color, Color> = when (accent) {
    ServiceAccent.Purple -> Triple(Color(0xFFEADDFF), Color(0xFFD0BCFF), Color(0xFF21005D))
    ServiceAccent.Rose -> Triple(Color(0xFFFFD8E4), Color(0xFFF2B8B5), Color(0xFF410002))
    ServiceAccent.Blue -> Triple(Color(0xFFD3E4FF), Color(0xFFADC1F9), Color(0xFF001D35))
    ServiceAccent.Green -> Triple(Color(0xFFC7F3D6), Color(0xFF8CE7A8), Color(0xFF072100))
    ServiceAccent.Orange -> Triple(Color(0xFFFFE0B2), Color(0xFFFFB74D), Color(0xFF4E342E))
    ServiceAccent.Lime -> Triple(Color(0xFFE2F1AF), Color(0xFFC5E1A5), Color(0xFF253600))
    ServiceAccent.Cyan -> Triple(Color(0xFFE0F7FA), Color(0xFF80DEEA), Color(0xFF00363A))
    ServiceAccent.Magenta -> Triple(Color(0xFFF3E5F5), Color(0xFFCE93D8), Color(0xFF4A0072))
}
