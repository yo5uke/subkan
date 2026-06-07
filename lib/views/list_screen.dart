import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:google_fonts/google_fonts.dart';
import '../models/subscription.dart';
import '../models/payment_card.dart';
import '../providers/subscription_provider.dart';
import '../providers/payment_card_provider.dart';
import '../providers/theme_provider.dart';
import '../widgets/subscription_card.dart';
import '../widgets/card_edit_dialog.dart';
import '../utils/card_brand_colors.dart';
import 'add_screen.dart';
import 'settings_screen.dart';

class ListScreen extends ConsumerWidget {
  const ListScreen({super.key});

  Future<void> _showCardDeleteDialog(BuildContext context, WidgetRef ref, PaymentCard card) async {
    final subscriptions = ref.read(subscriptionListProvider);
    final linkedSubs = subscriptions.where((sub) => sub.cardId == card.id).toList();

    final shouldDelete = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('カードを削除'),
        content: Text(linkedSubs.isNotEmpty
            ? 'このカードを削除すると、紐づいているすべてのサブスクリプション（${linkedSubs.length}件）も同時に削除されます。よろしいですか？'
            : 'このカードを削除してもよろしいですか？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('キャンセル'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red,
              foregroundColor: Colors.white,
            ),
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('削除'),
          ),
        ],
      ),
    );

    if (shouldDelete == true && context.mounted) {
      await _deleteCardAndSubscriptionsWithUndo(context, ref, card, linkedSubs);
    }
  }

  // タブ長押しで表示するアクションメニュー（並び替え／編集／削除）
  void _showCardActionMenu(
      BuildContext context, WidgetRef ref, PaymentCard card, List<PaymentCard> cards) {
    showModalBottomSheet(
      context: context,
      showDragHandle: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      builder: (sheetContext) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 4, 20, 8),
                child: Row(
                  children: [
                    Container(
                      width: 16,
                      height: 16,
                      decoration: BoxDecoration(color: card.color, shape: BoxShape.circle),
                    ),
                    const SizedBox(width: 10),
                    Flexible(
                      child: Text(
                        card.name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                ),
              ),
              ListTile(
                leading: const Icon(Icons.swap_vert),
                title: const Text('並び替え'),
                onTap: () {
                  Navigator.of(sheetContext).pop();
                  _showCardReorderSheet(context, ref);
                },
              ),
              ListTile(
                leading: const Icon(Icons.edit_outlined),
                title: const Text('名前・色を編集'),
                onTap: () {
                  Navigator.of(sheetContext).pop();
                  showDialog(
                    context: context,
                    builder: (context) =>
                        CardEditDialog(cardToEdit: card, nextOrder: cards.length),
                  );
                },
              ),
              ListTile(
                leading: const Icon(Icons.delete_outline, color: Colors.red),
                title: const Text('削除', style: TextStyle(color: Colors.red)),
                onTap: () {
                  Navigator.of(sheetContext).pop();
                  _showCardDeleteDialog(context, ref, card);
                },
              ),
            ],
          ),
        );
      },
    );
  }

  // ドラッグでカードを並び替えるボトムシート
  void _showCardReorderSheet(BuildContext context, WidgetRef ref) {
    showModalBottomSheet(
      context: context,
      showDragHandle: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      builder: (context) {
        return SafeArea(
          child: Consumer(
            builder: (context, ref, _) {
              final cards = ref.watch(paymentCardListProvider);
              return Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Padding(
                    padding: EdgeInsets.fromLTRB(20, 4, 20, 12),
                    child: Text(
                      'カードの並び替え',
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                  ),
                  ConstrainedBox(
                    constraints: BoxConstraints(
                      maxHeight: MediaQuery.of(context).size.height * 0.6,
                    ),
                    child: ReorderableListView.builder(
                      shrinkWrap: true,
                      buildDefaultDragHandles: false,
                      itemCount: cards.length,
                      onReorder: (oldIndex, newIndex) {
                        ref
                            .read(paymentCardListProvider.notifier)
                            .reorderCards(oldIndex, newIndex);
                      },
                      itemBuilder: (context, index) {
                        final card = cards[index];
                        return ListTile(
                          key: ValueKey(card.id),
                          leading: Container(
                            width: 24,
                            height: 24,
                            decoration:
                                BoxDecoration(color: card.color, shape: BoxShape.circle),
                          ),
                          title: Text(card.name),
                          trailing: ReorderableDragStartListener(
                            index: index,
                            child: const Icon(Icons.drag_handle),
                          ),
                        );
                      },
                    ),
                  ),
                ],
              );
            },
          ),
        );
      },
    );
  }

  Future<void> _deleteCardAndSubscriptionsWithUndo(
      BuildContext context, WidgetRef ref, PaymentCard card, List<Subscription> linkedSubs) async {
    await ref.read(paymentCardListProvider.notifier).deleteCard(card.id);
    for (var sub in linkedSubs) {
      await ref.read(subscriptionListProvider.notifier).deleteSubscription(sub.id);
    }

    if (context.mounted) {
      ScaffoldMessenger.of(context).hideCurrentSnackBar();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          duration: const Duration(seconds: 3),
          content: Text('${card.name}と紐づくサブスクリプションを削除しました'),
          action: SnackBarAction(
            label: '取り消す',
            onPressed: () async {
              await ref.read(paymentCardListProvider.notifier).addCard(card);
              for (var sub in linkedSubs) {
                await ref.read(subscriptionListProvider.notifier).addSubscription(sub);
              }
            },
          ),
        ),
      );

      // 強制消去タイマー
      Future.delayed(const Duration(seconds: 3), () {
        if (context.mounted) {
          ScaffoldMessenger.of(context).hideCurrentSnackBar();
        }
      });
    }
  }

  Widget _buildTabBarWithAddButton(BuildContext context, WidgetRef ref, List<PaymentCard> cards, String position) {
    final tabs = [
      const Tab(text: 'すべて'),
      ...cards.map((card) => Tab(
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onLongPress: () {
            _showCardActionMenu(context, ref, card, cards);
          },
          child: Container(
            alignment: Alignment.center,
            child: Text(card.name),
          ),
        ),
      )),
    ];

    return Container(
      color: position == 'bottom'
          ? Theme.of(context).colorScheme.surfaceContainer
          : Colors.transparent,
      child: Row(
        children: [
          Expanded(
            child: TabBar(
              isScrollable: true,
              tabAlignment: TabAlignment.start,
              labelStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
              unselectedLabelStyle: const TextStyle(fontWeight: FontWeight.normal, fontSize: 15),
              indicatorSize: TabBarIndicatorSize.tab,
              indicator: UnderlineTabIndicator(
                borderSide: BorderSide(
                  width: 3,
                  color: Theme.of(context).colorScheme.primary,
                ),
                borderRadius: const BorderRadius.only(
                  topLeft: Radius.circular(3),
                  topRight: Radius.circular(3),
                ),
              ),
              tabs: tabs,
            ),
          ),
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () {
              showDialog(
                context: context,
                builder: (context) => CardEditDialog(nextOrder: cards.length),
              );
            },
            tooltip: 'カードを追加',
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final subscriptions = ref.watch(subscriptionListProvider);
    final cards = ref.watch(paymentCardListProvider);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final tabBarPosition = ref.watch(tabBarPositionProvider);
    final sortOrder = ref.watch(subscriptionSortOrderProvider);
    final ascending = ref.watch(subscriptionSortAscendingProvider);
    // 通貨ごとの月額合計を計算
    final Map<String, double> totals = {};
    for (var sub in subscriptions) {
      double monthlyAmount = sub.billingCycle == 'yearly' ? sub.price / 12 : sub.price;
      totals[sub.currency] = (totals[sub.currency] ?? 0) + monthlyAmount;
    }

    String formatTotal(double amount, String currency) {
      final formatter = NumberFormat.currency(locale: 'ja_JP', symbol: '', decimalDigits: 0);
      final formatted = formatter.format(amount).trim();
      switch (currency) {
        case 'JPY':
          return '¥$formatted';
        case 'USD':
          return '\$$formatted';
        case 'EUR':
          return '€$formatted';
        default:
          return '$currency $formatted';
      }
    }

    // タブの数を決定（すべて + 各カード）
    final tabCount = 1 + cards.length;

    return DefaultTabController(
      length: tabCount,
      child: Scaffold(
        appBar: AppBar(
          title: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.bookmarks_outlined,
                color: Theme.of(context).colorScheme.primary,
                size: 28,
              ),
              const SizedBox(width: 8),
              Text(
                'SubKan',
                style: GoogleFonts.outfit(
                  fontWeight: FontWeight.w900,
                  fontSize: 26,
                  letterSpacing: 0.5,
                  color: isDark ? Colors.white : Colors.black87,
                ),
              ),
            ],
          ),
          actions: [
            PopupMenuButton<String>(
              icon: const Icon(Icons.sort),
              tooltip: '並び替え',
              onSelected: (String value) {
                if (value == 'asc') {
                  ref.read(subscriptionSortAscendingProvider.notifier).setAscending(true);
                } else if (value == 'desc') {
                  ref.read(subscriptionSortAscendingProvider.notifier).setAscending(false);
                } else {
                  ref.read(subscriptionSortOrderProvider.notifier).setSortOrder(value);
                }
              },
              itemBuilder: (BuildContext context) => <PopupMenuEntry<String>>[
                PopupMenuItem<String>(
                  value: 'default',
                  child: Row(
                    children: [
                      if (sortOrder == 'default') const Icon(Icons.check, size: 18),
                      if (sortOrder == 'default') const SizedBox(width: 8) else const SizedBox(width: 26),
                      const Text('登録日順'),
                    ],
                  ),
                ),
                PopupMenuItem<String>(
                  value: 'name',
                  child: Row(
                    children: [
                      if (sortOrder == 'name') const Icon(Icons.check, size: 18),
                      if (sortOrder == 'name') const SizedBox(width: 8) else const SizedBox(width: 26),
                      const Text('名称順'),
                    ],
                  ),
                ),
                PopupMenuItem<String>(
                  value: 'payment_date',
                  child: Row(
                    children: [
                      if (sortOrder == 'payment_date') const Icon(Icons.check, size: 18),
                      if (sortOrder == 'payment_date') const SizedBox(width: 8) else const SizedBox(width: 26),
                      const Text('支払日順'),
                    ],
                  ),
                ),
                const PopupMenuDivider(),
                PopupMenuItem<String>(
                  value: 'asc',
                  child: Row(
                    children: [
                      if (ascending) const Icon(Icons.check, size: 18),
                      if (ascending) const SizedBox(width: 8) else const SizedBox(width: 26),
                      const Text('昇順'),
                    ],
                  ),
                ),
                PopupMenuItem<String>(
                  value: 'desc',
                  child: Row(
                    children: [
                      if (!ascending) const Icon(Icons.check, size: 18),
                      if (!ascending) const SizedBox(width: 8) else const SizedBox(width: 26),
                      const Text('降順'),
                    ],
                  ),
                ),
              ],
            ),
            IconButton(
              icon: const Icon(Icons.settings),
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(builder: (context) => const SettingsScreen()),
                );
              },
              tooltip: '設定',
            ),
          ],
          bottom: tabBarPosition == 'top'
              ? PreferredSize(
                  preferredSize: const Size.fromHeight(48),
                  child: _buildTabBarWithAddButton(context, ref, cards, 'top'),
                )
              : null,
        ),
        body: Column(
          children: [
            Container(
              width: double.infinity,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: isDark
                      ? [
                          Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.15),
                          Theme.of(context).colorScheme.surfaceContainerLowest,
                        ]
                      : [
                          Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.35),
                          Theme.of(context).colorScheme.secondaryContainer.withValues(alpha: 0.1),
                        ],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: const BorderRadius.vertical(
                  bottom: Radius.circular(32),
                ),
              ),
              padding: const EdgeInsets.symmetric(horizontal: 28.0, vertical: 28.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '月額合計',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: Theme.of(context).colorScheme.onSurfaceVariant.withValues(alpha: 0.8),
                      letterSpacing: 0.5,
                    ),
                  ),
                  const SizedBox(height: 8),
                  if (totals.isEmpty)
                    Text(
                      '¥0',
                      style: GoogleFonts.outfit(
                        fontSize: 36,
                        fontWeight: FontWeight.w900,
                        color: Theme.of(context).colorScheme.onSurface,
                        letterSpacing: 0.5,
                      ),
                    )
                  else
                    Wrap(
                      spacing: 12,
                      runSpacing: 8,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: (() {
                        final sortedKeys = totals.keys.toList();
                        sortedKeys.sort((a, b) {
                          if (a == 'JPY') return -1;
                          if (b == 'JPY') return 1;
                          return a.compareTo(b);
                        });
                        
                        final List<Widget> widgets = [];
                        for (int i = 0; i < sortedKeys.length; i++) {
                          final currency = sortedKeys[i];
                          final total = totals[currency]!;
                          
                          widgets.add(Text(
                            formatTotal(total, currency),
                            style: GoogleFonts.outfit(
                              fontSize: 32,
                              fontWeight: FontWeight.w900,
                              color: Theme.of(context).colorScheme.onSurface,
                              letterSpacing: 0.5,
                            ),
                          ));

                          if (i < sortedKeys.length - 1) {
                            widgets.add(Text(
                              '+',
                              style: GoogleFonts.outfit(
                                fontSize: 24,
                                fontWeight: FontWeight.w600,
                                color: Theme.of(context).colorScheme.onSurfaceVariant.withValues(alpha: 0.5),
                              ),
                            ));
                          }
                        }
                        return widgets;
                      })(),
                    ),
                ],
              ),
            ),
            Expanded(
              child: TabBarView(
                children: [
                  // 「すべて」のタブ
                  _buildList(context, subscriptions, cards),
                  // 各カードごとのタブ
                  ...cards.map((card) {
                    final filteredSubs = subscriptions.where((sub) => sub.cardId == card.id).toList();
                    return _buildList(context, filteredSubs, cards);
                  }),
                ],
              ),
            ),
          ],
        ),
        floatingActionButton: FloatingActionButton.extended(
          onPressed: () {
            showModalBottomSheet(
              context: context,
              isScrollControlled: true,
              showDragHandle: true,
              shape: const RoundedRectangleBorder(
                borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
              ),
              builder: (context) => const AddScreen(),
            );
          },
          icon: const Icon(Icons.add),
          label: const Text('サブスクの追加'),
        ),
        bottomNavigationBar: tabBarPosition == 'bottom'
            ? SafeArea(
                child: _buildTabBarWithAddButton(context, ref, cards, 'bottom'),
              )
            : null,
      ),
    );
  }

  Widget _buildList(BuildContext context, List<Subscription> subs, List<PaymentCard> cards) {
    if (subs.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.library_books_outlined, size: 64, color: Colors.grey.shade400),
            const SizedBox(height: 16),
            Text(
              'サブスクリプションが登録されていません',
              style: TextStyle(color: Colors.grey.shade600),
            ),
          ],
        ),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.only(top: 8, bottom: 80),
      itemCount: subs.length,
      itemBuilder: (context, index) {
        final sub = subs[index];
        final card = cards.where((c) => c.id == sub.cardId).firstOrNull;
        return SubscriptionCard(
          subscription: sub,
          card: card,
          onTap: () {
            showModalBottomSheet(
              context: context,
              isScrollControlled: true,
              showDragHandle: true,
              shape: const RoundedRectangleBorder(
                borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
              ),
              builder: (context) => AddScreen(subscriptionToEdit: sub),
            );
          },
        );
      },
    );
  }
}
