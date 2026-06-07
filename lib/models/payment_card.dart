import 'package:hive/hive.dart';

part 'payment_card.g.dart';

@HiveType(typeId: 1)
class PaymentCard extends HiveObject {
  @HiveField(0)
  final String id;

  @HiveField(1)
  final String name;

  @HiveField(2)
  final String colorHex;

  @HiveField(3)
  final int order;

  PaymentCard({
    required this.id,
    required this.name,
    required this.colorHex,
    required this.order,
  });

  PaymentCard copyWith({
    String? name,
    String? colorHex,
    int? order,
  }) {
    return PaymentCard(
      id: id,
      name: name ?? this.name,
      colorHex: colorHex ?? this.colorHex,
      order: order ?? this.order,
    );
  }
}
