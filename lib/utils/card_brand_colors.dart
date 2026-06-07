import 'package:flutter/material.dart';
import '../models/payment_card.dart';

extension PaymentCardColor on PaymentCard {
  /// 保存されている16進カラーコード（例: 'BF0000'）からFlutterのColorを生成する。
  Color get color => Color(int.parse('FF$colorHex', radix: 16));
}

enum CardBrand {
  rakuten('楽天カード'),
  smbc('三井住友カード'),
  jcb('JCB'),
  visa('Visa'),
  mastercard('Mastercard'),
  other('その他');

  final String displayName;
  const CardBrand(this.displayName);
}

class CardBrandColors {
  static Color getColor(CardBrand brand) {
    switch (brand) {
      case CardBrand.rakuten:
        return const Color(0xFFBF0000); // 楽天の赤
      case CardBrand.smbc:
        return const Color(0xFF004D40); // 三井住友の深緑系
      case CardBrand.jcb:
        return const Color(0xFF1976D2); // JCBの青
      case CardBrand.visa:
        return const Color(0xFF1A1F71); // Visaの青
      case CardBrand.mastercard:
        return const Color(0xFFFF5F00); // Mastercardのオレンジ
      case CardBrand.other:
        return Colors.blueGrey;
    }
  }

  static Color getLightAccent(CardBrand brand) {
    // ダークモードでも視認性を高めるためのライトなアクセントカラー
    switch (brand) {
      case CardBrand.rakuten:
        return const Color(0xFFFFCDD2);
      case CardBrand.smbc:
        return const Color(0xFFB2DFDB);
      case CardBrand.jcb:
        return const Color(0xFFBBDEFB);
      case CardBrand.visa:
        return const Color(0xFFC5CAE9);
      case CardBrand.mastercard:
        return const Color(0xFFFFE0B2);
      case CardBrand.other:
        return Colors.grey.shade300;
    }
  }
}
