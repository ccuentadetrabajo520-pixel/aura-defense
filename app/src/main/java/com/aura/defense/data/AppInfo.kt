package com.aura.defense.data

data class AppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val installer: String?,
    val installDate: Long,
    val updateDate: Long,
    val targetSdkVersion: Int,
    val requestedPermissions: List<String>,
    val grantedPermissions: List<String>,
    val isSystemApp: Boolean,
    val isDebuggable: Boolean,
    val allowBackup: Boolean,
    val requestsInstallPackages: Boolean
) {
    val hasSensitivePermissions: Boolean
        get() = requestedPermissions.any(::isSensitivePermission)
}

fun isSensitivePermission(permission: String): Boolean {
    return permission in setOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.SEND_SMS",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.CALL_PHONE",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.QUERY_ALL_PACKAGES",
        "android.permission.REQUEST_INSTALL_PACKAGES"
    )
}
