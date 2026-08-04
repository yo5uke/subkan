package com.subkan.core.model

enum class ThemePreference {
    System,
    Light,
    Dark,
    ;

    companion object {
        fun fromNameOrDefault(value: String?): ThemePreference =
            entries.firstOrNull { it.name == value } ?: System
    }
}

/**
 * Where the card tab row sits.
 *
 * A real setting rather than a fixed layout because the tab row is the app's primary control and
 * the phones this app is used on are tall enough that the top of the screen is a stretch.
 */
enum class TabBarPosition {
    Top,
    Bottom,
    ;

    companion object {
        fun fromNameOrDefault(value: String?): TabBarPosition =
            entries.firstOrNull { it.name == value } ?: Top
    }
}

data class AppSettings(
    val theme: ThemePreference = ThemePreference.System,
    /**
     * Off by default. SubKan's seed colour is the app's identity — the launcher icon is that
     * purple — so the wallpaper only takes over when the user asks it to.
     */
    val useDynamicColour: Boolean = false,
    val tabBarPosition: TabBarPosition = TabBarPosition.Top,
    val sort: SubscriptionSort = SubscriptionSort.Registered,
    val sortAscending: Boolean = true,
    /**
     * Whether a rough amount is marked 「約」 wherever it is shown.
     *
     * On by default: hiding it makes an estimate read as a fixed figure, which is the more
     * misleading of the two states to be wrong about.
     */
    val showEstimatePrefix: Boolean = true,
    val amountNotation: AmountNotation = AmountNotation.Symbol,
    val notifications: NotificationSettings = NotificationSettings(),
    /**
     * Whether the OS notification permission has been asked for yet.
     *
     * Stored rather than inferred because "not granted" and "never asked" look identical to the
     * app, and the system dialog only appears the first time. It stays false for anyone who
     * installed before reminders existed, which is what makes them see the prompt on next open.
     */
    val notificationPermissionRequested: Boolean = false,
)
