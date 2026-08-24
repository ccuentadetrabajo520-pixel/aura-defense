package com.aura.defense.data

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import java.util.Date

data class SecurityFinding(
    val title: String,
    val severity: String,
    val technicalEvidence: String,
    val simpleExplanation: String,
    val recommendedAction: String,
    val settingsIntent: String?
)

data class DeviceTelemetry(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val totalRam: Long,
    val availableRam: Long,
    val batteryPercentage: Int,
    val networkType: String,
    val vpnStatus: Boolean,
    val privateDnsStatus: Boolean,
    val visibleAppsCount: Int,
    val foundRisksCount: Int
)

data class SecurityPostureResult(
    val score: Int,
    val findings: List<SecurityFinding>,
    val telemetry: DeviceTelemetry
)

object SecurityPostureEngine {
    fun evaluate(context: Context): SecurityPostureResult {
        val findings = mutableListOf<SecurityFinding>()
        var score = 100
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val activeCapabilities = connectivityManager?.activeNetwork?.let(connectivityManager::getNetworkCapabilities)
        val vpnActive = activeCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val privateDnsActive = isPrivateDnsActive(context)
        val validatedNetwork = activeCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val screenLockActive = isScreenLockActive(context)
        val adbEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        val developerOptionsEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        val unknownSourcesEnabled = isUnknownSourcesEnabled(context)
        val dangerousPermissionApps = findAppsWithDangerousPermissions(context)
        val unknownInstallerApps = findAppsWithUnknownInstallers(context)
        val accessibilityEnabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).isNullOrBlank().not()
        val notificationGuardActive = isNotificationGuardActive(context)

        if (!screenLockActive) {
            score -= 12
            findings += SecurityFinding(
                "Bloqueo de pantalla inactivo",
                "Critical",
                "KeyguardManager.isKeyguardSecure() devolvió false y no hay patrón de bloqueo activo.",
                "Sin bloqueo, cualquiera con acceso físico puede entrar al dispositivo.",
                "Configura un PIN, patrón o contraseña.",
                Settings.ACTION_SECURITY_SETTINGS
            )
        }
        if (adbEnabled) {
            score -= 8
            findings += SecurityFinding(
                "ADB habilitado",
                "High",
                "Settings.Global.ADB_ENABLED = 1.",
                "La depuración USB permite controlar el teléfono desde un ordenador autorizado.",
                "Desactiva la depuración USB cuando no la necesites.",
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            )
        }
        if (developerOptionsEnabled) {
            score -= 4
            findings += SecurityFinding(
                "Opciones de desarrollador activas",
                "Medium",
                "Settings.Global.DEVELOPMENT_SETTINGS_ENABLED = 1.",
                "Las opciones avanzadas aumentan la superficie de configuración del dispositivo.",
                "Desactiva las opciones de desarrollador si no las utilizas.",
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            )
        }
        if (unknownSourcesEnabled) {
            score -= 8
            findings += SecurityFinding(
                "Instalación desde fuentes desconocidas",
                "High",
                "La instalación de paquetes fuera de Google Play está permitida.",
                "Las aplicaciones instaladas desde fuera de una tienda confiable pueden contener malware.",
                "Desactiva la instalación de aplicaciones desconocidas.",
                Settings.ACTION_SECURITY_SETTINGS
            )
        }
        if (!vpnActive) {
            score -= 10
            findings += SecurityFinding(
                "VPN no activa",
                "High",
                "La red activa no anuncia TRANSPORT_VPN.",
                "El tráfico no está pasando por una VPN activa.",
                "Activa una VPN de confianza.",
                Settings.ACTION_VPN_SETTINGS
            )
        }
        if (!privateDnsActive) {
            score -= 7
            findings += SecurityFinding(
                "DNS privado inactivo",
                "Medium",
                "Settings.Global.PRIVATE_DNS_MODE está desactivado o no disponible.",
                "Las consultas DNS no están usando un proveedor privado configurado.",
                "Configura DNS privado en los ajustes de red.",
                Settings.ACTION_WIRELESS_SETTINGS
            )
        }
        if (!validatedNetwork) {
            score -= 7
            findings += SecurityFinding(
                "Red no validada",
                "High",
                "La red activa no anuncia NET_CAPABILITY_VALIDATED.",
                "Android no ha podido confirmar que la conexión tenga acceso fiable a Internet.",
                "Comprueba la red Wi-Fi o móvil actual.",
                Settings.ACTION_WIRELESS_SETTINGS
            )
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            score -= 5
            findings += SecurityFinding(
                "Versión de Android antigua",
                "Medium",
                "Build.VERSION.SDK_INT = ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE}).",
                "Una versión antigua puede carecer de protecciones modernas del sistema.",
                "Comprueba si existe una actualización de Android para este dispositivo.",
                Settings.ACTION_SECURITY_SETTINGS
            )
        }
        if (!hasRecentSecurityPatch()) {
            score -= 10
            findings += SecurityFinding(
                "Parche de seguridad desactualizado",
                "High",
                "Build.VERSION.SECURITY_PATCH = ${Build.VERSION.SECURITY_PATCH}.",
                "Un parche antiguo puede dejar vulnerabilidades conocidas sin corregir.",
                "Instala las actualizaciones del sistema disponibles.",
                Settings.ACTION_SECURITY_SETTINGS
            )
        }
        if (dangerousPermissionApps.isNotEmpty()) {
            score -= minOf(10, dangerousPermissionApps.size * 2)
            findings += SecurityFinding(
                "Aplicaciones con permisos peligrosos",
                "High",
                "${dangerousPermissionApps.size} aplicación(es) tienen permisos peligrosos concedidos.",
                "Algunas aplicaciones pueden acceder a datos o sensores sensibles.",
                "Revisa y revoca permisos que no sean necesarios.",
                Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
            )
        }
        if (unknownInstallerApps.isNotEmpty()) {
            score -= minOf(8, unknownInstallerApps.size * 2)
            findings += SecurityFinding(
                "Aplicaciones de instalador desconocido",
                "High",
                "${unknownInstallerApps.size} aplicación(es) no tienen un instalador verificable.",
                "Una aplicación sin origen identificable merece una revisión adicional.",
                "Desinstala aplicaciones cuyo origen no reconozcas.",
                Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
            )
        }
        if (accessibilityEnabled) {
            score -= 5
            findings += SecurityFinding(
                "Servicio de accesibilidad activo",
                "Medium",
                "ENABLED_ACCESSIBILITY_SERVICES contiene al menos un servicio.",
                "Un servicio de accesibilidad puede leer e interactuar con lo que aparece en pantalla.",
                "Revisa los servicios de accesibilidad habilitados.",
                Settings.ACTION_ACCESSIBILITY_SETTINGS
            )
        }
        if (!notificationGuardActive) {
            score -= 4
            findings += SecurityFinding(
                "Notification Guard inactivo",
                "Medium",
                "El paquete de Aura Defense no aparece en ENABLED_NOTIFICATION_LISTENERS.",
                "Aura Defense no puede observar notificaciones para advertirte sobre actividad sospechosa.",
                "Activa Notification Guard en los ajustes de notificaciones.",
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            )
        }

        score -= 5
        findings += SecurityFinding(
            "Firewall del dispositivo no verificable",
            "Low",
            "Android no expone el estado del firewall del sistema mediante una API pública estándar.",
            "No se ha podido confirmar una capa de firewall adicional.",
            "Mantén actualizado Android y usa una red de confianza.",
            Settings.ACTION_SECURITY_SETTINGS
        )

        val telemetry = collectTelemetry(
            context = context,
            activeCapabilities = activeCapabilities,
            vpnActive = vpnActive,
            privateDnsActive = privateDnsActive,
            visibleAppsCount = countVisibleApps(context),
            foundRisksCount = findings.size
        )
        return SecurityPostureResult(score.coerceIn(0, 100), findings, telemetry)
    }

    private fun collectTelemetry(
        context: Context,
        activeCapabilities: NetworkCapabilities?,
        vpnActive: Boolean,
        privateDnsActive: Boolean,
        visibleAppsCount: Int,
        foundRisksCount: Int
    ): DeviceTelemetry {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val batteryPercentage = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it >= 0 } ?: 0
        val networkType = when {
            vpnActive -> "VPN"
            activeCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            activeCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile"
            else -> "Desconocida"
        }
        return DeviceTelemetry(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            totalRam = memoryInfo.totalMem,
            availableRam = memoryInfo.availMem,
            batteryPercentage = batteryPercentage,
            networkType = networkType,
            vpnStatus = vpnActive,
            privateDnsStatus = privateDnsActive,
            visibleAppsCount = visibleAppsCount,
            foundRisksCount = foundRisksCount
        )
    }

    private fun isScreenLockActive(context: Context): Boolean {
        val keyguardManager = context.getSystemService(android.app.KeyguardManager::class.java)
        val patternEnabled = Settings.Secure.getInt(context.contentResolver, "lock_pattern_autolock", 0) == 1
        return keyguardManager?.isKeyguardSecure == true || patternEnabled
    }

    private fun isUnknownSourcesEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
        }
    }

    private fun isPrivateDnsActive(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return Settings.Global.getString(context.contentResolver, Settings.Global.PRIVATE_DNS_MODE)
            ?.lowercase()
            ?.let { it != "off" }
            ?: false
    }

    private fun hasRecentSecurityPatch(): Boolean {
        val patch = Build.VERSION.SECURITY_PATCH
        if (patch.isBlank()) return false
        return try {
            val patchDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(patch)
            patchDate != null && Date().time - patchDate.time <= 180L * 24L * 60L * 60L * 1000L
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun findAppsWithDangerousPermissions(context: Context): List<String> {
        return context.packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS).filter { packageInfo ->
            val requestedPermissions = packageInfo.requestedPermissions ?: return@filter false
            requestedPermissions.indices.any { index ->
                val granted = packageInfo.requestedPermissionsFlags?.getOrNull(index)
                    ?.and(PackageInfo.REQUESTED_PERMISSION_GRANTED) == PackageInfo.REQUESTED_PERMISSION_GRANTED
                if (!granted) return@any false
                val permissionInfo = runCatching {
                    context.packageManager.getPermissionInfo(requestedPermissions[index], 0)
                }.getOrNull()
                permissionInfo != null && permissionInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE == PermissionInfo.PROTECTION_DANGEROUS
            }
        }.map { it.packageName }
    }

    private fun findAppsWithUnknownInstallers(context: Context): List<String> {
        @Suppress("DEPRECATION")
        val installedPackages = context.packageManager.getInstalledPackages(0)
        return installedPackages.filter { packageInfo ->
            if (packageInfo.packageName == context.packageName) return@filter false
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(packageInfo.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(packageInfo.packageName)
            }
            installer.isNullOrBlank()
        }.map { it.packageName }
    }

    private fun isNotificationGuardActive(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.split(':').any { it.startsWith(context.packageName) }
    }

    @Suppress("DEPRECATION")
    private fun countVisibleApps(context: Context): Int {
        return context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA).count {
            it.packageName != context.packageName && it.enabled
        }
    }
}
