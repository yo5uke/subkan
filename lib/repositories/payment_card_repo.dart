import 'package:hive_flutter/hive_flutter.dart';
import '../models/payment_card.dart';

class PaymentCardRepository {
  static const String boxName = 'paymentCardsBox_v1';

  Future<void> init() async {
    if (!Hive.isAdapterRegistered(1)) {
      Hive.registerAdapter(PaymentCardAdapter());
    }
    await Hive.openBox<PaymentCard>(boxName);

    // 初期データの挿入
    if (_box.isEmpty) {
      await _insertDefaultCards();
    }
  }

  Box<PaymentCard> get _box => Hive.box<PaymentCard>(boxName);

  List<PaymentCard> getAll() {
    final list = _box.values.toList();
    list.sort((a, b) => a.order.compareTo(b.order));
    return list;
  }

  PaymentCard? getById(String id) {
    return _box.get(id);
  }

  Future<void> add(PaymentCard card) async {
    await _box.put(card.id, card);
  }

  Future<void> update(PaymentCard card) async {
    await _box.put(card.id, card);
  }

  Future<void> delete(String id) async {
    await _box.delete(id);
  }

  Future<void> _insertDefaultCards() async {
    final defaultCards = [
      PaymentCard(id: 'c1', name: '楽天カード', colorHex: 'BF0000', order: 0),
      PaymentCard(id: 'c2', name: '三井住友カード', colorHex: '004D40', order: 1),
      PaymentCard(id: 'c3', name: 'JCB', colorHex: '1976D2', order: 2),
      PaymentCard(id: 'c4', name: 'Visa', colorHex: '1A1F71', order: 3),
      PaymentCard(id: 'c5', name: 'Mastercard', colorHex: 'FF5F00', order: 4),
      PaymentCard(id: 'c6', name: 'American Express', colorHex: '002663', order: 5),
      PaymentCard(id: 'c7', name: 'Diners Club', colorHex: '005596', order: 6),
    ];
    for (var card in defaultCards) {
      await _box.put(card.id, card);
    }
  }
}
