package com.subkan.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.subkan.R
import com.subkan.core.model.AmountKind
import com.subkan.core.model.AmountNotation
import com.subkan.core.model.Currency
import com.subkan.core.model.Subscription
import com.subkan.core.model.amountKind
import com.subkan.core.model.format

/**
 * The amount *value* only — never the 「約」 marker.
 *
 * The marker is a separate view (`ui/components/AmountText`), so these deliberately stop at the
 * number. Concatenating the two is what made rows containing 約 render taller than the rest.
 */
@Composable
@ReadOnlyComposable
fun formatAmountValue(
    amount: Double?,
    currency: Currency,
    notation: AmountNotation,
): String = amount?.let { currency.format(it, notation) }
    ?: stringResource(R.string.amount_unset)

/** True when the row should carry the 「約」 marker. */
fun Subscription.showsEstimateMarker(showEstimatePrefix: Boolean): Boolean =
    showEstimatePrefix && amountKind == AmountKind.Estimated
