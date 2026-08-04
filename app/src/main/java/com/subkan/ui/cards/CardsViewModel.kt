package com.subkan.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subkan.core.model.PaymentCard
import com.subkan.data.repository.DeletedCard
import com.subkan.data.repository.PaymentCardRepository
import com.subkan.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardsUiState(
    val cards: List<PaymentCard> = emptyList(),
    /** How many subscriptions each card is currently paying for, keyed by card id. */
    val subscriptionCounts: Map<String, Int> = emptyMap(),
)

@HiltViewModel
class CardsViewModel @Inject constructor(
    private val paymentCardRepository: PaymentCardRepository,
    subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val events = Channel<DeletedCard>(Channel.BUFFERED)

    /** Emitted after a delete so the screen can offer an undo. */
    val deletedCardFlow: Flow<DeletedCard> = events.receiveAsFlow()

    val uiState: StateFlow<CardsUiState> = combine(
        paymentCardRepository.observeAll(),
        subscriptionRepository.observeAll(),
    ) { cards, subscriptions ->
        CardsUiState(
            cards = cards,
            subscriptionCounts = subscriptions
                .mapNotNull { it.cardId }
                .groupingBy { it }
                .eachCount(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CardsUiState(),
    )

    fun createCard(name: String, colorHex: String) = viewModelScope.launch {
        paymentCardRepository.create(name, colorHex)
    }

    fun updateCard(id: String, name: String, colorHex: String) = viewModelScope.launch {
        paymentCardRepository.update(id, name, colorHex)
    }

    fun reorderCards(orderedIds: List<String>) = viewModelScope.launch {
        paymentCardRepository.reorder(orderedIds)
    }

    /**
     * Deletes the card but keeps its subscriptions, which is what distinguishes this screen from
     * deleting a card via its tab on the list screen.
     */
    fun deleteCard(id: String) = viewModelScope.launch {
        val deleted = paymentCardRepository.deleteKeepingSubscriptions(id) ?: return@launch
        events.send(deleted)
    }

    fun restoreCard(deleted: DeletedCard) = viewModelScope.launch {
        paymentCardRepository.restore(deleted)
    }
}
