package com.anonymous.mobile

import android.content.Context
import android.view.accessibility.AccessibilityManager

class AccessibilityGuard(private val context: Context) {
    fun auditEnabledServices(): List<AccessibilityServiceAudit> {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return emptyList()

        val enabledServices = manager.getEnabledAccessibilityServiceList(AccessibilityManager.FEATURE_ACCESSIBILITY_ALL)
        return enabledServices.mapNotNull { serviceInfo ->
            val packageName = serviceInfo.resolveInfo?.resolvePackageName ?: serviceInfo.packageName
            if (packageName.isNullOrBlank()) null else AccessibilityServiceAudit(
                packageName = packageName,
                serviceName = serviceInfo.resolveInfo?.loadLabel(context.packageManager)?.toString().orEmpty(),
                enabled = true
            )
        }
    }
}

data class AccessibilityServiceAudit(
    val packageName: String,
    val serviceName: String,
    val enabled: Boolean
)