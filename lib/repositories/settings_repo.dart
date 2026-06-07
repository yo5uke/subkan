import 'package:hive_flutter/hive_flutter.dart';

class SettingsRepository {
  static const String boxName = 'settingsBox_v1';
  static const String themeModeKey = 'themeMode';
  static const String tabBarPositionKey = 'tabBarPosition';
  static const String sortOrderKey = 'subscriptionSortOrder';
  static const String sortAscendingKey = 'subscriptionSortAscending';

  Future<void> init() async {
    await Hive.openBox(boxName);
  }

  Box get _box => Hive.box(boxName);

  String getThemeMode() {
    return _box.get(themeModeKey, defaultValue: 'system') as String;
  }

  Future<void> setThemeMode(String mode) async {
    await _box.put(themeModeKey, mode);
  }

  String getTabBarPosition() {
    return _box.get(tabBarPositionKey, defaultValue: 'top') as String;
  }

  Future<void> setTabBarPosition(String position) async {
    await _box.put(tabBarPositionKey, position);
  }

  String getSubscriptionSortOrder() {
    return _box.get(sortOrderKey, defaultValue: 'default') as String;
  }

  Future<void> setSubscriptionSortOrder(String order) async {
    await _box.put(sortOrderKey, order);
  }

  bool getSubscriptionSortAscending() {
    return _box.get(sortAscendingKey, defaultValue: true) as bool;
  }

  Future<void> setSubscriptionSortAscending(bool ascending) async {
    await _box.put(sortAscendingKey, ascending);
  }
}
