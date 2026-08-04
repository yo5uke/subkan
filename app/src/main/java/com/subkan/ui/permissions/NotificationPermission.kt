package com.subkan.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Whether the app may currently post notifications. */
fun canPostNotifications(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

/**
 * Asks for the notification permission the first time the app is opened, and re-checks on every
 * later open.
 *
 * The "first time" is tracked in settings ([alreadyRequested]) rather than inferred, because the
 * system dialog only ever appears once and "denied" is indistinguishable from "never asked". Anyone
 * who installed before reminders existed has the flag unset, which is exactly what makes them see
 * the prompt the next time they open the app.
 *
 * Re-checking on resume matters because the permission can be granted or revoked from system
 * settings while the app is in the background, and nothing tells the app when that happens. When it
 * turns out to be granted, [onGranted] re-books the alarms — cheap, idempotent, and the only way
 * reminders start working after the user says yes in Settings rather than in the dialog.
 */
@Composable
fun NotificationPermissionEffect(
    alreadyRequested: Boolean,
    remindersEnabled: Boolean,
    onRequested: () -> Unit,
    onGranted: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentAlreadyRequested by rememberUpdatedState(alreadyRequested)
    val currentRemindersEnabled by rememberUpdatedState(remindersEnabled)
    val currentOnRequested by rememberUpdatedState(onRequested)
    val currentOnGranted by rememberUpdatedState(onGranted)

    // Survives rotation so a configuration change cannot re-trigger the dialog while the stored
    // flag is still making its way back through DataStore.
    var askedThisSession by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        currentOnRequested()
        if (granted) currentOnGranted()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver

            when {
                canPostNotifications(context) -> currentOnGranted()

                // Below Android 13 there is no runtime permission to ask for; notifications are
                // granted at install time and can only be switched off in system settings.
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> Unit

                currentAlreadyRequested || askedThisSession || !currentRemindersEnabled -> Unit

                else -> {
                    askedThisSession = true
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * Opens this app's notification settings.
 *
 * Once the dialog has been shown and dismissed, Android will not show it again — system settings is
 * the only remaining route, so the Settings screen links to it directly.
 */
fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching { context.startActivity(intent) }.onFailure {
        // Some OEM builds do not honour the per-app screen; the app detail page always exists.
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
