import { NativeModules, PermissionsAndroid, Platform } from 'react-native';

export type SpecialPermissionName = 'PACKAGE_USAGE_STATS' | 'REQUEST_INSTALL_PACKAGES' | 'FOREGROUND_SERVICE';

const AuraDefenseModule = NativeModules.AuraDefenseModule as {
  getPermissionState?: (permissionName: string) => Promise<boolean>;
  openPermissionSettings?: (permissionName: string) => Promise<boolean>;
};

export async function requestLocationPermission() {
  if (Platform.OS !== 'android') return true;

  const granted = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
  );

  return granted === PermissionsAndroid.RESULTS.GRANTED;
}

export async function requestSpecialPermission(permissionName: SpecialPermissionName) {
  if (Platform.OS !== 'android') return true;
  if (permissionName === 'FOREGROUND_SERVICE') return true;

  const current = await AuraDefenseModule.getPermissionState?.(permissionName);
  if (current) return true;

  await AuraDefenseModule.openPermissionSettings?.(permissionName);
  return false;
}
