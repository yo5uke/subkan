import 'package:hive_flutter/hive_flutter.dart';
import '../models/subscription.dart';

class SubscriptionRepository {
  static const String boxName = 'subscriptionsBox_v2';

  Future<void> init() async {
    // TypeAdapterの登録 (自動生成ファイルを使用)
    if (!Hive.isAdapterRegistered(0)) {
      Hive.registerAdapter(SubscriptionAdapter());
    }
    await Hive.openBox<Subscription>(boxName);
  }

  Box<Subscription> get _box => Hive.box<Subscription>(boxName);

  List<Subscription> getAll() {
    return _box.values.toList();
  }

  Future<void> add(Subscription subscription) async {
    await _box.put(subscription.id, subscription);
  }

  Future<void> update(Subscription subscription) async {
    await _box.put(subscription.id, subscription);
  }

  Future<void> delete(String id) async {
    await _box.delete(id);
  }
}
