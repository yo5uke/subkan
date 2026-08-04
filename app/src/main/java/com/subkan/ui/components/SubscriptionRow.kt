package com.subkan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subkan.R
import com.subkan.core.model.AmountNotation
import com.subkan.core.model.BillingCycle
import com.subkan.core.model.PaymentCard
import com.subkan.core.model.PaymentStatus
import com.subkan.core.model.Subscription
import com.subkan.core.model.daysUntilPayment
import com.subkan.core.model.paymentStatus
import com.subkan.ui.theme.AmountMedium
import com.subkan.ui.theme.cardColor
import com.subkan.ui.theme.paymentStatusColor
import com.subkan.ui.util.formatAmountValue
import com.subkan.ui.util.showsEstimateMarker
import java.time.LocalDate

@Composable
fun SubscriptionRow(
    subscription: Subscription,
    card: PaymentCard?,
    today: LocalDate,
    showEstimatePrefix: Boolean,
    notation: AmountNotation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = card?.let { cardColor(it.colorHex) } ?: MaterialTheme.colorScheme.outline
    val status = subscription.paymentStatus(today)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The card's colour as a rule down the leading edge — enough to group rows by card at a
            // glance without tinting anything that has text on it.
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent),
            )

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ServiceIcon(serviceName = subscription.name)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accent,
                        )
                        Text(
                            text = card?.name ?: stringResource(R.string.unknown_card),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    AmountText(
                        amount = formatAmountValue(
                            amount = subscription.price,
                            currency = subscription.currency,
                            notation = notation,
                        ),
                        isEstimated = subscription.showsEstimateMarker(showEstimatePrefix),
                        notation = notation,
                        style = AmountMedium,
                        color = if (subscription.price == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = cycleLabel(subscription),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CountdownBadge(status, subscription.daysUntilPayment(today))
                    }
                }
            }
        }
    }
}

/** 「毎月5日」 / 「毎年3月5日」 — the recurring shape of the charge, not the stored date. */
@Composable
private fun cycleLabel(subscription: Subscription): String {
    val date = subscription.nextPaymentDate
    return when (subscription.billingCycle) {
        BillingCycle.Monthly -> stringResource(R.string.cycle_monthly, date.dayOfMonth)
        BillingCycle.Yearly ->
            stringResource(R.string.cycle_yearly, date.monthValue, date.dayOfMonth)
    }
}

@Composable
private fun CountdownBadge(status: PaymentStatus, daysUntil: Long) {
    val color = paymentStatusColor(status)
    val label = when (status) {
        PaymentStatus.Today -> stringResource(R.string.status_today)
        PaymentStatus.Soon, PaymentStatus.Later ->
            stringResource(R.string.status_days_left, daysUntil)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = BadgeTint))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

/**
 * The badge is a tint of its own text colour rather than a container role, so it stays legible
 * whatever hue dynamic colour hands `error` or `primary`.
 */
private const val BadgeTint = 0.12f
