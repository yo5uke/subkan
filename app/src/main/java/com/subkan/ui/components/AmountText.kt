package com.subkan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.subkan.R
import com.subkan.core.model.AmountNotation

/**
 * An amount, optionally marked as an estimate.
 *
 * **The 「約」 is a separate `Text`, deliberately.** Inlining it into the amount string puts a CJK
 * glyph into an otherwise Latin run, so font fallback resolves a Japanese face whose ascent and
 * descent are taller than the Latin one — and the line box grows. The visible result is that
 * 「約 ¥10,000」 sits at a different height from 「$20」 in the same list, which is exactly the
 * jump this splits apart.
 *
 * Keeping the marker in its own node also lets it be smaller and quieter than the number, so it
 * cannot drive the row's height either way.
 *
 * In [AmountNotation.Japanese] the amount already ends in 円 / ドル, so the whole column is CJK and
 * there is no mismatch to avoid — but the split costs nothing and keeps one code path.
 */
@Composable
fun AmountText(
    amount: String,
    isEstimated: Boolean,
    notation: AmountNotation,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (!isEstimated) {
        Text(text = amount, style = style, color = color, modifier = modifier)
        return
    }

    // Two views, one thing to say. Without this a screen reader announces 「約」 and the amount as
    // separate items, which is the price of splitting them for layout.
    val spoken = stringResource(R.string.cd_amount_estimated, amount)

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
        // 「約4,000円」 reads as one word; 「約 ¥4,000」 needs air, because the symbol is a prefix
        // too and the two would otherwise collide.
        horizontalArrangement = Arrangement.spacedBy(
            when (notation) {
                AmountNotation.Symbol -> 4.dp
                AmountNotation.Japanese -> 1.dp
            },
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.amount_estimate_marker),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
        Text(text = amount, style = style, color = color)
    }
}
