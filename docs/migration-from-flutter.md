# Flutter からの移行記録

SubKan は v1.0.2 まで Flutter（Dart + Hive + Riverpod）で書かれていました。v2.0.0 で
Kotlin + Jetpack Compose + Room に全面的に書き換えています。

## なぜ書き換えたか

Material Design 3 を本格的に導入するためです。Flutter の Material 3 サポートは「M3 風に見える
ウィジェット」であり、ダイナミックカラー・トーナルパレット・Android のシステム UI との統合は
どうしても後追いになります。Compose の `androidx.compose.material3` は Android の M3 実装そのもの
なので、配色ロール・タイポグラフィスケール・コンポーネントの仕様が最初から一致します。

副次的な効果として、`compileSdk 37` / エッジツーエッジ / 予測型「戻る」といった Android 側の
新機能に、プラグインの対応を待たずに追随できるようになりました。

## 仕様は変えていない

機能は 1 対 1 で移植しています。以下はすべて従来どおり動きます。

- サブスクの登録・編集・削除（サービス名 / 金額 / 通貨 / 支払い間隔 / 次回支払日 / 支払いカード）
- 「すべて」＋カードごとのタブと、スワイプによる切り替え
- 表示中のタブに追従する月額合計（複数通貨は並記）
- 年額プランを 12 で割って月額換算する集計
- タブバーの上下配置切り替え
- カードタブ長押しからの並び替え / 名前・色の編集 / 削除（紐づくサブスクごと削除、取り消し可能）
- サブスク編集画面からの「新しいカードを追加…」
- 並び替え（登録日順 / 名称順 / 支払日順、昇順・降順）とその設定画面からの変更
- 支払いカードの管理画面（追加・編集・削除・並べ替え、削除は取り消し可能）
- サービス名からのロゴ自動取得と、取得できない場合の頭文字グラデーションタイル
- 支払日までの残日数バッジ（済 / 今日 / あと N 日）
- テーマ設定（システム / ライト / ダーク）
- このアプリについて / 免責事項・プライバシー / GitHub リンク
- 初期カード 7 枚の投入

## 対応表

| Flutter | Kotlin |
| --- | --- |
| `lib/models/subscription.dart` | `core/model/Subscription.kt` + `data/local/entity/SubscriptionEntity.kt` |
| `lib/models/payment_card.dart` | `core/model/PaymentCard.kt` + `data/local/entity/PaymentCardEntity.kt` |
| `lib/repositories/*_repo.dart`（Hive） | `data/repository/Offline*Repository.kt`（Room） |
| `lib/repositories/settings_repo.dart` | `data/preferences/SettingsRepository.kt`（DataStore） |
| `lib/providers/*.dart`（Riverpod） | 各 `ui/*/…ViewModel.kt`（Hilt + StateFlow） |
| `lib/views/list_screen.dart` | `ui/home/HomeScreen.kt` + `HomeViewModel.kt` |
| `lib/views/add_screen.dart` | `ui/editor/SubscriptionEditorSheet.kt` |
| `lib/views/settings_screen.dart` | `ui/settings/SettingsScreen.kt` |
| `lib/views/card_management_screen.dart` | `ui/cards/CardManagementScreen.kt` |
| `lib/widgets/subscription_card.dart` | `ui/components/SubscriptionRow.kt` + `ServiceIcon.kt` |
| `lib/widgets/card_edit_dialog.dart` | `ui/editor/CardEditorDialog.kt` |
| `lib/utils/app_theme.dart` | `ui/theme/`（Theme / Color / Type / Shape / Accents） |
| `lib/utils/icon_fetcher.dart` | `data/icon/ServiceIconUrl.kt` |
| `lib/utils/card_brand_colors.dart` | `ui/theme/Accents.kt` の `cardColor()` |
| `TabController` + `TabBarView` | `PrimaryScrollableTabRow` + `HorizontalPager` |
| `cached_network_image` | Coil 3 |
| `intl` の `NumberFormat` | `java.text.NumberFormat`（`core/model/Money.kt`） |

## 意図的に変えたところ

### データは引き継がれません

Hive のボックスから Room への自動移行は実装していません。ストレージ形式もアプリ ID も変わるため、
v2.0.0 は新規インストール扱いになります。旧バージョンからのアップグレードではなく、別アプリとして
入る点に注意してください。

### アプリ ID

`com.example.subscription_manager` → `com.subkan`。旧 ID は Flutter のテンプレートが生成した
プレースホルダで、`com.example.*` は Google Play に公開できません。

### フォント

Flutter 版は `google_fonts` で Inter と Outfit をダウンロードしていました。Kotlin 版は端末の
システムフォントを使います。日本語・英数字・絵文字を 1 つのフォントで正しく描き分けられ、
アクセシビリティのフォントスケールにも最初から対応しているためです。

金額の見た目は書体ではなくウェイトと字間で作っています（`ui/theme/Type.kt` の `AmountLarge` /
`AmountMedium`）。

### カードの並べ替え操作

Flutter 版の `ReorderableListView` に相当する機能は、ドラッグハンドルに加えて **上/下ボタン**でも
操作できるようにしました。ドラッグは TalkBack から到達できないためです。機能としては同じもので、
劣った代替ではありません。

### カード管理画面のスワイプ削除

スワイプによる削除は、明示的な削除ボタン（＋確認ダイアログ、取り消し可能）に置き換えました。
発見しやすく、スクリーンリーダーからも操作できます。削除できること自体は変わりません。

### ダイナミックカラー（新機能）

Android 12 以降で壁紙由来の配色を使う設定を追加しました。M3 導入の中心的な機能であるため入れて
いますが、**既定はオフ**です。オフのままなら従来と同じ紫のテーマで動きます。

### ドキュメント

`README.md` は日本語のみです（taskan と異なり英語版は作っていません）。
