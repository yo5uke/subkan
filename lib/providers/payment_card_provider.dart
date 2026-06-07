import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/payment_card.dart';
import '../repositories/payment_card_repo.dart';

final paymentCardRepositoryProvider = Provider<PaymentCardRepository>((ref) {
  return PaymentCardRepository();
});

final paymentCardListProvider = StateNotifierProvider<PaymentCardListNotifier, List<PaymentCard>>((ref) {
  final repo = ref.watch(paymentCardRepositoryProvider);
  return PaymentCardListNotifier(repo);
});

class PaymentCardListNotifier extends StateNotifier<List<PaymentCard>> {
  final PaymentCardRepository _repository;

  PaymentCardListNotifier(this._repository) : super([]) {
    loadCards();
  }

  void loadCards() {
    state = _repository.getAll();
  }

  Future<void> addCard(PaymentCard card) async {
    await _repository.add(card);
    loadCards();
  }

  Future<void> updateCard(PaymentCard card) async {
    await _repository.update(card);
    loadCards();
  }

  Future<void> deleteCard(String id) async {
    await _repository.delete(id);
    loadCards();
  }

  Future<void> reorderCards(int oldIndex, int newIndex) async {
    if (oldIndex < newIndex) {
      newIndex -= 1;
    }
    final card = state.removeAt(oldIndex);
    state.insert(newIndex, card);

    // すべてのorderを更新して保存
    for (int i = 0; i < state.length; i++) {
      final updatedCard = state[i].copyWith(order: i);
      await _repository.update(updatedCard);
    }
    loadCards();
  }
}
