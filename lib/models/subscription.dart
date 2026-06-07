import 'package:hive/hive.dart';

part 'subscription.g.dart';

@HiveType(typeId: 0)
class Subscription extends HiveObject {
  @HiveField(0)
  final String id;

  @HiveField(1)
  final String name;

  @HiveField(2)
  final double price;

  @HiveField(3)
  final DateTime nextPaymentDate;

  @HiveField(4)
  final String cardId;

  @HiveField(5)
  final String currency; // 将来の多通貨対応用

  @HiveField(6)
  final String billingCycle; // 例: 'monthly', 'yearly'

  Subscription({
    required this.id,
    required this.name,
    required this.price,
    required this.nextPaymentDate,
    required this.cardId,
    this.currency = 'JPY',
    this.billingCycle = 'monthly',
  });

  Subscription copyWith({
    String? name,
    double? price,
    DateTime? nextPaymentDate,
    String? cardId,
    String? currency,
    String? billingCycle,
  }) {
    return Subscription(
      id: id,
      name: name ?? this.name,
      price: price ?? this.price,
      nextPaymentDate: nextPaymentDate ?? this.nextPaymentDate,
      cardId: cardId ?? this.cardId,
      currency: currency ?? this.currency,
      billingCycle: billingCycle ?? this.billingCycle,
    );
  }
}
