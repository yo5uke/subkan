package com.subkan.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.subkan.core.model.ThemePreference

/** True when dynamic colour is actually available, not merely requested. */
val supportsDynamicColour: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun SubKanTheme(
    themePreference: ThemePreference = ThemePreference.System,
    useDynamicColour: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemePreference.System -> isSystemInDarkTheme()
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColour && supportsDynamicColour ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> SubKanDarkColorScheme
        else -> SubKanLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SubKanTypography,
        shapes = SubKanShapes,
        content = content,
    )
}
