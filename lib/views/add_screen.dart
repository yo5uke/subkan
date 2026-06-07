import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:uuid/uuid.dart';
import '../models/subscription.dart';
import '../providers/subscription_provider.dart';
import '../providers/payment_card_provider.dart';
import '../widgets/card_edit_dialog.dart';
import '../utils/card_brand_colors.dart';

class AddScreen extends ConsumerStatefulWidget {
  final Subscription? subscriptionToEdit;

  const AddScreen({super.key, this.subscriptionToEdit});

  @override
  ConsumerState<AddScreen> createState() => _AddScreenState();
}

class _AddScreenState extends ConsumerState<AddScreen> {
  final _formKey = GlobalKey<FormState>();
  final _cardDropdownKey = GlobalKey<FormFieldState<String>>();

  late TextEditingController _nameController;
  late TextEditingController _priceController;
  DateTime _nextPaymentDate = DateTime.now();
  String? _selectedCardId;
  String _billingCycle = 'monthly';
  String _currency = 'JPY'; // 拡張性: デフォルトは日本円

  @override
  void initState() {
    super.initState();
    final sub = widget.subscriptionToEdit;
    _nameController = TextEditingController(text: sub?.name ?? '');
    _priceController = TextEditingController(text: sub?.price.toString() ?? '');

    if (sub != null) {
      _nextPaymentDate = sub.nextPaymentDate;
      _selectedCardId = sub.cardId;
      _billingCycle = sub.billingCycle;
      _currency = sub.currency;
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _priceController.dispose();
    super.dispose();
  }

  void _save() {
    if (_formKey.currentState!.validate() && _selectedCardId != null) {
      final name = _nameController.text;
      final price = double.tryParse(_priceController.text) ?? 0;

      final subscription = Subscription(
        id: widget.subscriptionToEdit?.id ?? const Uuid().v4(),
        name: name,
        price: price,
        nextPaymentDate: _nextPaymentDate,
        cardId: _selectedCardId!,
        currency: _currency,
        billingCycle: _billingCycle,
      );

      if (widget.subscriptionToEdit == null) {
        ref
            .read(subscriptionListProvider.notifier)
            .addSubscription(subscription);
      } else {
        ref
            .read(subscriptionListProvider.notifier)
            .updateSubscription(subscription);
      }

      Navigator.of(context).pop();
    } else if (_selectedCardId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('支払カードを選択してください')),
      );
    }
  }

  void _delete() {
    if (widget.subscriptionToEdit != null) {
      ref
          .read(subscriptionListProvider.notifier)
          .deleteSubscription(widget.subscriptionToEdit!.id);
      Navigator.of(context).pop();
    }
  }

  Future<void> _selectDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: _nextPaymentDate,
      firstDate: DateTime.now().subtract(const Duration(days: 365)),
      lastDate: DateTime.now().add(const Duration(days: 365 * 5)),
    );
    if (picked != null && picked != _nextPaymentDate) {
      setState(() {
        _nextPaymentDate = picked;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.subscriptionToEdit != null;
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;
    final dateFormatter = DateFormat('yyyy/MM/dd', 'ja_JP');
    final cards = ref.watch(paymentCardListProvider);

    // Initial setup for default card selection if not editing and cards are available
    if (_selectedCardId == null && cards.isNotEmpty) {
      _selectedCardId = cards.first.id;
    }

    return Padding(
      padding: EdgeInsets.only(bottom: bottomInset),
      child: Container(
        padding: const EdgeInsets.only(left: 24, right: 24, bottom: 24, top: 4),
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(context).size.height * 0.9,
        ),
        child: SingleChildScrollView(
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      isEditing ? 'サブスクの編集' : '新しいサブスク',
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    if (isEditing)
                      IconButton(
                        icon: const Icon(Icons.delete, color: Colors.red),
                        onPressed: _delete,
                      ),
                  ],
                ),
                const SizedBox(height: 24),
                // サービス名
                TextFormField(
                  controller: _nameController,
                  decoration: const InputDecoration(
                    labelText: 'サービス名',
                    hintText: '例: Netflix, Amazon Prime',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.design_services),
                  ),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return 'サービス名を入力してください';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 16),

                // 金額と通貨（拡張性用）
                Row(
                  children: [
                    Expanded(
                      flex: 3,
                      child: TextFormField(
                        controller: _priceController,
                        decoration: const InputDecoration(
                          labelText: '金額',
                          border: OutlineInputBorder(),
                          prefixIcon: Icon(Icons.attach_money),
                        ),
                        keyboardType: TextInputType.number,
                        validator: (value) {
                          if (value == null || value.isEmpty) {
                            return '金額を入力してください';
                          }
                          if (double.tryParse(value) == null) {
                            return '数値を入力してください';
                          }
                          return null;
                        },
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      flex: 1,
                      child: DropdownButtonFormField<String>(
                        initialValue: _currency,
                        decoration: const InputDecoration(
                          labelText: '通貨',
                          border: OutlineInputBorder(),
                        ),
                        items: ['JPY', 'USD', 'EUR'].map((String value) {
                          return DropdownMenuItem<String>(
                            value: value,
                            child: Text(value),
                          );
                        }).toList(),
                        onChanged: (newValue) {
                          setState(() {
                            _currency = newValue!;
                          });
                        },
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),

                // 支払い間隔
                DropdownButtonFormField<String>(
                  initialValue: _billingCycle,
                  decoration: const InputDecoration(
                    labelText: '支払い間隔',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.autorenew),
                  ),
                  items: const [
                    DropdownMenuItem(value: 'monthly', child: Text('月額')),
                    DropdownMenuItem(value: 'yearly', child: Text('年額')),
                  ],
                  onChanged: (newValue) {
                    setState(() {
                      _billingCycle = newValue!;
                    });
                  },
                ),
                const SizedBox(height: 16),

                // 次回支払日
                InkWell(
                  onTap: () => _selectDate(context),
                  child: InputDecorator(
                    decoration: const InputDecoration(
                      labelText: '次回支払日',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.calendar_today),
                    ),
                    child: Text(dateFormatter.format(_nextPaymentDate)),
                  ),
                ),
                const SizedBox(height: 16),

                // 支払カード
                DropdownButtonFormField<String>(
                  key: _cardDropdownKey,
                  initialValue: cards.any((c) => c.id == _selectedCardId)
                      ? _selectedCardId
                      : null,
                  decoration: const InputDecoration(
                    labelText: '支払カード',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.credit_card),
                  ),
                  items: [
                    ...cards.map((card) {
                      return DropdownMenuItem<String>(
                        value: card.id,
                        child: Row(
                          children: [
                            Container(
                              width: 12,
                              height: 12,
                              decoration: BoxDecoration(
                                color: card.color,
                                shape: BoxShape.circle,
                              ),
                            ),
                            const SizedBox(width: 8),
                            Text(card.name),
                          ],
                        ),
                      );
                    }),
                    const DropdownMenuItem<String>(
                      value: '_add_new_card_',
                      child: Row(
                        children: [
                          Icon(Icons.add, size: 16, color: Colors.blue),
                          SizedBox(width: 8),
                          Text(
                            '新しいカードを追加...',
                            style: TextStyle(
                              color: Colors.blue,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                  onChanged: (newValue) async {
                    if (newValue == '_add_new_card_') {
                      final previousSelected = _selectedCardId;
                      // 一時的に選択表示を元の値に戻す
                      _cardDropdownKey.currentState?.didChange(previousSelected);

                      await showDialog(
                        context: context,
                        builder: (context) => CardEditDialog(nextOrder: cards.length),
                      );

                      final updatedCards = ref.read(paymentCardListProvider);
                      if (updatedCards.length > cards.length) {
                        final newCardId = updatedCards.last.id;
                        setState(() {
                          _selectedCardId = newCardId;
                        });
                        // 新しいカードのIDをドロップダウンに反映する
                        _cardDropdownKey.currentState?.didChange(newCardId);
                      }
                    } else {
                      setState(() {
                        _selectedCardId = newValue;
                      });
                    }
                  },
                  validator: (value) => value == null ? '支払カードを選択してください' : null,
                ),
                const SizedBox(height: 32),

                // 保存ボタン
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                  onPressed: _save,
                  child: Text(
                    isEditing ? '更新する' : '登録する',
                    style: const TextStyle(
                        fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
