package com.example.auradefensa;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

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

  public AuraNativeModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @ReactMethod
  public void getCurrentWifiInfo(final Promise promise) {
    try {
      final WifiManager wifiManager = (WifiManager) getReactApplicationContext()
        .getApplicationContext()
        .getSystemService(Context.WIFI_SERVICE);

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
  public void detectArpSpoofing(final Promise promise) {
    try {
      final List<String> lines = readArpTable();
      final List<String> duplicateMacs = findDuplicateMacs(lines);
      final boolean suspicious = !duplicateMacs.isEmpty() || hasIncompleteOrStaleEntries(lines);

      final WritableMap response = Arguments.createMap();
      response.putBoolean("suspicious", suspicious);
      response.putString("status", suspicious ? "ALERT" : "CLEAR");
      response.putString("message", suspicious
        ? (duplicateMacs.isEmpty() ? "ARP table contains stale or incomplete entries." : "Duplicate MAC addresses detected on the local network.")
        : "ARP table looks consistent.");
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
    return String.format(
      Locale.US,
      "%d.%d.%d.%d",
      (ipAddress & 0xff),
      (ipAddress >> 8 & 0xff),
      (ipAddress >> 16 & 0xff),
      (ipAddress >> 24 & 0xff)
    );
  }
}
