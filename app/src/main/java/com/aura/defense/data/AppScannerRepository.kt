package com.aura.defense.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.aura.defense.BuildConfig

class AppScannerRepository(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    fun scanVisibleApps(): List<AppInfo> {
        val applications = packageManager.getInstalledApplications(applicationQueryFlags())
        return applications
            .mapNotNull { application -> packageInfoFor(application.packageName) }
            .map { it.toAppInfo() }
            .sortedBy { it.name.lowercase() }
    }

    private fun applicationQueryFlags(): Int {
        return if (BuildConfig.FLAVOR == "privateRelease") {
            PackageManager.GET_META_DATA
        } else {
            PackageManager.GET_META_DATA
        }
    }

    private fun packageInfoFor(packageName: String): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
        }.getOrNull()
    }

    private fun PackageInfo.toAppInfo(): AppInfo {
        val applicationInfo = applicationInfo
        val requestedPermissions = requestedPermissions?.toList().orEmpty()
        val grantedPermissions = requestedPermissions.mapIndexedNotNull { index, permission ->
            val flags = requestedPermissionsFlags?.getOrNull(index) ?: 0
            if (flags and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) permission else null
        }
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        }.getOrNull()

        return AppInfo(
            name = applicationInfo.loadLabel(packageManager).toString(),
            packageName = packageName,
            versionName = versionName ?: "Desconocida",
            installer = installer,
            installDate = firstInstallTime,
            updateDate = lastUpdateTime,
            targetSdkVersion = applicationInfo.targetSdkVersion,
            requestedPermissions = requestedPermissions,
            grantedPermissions = grantedPermissions,
            isSystemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
            allowBackup = applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0,
            requestsInstallPackages = "android.permission.REQUEST_INSTALL_PACKAGES" in requestedPermissions
        )
    }
}
