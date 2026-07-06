// SubKanアプリの起動確認用スモークテスト。
//
// Hiveなど実ストレージには依存させず、各プロバイダーを固定データで
// 上書きしてMyAppを描画できることと、主要な画面要素が表示されることを確認する。

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:subkan/main.dart';
import 'package:subkan/models/payment_card.dart';
import 'package:subkan/models/subscription.dart';
import 'package:subkan/providers/payment_card_provider.dart';
import 'package:subkan/providers/subscription_provider.dart';
import 'package:subkan/providers/theme_provider.dart';
import 'package:subkan/repositories/payment_card_repo.dart';
import 'package:subkan/repositories/settings_repo.dart';
import 'package:subkan/repositories/subscription_repo.dart';

class _FixedCardNotifier extends PaymentCardListNotifier {
  _FixedCardNotifier(List<PaymentCard> cards)
      : super(PaymentCardRepository()) {
    state = cards;
  }

  @override
  void loadCards() {}
}

class _FixedSubNotifier extends SubscriptionListNotifier {
  _FixedSubNotifier(List<Subscription> subs)
      : super(SubscriptionRepository(), 'default', true) {
    state = subs;
  }

  @override
  void loadSubscriptions() {}
}

class _FakeSettingsRepository implements SettingsRepository {
  @override
  String getThemeMode() => 'system';
  @override
  String getTabBarPosition() => 'top';
  @override
  String getSubscriptionSortOrder() => 'default';
  @override
  bool getSubscriptionSortAscending() => true;
  @override
  Future<void> setThemeMode(String mode) async {}
  @override
  Future<void> setTabBarPosition(String position) async {}
  @override
  Future<void> setSubscriptionSortOrder(String order) async {}
  @override
  Future<void> setSubscriptionSortAscending(bool ascending) async {}
  @override
  Future<void> init() async {}
}

void main() {
  testWidgets('カード未登録時はホーム画面に空状態メッセージが表示される',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          paymentCardListProvider
              .overrideWith((ref) => _FixedCardNotifier(const [])),
          subscriptionListProvider
              .overrideWith((ref) => _FixedSubNotifier(const [])),
          settingsRepositoryProvider
              .overrideWith((ref) => _FakeSettingsRepository()),
        ],
        child: const MyApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('SubKan'), findsOneWidget);
    expect(find.text('サブスクリプションが登録されていません'), findsOneWidget);
    expect(find.text('¥0'), findsOneWidget);
  });

  testWidgets('カード登録済みの場合は合計金額とサブスク一覧が表示される',
      (WidgetTester tester) async {
    final cards = [
      PaymentCard(id: 'card-a', name: 'カードA', colorHex: 'FF0000', order: 0),
    ];
    final subs = [
      Subscription(
        id: 'sub-a',
        name: 'Netflix',
        price: 1980,
        nextPaymentDate: DateTime(2026, 1, 1),
        cardId: 'card-a',
      ),
    ];

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          paymentCardListProvider
              .overrideWith((ref) => _FixedCardNotifier(cards)),
          subscriptionListProvider
              .overrideWith((ref) => _FixedSubNotifier(subs)),
          settingsRepositoryProvider
              .overrideWith((ref) => _FakeSettingsRepository()),
        ],
        child: const MyApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('¥1,980'), findsWidgets);
    expect(find.text('Netflix'), findsOneWidget);
  });
}
