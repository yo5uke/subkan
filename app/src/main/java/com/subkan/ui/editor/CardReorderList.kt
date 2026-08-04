package com.subkan.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.subkan.R
import com.subkan.core.model.PaymentCard
import com.subkan.ui.theme.cardColor
import kotlin.math.roundToInt

/**
 * A drag-to-reorder list of payment cards.
 *
 * Deliberately a plain [Column], not a `LazyColumn`: the list is a handful of cards, and knowing
 * every row is composed makes reordering a matter of moving one entry rather than reconciling
 * against a viewport.
 *
 * Row height is measured rather than assumed, so the drag threshold stays correct when the user has
 * a large font scale set.
 *
 * Dragging is not reachable with TalkBack, so every row also carries explicit move up / move down
 * buttons. They are the same operation, not a lesser one.
 */
@Composable
fun CardReorderList(
    cards: List<PaymentCard>,
    onOrderChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    onRowClick: ((PaymentCard) -> Unit)? = null,
    trailingContent: @Composable (PaymentCard) -> Unit = {},
) {
    /*
     * While a drag is in flight the finger owns the order, so it is held here — as *ids*, never as
     * card objects. Row contents always come from `cards`, so renaming or recolouring a card shows
     * up immediately instead of being masked by a stale local copy.
     *
     * The override is not cleared on drop. Once the write lands, `cards` arrives in exactly this
     * order and applying the override becomes a no-op; clearing it early would flash the old order
     * for the length of the database round-trip. A card added later is simply appended, which is
     * where its `sort_order` puts it anyway.
     */
    var dragOrder by remember { mutableStateOf<List<String>?>(null) }

    val ordered = remember(cards, dragOrder) {
        val order = dragOrder
        if (order == null) {
            cards
        } else {
            val byId = cards.associateBy { it.id }
            order.mapNotNull(byId::get) + cards.filterNot { it.id in order }
        }
    }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var rowHeightPx by remember { mutableIntStateOf(0) }

    Column(modifier = modifier) {
        ordered.forEachIndexed { index, card ->
            val isDragging = index == draggingIndex

            Surface(
                color = if (isDragging) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.surface
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { if (it.height > 0) rowHeightPx = it.height }
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f },
            ) {
                Row(
                    modifier = Modifier
                        .then(
                            if (onRowClick != null) {
                                Modifier.clickable { onRowClick(card) }
                            } else {
                                Modifier
                            },
                        )
                        .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(cardColor(card.colorHex)),
                    )
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    trailingContent(card)

                    IconButton(
                        onClick = { onOrderChanged(ordered.movedIds(index, index - 1)) },
                        enabled = index > 0,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.cd_move_up),
                        )
                    }
                    IconButton(
                        onClick = { onOrderChanged(ordered.movedIds(index, index + 1)) },
                        enabled = index < ordered.lastIndex,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_move_down),
                        )
                    }

                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = stringResource(R.string.cd_drag_handle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(24.dp)
                            .pointerInput(index, rowHeightPx) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragOrder = ordered.map { it.id }
                                        draggingIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        dragOrder?.let(onOrderChanged)
                                        draggingIndex = -1
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = -1
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        if (rowHeightPx <= 0) return@detectDragGestures
                                        dragOffsetY += amount.y

                                        // One row of travel moves the item one place; the leftover
                                        // offset is kept so a slow drag does not ratchet.
                                        val steps = (dragOffsetY / rowHeightPx).roundToInt()
                                        if (steps == 0) return@detectDragGestures

                                        val from = draggingIndex
                                        val to = (from + steps).coerceIn(0, ordered.lastIndex)
                                        if (to == from) return@detectDragGestures

                                        dragOrder = ordered.movedIds(from, to)
                                        draggingIndex = to
                                        dragOffsetY -= (to - from) * rowHeightPx
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}

/** The id order this list would have if the card at [from] moved to [to]. */
private fun List<PaymentCard>.movedIds(from: Int, to: Int): List<String> {
    val ids = map { it.id }.toMutableList()
    if (from !in ids.indices || to !in ids.indices) return ids
    ids.add(to, ids.removeAt(from))
    return ids
}
