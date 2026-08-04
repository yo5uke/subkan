package com.subkan.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.subkan.R
import com.subkan.core.model.PaymentCard
import com.subkan.core.model.presetCardColors
import com.subkan.ui.theme.cardColor

/**
 * Add or rename a payment card and pick its colour.
 *
 * A dialog rather than a sheet because it is two fields and is opened from three different places,
 * including from on top of the subscription editor sheet.
 */
@Composable
fun CardEditorDialog(
    cardToEdit: PaymentCard?,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String) -> Unit,
) {
    var name by remember { mutableStateOf(cardToEdit?.name.orEmpty()) }
    var colorHex by remember {
        mutableStateOf(cardToEdit?.colorHex ?: presetCardColors.first())
    }
    var showNameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (cardToEdit == null) {
                        R.string.card_editor_title_new
                    } else {
                        R.string.card_editor_title_edit
                    },
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showNameError = false
                    },
                    label = { Text(stringResource(R.string.field_card_name)) },
                    singleLine = true,
                    isError = showNameError,
                    supportingText = if (showNameError) {
                        { Text(stringResource(R.string.error_required)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.card_theme_color),
                    style = MaterialTheme.typography.labelLarge,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presetCardColors.forEach { hex ->
                        ColorSwatch(
                            colorHex = hex,
                            selected = hex == colorHex,
                            onClick = { colorHex = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        showNameError = true
                    } else {
                        onSave(name.trim(), colorHex)
                    }
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ColorSwatch(
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val swatch = cardColor(colorHex)
    val selectedDescription = stringResource(R.string.card_theme_color)

    Box(
        modifier = Modifier
            // 48dp of touch target around a 36dp swatch, so the row stays reachable.
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$selectedDescription $colorHex" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(swatch)
                .then(
                    if (selected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
