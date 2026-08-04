package com.subkan.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subkan.BuildConfig
import com.subkan.R
import com.subkan.core.model.AmountNotation
import com.subkan.core.model.SubscriptionSort
import com.subkan.core.model.TabBarPosition
import com.subkan.core.model.ThemePreference
import com.subkan.ui.components.TimePickerDialog
import com.subkan.ui.components.inheritedListItemColors
import com.subkan.ui.home.labelRes
import com.subkan.ui.permissions.canPostNotifications
import com.subkan.ui.permissions.openNotificationSettings
import com.subkan.ui.theme.supportsDynamicColour
import com.subkan.ui.util.formatForDisplay
import kotlinx.coroutines.launch

private const val GITHUB_URL = "https://github.com/yo5uke/subkan"

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onOpenCardManagement: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissionGranted by viewModel.permissionGranted.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var openDialog by remember { mutableStateOf<SettingsDialog?>(null) }

    // The user may have just come back from system settings, where they could have granted or
    // revoked the permission this screen is reporting on.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermission(canPostNotifications(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_section_display))

            SettingsRow(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.settings_theme),
                subtitle = stringResource(settings.theme.labelRes()),
                onClick = { openDialog = SettingsDialog.Theme },
            )

            ListItem(
                leadingContent = { Icon(Icons.Outlined.ColorLens, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.settings_dynamic_colour)) },
                supportingContent = {
                    Text(
                        stringResource(
                            if (supportsDynamicColour) {
                                R.string.settings_dynamic_colour_summary
                            } else {
                                R.string.settings_dynamic_colour_unsupported
                            },
                        ),
                    )
                },
                trailingContent = {
                    Switch(
                        checked = settings.useDynamicColour && supportsDynamicColour,
                        onCheckedChange = viewModel::setDynamicColour,
                        enabled = supportsDynamicColour,
                    )
                },
            )

            SettingsRow(
                icon = Icons.Outlined.Payments,
                title = stringResource(R.string.settings_amount_notation),
                subtitle = stringResource(settings.amountNotation.labelRes()),
                onClick = { openDialog = SettingsDialog.AmountNotation },
            )

            ListItem(
                leadingContent = { Icon(Icons.Outlined.Calculate, contentDescription = null) },
                headlineContent = {
                    Text(stringResource(R.string.settings_show_estimate_prefix))
                },
                supportingContent = {
                    Text(stringResource(R.string.settings_show_estimate_prefix_summary))
                },
                trailingContent = {
                    Switch(
                        checked = settings.showEstimatePrefix,
                        onCheckedChange = viewModel::setShowEstimatePrefix,
                    )
                },
            )

            SettingsRow(
                icon = Icons.Outlined.Splitscreen,
                title = stringResource(R.string.settings_tab_position),
                subtitle = stringResource(settings.tabBarPosition.labelRes()),
                onClick = { openDialog = SettingsDialog.TabPosition },
            )

            SectionHeader(stringResource(R.string.settings_section_notifications))

            // Shown only when it is actually a problem: reminders are on but the OS will not let
            // them through. Silently switched-on reminders that never arrive are the worst outcome.
            if (settings.notifications.anyEnabled && !permissionGranted) {
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.notification_permission_required),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    supportingContent = {
                        Text(stringResource(R.string.notification_permission_required_summary))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openNotificationSettings(context) },
                )
            }

            ListItem(
                leadingContent = {
                    Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                },
                headlineContent = { Text(stringResource(R.string.settings_notify_day_before)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_notify_day_before_summary))
                },
                trailingContent = {
                    Switch(
                        checked = settings.notifications.notifyDayBefore,
                        onCheckedChange = viewModel::setNotifyDayBefore,
                    )
                },
            )

            SettingsRow(
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.settings_notify_day_before_time),
                subtitle = settings.notifications.dayBeforeTime.formatForDisplay(),
                enabled = settings.notifications.notifyDayBefore,
                onClick = { openDialog = SettingsDialog.DayBeforeTime },
            )

            ListItem(
                leadingContent = {
                    Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                },
                headlineContent = { Text(stringResource(R.string.settings_notify_on_day)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_notify_on_day_summary))
                },
                trailingContent = {
                    Switch(
                        checked = settings.notifications.notifyOnDay,
                        onCheckedChange = viewModel::setNotifyOnDay,
                    )
                },
            )

            SettingsRow(
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.settings_notify_on_day_time),
                subtitle = settings.notifications.onDayTime.formatForDisplay(),
                enabled = settings.notifications.notifyOnDay,
                onClick = { openDialog = SettingsDialog.OnDayTime },
            )

            SectionHeader(stringResource(R.string.settings_section_management))

            SettingsRow(
                icon = Icons.Outlined.CreditCard,
                title = stringResource(R.string.settings_cards),
                subtitle = stringResource(R.string.settings_cards_summary),
                trailingIcon = Icons.Filled.ChevronRight,
                onClick = onOpenCardManagement,
            )

            SectionHeader(stringResource(R.string.settings_section_sort))

            SettingsRow(
                icon = Icons.AutoMirrored.Outlined.Sort,
                title = stringResource(R.string.settings_sort),
                subtitle = stringResource(settings.sort.labelRes()),
                onClick = { openDialog = SettingsDialog.Sort },
            )

            SettingsRow(
                icon = Icons.Outlined.SwapVert,
                title = stringResource(R.string.settings_sort_direction),
                subtitle = stringResource(
                    if (settings.sortAscending) R.string.sort_ascending else R.string.sort_descending,
                ),
                onClick = { openDialog = SettingsDialog.SortDirection },
            )

            SectionHeader(stringResource(R.string.settings_section_about))

            SettingsRow(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.settings_about_summary),
                onClick = { openDialog = SettingsDialog.About },
            )

            SettingsRow(
                icon = Icons.Outlined.PrivacyTip,
                title = stringResource(R.string.settings_disclaimer),
                subtitle = stringResource(R.string.settings_disclaimer_summary),
                onClick = { openDialog = SettingsDialog.Disclaimer },
            )

            val linkFailed = stringResource(R.string.error_open_link)
            SettingsRow(
                icon = Icons.Outlined.Code,
                title = stringResource(R.string.settings_source),
                subtitle = stringResource(R.string.settings_source_summary),
                trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri()),
                        )
                    } catch (_: ActivityNotFoundException) {
                        // No browser at all — rare, but the alternative is an uncaught crash.
                        scope.launch { snackbarHostState.showSnackbar(linkFailed) }
                    }
                },
            )
        }
    }

    when (openDialog) {
        null -> Unit

        SettingsDialog.Theme -> ChoiceDialog(
            title = stringResource(R.string.dialog_theme_title),
            options = ThemePreference.entries,
            selected = settings.theme,
            label = { stringResource(it.labelRes()) },
            onSelected = viewModel::setTheme,
            onDismiss = { openDialog = null },
        )

        SettingsDialog.TabPosition -> ChoiceDialog(
            title = stringResource(R.string.dialog_tab_position_title),
            options = TabBarPosition.entries,
            selected = settings.tabBarPosition,
            label = { stringResource(it.labelRes()) },
            onSelected = viewModel::setTabBarPosition,
            onDismiss = { openDialog = null },
        )

        SettingsDialog.Sort -> ChoiceDialog(
            title = stringResource(R.string.dialog_sort_title),
            options = SubscriptionSort.entries,
            selected = settings.sort,
            label = { stringResource(it.labelRes()) },
            onSelected = viewModel::setSort,
            onDismiss = { openDialog = null },
        )

        SettingsDialog.SortDirection -> ChoiceDialog(
            title = stringResource(R.string.dialog_sort_direction_title),
            options = listOf(true, false),
            selected = settings.sortAscending,
            label = {
                stringResource(if (it) R.string.sort_ascending else R.string.sort_descending)
            },
            onSelected = viewModel::setSortAscending,
            onDismiss = { openDialog = null },
        )

        SettingsDialog.AmountNotation -> ChoiceDialog(
            title = stringResource(R.string.dialog_amount_notation_title),
            options = AmountNotation.entries,
            selected = settings.amountNotation,
            label = { stringResource(it.labelRes()) },
            onSelected = viewModel::setAmountNotation,
            onDismiss = { openDialog = null },
        )

        SettingsDialog.DayBeforeTime -> TimePickerDialog(
            title = stringResource(R.string.settings_notify_day_before_time),
            initialTime = settings.notifications.dayBeforeTime,
            onDismiss = { openDialog = null },
            onConfirm = {
                viewModel.setDayBeforeTime(it)
                openDialog = null
            },
        )

        SettingsDialog.OnDayTime -> TimePickerDialog(
            title = stringResource(R.string.settings_notify_on_day_time),
            initialTime = settings.notifications.onDayTime,
            onDismiss = { openDialog = null },
            onConfirm = {
                viewModel.setOnDayTime(it)
                openDialog = null
            },
        )

        SettingsDialog.About -> InfoDialog(
            title = stringResource(R.string.about_title),
            body = buildString {
                appendLine(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
                appendLine()
                appendLine(stringResource(R.string.about_description))
                appendLine()
                append(stringResource(R.string.about_legalese))
            },
            onDismiss = { openDialog = null },
        )

        SettingsDialog.Disclaimer -> InfoDialog(
            title = stringResource(R.string.disclaimer_title),
            body = stringResource(R.string.disclaimer_body),
            onDismiss = { openDialog = null },
        )
    }
}

private enum class SettingsDialog {
    Theme,
    TabPosition,
    Sort,
    SortDirection,
    AmountNotation,
    DayBeforeTime,
    OnDayTime,
    About,
    Disclaimer,
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

/**
 * [enabled] dims and un-clicks a row whose parent switch is off — the time still shows, so the user
 * can see what it would be, but changing it would have no effect.
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val contentAlpha = if (enabled) 1f else DisabledAlpha
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = contentAlpha),
            )
        },
        headlineContent = {
            Text(text = title, color = LocalContentColor.current.copy(alpha = contentAlpha))
        },
        supportingContent = {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            )
        },
        trailingContent = trailingIcon?.let {
            { Icon(it, contentDescription = null) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    )
}

/** Material 3's disabled-content opacity. */
private const val DisabledAlpha = 0.38f

/** A single-choice dialog. Choosing applies immediately and closes — there is no confirm step. */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    ListItem(
                        headlineContent = { Text(label(option)) },
                        trailingContent = {
                            if (option == selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        // The dialog is surfaceContainerHigh; the ListItem default is surface,
                        // which would sit inside it as a lighter block.
                        colors = inheritedListItemColors(),
                        modifier = Modifier.clickable {
                            onSelected(option)
                            onDismiss()
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

private fun ThemePreference.labelRes(): Int = when (this) {
    ThemePreference.System -> R.string.theme_system
    ThemePreference.Light -> R.string.theme_light
    ThemePreference.Dark -> R.string.theme_dark
}

private fun AmountNotation.labelRes(): Int = when (this) {
    AmountNotation.Symbol -> R.string.notation_symbol
    AmountNotation.Japanese -> R.string.notation_japanese
}

private fun TabBarPosition.labelRes(): Int = when (this) {
    TabBarPosition.Top -> R.string.tab_position_top
    TabBarPosition.Bottom -> R.string.tab_position_bottom
}
