import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/subscription.dart';
import '../repositories/subscription_repo.dart';
import 'theme_provider.dart';

final repositoryProvider = Provider<SubscriptionRepository>((ref) {
  return SubscriptionRepository();
});

final subscriptionListProvider = StateNotifierProvider<SubscriptionListNotifier, List<Subscription>>((ref) {
  final repo = ref.watch(repositoryProvider);
  final sortOrder = ref.watch(subscriptionSortOrderProvider);
  final ascending = ref.watch(subscriptionSortAscendingProvider);
  return SubscriptionListNotifier(repo, sortOrder, ascending);
});

class SubscriptionListNotifier extends StateNotifier<List<Subscription>> {
  final SubscriptionRepository _repository;
  final String _sortOrder;
  final bool _ascending;

  SubscriptionListNotifier(this._repository, this._sortOrder, this._ascending) : super([]) {
    loadSubscriptions();
  }

  void loadSubscriptions() {
    final list = _repository.getAll();
    
    switch (_sortOrder) {
      case 'name':
        list.sort((a, b) => a.name.toLowerCase().compareTo(b.name.toLowerCase()));
        break;
      case 'payment_date':
        list.sort((a, b) => a.nextPaymentDate.compareTo(b.nextPaymentDate));
        break;
      case 'default':
      default:
        // 格納順（何もしない）
        break;
    }
    
    if (!_ascending) {
      state = list.reversed.toList();
    } else {
      state = list;
    }
  }

  Future<void> addSubscription(Subscription sub) async {
    await _repository.add(sub);
    loadSubscriptions();
  }

  Future<void> updateSubscription(Subscription sub) async {
    await _repository.update(sub);
    loadSubscriptions();
  }

  Future<void> deleteSubscription(String id) async {
    await _repository.delete(id);
    loadSubscriptions();
  }
}
