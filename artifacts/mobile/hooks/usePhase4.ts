import { Alert, Linking, Platform } from 'react-native';
import { NativeModules } from 'react-native';
import * as Location from 'expo-location';

const AuraNativeModule = NativeModules.AuraNativeModule as {
  getNetworkThreatProfile?: () => Promise<{
    ssid?: string;
    encryption?: string;
    arpSpoofing?: boolean;
    wpsEnabled?: boolean;
    openCameraEndpoints?: string[];
  }>;
  checkPwnedAccount?: (email: string) => Promise<string[]>;
  isAdultDomainBlocked?: (domain: string) => Promise<boolean>;
};

export async function checkAdultDomain(domain: string) {
  if (Platform.OS !== 'android') return false;
  const blocked = await AuraNativeModule.isAdultDomainBlocked?.(domain);
  return Boolean(blocked);
}

export async function runPwnedCheck(email: string) {
  if (Platform.OS !== 'android') return [] as string[];
  const result = await AuraNativeModule.checkPwnedAccount?.(email);
  return Array.isArray(result) ? result : [];
}

export async function runHiddenCameraScan() {
  if (Platform.OS !== 'android') return { endpoints: [] as string[], risk: 'unknown' as const };
  const profile = await AuraNativeModule.getNetworkThreatProfile?.();
  const endpoints = profile?.openCameraEndpoints ?? [];
  return {
    endpoints,
    risk: endpoints.length > 0 ? 'high' : 'low',
  };
}

export async function runGhostLocationProtection() {
  if (Platform.OS !== 'android') {
    return { enabled: false, reason: 'Android-only feature' };
  }

  const { status } = await Location.requestForegroundPermissionsAsync();
  if (status !== 'granted') {
    return { enabled: false, reason: 'location permission required' };
  }

  const position = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Lowest });
  return {
    enabled: true,
    latitude: position.coords.latitude,
    longitude: position.coords.longitude,
    reason: 'ghost-protection ready',
  };
}

export async function triggerEmergencySos(contactPhone: string, customMessage?: string) {
  if (Platform.OS !== 'android') return false;
  const message = encodeURIComponent(customMessage ?? 'Emergency alert from Aura Defense');
  const url = `sms:${contactPhone}?body=${message}`;
  const canOpen = await Linking.canOpenURL(url);
  if (!canOpen) {
    Alert.alert('Emergency SOS unavailable', 'The device cannot open SMS for SOS.');
    return false;
  }
  await Linking.openURL(url);
  return true;
}

export async function openRemoteDashboard(baseUrl: string) {
  if (Platform.OS !== 'android') return false;
  const target = baseUrl.startsWith('http') ? baseUrl : `http://${baseUrl}`;
  const canOpen = await Linking.canOpenURL(target);
  if (!canOpen) {
    Alert.alert('Dashboard unavailable', 'The remote dashboard URL is not accessible on this device.');
    return false;
  }
  await Linking.openURL(target);
  return true;
}
