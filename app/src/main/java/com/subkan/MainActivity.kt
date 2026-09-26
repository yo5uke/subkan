package com.subkan

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subkan.core.model.ThemePreference
import com.subkan.ui.SubKanApp
import com.subkan.ui.permissions.NotificationPermissionEffect
import com.subkan.ui.theme.SubKanTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.setLocales(LocaleList(Locale.JAPAN))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash until the stored theme is known, otherwise a dark-mode user sees a white
        // flash while DataStore loads.
        splash.setKeepOnScreenCondition { !viewModel.isReady }

        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            SubKanTheme(
                themePreference = settings?.theme ?: ThemePreference.System,
                useDynamicColour = settings?.useDynamicColour ?: false,
            ) {
                // Only once settings have loaded, so the first open does not ask before it knows
                // whether it has already asked.
                settings?.let {
                    NotificationPermissionEffect(
                        alreadyRequested = it.notificationPermissionRequested,
                        remindersEnabled = it.notifications.anyEnabled,
                        onRequested = viewModel::markNotificationPermissionRequested,
                        onGranted = viewModel::rescheduleReminders,
                    )
                }

                SubKanApp()
            }
        }
    }
}
