package com.anonymous.mobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppScannerRepository(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    private val _apps = MutableStateFlow<List<AppAudit>>(emptyList())
    val apps: StateFlow<List<AppAudit>> = _apps

    fun scanInstalledApps(): List<AppAudit> {
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }

        val audits = packages.mapNotNull { pkg ->
            val requestedPermissions = pkg.requestedPermissions.orEmpty()
            val riskScore = evaluateRiskScore(requestedPermissions)
            if (riskScore <= 0) return@mapNotNull null
            AppAudit(
                packageName = pkg.packageName,
                appName = pkg.applicationInfo?.loadLabel(packageManager).toString(),
                versionName = pkg.versionName.orEmpty(),
                riskScore = riskScore,
                sensitivePermissions = requestedPermissions.filter { it in SENSITIVE_PERMISSIONS }
            )
        }.sortedByDescending { it.riskScore }

        _apps.value = audits
        return audits
    }

    fun uninstallPackage(packageName: String, activity: Activity?) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        if (activity != null) {
            activity.startActivity(intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun evaluateRiskScore(permissions: List<String>): Int {
        var score = 0
        permissions.forEach { permission ->
            score += when (permission) {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION -> 18
                android.Manifest.permission.RECORD_AUDIO -> 16
                android.Manifest.permission.CAMERA -> 14
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.READ_SMS -> 20
                android.Manifest.permission.SYSTEM_ALERT_WINDOW -> 15
                android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE -> 25
                android.Manifest.permission.QUERY_ALL_PACKAGES -> 10
                else -> 0
            }
        }
        return score.coerceAtMost(100)
    }

    companion object {
        private val SENSITIVE_PERMISSIONS = setOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.SYSTEM_ALERT_WINDOW,
            android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
            android.Manifest.permission.QUERY_ALL_PACKAGES
        )
    }
}

data class AppAudit(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val riskScore: Int,
    val sensitivePermissions: List<String>
)