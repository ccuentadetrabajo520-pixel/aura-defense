package com.anonymous.mobile

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap

class AuraDefenseModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String = "AuraDefenseModule"

  @ReactMethod
  fun getInstalledPackages(promise: Promise) {
    try {
      val packageManager = reactContext.packageManager
      val apps = packageManager.getInstalledApplications(0)
      val payload: WritableArray = Arguments.createArray()

      for (app in apps) {
        val packageName = app.packageName
        if (packageName == reactContext.packageName) continue
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()

        val appMap: WritableMap = Arguments.createMap()
        appMap.putString("packageName", packageName)
        appMap.putString("name", packageManager.getApplicationLabel(app).toString())
        appMap.putString("version", packageInfo.versionName ?: "")
        appMap.putBoolean("isSystem", (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)

        val permissionsArray: WritableArray = Arguments.createArray()
        for (permission in requestedPermissions) {
          permissionsArray.pushString(permission)
        }
        appMap.putArray("permissions", permissionsArray)
        payload.pushMap(appMap)
      }

      promise.resolve(payload)
    } catch (error: Exception) {
      promise.reject("GET_PACKAGES_ERROR", error)
    }
  }

  @ReactMethod
  fun getPermissionState(permissionName: String, promise: Promise) {
    val result = when (permissionName) {
      "PACKAGE_USAGE_STATS" -> hasUsageStatsPermission()
      "REQUEST_INSTALL_PACKAGES" -> canRequestInstallPackages()
      "FOREGROUND_SERVICE" -> true
      else -> false
    }
    promise.resolve(result)
  }

  @ReactMethod
  fun openPermissionSettings(permissionName: String, promise: Promise) {
    val intent = when (permissionName) {
      "PACKAGE_USAGE_STATS" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
      "REQUEST_INSTALL_PACKAGES" -> Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${reactContext.packageName}"))
      else -> null
    }

    if (intent != null) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      reactContext.startActivity(intent)
      promise.resolve(true)
    } else {
      promise.resolve(false)
    }
  }

  @ReactMethod
  fun uninstallPackage(packageName: String, promise: Promise) {
    try {
      val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:$packageName"))
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      reactContext.startActivity(intent)
      promise.resolve(true)
    } catch (error: Exception) {
      promise.reject("UNINSTALL_ERROR", error)
    }
  }

  @ReactMethod
  fun killPackage(packageName: String, promise: Promise) {
    try {
      val activityManager = reactContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      activityManager.killBackgroundProcesses(packageName)
      promise.resolve(true)
    } catch (error: Exception) {
      promise.reject("KILL_ERROR", error)
    }
  }

  @ReactMethod
  fun getLocalNetworkInfo(promise: Promise) {
    try {
      val wifiManager = reactContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
      val connectionInfo = wifiManager.connectionInfo
      val map: WritableMap = Arguments.createMap()
      map.putString("ssid", connectionInfo.ssid ?: "unknown")
      map.putString("ipAddress", formatIp(connectionInfo.ipAddress))
      map.putString("macAddress", connectionInfo.macAddress ?: "unknown")
      promise.resolve(map)
    } catch (error: Exception) {
      promise.reject("NETWORK_ERROR", error)
    }
  }

  private fun hasUsageStatsPermission(): Boolean {
    val appOps = reactContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      appOps.unsafeCheckOpNoThrow(
        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        reactContext.packageName,
      )
    } else {
      appOps.checkOpNoThrow(
        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        reactContext.packageName,
      )
    }
    return mode == android.app.AppOpsManager.MODE_ALLOWED
  }

  private fun canRequestInstallPackages(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      reactContext.packageManager.canRequestPackageInstalls()
    } else {
      true
    }
  }

  private fun formatIp(ip: Int): String {
    return String.format(
      "%d.%d.%d.%d",
      ip and 0xff,
      (ip shr 8) and 0xff,
      (ip shr 16) and 0xff,
      (ip shr 24) and 0xff,
    )
  }
}
