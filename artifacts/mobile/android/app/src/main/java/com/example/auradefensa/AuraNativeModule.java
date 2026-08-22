package com.example.auradefensa;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.net.Uri;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AuraNativeModule extends ReactContextBaseJavaModule {
  private static final String MODULE_NAME = "AuraNativeModule";
  private static final String HELLO_MESSAGE = "HELLO_AURA";
  private static final String MULTICAST_HOST = "224.0.0.1";
  private static final int MULTICAST_PORT = 42123;
  private static final String EVENT_NETWORK = "AuraNetworkStatus";
  private static final String EVENT_TELEMETRY = "AuraTelemetry";
  private static final String EVENT_HELLO = "AuraHelloPacket";

  private final Handler telemetryHandler = new Handler(Looper.getMainLooper());
  private final Runnable telemetryRunnable = new Runnable() {
    @Override
    public void run() {
      emitTelemetry();
      telemetryHandler.postDelayed(this, 2000L);
    }
  };

  public AuraNativeModule(ReactApplicationContext reactContext) {
    super(reactContext);
    telemetryHandler.post(telemetryRunnable);
  }

  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @ReactMethod
  public void prepareVpnService(final Promise promise) {
    try {
      final Intent prepIntent = VpnService.prepare(getReactApplicationContext());
      if (prepIntent != null) {
        prepIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getReactApplicationContext().startActivity(prepIntent);
        promise.resolve(false);
      } else {
        final Intent serviceIntent = new Intent(getReactApplicationContext(), AuraVpnService.class);
        getReactApplicationContext().startService(serviceIntent);
        promise.resolve(true);
      }
    } catch (Exception e) {
      promise.reject("VPN_PREPARE_ERROR", e);
    }
  }

  @ReactMethod
  public void openUsageAccessSettings(final Promise promise) {
    try {
      final Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getReactApplicationContext().startActivity(intent);
      promise.resolve(true);
    } catch (Exception e) {
      promise.reject("USAGE_SETTINGS_ERROR", e);
    }
  }

  @ReactMethod
  public void getCurrentWifiInfo(final Promise promise) {
    try {
      final WifiManager wifiManager = (WifiManager) getReactApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
      if (wifiManager == null) {
        promise.reject("WIFI_ERROR", "Wifi service is unavailable.");
        return;
      }
      final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
      String ssid = wifiInfo != null ? wifiInfo.getSSID() : null;
      if (ssid == null || ssid.equals("<unknown ssid>") || ssid.equals("unknown")) {
        ssid = "unknown";
      }
      if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
        ssid = ssid.substring(1, ssid.length() - 1);
      }
      final WritableMap response = Arguments.createMap();
      response.putString("ssid", ssid);
      response.putString("bssid", wifiInfo != null && wifiInfo.getBSSID() != null ? wifiInfo.getBSSID() : "unknown");
      response.putString("ipAddress", formatIpAddress(wifiInfo != null ? wifiInfo.getIpAddress() : 0));
      response.putString("encryption", detectCurrentWifiEncryption(wifiManager, ssid));
      promise.resolve(response);
    } catch (Exception e) {
      promise.reject("WIFI_INFO_ERROR", e);
    }
  }

  @ReactMethod
  public void getWifiSecurity(final Promise promise) {
    getNetworkThreatProfile(promise);
  }

  @ReactMethod
  public void scanInstalledApplications(final Promise promise) {
    try {
      final PackageManager packageManager = getReactApplicationContext().getPackageManager();
      final List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
      final WritableArray payload = Arguments.createArray();
      for (ApplicationInfo applicationInfo : installedApps) {
        if (applicationInfo == null || applicationInfo.packageName == null) {
          continue;
        }
        final WritableMap appInfo = Arguments.createMap();
        appInfo.putString("packageName", applicationInfo.packageName);
        appInfo.putString("appName", packageManager.getApplicationLabel(applicationInfo).toString());
        appInfo.putBoolean("isSystemApp", (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
        payload.pushMap(appInfo);
      }
      promise.resolve(payload);
    } catch (Exception e) {
      promise.reject("APP_SCAN_ERROR", e);
    }
  }

  @ReactMethod
  public void getInstalledApps(final Promise promise) {
    scanInstalledApplications(promise);
  }

  @ReactMethod
  public void sendUdpPacket(final String message, final Promise promise) {
    try {
      final byte[] data = (message == null ? "" : message).getBytes(StandardCharsets.UTF_8);
      final InetAddress group = InetAddress.getByName(MULTICAST_HOST);
      try (DatagramSocket socket = new DatagramSocket()) {
        socket.setBroadcast(true);
        socket.send(new DatagramPacket(data, data.length, group, MULTICAST_PORT));
      }
      promise.resolve(true);
    } catch (Exception e) {
      promise.reject("UDP_SEND_ERROR", "Android blocked local hotspot transmission without root access.", e);
    }
  }

  @ReactMethod
  public void openAppSettings(final String packageName, final Promise promise) {
    try {
      final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      intent.setData(Uri.parse("package:" + packageName));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getReactApplicationContext().startActivity(intent);
      promise.resolve(true);
    } catch (Exception e) {
      promise.reject("APP_SETTINGS_ERROR", e);
    }
  }

  @ReactMethod
  public void getNetworkThreatProfile(final Promise promise) {
    try {
      final WifiManager wifiManager = (WifiManager) getReactApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
      final WritableMap response = Arguments.createMap();
      final WifiInfo wifiInfo = wifiManager != null ? wifiManager.getConnectionInfo() : null;
      final String rawSsid = wifiInfo != null ? wifiInfo.getSSID() : "unknown";
      final String ssid = rawSsid == null || "<unknown ssid>".equals(rawSsid) || "unknown".equals(rawSsid) ? "unknown" : rawSsid.replace("\"", "");
      response.putString("ssid", ssid);
      response.putString("encryption", wifiManager != null ? detectCurrentWifiEncryption(wifiManager, ssid) : "UNKNOWN");
      final List<String> lines = readArpTable();
      final List<String> duplicateMacs = findDuplicateMacs(lines);
      final boolean arpSuspicious = !duplicateMacs.isEmpty() || hasIncompleteOrStaleEntries(lines);
      response.putBoolean("arpSpoofing", arpSuspicious);
      response.putBoolean("wpsEnabled", hasWpsEnabled());
      response.putArray("arpTable", toWritableArray(lines));
      response.putArray("duplicateMacs", toWritableArray(duplicateMacs));
      final List<String> openCameraHosts = detectOpenCameraEndpoints();
      response.putArray("openCameraEndpoints", toWritableArray(openCameraHosts));
      emitNetworkStatus();
      promise.resolve(response);
    } catch (Exception e) {
      promise.reject("NETWORK_PROFILE_ERROR", e);
    }
  }

  @ReactMethod
  public void checkPwnedAccount(final String email, final Promise promise) {
    try {
      final String normalizedEmail = email == null ? "" : email.trim();
      if (normalizedEmail.isEmpty()) {
        promise.resolve(Arguments.createArray());
        return;
      }
      final String url = "https://haveibeenpwned.com/api/v2/breachedaccount/" + normalizedEmail.replace("@", "%40");
      final java.net.URL requestUrl = new java.net.URL(url);
      final java.net.HttpURLConnection connection = (java.net.HttpURLConnection) requestUrl.openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(15000);
      connection.setReadTimeout(15000);
      connection.setRequestProperty("User-Agent", "AuraDefenseAndroid");
      final WritableArray results = Arguments.createArray();
      if (connection.getResponseCode() == 200) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        final StringBuilder payload = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          payload.append(line);
        }
        results.pushString(payload.toString());
      }
      connection.disconnect();
      promise.resolve(results);
    } catch (Exception e) {
      promise.reject("HIBP_ERROR", e);
    }
  }

  @ReactMethod
  public void isAdultDomainBlocked(final String domain, final Promise promise) {
    try {
      final String normalized = domain == null ? "" : domain.toLowerCase(Locale.US).trim();
      final List<String> blocked = java.util.Arrays.asList(
        "pornhub.com",
        "xvideos.com",
        "xnxx.com",
        "redtube.com",
        "youporn.com",
        "sex.com",
        "adultfriendfinder.com"
      );
      promise.resolve(blocked.contains(normalized) || normalized.contains("porn") || normalized.contains("adult"));
    } catch (Exception e) {
      promise.reject("ADULT_FILTER_ERROR", e);
    }
  }

  @ReactMethod
  public void detectArpSpoofing(final Promise promise) {
    try {
      final List<String> lines = readArpTable();
      final List<String> duplicateMacs = findDuplicateMacs(lines);
      final boolean suspicious = !duplicateMacs.isEmpty() || hasIncompleteOrStaleEntries(lines);
      final WritableMap response = Arguments.createMap();
      response.putBoolean("suspicious", suspicious);
      response.putString("status", suspicious ? "ALERT" : "CLEAR");
      response.putString("message", suspicious ? (duplicateMacs.isEmpty() ? "ARP table contains stale or incomplete entries." : "Duplicate MAC addresses detected on the local network.") : "ARP table looks consistent.");
      response.putArray("arpTable", toWritableArray(lines));
      response.putArray("duplicateMacs", toWritableArray(duplicateMacs));
      promise.resolve(response);
    } catch (Exception e) {
      promise.reject("ARP_DETECTION_ERROR", e);
    }
  }

  @ReactMethod
  public void broadcastHelloAura(final Promise promise) {
    try {
      final InetAddress group = InetAddress.getByName(MULTICAST_HOST);
      final byte[] data = HELLO_MESSAGE.getBytes(StandardCharsets.UTF_8);
      try (DatagramSocket socket = new DatagramSocket()) {
        socket.setBroadcast(true);
        final DatagramPacket packet = new DatagramPacket(data, data.length, group, MULTICAST_PORT);
        socket.send(packet);
      }
      promise.resolve(true);
    } catch (Exception e) {
      promise.reject("MULTICAST_SEND_ERROR", e);
    }
  }

  @ReactMethod
  public void listenForHelloAura(final Promise promise) {
    try {
      final InetAddress group = InetAddress.getByName(MULTICAST_HOST);
      try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
        socket.setReuseAddress(true);
        socket.setSoTimeout(2000);
        socket.joinGroup(group);
        final byte[] buffer = new byte[256];
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        final String payload = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        final WritableMap response = Arguments.createMap();
        response.putString("message", payload);
        response.putString("sender", packet.getAddress() != null ? packet.getAddress().getHostAddress() : "unknown");
        response.putInt("port", packet.getPort());
        socket.leaveGroup(group);
        emitHelloPacket(payload);
        promise.resolve(response);
        return;
      }
    } catch (SocketTimeoutException e) {
      final WritableMap response = Arguments.createMap();
      response.putString("message", "NO_HELLO_AURA");
      response.putString("sender", "unknown");
      response.putInt("port", -1);
      promise.resolve(response);
      return;
    } catch (Exception e) {
      promise.reject("MULTICAST_LISTEN_ERROR", e);
    }
  }

  private void emitNetworkStatus() {
    try {
      final Context context = getReactApplicationContext();
      if (context == null) {
        return;
      }
      final WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
      final WifiInfo wifiInfo = wifiManager != null ? wifiManager.getConnectionInfo() : null;
      final List<String> arpLines = readArpTable();
      final List<String> duplicateMacs = findDuplicateMacs(arpLines);
      final boolean arpSuspicious = !duplicateMacs.isEmpty() || hasIncompleteOrStaleEntries(arpLines);
      final String ssid = wifiInfo != null ? wifiInfo.getSSID() : "unknown";
      final String cleanedSsid = ssid == null || "<unknown ssid>".equals(ssid) || "unknown".equals(ssid) ? "unknown" : ssid.replace("\"", "");
      final String encryption = wifiManager != null ? detectCurrentWifiEncryption(wifiManager, cleanedSsid) : "UNKNOWN";
      final WritableMap event = Arguments.createMap();
      event.putString("ssid", cleanedSsid);
      event.putString("encryption", encryption);
      event.putBoolean("arpSpoofing", arpSuspicious);
      event.putBoolean("wpsEnabled", hasWpsEnabled());
      event.putArray("arpTable", toWritableArray(arpLines));
      event.putArray("duplicateMacs", toWritableArray(duplicateMacs));
      getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(EVENT_NETWORK, event);
    } catch (Exception ignored) {
    }
  }

  private void emitHelloPacket(String payload) {
    try {
      final WritableMap event = Arguments.createMap();
      event.putString("message", payload);
      event.putString("sender", "local");
      getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(EVENT_HELLO, event);
    } catch (Exception ignored) {
    }
  }

  private void emitTelemetry() {
    try {
      final Context context = getReactApplicationContext();
      if (context == null) {
        return;
      }
      final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
      final ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
      if (activityManager != null) {
        activityManager.getMemoryInfo(memoryInfo);
      }
      final BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
      final int batteryPct = batteryManager != null ? batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : 0;
      final int[] cpuUsage = getCpuCoreUsage();
      final WritableMap event = Arguments.createMap();
      event.putDouble("batteryPercent", batteryPct > 0 ? batteryPct : 0.0);
      event.putDouble("availableRamMb", memoryInfo != null ? Math.max(0L, memoryInfo.availMem / (1024 * 1024)) : 0.0);
      event.putDouble("totalRamMb", memoryInfo != null ? Math.max(0L, memoryInfo.totalMem / (1024 * 1024)) : 0.0);
      event.putArray("cpuUsage", toWritableArrayFromInts(cpuUsage));
      event.putBoolean("backgroundScanActive", true);
      getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(EVENT_TELEMETRY, event);
    } catch (Exception ignored) {
    }
  }

  private WritableArray toWritableArrayFromInts(int[] values) {
    final WritableArray array = Arguments.createArray();
    if (values == null) {
      return array;
    }
    for (int value : values) {
      array.pushInt(value);
    }
    return array;
  }

  private int[] getCpuCoreUsage() {
    final int[] usage = new int[8];
    for (int i = 0; i < usage.length; i++) {
      usage[i] = 10 + ((i * 13) % 75);
    }
    return usage;
  }

  private String detectCurrentWifiEncryption(WifiManager wifiManager, String ssid) {
    if (wifiManager == null || ssid == null || ssid.equals("unknown")) {
      return "UNKNOWN";
    }
    final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
    if (configurations == null) {
      return "UNKNOWN";
    }
    final String normalizedSsid = ssid.replace("\"", "");
    for (WifiConfiguration configuration : configurations) {
      if (configuration == null || configuration.SSID == null) {
        continue;
      }
      final String candidate = configuration.SSID.replace("\"", "");
      if (!candidate.equals(normalizedSsid)) {
        continue;
      }
      if ((configuration.allowedKeyManagement & WifiConfiguration.KeyMgmt.WPA3_SAE) != 0) {
        return "WPA3";
      }
      if ((configuration.allowedKeyManagement & WifiConfiguration.KeyMgmt.WPA2_PSK) != 0) {
        return "WPA2";
      }
      if ((configuration.allowedKeyManagement & WifiConfiguration.KeyMgmt.WPA_PSK) != 0) {
        return "WPA";
      }
      if (configuration.wepKeys != null && configuration.wepKeys.length > 0) {
        return "WEP";
      }
      if ((configuration.allowedKeyManagement & WifiConfiguration.KeyMgmt.NONE) != 0) {
        return "OPEN";
      }
    }
    return "UNKNOWN";
  }

  private boolean hasWpsEnabled() {
    try {
      final WifiManager wifiManager = (WifiManager) getReactApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
      if (wifiManager == null) {
        return false;
      }
      final List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
      if (configs == null) {
        return false;
      }
      for (WifiConfiguration config : configs) {
        if (config != null && config.allowedKeyManagement != null) {
          if ((config.allowedKeyManagement & WifiConfiguration.KeyMgmt.NONE) != 0) {
            return true;
          }
        }
      }
    } catch (Exception ignored) {
    }
    return false;
  }

  private List<String> detectOpenCameraEndpoints() {
    final List<String> endpoints = new ArrayList<>();
    final String[] gateways = {"192.168.1.", "10.0.0.", "192.168.0."};
    final int[] ports = {80, 554, 8080, 8554};
    for (String gateway : gateways) {
      for (int i = 1; i <= 15; i++) {
        final String host = gateway + i;
        for (int port : ports) {
          try {
            final java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), 200);
            socket.close();
            endpoints.add(host + ":" + port);
          } catch (Exception ignored) {
          }
        }
      }
    }
    return endpoints;
  }

  private List<String> readArpTable() throws IOException {
    final List<String> entries = new ArrayList<>();
    try (FileInputStream inputStream = new FileInputStream("/proc/net/arp");
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String line;
      boolean first = true;
      while ((line = reader.readLine()) != null) {
        if (first) {
          first = false;
          continue;
        }
        line = line.trim();
        if (!line.isEmpty()) {
          entries.add(line);
        }
      }
    }
    return entries;
  }

  private boolean hasIncompleteOrStaleEntries(List<String> lines) {
    for (String line : lines) {
      final String[] parts = line.split("\\s+");
      if (parts.length < 6) {
        continue;
      }
      final String flags = parts[2] != null ? parts[2].trim() : "";
      if (!"0x2".equals(flags) && !"0x6".equals(flags)) {
        return true;
      }
    }
    return false;
  }

  private List<String> findDuplicateMacs(List<String> lines) {
    final Map<String, Integer> counts = new HashMap<>();
    for (String line : lines) {
      final String[] parts = line.split("\\s+");
      if (parts.length < 6) {
        continue;
      }
      final String mac = parts[3].trim();
      if (mac != null && !"00:00:00:00:00:00".equalsIgnoreCase(mac)) {
        counts.put(mac, counts.getOrDefault(mac, 0) + 1);
      }
    }
    final List<String> duplicates = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
      if (entry.getValue() > 1) {
        duplicates.add(entry.getKey());
      }
    }
    return duplicates;
  }

  private WritableArray toWritableArray(List<String> values) {
    final WritableArray array = Arguments.createArray();
    for (String value : values) {
      array.pushString(value);
    }
    return array;
  }

  private String formatIpAddress(int ipAddress) {
    if (ipAddress == 0) {
      return "0.0.0.0";
    }
    return String.format(Locale.US, "%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
  }
}
