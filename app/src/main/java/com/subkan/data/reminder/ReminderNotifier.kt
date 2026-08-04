package com.subkan.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.subkan.MainActivity
import com.subkan.R
import com.subkan.core.model.AmountNotation
import com.subkan.core.model.Currency
import com.subkan.core.model.ReminderKind
import com.subkan.core.model.Subscription
import com.subkan.core.model.format
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and posts the payment reminder.
 *
 * One notification per [ReminderKind], covering every subscription due that day — 「明日は 3 件の
 * 支払日です」 rather than three separate buzzes.
 */
@Singleton
class ReminderNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /**
     * Created up front rather than on the first reminder, so the channel is already there for the
     * user to configure in system settings before anything has fired.
     */
    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyDue(
        kind: ReminderKind,
        subscriptions: List<Subscription>,
        showEstimatePrefix: Boolean,
        notation: AmountNotation,
    ) {
        if (subscriptions.isEmpty()) return

        val manager = NotificationManagerCompat.from(context)
        // Posting without permission throws on some OEM builds and is a silent no-op on others;
        // either way there is nothing to gain by trying.
        if (!manager.areNotificationsEnabled()) return

        val title = title(kind, subscriptions)
        val body = body(subscriptions, showEstimatePrefix, notation)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(kind))
            .build()

        try {
            manager.notify(kind.notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check above and here. Nothing useful to do.
        }
    }

    private fun title(kind: ReminderKind, subscriptions: List<Subscription>): String =
        if (subscriptions.size == 1) {
            val name = subscriptions.single().name
            when (kind) {
                ReminderKind.DayBefore ->
                    context.getString(R.string.notification_title_day_before_single, name)

                ReminderKind.OnDay ->
                    context.getString(R.string.notification_title_on_day_single, name)
            }
        } else {
            when (kind) {
                ReminderKind.DayBefore ->
                    context.getString(
                        R.string.notification_title_day_before_multi,
                        subscriptions.size,
                    )

                ReminderKind.OnDay ->
                    context.getString(R.string.notification_title_on_day_multi, subscriptions.size)
            }
        }

    /**
     * A single charge shows its own price; a handful show their names and a total; a long list
     * drops the names.
     *
     * Past [MAX_LISTED_NAMES] the names stop being information and start being a wall of text —
     * the title already says how many there are, so the total is the only part still worth reading.
     *
     * The total is the amount actually leaving the account that day, so a yearly plan contributes
     * its full price here — unlike the 月額合計 on the list screen, which divides by twelve.
     */
    private fun body(
        subscriptions: List<Subscription>,
        showEstimatePrefix: Boolean,
        notation: AmountNotation,
    ): String {
        if (subscriptions.size == 1) {
            val sub = subscriptions.single()
            return formatAmount(
                sub.price,
                sub.currency,
                sub.isEstimated,
                showEstimatePrefix,
                notation,
            )
        }

        val totals = subscriptions
            // Amounts that were never entered contribute nothing rather than counting as zero.
            .mapNotNull { sub -> sub.price?.let { sub to it } }
            .groupBy { (sub, _) -> sub.currency }
            .toList()
            .sortedBy { (currency, _) -> currency.ordinal }
            .joinToString(" + ") { (currency, entries) ->
                formatAmount(
                    amount = entries.sumOf { (_, price) -> price },
                    currency = currency,
                    isEstimated = entries.any { (sub, _) -> sub.isEstimated },
                    showEstimatePrefix = showEstimatePrefix,
                    notation = notation,
                )
            }

        val total = if (totals.isEmpty()) {
            context.getString(R.string.amount_unset)
        } else {
            context.getString(R.string.notification_body_total, totals)
        }

        return if (subscriptions.size > MAX_LISTED_NAMES) {
            total
        } else {
            subscriptions.joinToString("、") { it.name } + "\n" + total
        }
    }

    /**
     * The notification's counterpart to `ui/util/formatAmountValue` + `ui/components/AmountText`.
     *
     * Here the marker *is* concatenated, because notification text has no layout to split it
     * across — and no line-height mismatch to avoid either, since there is nothing beside it to
     * line up with. In symbol notation it takes a space, so 「約」 and 「¥」 do not run together.
     */
    private fun formatAmount(
        amount: Double?,
        currency: Currency,
        isEstimated: Boolean,
        showEstimatePrefix: Boolean,
        notation: AmountNotation,
    ): String {
        if (amount == null) return context.getString(R.string.amount_unset)

        val value = currency.format(amount, notation)
        if (!isEstimated || !showEstimatePrefix) return value

        return when (notation) {
            AmountNotation.Symbol ->
                context.getString(R.string.amount_estimated_symbol, value)

            AmountNotation.Japanese ->
                context.getString(R.string.amount_estimated_japanese, value)
        }
    }

    private fun openAppIntent(kind: ReminderKind): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            kind.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val CHANNEL_ID = "payment_reminders"

        /** Above this, the notification gives the count and the total but not the names. */
        const val MAX_LISTED_NAMES = 6
    }
}

/** Distinct per kind, so the morning reminder does not replace last night's. */
internal val ReminderKind.notificationId: Int
    get() = when (this) {
        ReminderKind.DayBefore -> 2001
        ReminderKind.OnDay -> 2002
    }
