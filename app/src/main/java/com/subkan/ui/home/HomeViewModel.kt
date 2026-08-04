package com.subkan.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subkan.core.model.AmountNotation
import com.subkan.core.model.PaymentCard
import com.subkan.core.model.SubscriptionSort
import com.subkan.core.model.Subscription
import com.subkan.core.model.TabBarPosition
import com.subkan.core.model.sorted
import com.subkan.core.time.AppClock
import com.subkan.data.preferences.SettingsRepository
import com.subkan.data.repository.DeletedCard
import com.subkan.data.repository.PaymentCardRepository
import com.subkan.data.repository.SubscriptionDraft
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
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val cards: List<PaymentCard> = emptyList(),
    /** Already ordered by the user's chosen sort — the screen renders it as it arrives. */
    val subscriptions: List<Subscription> = emptyList(),
    val sort: SubscriptionSort = SubscriptionSort.Registered,
    val sortAscending: Boolean = true,
    val tabBarPosition: TabBarPosition = TabBarPosition.Top,
    val showEstimatePrefix: Boolean = true,
    val notation: AmountNotation = AmountNotation.Symbol,
    val today: LocalDate = LocalDate.EPOCH,
    val isLoading: Boolean = true,
)

/**
 * Things that should happen once and not replay on rotation.
 *
 * Both carry enough to undo themselves, which is why the payloads are whole objects rather than
 * ids — once the row is gone the database cannot answer what it used to contain.
 */
sealed interface HomeEvent {
    data class CardDeleted(val deleted: DeletedCard) : HomeEvent
    data class SubscriptionDeleted(val subscription: Subscription) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val paymentCardRepository: PaymentCardRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val eventFlow: Flow<HomeEvent> = events.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        paymentCardRepository.observeAll(),
        subscriptionRepository.observeAll(),
        settingsRepository.settings,
    ) { cards, subscriptions, settings ->
        val today = clock.today()
        HomeUiState(
            cards = cards,
            subscriptions = subscriptions.sorted(settings.sort, settings.sortAscending, today),
            sort = settings.sort,
            sortAscending = settings.sortAscending,
            tabBarPosition = settings.tabBarPosition,
            showEstimatePrefix = settings.showEstimatePrefix,
            notation = settings.amountNotation,
            today = today,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    // --- Sorting -----------------------------------------------------------------------------

    fun setSort(sort: SubscriptionSort) = viewModelScope.launch {
        settingsRepository.setSort(sort)
    }

    fun setSortAscending(ascending: Boolean) = viewModelScope.launch {
        settingsRepository.setSortAscending(ascending)
    }

    // --- Subscriptions -----------------------------------------------------------------------

    fun createSubscription(draft: SubscriptionDraft) = viewModelScope.launch {
        subscriptionRepository.create(draft)
    }

    fun updateSubscription(id: String, draft: SubscriptionDraft) = viewModelScope.launch {
        subscriptionRepository.update(id, draft)
    }

    fun deleteSubscription(subscription: Subscription) = viewModelScope.launch {
        subscriptionRepository.delete(subscription.id)
        events.send(HomeEvent.SubscriptionDeleted(subscription))
    }

    fun restoreSubscription(subscription: Subscription) = viewModelScope.launch {
        subscriptionRepository.restore(subscription)
    }

    // --- Cards -------------------------------------------------------------------------------

    /**
     * [onCreated] exists for the editor sheet, which offers 「新しいカードを追加…」 inline and has to
     * select the card that was just made. Waiting for it to appear in the observed list would work
     * too, but only by guessing that the newest card is the last one.
     */
    fun createCard(
        name: String,
        colorHex: String,
        onCreated: (String) -> Unit = {},
    ) = viewModelScope.launch {
        onCreated(paymentCardRepository.create(name, colorHex))
    }

    fun updateCard(id: String, name: String, colorHex: String) = viewModelScope.launch {
        paymentCardRepository.update(id, name, colorHex)
    }

    fun reorderCards(orderedIds: List<String>) = viewModelScope.launch {
        paymentCardRepository.reorder(orderedIds)
    }

    /**
     * Deleting a card from its own tab takes its subscriptions with it — which is why the screen
     * confirms with the count first.
     */
    fun deleteCardWithSubscriptions(cardId: String) = viewModelScope.launch {
        val deleted = paymentCardRepository.deleteWithSubscriptions(cardId) ?: return@launch
        events.send(HomeEvent.CardDeleted(deleted))
    }

    fun restoreCard(deleted: DeletedCard) = viewModelScope.launch {
        paymentCardRepository.restore(deleted)
    }
}
