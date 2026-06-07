import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:url_launcher/url_launcher.dart';
import '../providers/theme_provider.dart';
import 'card_management_screen.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  // ソースコード／フィードバックの公開先。リポジトリ名に合わせて変更してください。
  static const String _githubUrl = 'https://github.com/yo5uke/subkan';

  // 設定項目をカテゴリ分けするためのセクション見出し。
  Widget _sectionHeader(BuildContext context, String title) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
      child: Text(
        title,
        style: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.bold,
          letterSpacing: 0.5,
          color: Theme.of(context).colorScheme.primary,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode = ref.watch(themeModeProvider);
    final tabBarPosition = ref.watch(tabBarPositionProvider);
    final sortOrder = ref.watch(subscriptionSortOrderProvider);
    final ascending = ref.watch(subscriptionSortAscendingProvider);

    String getThemeModeLabel(ThemeMode mode) {
      switch (mode) {
        case ThemeMode.light:
          return 'ライトテーマ';
        case ThemeMode.dark:
          return 'ダークテーマ';
        case ThemeMode.system:
          return 'システム設定に合わせる';
      }
    }

    String getSortOrderLabel(String order) {
      switch (order) {
        case 'name':
          return '名称順';
        case 'payment_date':
          return '支払日順';
        case 'default':
        default:
          return '登録日順';
      }
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('設定'),
      ),
      body: ListView(
        children: [
          _sectionHeader(context, '表示'),
          ListTile(
            leading: const Icon(Icons.palette_outlined),
            title: const Text('テーマ設定'),
            subtitle: Text(getThemeModeLabel(themeMode)),
            onTap: () {
              showDialog(
                context: context,
                builder: (context) => AlertDialog(
                  title: const Text('テーマを選択'),
                  content: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      ListTile(
                        title: const Text('システム設定に合わせる'),
                        trailing: themeMode == ThemeMode.system
                            ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                            : null,
                        onTap: () {
                          ref.read(themeModeProvider.notifier).setThemeMode(ThemeMode.system);
                          Navigator.of(context).pop();
                        },
                      ),
                      ListTile(
                        title: const Text('ライトテーマ'),
                        trailing: themeMode == ThemeMode.light
                            ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                            : null,
                        onTap: () {
                          ref.read(themeModeProvider.notifier).setThemeMode(ThemeMode.light);
                          Navigator.of(context).pop();
                        },
                      ),
                      ListTile(
                        title: const Text('ダークテーマ'),
                        trailing: themeMode == ThemeMode.dark
                            ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                            : null,
                        onTap: () {
                          ref.read(themeModeProvider.notifier).setThemeMode(ThemeMode.dark);
                          Navigator.of(context).pop();
                        },
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
          ListTile(
            leading: const Icon(Icons.splitscreen_outlined),
            title: const Text('タブバーの配置'),
            subtitle: Text(tabBarPosition == 'top' ? '画面上部に表示' : '画面下部に表示'),
            onTap: () {
              showDialog(
                context: context,
                builder: (context) => AlertDialog(
                  title: const Text('タブバーの配置'),
                  content: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      ListTile(
                        title: const Text('画面上部に表示'),
                        trailing: tabBarPosition == 'top'
                            ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                            : null,
                        onTap: () {
                          ref.read(tabBarPositionProvider.notifier).setPosition('top');
                          Navigator.of(context).pop();
                        },
                      ),
                      ListTile(
                        title: const Text('画面下部に表示'),
                        trailing: tabBarPosition == 'bottom'
                            ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                            : null,
                        onTap: () {
                          ref.read(tabBarPositionProvider.notifier).setPosition('bottom');
                          Navigator.of(context).pop();
                        },
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
          _sectionHeader(context, '管理'),
          ListTile(
            leading: const Icon(Icons.credit_card_outlined),
            title: const Text('支払いカードの管理'),
            subtitle: const Text('カードの追加、編集、削除、並べ替え'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (context) => const CardManagementScreen()),
              );
            },
          ),
          _sectionHeader(context, '並び替え'),
          ListTile(
            leading: const Icon(Icons.sort_outlined),
            title: const Text('サブスクの並び替え'),
            subtitle: Text(getSortOrderLabel(sortOrder)),
            onTap: () {
              showDialog(
                context: context,
                builder: (context) => AlertDialog(
                  title: const Text('並び替え順を選択'),
                  content: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      ListTile(
                        title: const Text('登録日順'),
                        trailing: sortOrder == 'default'
                            ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                            : null,
                        onTap: () {
                          ref.read(subscriptionSortOrderProvider.notifier).setSortOrder('default');
                          Navigator.of(context).pop();
                        },
                      ),
                      ListTile(
                        title: const Text('名称順'),
                        trailing: sortOrder == 'name'
                            ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                            : null,
                        onTap: () {
                          ref.read(subscriptionSortOrderProvider.notifier).setSortOrder('name');
                          Navigator.of(context).pop();
                        },
                      ),
                      ListTile(
                        title: const Text('支払日順'),
                        trailing: sortOrder == 'payment_date'
                            ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                            : null,
                        onTap: () {
                          ref.read(subscriptionSortOrderProvider.notifier).setSortOrder('payment_date');
                          Navigator.of(context).pop();
                        },
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
          ListTile(
            leading: const Icon(Icons.swap_vert_outlined),
            title: const Text('サブスクの並べ替え順序'),
            subtitle: Text(ascending ? '昇順' : '降順'),
            onTap: () {
              showDialog(
                context: context,
                builder: (context) => AlertDialog(
                  title: const Text('順序を選択'),
                  content: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      ListTile(
                        title: const Text('昇順'),
                        trailing: ascending
                            ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                            : null,
                        onTap: () {
                          ref.read(subscriptionSortAscendingProvider.notifier).setAscending(true);
                          Navigator.of(context).pop();
                        },
                      ),
                      ListTile(
                        title: const Text('降順'),
                        trailing: !ascending
                            ? Icon(Icons.check, color: Theme.of(context).colorScheme.primary)
                            : null,
                        onTap: () {
                          ref.read(subscriptionSortAscendingProvider.notifier).setAscending(false);
                          Navigator.of(context).pop();
                        },
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
          _sectionHeader(context, 'アプリについて'),
          ListTile(
            leading: const Icon(Icons.info_outline),
            title: const Text('このアプリについて'),
            subtitle: const Text('バージョン情報・オープンソースライセンス'),
            onTap: () => _showAboutDialog(context),
          ),
          ListTile(
            leading: const Icon(Icons.privacy_tip_outlined),
            title: const Text('免責事項・プライバシー'),
            subtitle: const Text('データの取り扱いと注意事項'),
            onTap: () => _showDisclaimerDialog(context),
          ),
          ListTile(
            leading: const Icon(Icons.code),
            title: const Text('ソースコード・フィードバック'),
            subtitle: const Text('GitHub で開く'),
            trailing: const Icon(Icons.open_in_new),
            onTap: () => _openGithub(context),
          ),
        ],
      ),
    );
  }

  Future<void> _showAboutDialog(BuildContext context) async {
    // PackageInfo の取得はプラットフォームによっては失敗・遅延するため、
    // 失敗してもダイアログ自体は必ず表示できるようフォールバックする。
    String version;
    try {
      final info = await PackageInfo.fromPlatform()
          .timeout(const Duration(seconds: 2));
      version = info.version;
    } catch (_) {
      version = '1.0.0';
    }
    if (!context.mounted) return;
    showAboutDialog(
      context: context,
      applicationName: 'SubKan',
      applicationVersion: 'バージョン $version',
      applicationIcon: Icon(
        Icons.bookmarks_outlined,
        size: 40,
        color: Theme.of(context).colorScheme.primary,
      ),
      applicationLegalese: '© 2026 Yosuke Abe',
      children: const [
        SizedBox(height: 16),
        Text('サブスクの料金・支払日・支払いカードをまとめて管理するアプリです。'),
      ],
    );
  }

  void _showDisclaimerDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('免責事項・プライバシー'),
        content: const SingleChildScrollView(
          child: Text(
            'SubKan は個人が開発した趣味のアプリです。本アプリの利用によって生じた'
            'いかなる損害についても、開発者は責任を負いません。自己責任でご利用ください。\n\n'
            '【データの取り扱い】\n'
            '・登録したサブスクやカードの情報は、お使いの端末内にのみ保存されます。'
            '外部のサーバーへ送信・収集されることはありません。\n\n'
            '・ただし、サービスのアイコンを自動取得する際に、入力したサービス名'
            '（ドメイン）が画像取得サービス（Google および wsrv.nl）へ送信されます。\n\n'
            '・アプリをアンインストールすると、保存したデータはすべて削除されます。',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('閉じる'),
          ),
        ],
      ),
    );
  }

  Future<void> _openGithub(BuildContext context) async {
    final messenger = ScaffoldMessenger.of(context);
    final opened = await launchUrl(
      Uri.parse(_githubUrl),
      mode: LaunchMode.externalApplication,
    );
    if (!opened) {
      messenger.showSnackBar(
        const SnackBar(content: Text('リンクを開けませんでした')),
      );
    }
  }
}
