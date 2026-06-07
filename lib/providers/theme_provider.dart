import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../repositories/settings_repo.dart';

final settingsRepositoryProvider = Provider<SettingsRepository>((ref) {
  return SettingsRepository();
});

final themeModeProvider = StateNotifierProvider<ThemeModeNotifier, ThemeMode>((ref) {
  final repo = ref.watch(settingsRepositoryProvider);
  return ThemeModeNotifier(repo);
});

class ThemeModeNotifier extends StateNotifier<ThemeMode> {
  final SettingsRepository _repo;

  ThemeModeNotifier(this._repo) : super(ThemeMode.system) {
    _loadThemeMode();
  }

  void _loadThemeMode() {
    final modeStr = _repo.getThemeMode();
    state = _stringToThemeMode(modeStr);
  }

  Future<void> setThemeMode(ThemeMode mode) async {
    state = mode;
    await _repo.setThemeMode(_themeModeToString(mode));
  }

  ThemeMode _stringToThemeMode(String mode) {
    switch (mode) {
      case 'light':
        return ThemeMode.light;
      case 'dark':
        return ThemeMode.dark;
      case 'system':
      default:
        return ThemeMode.system;
    }
  }

  String _themeModeToString(ThemeMode mode) {
    switch (mode) {
      case ThemeMode.light:
        return 'light';
      case ThemeMode.dark:
        return 'dark';
      case ThemeMode.system:
        return 'system';
    }
  }
}

final tabBarPositionProvider = StateNotifierProvider<TabBarPositionNotifier, String>((ref) {
  final repo = ref.watch(settingsRepositoryProvider);
  return TabBarPositionNotifier(repo);
});

class TabBarPositionNotifier extends StateNotifier<String> {
  final SettingsRepository _repo;

  TabBarPositionNotifier(this._repo) : super('top') {
    _loadPosition();
  }

  void _loadPosition() {
    state = _repo.getTabBarPosition();
  }

  Future<void> setPosition(String position) async {
    state = position;
    await _repo.setTabBarPosition(position);
  }
}

final subscriptionSortOrderProvider = StateNotifierProvider<SubscriptionSortOrderNotifier, String>((ref) {
  final repo = ref.watch(settingsRepositoryProvider);
  return SubscriptionSortOrderNotifier(repo);
});

class SubscriptionSortOrderNotifier extends StateNotifier<String> {
  final SettingsRepository _repo;

  SubscriptionSortOrderNotifier(this._repo) : super('default') {
    _loadSortOrder();
  }

  void _loadSortOrder() {
    state = _repo.getSubscriptionSortOrder();
  }

  Future<void> setSortOrder(String order) async {
    state = order;
    await _repo.setSubscriptionSortOrder(order);
  }
}

final subscriptionSortAscendingProvider = StateNotifierProvider<SubscriptionSortAscendingNotifier, bool>((ref) {
  final repo = ref.watch(settingsRepositoryProvider);
  return SubscriptionSortAscendingNotifier(repo);
});

class SubscriptionSortAscendingNotifier extends StateNotifier<bool> {
  final SettingsRepository _repo;

  SubscriptionSortAscendingNotifier(this._repo) : super(true) {
    _loadAscending();
  }

  void _loadAscending() {
    state = _repo.getSubscriptionSortAscending();
  }

  Future<void> setAscending(bool ascending) async {
    state = ascending;
    await _repo.setSubscriptionSortAscending(ascending);
  }
}
