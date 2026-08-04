package com.subkan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.subkan.core.model.ServiceAccent
import com.subkan.core.model.serviceInitial
import com.subkan.data.icon.ServiceIconUrl
import com.subkan.ui.theme.serviceAccentColors

private val IconSize = 48.dp
private val IconShape = RoundedCornerShape(14.dp)

/**
 * A service's logo, or a lettered tile when there is no logo to show.
 *
 * The tile is also what is shown *while* loading, rather than a blank grey box — a list scrolled
 * into view should never flash empty squares.
 */
@Composable
fun ServiceIcon(
    serviceName: String,
    modifier: Modifier = Modifier,
) {
    val url = remember(serviceName) { ServiceIconUrl.forServiceName(serviceName) }

    Box(modifier = modifier.size(IconSize).clip(IconShape)) {
        if (url == null) {
            ServiceInitialTile(serviceName)
        } else {
            SubcomposeAsyncImage(
                model = url,
                // The name is already rendered next to the icon, so announcing it twice would only
                // slow a screen reader down.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(IconSize),
                loading = { ServiceInitialTile(serviceName) },
                error = { ServiceInitialTile(serviceName) },
            )
        }
    }
}

/**
 * The fallback tile: a two-stop gradient chosen from the service name, with its initial on top.
 *
 * Fixed colours rather than scheme colours — see `serviceAccentColors`.
 */
@Composable
private fun ServiceInitialTile(serviceName: String) {
    val accent = remember(serviceName) { ServiceAccent.forName(serviceName) }
    val (start, end, onAccent) = serviceAccentColors(accent)

    Box(
        modifier = Modifier
            .size(IconSize)
            .background(Brush.linearGradient(listOf(start, end))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = serviceInitial(serviceName),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = onAccent,
        )
    }
}
