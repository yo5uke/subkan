import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/payment_card.dart';
import '../providers/payment_card_provider.dart';
import '../providers/subscription_provider.dart';
import '../widgets/card_edit_dialog.dart';
import '../utils/card_brand_colors.dart';

class CardManagementScreen extends ConsumerWidget {
  const CardManagementScreen({super.key});

  Future<void> _deleteCardWithUndo(BuildContext context, WidgetRef ref, PaymentCard card) async {
    final removedCard = card;
    await ref.read(paymentCardListProvider.notifier).deleteCard(card.id);

    if (context.mounted) {
      ScaffoldMessenger.of(context).hideCurrentSnackBar();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          duration: const Duration(seconds: 3),
          content: Text('${removedCard.name}を削除しました'),
          action: SnackBarAction(
            label: '取り消す',
            onPressed: () async {
              await ref.read(paymentCardListProvider.notifier).addCard(removedCard);
            },
          ),
        ),
      );

      Future.delayed(const Duration(seconds: 3), () {
        if (context.mounted) {
          ScaffoldMessenger.of(context).hideCurrentSnackBar();
        }
      });
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cards = ref.watch(paymentCardListProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('支払いカードの管理'),
      ),
      body: cards.isEmpty
          ? const Center(child: Text('カードがありません。右下のボタンから追加してください。'))
          : ReorderableListView.builder(
              buildDefaultDragHandles: false,
              itemCount: cards.length,
              onReorder: (oldIndex, newIndex) {
                ref.read(paymentCardListProvider.notifier).reorderCards(oldIndex, newIndex);
              },
              itemBuilder: (context, index) {
                final card = cards[index];
                return Dismissible(
                  key: ValueKey(card.id),
                  direction: DismissDirection.endToStart,
                  background: Container(
                    color: Colors.red,
                    alignment: Alignment.centerRight,
                    padding: const EdgeInsets.only(right: 20),
                    child: const Icon(Icons.delete, color: Colors.white),
                  ),
                  onDismissed: (direction) async {
                    await _deleteCardWithUndo(context, ref, card);
                  },
                  child: ListTile(
                    leading: Container(
                      width: 24,
                      height: 24,
                      decoration: BoxDecoration(
                        color: card.color,
                        shape: BoxShape.circle,
                      ),
                    ),
                    title: Text(card.name),
                    onLongPress: () async {
                      final subscriptions = ref.read(subscriptionListProvider);
                      final isLinked = subscriptions.any((sub) => sub.cardId == card.id);

                      final shouldDelete = await showDialog<bool>(
                        context: context,
                        builder: (context) => AlertDialog(
                          title: const Text('カードを削除'),
                          content: Text(isLinked
                              ? 'このカードは現在サブスクリプションの支払いに設定されています。削除すると、対象サブスクリプションの支払いカード情報が未設定になります。よろしいですか？'
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
                        await _deleteCardWithUndo(context, ref, card);
                      }
                    },
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        IconButton(
                          icon: const Icon(Icons.edit),
                          onPressed: () {
                            showDialog(
                              context: context,
                              builder: (context) => CardEditDialog(cardToEdit: card, nextOrder: cards.length),
                            );
                          },
                        ),
                        ReorderableDragStartListener(
                          index: index,
                          child: const Icon(Icons.drag_handle),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          showDialog(
            context: context,
            builder: (context) => CardEditDialog(nextOrder: cards.length),
          );
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
