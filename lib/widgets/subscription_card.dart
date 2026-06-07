import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:intl/intl.dart';
import 'package:google_fonts/google_fonts.dart';
import '../models/subscription.dart';
import '../models/payment_card.dart';
import '../utils/icon_fetcher.dart';
import '../utils/card_brand_colors.dart';

class SubscriptionCard extends StatelessWidget {
  final Subscription subscription;
  final PaymentCard? card;
  final VoidCallback onTap;

  const SubscriptionCard({
    super.key,
    required this.subscription,
    this.card,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    String getFormattedPrice(double price, String currency) {
      final formatter = NumberFormat.currency(
        locale: 'ja_JP',
        symbol: '',
        decimalDigits: 0,
      );
      final formattedValue = formatter.format(price).trim();
      switch (currency) {
        case 'JPY':
          return '¥$formattedValue';
        case 'USD':
          return '\$$formattedValue';
        case 'EUR':
          return '€$formattedValue';
        default:
          return '$currency $formattedValue';
      }
    }
    final dateFormatter = DateFormat('yyyy/MM/dd');

    // 日付の計算 (残り日数)
    final now = DateTime.now();
    final nextDate = DateTime(subscription.nextPaymentDate.year, subscription.nextPaymentDate.month, subscription.nextPaymentDate.day);
    final today = DateTime(now.year, now.month, now.day);
    final diffDays = nextDate.difference(today).inDays;

    String diffText;
    Color diffColor;
    if (diffDays < 0) {
      diffText = '支払い遅延';
      diffColor = Colors.red;
    } else if (diffDays == 0) {
      diffText = '今日';
      diffColor = Colors.orange;
    } else if (diffDays <= 3) {
      diffText = 'あと $diffDays 日';
      diffColor = Colors.orange;
    } else {
      diffText = 'あと $diffDays 日';
      diffColor = Colors.grey.shade600;
    }

    final isDark = Theme.of(context).brightness == Brightness.dark;
    final iconUrl = IconFetcher.getIconUrl(subscription.name);

    // カード情報が存在しない場合のフォールバックカラー
    final brandColor = card?.color ?? Colors.blueGrey;
    // ダークモードでも視認性を高めるため少し明るくする（簡易的な調整）
    final accentColor = isDark ? brandColor.withValues(alpha: 0.7) : brandColor;

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Container(
          decoration: BoxDecoration(
            border: Border(
              left: BorderSide(
                color: accentColor,
                width: 6,
              ),
            ),
          ),
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              // サービスアイコン
              ClipRRect(
                borderRadius: BorderRadius.circular(14),
                child: SizedBox(
                  width: 48,
                  height: 48,
                  // ロゴURLが特定できないサービスは、地球儀アイコンを避けて
                  // 直接M3スタイルのフォールバックタイルを表示する。
                  child: iconUrl.isEmpty
                      ? _buildFallbackIcon(subscription.name)
                      : CachedNetworkImage(
                          imageUrl: iconUrl,
                          fit: BoxFit.cover,
                          fadeInDuration: const Duration(milliseconds: 250),
                          // 48pxの表示枠に合わせてデコードサイズを抑え、メモリ使用を最適化
                          memCacheWidth: 96,
                          memCacheHeight: 96,
                          // 読み込み中・失敗時いずれも素のグレーではなくおしゃれなタイルを表示
                          placeholder: (context, url) => _buildFallbackIcon(subscription.name),
                          errorWidget: (context, url, error) => _buildFallbackIcon(subscription.name),
                        ),
                ),
              ),
              const SizedBox(width: 16),
              // メイン情報
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      subscription.name,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Icon(Icons.credit_card, size: 14, color: accentColor),
                        const SizedBox(width: 4),
                        Text(
                          card?.name ?? '不明なカード',
                          style: TextStyle(
                            fontSize: 12,
                            color: isDark ? Colors.grey.shade400 : Colors.grey.shade600,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              // 価格と日付
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    getFormattedPrice(subscription.price, subscription.currency),
                    style: GoogleFonts.outfit(
                      fontSize: 20,
                      fontWeight: FontWeight.w800,
                      color: Theme.of(context).colorScheme.onSurface,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Row(
                    children: [
                      Text(
                        dateFormatter.format(subscription.nextPaymentDate),
                        style: TextStyle(
                          fontSize: 12,
                          color: isDark ? Colors.grey.shade400 : Colors.grey.shade600,
                        ),
                      ),
                      const SizedBox(width: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: diffColor.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          diffText,
                          style: TextStyle(
                            fontSize: 10,
                            fontWeight: FontWeight.w800,
                            color: diffColor,
                            letterSpacing: 0.5,
                          ),
                        ),
                      ),
                    ],
                  )
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  // M3 Expressiveカラーのグラデーションペア (背景色)。再生成を避けるためstatic const化。
  static const List<List<Color>> _fallbackGradients = [
    [Color(0xFFEADDFF), Color(0xFFD0BCFF)], // パープル / ラベンダー
    [Color(0xFFFFD8E4), Color(0xFFF2B8B5)], // ピンク / ローズ
    [Color(0xFFD3E4FF), Color(0xFFADC1F9)], // ライトブルー / ブルー
    [Color(0xFFC7F3D6), Color(0xFF8CE7A8)], // ミント / グリーン
    [Color(0xFFFFE0B2), Color(0xFFFFB74D)], // オレンジ
    [Color(0xFFE2F1AF), Color(0xFFC5E1A5)], // ライム
    [Color(0xFFE0F7FA), Color(0xFF80DEEA)], // シアン
    [Color(0xFFF3E5F5), Color(0xFFCE93D8)], // マゼンタ
  ];

  // グラデーションの背景色と調和する濃い同系色のテキスト色
  static const List<Color> _fallbackTextColors = [
    Color(0xFF21005D), // パープル用
    Color(0xFF410002), // ローズ用
    Color(0xFF001D35), // ブルー用
    Color(0xFF072100), // グリーン用
    Color(0xFF4E342E), // オレンジ用
    Color(0xFF253600), // ライム用
    Color(0xFF00363A), // シアン用
    Color(0xFF4A0072), // マゼンタ用
  ];

  /// 画像が無い／取得できないサービス向けの、M3 Expressive準拠の
  /// グラデーションタイル。サービス名から一貫した配色を決定する。
  Widget _buildFallbackIcon(String name) {
    final initial = name.trim().isNotEmpty
        ? name.trim().characters.first.toUpperCase()
        : '?';

    // 文字列のハッシュ値を計算して一貫した配色を選択
    final int hash = name.codeUnits.fold(0, (prev, element) => prev + element);

    final int index = hash % _fallbackGradients.length;
    final selectedGradient = _fallbackGradients[index];
    final selectedTextColor = _fallbackTextColors[index];

    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: selectedGradient,
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        // 縁にトーンを重ねて立体感を与える（M3 Expressiveの container 表現）
        border: Border.all(
          color: selectedTextColor.withValues(alpha: 0.08),
          width: 1,
        ),
      ),
      child: Center(
        child: Text(
          initial,
          style: GoogleFonts.outfit(
            fontSize: 24,
            height: 1,
            fontWeight: FontWeight.w800,
            color: selectedTextColor,
            letterSpacing: -0.5,
          ),
        ),
      ),
    );
  }
}
