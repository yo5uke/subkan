package com.subkan.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DesignServices
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subkan.R
import com.subkan.core.model.BillingCycle
import com.subkan.core.model.Currency
import com.subkan.core.model.PaymentCard
import com.subkan.core.model.Subscription
import com.subkan.data.repository.SubscriptionDraft
import com.subkan.ui.theme.cardColor
import com.subkan.ui.util.formatForDisplay
import com.subkan.ui.util.localDateFromPickerMillis
import com.subkan.ui.util.toPickerMillis
import java.time.LocalDate

/**
 * Add or edit one subscription.
 *
 * The same sheet does both; [subscriptionToEdit] being null is what makes it "new". Editing also
 * shows a delete affordance in the header, which is the only way to remove a subscription.
 */
@Composable
fun SubscriptionEditorSheet(
    subscriptionToEdit: Subscription?,
    cards: List<PaymentCard>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (SubscriptionDraft) -> Unit,
    onDelete: (Subscription) -> Unit,
    onCreateCard: (name: String, colorHex: String, onCreated: (String) -> Unit) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var name by remember { mutableStateOf(subscriptionToEdit?.name.orEmpty()) }
    var price by remember {
        mutableStateOf(subscriptionToEdit?.price?.let { formatPriceForEditing(it) }.orEmpty())
    }
    var isEstimated by remember { mutableStateOf(subscriptionToEdit?.isEstimated ?: false) }
    var currency by remember {
        mutableStateOf(subscriptionToEdit?.currency ?: Currency.JPY)
    }
    var billingCycle by remember {
        mutableStateOf(subscriptionToEdit?.billingCycle ?: BillingCycle.Monthly)
    }
    var nextPaymentDate by remember {
        mutableStateOf(subscriptionToEdit?.nextPaymentDate ?: today)
    }
    // Falls back to the first card so a new subscription opens ready to save, the way the tab row
    // opens on 「すべて」.
    var selectedCardId by remember {
        mutableStateOf(subscriptionToEdit?.cardId ?: cards.firstOrNull()?.id)
    }

    var nameError by remember { mutableStateOf<Int?>(null) }
    var priceError by remember { mutableStateOf<Int?>(null) }
    var cardError by remember { mutableStateOf<Int?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCardEditor by remember { mutableStateOf(false) }

    val isEditing = subscriptionToEdit != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        if (isEditing) R.string.editor_title_edit else R.string.editor_title_new,
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (subscriptionToEdit != null) {
                    IconButton(onClick = { onDelete(subscriptionToEdit) }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = { Text(stringResource(R.string.field_service_name)) },
                placeholder = { Text(stringResource(R.string.field_service_name_hint)) },
                leadingIcon = {
                    Icon(Icons.Outlined.DesignServices, contentDescription = null)
                },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        price = it
                        priceError = null
                    },
                    label = { Text(stringResource(R.string.field_price_optional)) },
                    placeholder = { Text(stringResource(R.string.field_price_hint)) },
                    leadingIcon = { Text(currency.symbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = priceError != null,
                    supportingText = priceError?.let { { Text(stringResource(it)) } },
                    modifier = Modifier.weight(3f),
                )

                EnumDropdown(
                    label = stringResource(R.string.field_currency),
                    options = Currency.entries,
                    selected = currency,
                    optionLabel = { it.code },
                    onSelected = { currency = it },
                    modifier = Modifier.weight(2f),
                )
            }

            // Only meaningful once there is an amount to qualify — with the field empty there is
            // nothing to be approximate about.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.field_is_estimated),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (price.isBlank()) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Text(
                        text = stringResource(R.string.field_is_estimated_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = isEstimated && price.isNotBlank(),
                    onCheckedChange = { isEstimated = it },
                    enabled = price.isNotBlank(),
                )
            }

            EnumDropdown(
                label = stringResource(R.string.field_billing_cycle),
                options = BillingCycle.entries,
                selected = billingCycle,
                optionLabel = { stringResource(it.labelRes()) },
                leadingIcon = { Icon(Icons.Outlined.Autorenew, contentDescription = null) },
                onSelected = { billingCycle = it },
                modifier = Modifier.fillMaxWidth(),
            )

            // Read-only field that opens the picker: a date is chosen, never typed.
            OutlinedTextField(
                value = nextPaymentDate.formatForDisplay(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.field_next_payment_date)) },
                leadingIcon = { Icon(Icons.Outlined.CalendarToday, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = stringResource(R.string.field_next_payment_date),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            CardDropdown(
                cards = cards,
                selectedCardId = selectedCardId,
                errorRes = cardError,
                onSelected = {
                    selectedCardId = it
                    cardError = null
                },
                onAddNewCard = { showCardEditor = true },
            )

            Button(
                onClick = {
                    val trimmedPrice = price.trim()
                    val parsedPrice = trimmedPrice.toDoubleOrNull()
                    nameError = if (name.isBlank()) R.string.error_service_name_required else null
                    // Blank is allowed — that is a subscription registered by name alone. Only a
                    // non-numeric entry is a mistake.
                    priceError = if (trimmedPrice.isNotEmpty() && parsedPrice == null) {
                        R.string.error_price_invalid
                    } else {
                        null
                    }
                    cardError = if (selectedCardId == null) R.string.error_card_required else null

                    if (nameError == null && priceError == null && cardError == null) {
                        onSave(
                            SubscriptionDraft(
                                name = name.trim(),
                                price = parsedPrice,
                                isEstimated = parsedPrice != null && isEstimated,
                                currency = currency,
                                billingCycle = billingCycle,
                                nextPaymentDate = nextPaymentDate,
                                cardId = selectedCardId,
                            ),
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = stringResource(
                        if (isEditing) R.string.action_update else R.string.action_register,
                    ),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = nextPaymentDate.toPickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            nextPaymentDate = localDateFromPickerMillis(it)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showCardEditor) {
        CardEditorDialog(
            cardToEdit = null,
            onDismiss = { showCardEditor = false },
            onSave = { cardName, colorHex ->
                onCreateCard(cardName, colorHex) { newId ->
                    selectedCardId = newId
                    cardError = null
                }
                showCardEditor = false
            },
        )
    }
}

@Composable
private fun CardDropdown(
    cards: List<PaymentCard>,
    selectedCardId: String?,
    errorRes: Int?,
    onSelected: (String) -> Unit,
    onAddNewCard: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCard = cards.firstOrNull { it.id == selectedCardId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedCard?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.field_card)) },
            leadingIcon = {
                if (selectedCard != null) {
                    ColorDot(selectedCard.colorHex)
                } else {
                    Icon(Icons.Outlined.CreditCard, contentDescription = null)
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = errorRes != null,
            supportingText = errorRes?.let { { Text(stringResource(it)) } },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            cards.forEach { card ->
                DropdownMenuItem(
                    text = {
                        Text(card.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    leadingIcon = { ColorDot(card.colorHex) },
                    onClick = {
                        onSelected(card.id)
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.card_add_new),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                onClick = {
                    expanded = false
                    onAddNewCard()
                },
            )
        }
    }
}

@Composable
private fun ColorDot(colorHex: String) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(cardColor(colorHex)),
    )
}

/** The two small closed-set pickers — currency and billing cycle — share one implementation. */
@Composable
private fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = leadingIcon,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun BillingCycle.labelRes(): Int = when (this) {
    BillingCycle.Monthly -> R.string.billing_monthly
    BillingCycle.Yearly -> R.string.billing_yearly
}

/**
 * Prices are whole numbers in practice, so a stored 980.0 is shown as "980" rather than "980.0" —
 * otherwise every edit starts by deleting a decimal point the user never typed.
 */
private fun formatPriceForEditing(price: Double): String =
    if (price % 1.0 == 0.0) price.toLong().toString() else price.toString()
