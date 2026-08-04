package com.subkan.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.subkan.R
import com.subkan.core.model.PaymentCard
import com.subkan.ui.components.inheritedListItemColors
import com.subkan.ui.theme.cardColor

/** What long-pressing a card's tab offers. */
@Composable
fun CardActionsSheet(
    card: PaymentCard,
    onDismiss: () -> Unit,
    onReorder: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(cardColor(card.colorHex)),
                )
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Transparent containers: the sheet is surfaceContainerLow, and the ListItem default
            // of surface would stack a lighter block on top of it.
            ListItem(
                headlineContent = { Text(stringResource(R.string.card_action_reorder)) },
                leadingContent = { Icon(Icons.Filled.SwapVert, contentDescription = null) },
                colors = inheritedListItemColors(),
                modifier = Modifier.clickableListItem(onReorder),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.card_action_edit)) },
                leadingContent = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                colors = inheritedListItemColors(),
                modifier = Modifier.clickableListItem(onEdit),
            )
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                colors = inheritedListItemColors(),
                modifier = Modifier.clickableListItem(onDelete),
            )
        }
    }
}

/** Drag the cards into the order their tabs should appear in. */
@Composable
fun CardReorderSheet(
    cards: List<PaymentCard>,
    onDismiss: () -> Unit,
    onOrderChanged: (List<String>) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                text = stringResource(R.string.card_reorder_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            )
            CardReorderList(
                cards = cards,
                onOrderChanged = onOrderChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}

private fun Modifier.clickableListItem(onClick: () -> Unit): Modifier =
    fillMaxWidth().clickable(onClick = onClick)
