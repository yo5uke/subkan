package com.subkan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.subkan.core.model.AmountNotation
import com.subkan.core.model.AppSettings
import com.subkan.core.model.NotificationSettings
import com.subkan.core.model.SubscriptionSort
import com.subkan.core.model.localTimeFromMinuteOfDay
import com.subkan.core.model.TabBarPosition
import com.subkan.core.model.ThemePreference
import com.subkan.core.model.toMinuteOfDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val defaults = NotificationSettings()
        AppSettings(
            theme = ThemePreference.fromNameOrDefault(prefs[Keys.Theme]),
            useDynamicColour = prefs[Keys.DynamicColour] ?: false,
            tabBarPosition = TabBarPosition.fromNameOrDefault(prefs[Keys.TabBarPosition]),
            sort = SubscriptionSort.fromNameOrDefault(prefs[Keys.Sort]),
            sortAscending = prefs[Keys.SortAscending] ?: true,
            showEstimatePrefix = prefs[Keys.ShowEstimatePrefix] ?: true,
            amountNotation = AmountNotation.fromNameOrDefault(prefs[Keys.AmountNotation]),
            notifications = NotificationSettings(
                notifyDayBefore = prefs[Keys.NotifyDayBefore] ?: defaults.notifyDayBefore,
                // Stored as minute-of-day for the same reason payment dates are stored as day
                // numbers: it is a wall-clock time the user picked, not an instant.
                dayBeforeTime = prefs[Keys.DayBeforeMinute]?.let(::localTimeFromMinuteOfDay)
                    ?: defaults.dayBeforeTime,
                notifyOnDay = prefs[Keys.NotifyOnDay] ?: defaults.notifyOnDay,
                onDayTime = prefs[Keys.OnDayMinute]?.let(::localTimeFromMinuteOfDay) ?: defaults.onDayTime,
            ),
            notificationPermissionRequested = prefs[Keys.PermissionRequested] ?: false,
        )
    }

    suspend fun setTheme(theme: ThemePreference) = edit { it[Keys.Theme] = theme.name }

    suspend fun setDynamicColour(enabled: Boolean) = edit { it[Keys.DynamicColour] = enabled }

    suspend fun setTabBarPosition(position: TabBarPosition) =
        edit { it[Keys.TabBarPosition] = position.name }

    suspend fun setSort(sort: SubscriptionSort) = edit { it[Keys.Sort] = sort.name }

    suspend fun setSortAscending(ascending: Boolean) =
        edit { it[Keys.SortAscending] = ascending }

    suspend fun setShowEstimatePrefix(show: Boolean) =
        edit { it[Keys.ShowEstimatePrefix] = show }

    suspend fun setAmountNotation(notation: AmountNotation) =
        edit { it[Keys.AmountNotation] = notation.name }

    suspend fun setNotifyDayBefore(enabled: Boolean) =
        edit { it[Keys.NotifyDayBefore] = enabled }

    suspend fun setDayBeforeTime(time: LocalTime) =
        edit { it[Keys.DayBeforeMinute] = time.toMinuteOfDay() }

    suspend fun setNotifyOnDay(enabled: Boolean) = edit { it[Keys.NotifyOnDay] = enabled }

    suspend fun setOnDayTime(time: LocalTime) =
        edit { it[Keys.OnDayMinute] = time.toMinuteOfDay() }

    suspend fun setNotificationPermissionRequested(requested: Boolean) =
        edit { it[Keys.PermissionRequested] = requested }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private object Keys {
        val Theme = stringPreferencesKey("theme")
        val DynamicColour = booleanPreferencesKey("dynamic_colour")
        val TabBarPosition = stringPreferencesKey("tab_bar_position")
        val Sort = stringPreferencesKey("subscription_sort")
        val SortAscending = booleanPreferencesKey("subscription_sort_ascending")
        val ShowEstimatePrefix = booleanPreferencesKey("show_estimate_prefix")
        val AmountNotation = stringPreferencesKey("amount_notation")
        val NotifyDayBefore = booleanPreferencesKey("notify_day_before")
        val DayBeforeMinute = intPreferencesKey("notify_day_before_minute")
        val NotifyOnDay = booleanPreferencesKey("notify_on_day")
        val OnDayMinute = intPreferencesKey("notify_on_day_minute")
        val PermissionRequested = booleanPreferencesKey("notification_permission_requested")
    }
}
