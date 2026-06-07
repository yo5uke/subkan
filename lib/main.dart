import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'providers/theme_provider.dart';
import 'utils/app_theme.dart';
import 'views/list_screen.dart';
import 'repositories/subscription_repo.dart';
import 'repositories/payment_card_repo.dart';
import 'repositories/settings_repo.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Hiveの初期化
  await Hive.initFlutter();

  // 設定用リポジトリの初期化
  final settingsRepo = SettingsRepository();
  await settingsRepo.init();

  // Intlの初期化 (日本時間用)
  await initializeDateFormatting('ja_JP', null);

  final cardRepo = PaymentCardRepository();
  await cardRepo.init();

  final subRepo = SubscriptionRepository();
  await subRepo.init();

  runApp(
    const ProviderScope(
      child: MyApp(),
    ),
  );
}

class MyApp extends ConsumerWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final themeMode = ref.watch(themeModeProvider);

    return MaterialApp(
      title: 'SubKan',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: themeMode,
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [
        Locale('ja', 'JP'),
      ],
      home: const ListScreen(),
    );
  }
}
