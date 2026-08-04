package com.subkan.ui.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subkan.R
import com.subkan.core.model.PaymentCard
import com.subkan.ui.components.EmptyState
import com.subkan.ui.editor.CardEditorDialog
import com.subkan.ui.editor.CardReorderList

/**
 * Add, rename, recolour, reorder and delete payment cards.
 *
 * Deleting here keeps the card's subscriptions — they simply stop naming a card and render as
 * 「不明なカード」. Deleting from a card's tab on the list screen is the destructive variant.
 */
@Composable
fun CardManagementScreen(
    onNavigateUp: () -> Unit,
    viewModel: CardsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var editorTarget by remember { mutableStateOf<CardEditorTarget?>(null) }
    var pendingDelete by remember { mutableStateOf<PaymentCard?>(null) }

    LaunchedEffect(Unit) {
        viewModel.deletedCardFlow.collect { deleted ->
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.card_deleted, deleted.card.name),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreCard(deleted)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.card_management_title)) },
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
        floatingActionButton = {
            FloatingActionButton(onClick = { editorTarget = CardEditorTarget.New }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.action_add_card),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Outlined.CreditCard,
                    title = stringResource(R.string.card_management_empty),
                    body = stringResource(R.string.card_management_empty_body),
                )
            }
        } else {
            CardReorderList(
                cards = uiState.cards,
                onOrderChanged = viewModel::reorderCards,
                onRowClick = { editorTarget = CardEditorTarget.Edit(it) },
                trailingContent = { card ->
                    IconButton(onClick = { pendingDelete = card }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }

    when (val target = editorTarget) {
        null -> Unit
        else -> {
            val editing = (target as? CardEditorTarget.Edit)?.card
            CardEditorDialog(
                cardToEdit = editing,
                onDismiss = { editorTarget = null },
                onSave = { name, colorHex ->
                    if (editing == null) {
                        viewModel.createCard(name, colorHex)
                    } else {
                        viewModel.updateCard(editing.id, name, colorHex)
                    }
                    editorTarget = null
                },
            )
        }
    }

    pendingDelete?.let { card ->
        val linkedCount = uiState.subscriptionCounts[card.id] ?: 0
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.card_delete_title)) },
            text = {
                Text(
                    if (linkedCount > 0) {
                        stringResource(R.string.card_delete_message_detaches)
                    } else {
                        stringResource(R.string.card_delete_message)
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCard(card.id)
                        pendingDelete = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private sealed interface CardEditorTarget {
    data object New : CardEditorTarget
    data class Edit(val card: PaymentCard) : CardEditorTarget
}
