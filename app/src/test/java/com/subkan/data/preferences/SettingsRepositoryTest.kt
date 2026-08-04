package com.subkan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.subkan.core.model.NotificationSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalTime

/**
 * DataStore runs on a plain JVM, so these are fast tests against the real thing rather than a mock.
 *
 * They exist mostly to catch two silent failure modes: a mistyped preference key (which reads back
 * as the default forever) and a broken minute-of-day round trip (which quietly moves the user's
 * reminder to a different hour).
 */
class SettingsRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun repository(scope: CoroutineScope): SettingsRepository {
        // Deliberately *not* TemporaryFolder.newFile(): that creates the file, and DataStore writes
        // by renaming a .tmp over the target, which fails on Windows when the target already
        // exists. TemporaryFolder still owns the directory, so it is cleaned up either way.
        val file = File(temporaryFolder.root, "settings.preferences_pb")
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )
        return SettingsRepository(dataStore)
    }

    @Test
    fun `notification defaults are the evening before and the morning of`() = runTest {
        val settings = repository(backgroundScope).settings.first()

        assertEquals(NotificationSettings(), settings.notifications)
        assertEquals(true, settings.notifications.notifyDayBefore)
        assertEquals(LocalTime.of(20, 0), settings.notifications.dayBeforeTime)
        assertEquals(true, settings.notifications.notifyOnDay)
        assertEquals(LocalTime.of(8, 0), settings.notifications.onDayTime)
    }

    @Test
    fun `a reminder time survives a round trip`() = runTest(StandardTestDispatcher()) {
        val repository = repository(backgroundScope)

        repository.setDayBeforeTime(LocalTime.of(21, 45))

        assertEquals(LocalTime.of(21, 45), repository.settings.first().notifications.dayBeforeTime)
    }

    /*
     * Only one write per test here. DataStore commits by renaming a .tmp over the target, and on
     * Windows the second rename in a row fails before the first handle is released — a quirk of the
     * JVM host, not of Android or of this code. The encoding that write depends on is covered
     * directly in PaymentReminderTest instead.
     */

    @Test
    fun `the two reminders switch independently`() = runTest(StandardTestDispatcher()) {
        val repository = repository(backgroundScope)

        repository.setNotifyDayBefore(false)

        val notifications = repository.settings.first().notifications
        assertEquals(false, notifications.notifyDayBefore)
        assertEquals(true, notifications.notifyOnDay)
        assertEquals(true, notifications.anyEnabled)
    }

    @Test
    fun `the permission-asked flag starts false so existing installs get prompted`() = runTest(
        StandardTestDispatcher(),
    ) {
        val repository = repository(backgroundScope)

        assertEquals(false, repository.settings.first().notificationPermissionRequested)

        repository.setNotificationPermissionRequested(true)
        assertEquals(true, repository.settings.first().notificationPermissionRequested)
    }
}
