package com.subkan.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subkan.R
import com.subkan.core.model.AmountNotation
import com.subkan.core.model.Currency
import com.subkan.core.model.CurrencyTotal
import com.subkan.core.model.PaymentCard
import com.subkan.core.model.Subscription
import com.subkan.core.model.SubscriptionSort
import com.subkan.core.model.TabBarPosition
import com.subkan.core.model.format
import com.subkan.core.model.monthlyTotals
import com.subkan.ui.components.EmptyState
import com.subkan.ui.components.SubscriptionRow
import com.subkan.ui.editor.CardActionsSheet
import com.subkan.ui.editor.CardEditorDialog
import com.subkan.ui.editor.CardReorderSheet
import com.subkan.ui.editor.SubscriptionEditorSheet
import com.subkan.ui.theme.AmountLarge
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/** Which editor, if any, is open. Held here rather than in the ViewModel: it is UI state. */
private sealed interface EditorTarget {
    data object NewSubscription : EditorTarget
    data class EditSubscription(val subscription: Subscription) : EditorTarget
    data object NewCard : EditorTarget
    data class EditCard(val card: PaymentCard) : EditorTarget
}

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }
    var cardActionsFor by remember { mutableStateOf<PaymentCard?>(null) }
    var showReorderSheet by remember { mutableStateOf(false) }
    var cardPendingDelete by remember { mutableStateOf<PaymentCard?>(null) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            val (message, undo) = when (event) {
                is HomeEvent.CardDeleted -> {
                    val text = if (event.deleted.removedSubscriptions.isEmpty()) {
                        context.getString(R.string.card_deleted, event.deleted.card.name)
                    } else {
                        context.getString(
                            R.string.card_deleted_with_subscriptions,
                            event.deleted.card.name,
                        )
                    }
                    text to { viewModel.restoreCard(event.deleted); Unit }
                }

                is HomeEvent.SubscriptionDeleted -> {
                    context.getString(
                        R.string.subscription_deleted,
                        event.subscription.name,
                    ) to { viewModel.restoreSubscription(event.subscription); Unit }
                }
            }

            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) undo()
        }
    }

    val pageCount = 1 + uiState.cards.size
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // Reading the *fractional* page is what makes the header total follow a swipe in real time
    // rather than snapping when the page settles.
    //
    // The bound comes from `pagerState.pageCount` rather than the local `pageCount`, because this
    // lambda is remembered once: capturing the local would freeze the clamp at however many cards
    // existed on first composition, and adding one would leave the last tab unreachable.
    val visibleTab by remember {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .roundToInt()
                .coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
        }
    }

    val tabRow: @Composable () -> Unit = {
        CardTabRow(
            cards = uiState.cards,
            selectedIndex = pagerState.currentPage.coerceIn(0, pageCount - 1),
            onTabSelected = { scope.launch { pagerState.animateScrollToPage(it) } },
            onCardLongPress = { cardActionsFor = it },
            onAddCard = { editorTarget = EditorTarget.NewCard },
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { HomeTitle() },
                    actions = {
                        SortMenu(
                            sort = uiState.sort,
                            ascending = uiState.sortAscending,
                            onSortSelected = viewModel::setSort,
                            onAscendingSelected = viewModel::setSortAscending,
                        )
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.action_settings),
                            )
                        }
                    },
                )
                if (uiState.tabBarPosition == TabBarPosition.Top) {
                    tabRow()
                }
            }
        },
        bottomBar = {
            if (uiState.tabBarPosition == TabBarPosition.Bottom) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Box(modifier = Modifier.navigationBarsPadding()) { tabRow() }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editorTarget = EditorTarget.NewSubscription },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_subscription)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SummaryHeader(
                label = summaryLabel(uiState.cards, visibleTab),
                totals = monthlyTotals(subscriptionsForTab(uiState, visibleTab)),
                showEstimatePrefix = uiState.showEstimatePrefix,
                notation = uiState.notation,
            )

            // weight, not fillMaxSize: inside a Column the latter would claim the full height and
            // push the list down behind the summary header instead of taking what is left.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                SubscriptionList(
                    subscriptions = subscriptionsForTab(uiState, page),
                    cards = uiState.cards,
                    today = uiState.today,
                    showEstimatePrefix = uiState.showEstimatePrefix,
                    notation = uiState.notation,
                    onSubscriptionClick = { editorTarget = EditorTarget.EditSubscription(it) },
                )
            }
        }
    }

    when (val target = editorTarget) {
        null -> Unit

        EditorTarget.NewSubscription, is EditorTarget.EditSubscription -> {
            val editing = (target as? EditorTarget.EditSubscription)?.subscription
            SubscriptionEditorSheet(
                subscriptionToEdit = editing,
                cards = uiState.cards,
                today = uiState.today,
                onDismiss = { editorTarget = null },
                onSave = { draft ->
                    if (editing == null) {
                        viewModel.createSubscription(draft)
                    } else {
                        viewModel.updateSubscription(editing.id, draft)
                    }
                    editorTarget = null
                },
                onDelete = {
                    viewModel.deleteSubscription(it)
                    editorTarget = null
                },
                onCreateCard = viewModel::createCard,
            )
        }

        EditorTarget.NewCard, is EditorTarget.EditCard -> {
            val editing = (target as? EditorTarget.EditCard)?.card
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

    cardActionsFor?.let { card ->
        CardActionsSheet(
            card = card,
            onDismiss = { cardActionsFor = null },
            onReorder = {
                cardActionsFor = null
                showReorderSheet = true
            },
            onEdit = {
                cardActionsFor = null
                editorTarget = EditorTarget.EditCard(card)
            },
            onDelete = {
                cardActionsFor = null
                cardPendingDelete = card
            },
        )
    }

    if (showReorderSheet) {
        CardReorderSheet(
            cards = uiState.cards,
            onDismiss = { showReorderSheet = false },
            onOrderChanged = viewModel::reorderCards,
        )
    }

    cardPendingDelete?.let { card ->
        val linkedCount = uiState.subscriptions.count { it.cardId == card.id }
        AlertDialog(
            onDismissRequest = { cardPendingDelete = null },
            title = { Text(stringResource(R.string.card_delete_title)) },
            text = {
                Text(
                    if (linkedCount > 0) {
                        stringResource(
                            R.string.card_delete_message_with_subscriptions,
                            linkedCount,
                        )
                    } else {
                        stringResource(R.string.card_delete_message)
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCardWithSubscriptions(card.id)
                        cardPendingDelete = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { cardPendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** Page 0 is 「すべて」; page n is the (n-1)th card. */
private fun subscriptionsForTab(state: HomeUiState, tabIndex: Int): List<Subscription> {
    if (tabIndex <= 0) return state.subscriptions
    val card = state.cards.getOrNull(tabIndex - 1) ?: return state.subscriptions
    return state.subscriptions.filter { it.cardId == card.id }
}

@Composable
private fun summaryLabel(cards: List<PaymentCard>, tabIndex: Int): String {
    val card = if (tabIndex <= 0) null else cards.getOrNull(tabIndex - 1)
    return if (card == null) {
        stringResource(R.string.list_summary_total)
    } else {
        stringResource(R.string.list_summary_card, card.name)
    }
}

@Composable
private fun HomeTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Bookmarks,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/**
 * The month's total for whichever tab is on screen.
 *
 * The gradient runs primaryContainer → secondaryContainer and the text is `onPrimaryContainer`.
 * That pairing is safe because M3 puts every `*Container` role at the same tone, so the contrast
 * `onPrimaryContainer` guarantees against one of them holds against the other.
 */
@Composable
private fun SummaryHeader(
    label: String,
    totals: List<CurrencyTotal>,
    showEstimatePrefix: Boolean,
    notation: AmountNotation,
) {
    // The qualifier rides beside the label rather than in front of the number. A total that mixes
    // confirmed and estimated amounts is not itself "約" — it *contains* an estimate — and putting
    // it here keeps the number clean and the label short enough for a long card name.
    val estimated = showEstimatePrefix && totals.any { it.isEstimated }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            )
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (estimated) {
                    EstimateChip()
                }
            }
            if (totals.isEmpty()) {
                Text(
                    text = Currency.JPY.format(0.0, notation),
                    style = AmountLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                // Currencies are never added together — they are listed, joined by "+", so the
                // user can see the shape of a mixed-currency month at a glance.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    totals.forEachIndexed { index, total ->
                        Text(
                            text = total.currency.format(total.amount, notation),
                            style = AmountLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (index < totals.lastIndex) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.6f,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 「概算含む」 beside the summary label.
 *
 * Tinted from `onPrimaryContainer` rather than given a container role of its own, because it sits
 * on the header's gradient — a `secondaryContainer` pill would vanish into the right-hand end of it.
 */
@Composable
private fun EstimateChip() {
    Surface(
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(R.string.summary_estimate_chip),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun CardTabRow(
    cards: List<PaymentCard>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onCardLongPress: (PaymentCard) -> Unit,
    onAddCard: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 0.dp,
            divider = {},
            modifier = Modifier.weight(1f),
        ) {
            Tab(
                selected = selectedIndex == 0,
                onClick = { onTabSelected(0) },
            ) {
                TabLabel(text = stringResource(R.string.list_tab_all), selected = selectedIndex == 0)
            }

            cards.forEachIndexed { index, card ->
                val tabIndex = index + 1
                Tab(
                    selected = selectedIndex == tabIndex,
                    onClick = { onTabSelected(tabIndex) },
                ) {
                    // Long-press lives on the label rather than the Tab so it does not fight the
                    // Tab's own selection handling.
                    Box(
                        modifier = Modifier.combinedClickable(
                            onClick = { onTabSelected(tabIndex) },
                            onLongClick = { onCardLongPress(card) },
                        ),
                    ) {
                        TabLabel(text = card.name, selected = selectedIndex == tabIndex)
                    }
                }
            }
        }

        IconButton(onClick = onAddCard) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.action_add_card),
            )
        }
    }
}

@Composable
private fun TabLabel(text: String, selected: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Composable
private fun SubscriptionList(
    subscriptions: List<Subscription>,
    cards: List<PaymentCard>,
    today: LocalDate,
    showEstimatePrefix: Boolean,
    notation: AmountNotation,
    onSubscriptionClick: (Subscription) -> Unit,
) {
    if (subscriptions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                title = stringResource(R.string.list_empty),
                body = stringResource(R.string.list_empty_body),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Room for the extended FAB to sit over the end of the list without hiding a row.
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 8.dp,
            bottom = 96.dp,
        ),
    ) {
        items(items = subscriptions, key = { it.id }) { subscription ->
            SubscriptionRow(
                subscription = subscription,
                card = cards.firstOrNull { it.id == subscription.cardId },
                today = today,
                showEstimatePrefix = showEstimatePrefix,
                notation = notation,
                onClick = { onSubscriptionClick(subscription) },
            )
        }
    }
}

@Composable
private fun SortMenu(
    sort: SubscriptionSort,
    ascending: Boolean,
    onSortSelected: (SubscriptionSort) -> Unit,
    onAscendingSelected: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.action_sort),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SubscriptionSort.entries.forEach { option ->
                CheckableMenuItem(
                    label = stringResource(option.labelRes()),
                    checked = option == sort,
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    },
                )
            }
            HorizontalDivider()
            CheckableMenuItem(
                label = stringResource(R.string.sort_ascending),
                checked = ascending,
                onClick = {
                    onAscendingSelected(true)
                    expanded = false
                },
            )
            CheckableMenuItem(
                label = stringResource(R.string.sort_descending),
                checked = !ascending,
                onClick = {
                    onAscendingSelected(false)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun CheckableMenuItem(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        onClick = onClick,
    )
}

internal fun SubscriptionSort.labelRes(): Int = when (this) {
    SubscriptionSort.Registered -> R.string.sort_registered
    SubscriptionSort.Name -> R.string.sort_name
    SubscriptionSort.PaymentDate -> R.string.sort_payment_date
}
