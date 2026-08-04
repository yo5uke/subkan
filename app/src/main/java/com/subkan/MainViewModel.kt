package com.subkan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subkan.core.model.AppSettings
import com.subkan.data.preferences.SettingsRepository
import com.subkan.data.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds the theme long enough for the splash screen to decide when to let go, and owns the
 * app-level notification permission handshake.
 *
 * [settings] starts as null rather than [AppSettings] defaults so "not read yet" is distinguishable
 * from "the user chose the defaults" — otherwise a dark-mode user gets a white flash while DataStore
 * loads.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _settings = MutableStateFlow<AppSettings?>(null)
    val settings: StateFlow<AppSettings?> = _settings.asStateFlow()

    val isReady: Boolean
        get() = _settings.value != null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { _settings.value = it }
        }
    }

    fun markNotificationPermissionRequested() = viewModelScope.launch {
        settingsRepository.setNotificationPermissionRequested(true)
    }

    /**
     * Re-books the alarms now that the app may post notifications.
     *
     * Runs on every resume where permission is held, not only on the transition — booking an alarm
     * is idempotent, and the alternative is tracking the previous state to catch a grant that
     * happened in system settings while the app was backgrounded.
     */
    fun rescheduleReminders() = viewModelScope.launch {
        reminderScheduler.rescheduleAll()
    }
}
