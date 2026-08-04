package com.subkan.core.model

import java.time.LocalDate

/**
 * The orderings offered in the app bar's sort menu and mirrored in Settings.
 *
 * [Registered] is "the order they were added", which is why it sorts on `createdAt` rather than
 * leaving the list untouched — the SQL result set has no inherent order to fall back on.
 */
enum class SubscriptionSort {
    Registered,
    Name,
    PaymentDate,
    ;

    companion object {
        fun fromNameOrDefault(value: String?): SubscriptionSort =
            entries.firstOrNull { it.name == value } ?: Registered
    }
}

/**
 * Applies [sort] and then [ascending].
 *
 * Direction is a reversal of the finished list rather than a flipped comparator, so descending
 * 「登録日順」 means newest first and ties keep a stable, predictable order either way.
 *
 * Sorting lives here rather than in SQL because the list is small, the comparison for [Name] is
 * case-insensitive in a way SQLite's `COLLATE NOCASE` does not extend to Japanese, and this is the
 * layer that gets unit tests.
 */
fun List<Subscription>.sorted(
    sort: SubscriptionSort,
    ascending: Boolean,
    today: LocalDate,
): List<Subscription> {
    val ordered = when (sort) {
        SubscriptionSort.Registered -> sortedBy { it.createdAt }
        SubscriptionSort.Name -> sortedBy { it.name.lowercase() }
        // The *next* charge, not the stored anchor: sorting on the anchor pins every past-dated
        // subscription to the top of the list forever.
        SubscriptionSort.PaymentDate -> sortedBy { it.nextChargeDate(today) }
    }
    return if (ascending) ordered else ordered.reversed()
}
