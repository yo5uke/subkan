import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';
import '../models/payment_card.dart';
import '../providers/payment_card_provider.dart';

class CardEditDialog extends ConsumerStatefulWidget {
  final PaymentCard? cardToEdit;
  final int nextOrder;

  const CardEditDialog({super.key, this.cardToEdit, required this.nextOrder});

  @override
  ConsumerState<CardEditDialog> createState() => _CardEditDialogState();
}

class _CardEditDialogState extends ConsumerState<CardEditDialog> {
  final _formKey = GlobalKey<FormState>();
  late TextEditingController _nameController;
  late String _colorHex;

  final List<String> _presetColors = [
    'BF0000', '004D40', '1976D2', '1A1F71', 'FF5F00', '002663', '005596', '333333', '9C27B0', 'E91E63'
  ];

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.cardToEdit?.name ?? '');
    _colorHex = widget.cardToEdit?.colorHex ?? _presetColors.first;
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  void _save() {
    if (_formKey.currentState!.validate()) {
      final card = PaymentCard(
        id: widget.cardToEdit?.id ?? const Uuid().v4(),
        name: _nameController.text,
        colorHex: _colorHex,
        order: widget.cardToEdit?.order ?? widget.nextOrder,
      );

      if (widget.cardToEdit == null) {
        ref.read(paymentCardListProvider.notifier).addCard(card);
      } else {
        ref.read(paymentCardListProvider.notifier).updateCard(card);
      }

      Navigator.of(context).pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(28),
      ),
      title: Text(widget.cardToEdit == null ? 'カードを追加' : 'カードを編集'),
      content: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(labelText: 'カード名 (例: 楽天カード)'),
              validator: (val) => (val == null || val.isEmpty) ? '必須項目です' : null,
            ),
            const SizedBox(height: 16),
            const Text('テーマカラー'),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: _presetColors.map((hex) {
                final isSelected = hex == _colorHex;
                return GestureDetector(
                  onTap: () {
                    setState(() {
                      _colorHex = hex;
                    });
                  },
                  child: Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      color: Color(int.parse('FF$hex', radix: 16)),
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: isSelected ? Colors.black : Colors.transparent,
                        width: 3,
                      ),
                    ),
                    child: isSelected ? const Icon(Icons.check, color: Colors.white, size: 20) : null,
                  ),
                );
              }).toList(),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('キャンセル'),
        ),
        ElevatedButton(
          onPressed: _save,
          child: const Text('保存'),
        ),
      ],
    );
  }
}
