package com.subkan.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subkan.core.model.AmountNotation
import com.subkan.core.model.AppSettings
import com.subkan.core.model.SubscriptionSort
import com.subkan.core.model.TabBarPosition
import com.subkan.core.model.ThemePreference
import com.subkan.data.preferences.SettingsRepository
import com.subkan.data.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    private val _permissionGranted = MutableStateFlow(true)

    /**
     * Granted outside the app, in system settings, with nothing to tell us when it changes — so the
     * screen re-reads it every time it comes back to the foreground.
     */
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    fun refreshPermission(granted: Boolean) {
        _permissionGranted.value = granted
    }

    fun setTheme(theme: ThemePreference) = viewModelScope.launch {
        settingsRepository.setTheme(theme)
    }

    fun setDynamicColour(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setDynamicColour(enabled)
    }

    fun setTabBarPosition(position: TabBarPosition) = viewModelScope.launch {
        settingsRepository.setTabBarPosition(position)
    }

    fun setSort(sort: SubscriptionSort) = viewModelScope.launch {
        settingsRepository.setSort(sort)
    }

    fun setSortAscending(ascending: Boolean) = viewModelScope.launch {
        settingsRepository.setSortAscending(ascending)
    }

    fun setShowEstimatePrefix(show: Boolean) = viewModelScope.launch {
        settingsRepository.setShowEstimatePrefix(show)
    }

    fun setAmountNotation(notation: AmountNotation) = viewModelScope.launch {
        settingsRepository.setAmountNotation(notation)
    }

    // Every notification setting re-books the alarms after writing, because both *whether* and
    // *when* a reminder fires are decided at scheduling time. Skipping this leaves the old time
    // booked until the next reboot.

    fun setNotifyDayBefore(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNotifyDayBefore(enabled)
        reminderScheduler.rescheduleAll()
    }

    fun setDayBeforeTime(time: LocalTime) = viewModelScope.launch {
        settingsRepository.setDayBeforeTime(time)
        reminderScheduler.rescheduleAll()
    }

    fun setNotifyOnDay(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNotifyOnDay(enabled)
        reminderScheduler.rescheduleAll()
    }

    fun setOnDayTime(time: LocalTime) = viewModelScope.launch {
        settingsRepository.setOnDayTime(time)
        reminderScheduler.rescheduleAll()
    }
}
