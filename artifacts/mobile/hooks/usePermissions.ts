import { NativeModules, PermissionsAndroid, Platform } from 'react-native';

export type SpecialPermissionName = 'PACKAGE_USAGE_STATS' | 'REQUEST_INSTALL_PACKAGES' | 'FOREGROUND_SERVICE';

const AuraNativeModule = NativeModules.AuraNativeModule as {
  getPermissionState?: (permissionName: string) => Promise<boolean>;
  openPermissionSettings?: (permissionName: string) => Promise<boolean>;
  prepareVpnService?: () => Promise<boolean>;
  openUsageAccessSettings?: () => Promise<boolean>;
};

export async function requestLocationPermission() {
  if (Platform.OS !== 'android') return true;

  const granted = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
  );

  return granted === PermissionsAndroid.RESULTS.GRANTED;
}

export async function requestUsageAccessPermission() {
  if (Platform.OS !== 'android') return true;
  const current = await AuraNativeModule.getPermissionState?.('PACKAGE_USAGE_STATS');
  if (current) return true;
  if (AuraNativeModule.openUsageAccessSettings) {
    await AuraNativeModule.openUsageAccessSettings();
  }
  return false;
}

export async function requestVpnPermission() {
  if (Platform.OS !== 'android') return true;
  if (AuraNativeModule.prepareVpnService) {
    return AuraNativeModule.prepareVpnService();
  }
  return false;
}

export async function requestSpecialPermission(permissionName: SpecialPermissionName) {
  if (Platform.OS !== 'android') return true;
  if (permissionName === 'FOREGROUND_SERVICE') return true;

  const current = await AuraNativeModule.getPermissionState?.(permissionName);
  if (current) return true;

  await AuraNativeModule.openPermissionSettings?.(permissionName);
  return false;
}
