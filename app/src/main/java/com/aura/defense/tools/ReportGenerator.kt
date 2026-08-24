package com.aura.defense.tools

import android.content.Context
import com.aura.defense.data.AppInfo
import com.aura.defense.data.AuraSecurityEngine
import com.aura.defense.data.DeviceTelemetry
import com.aura.defense.data.SecurityFinding
import com.aura.defense.data.SecurityPostureResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportGenerator {
    fun generateTxtReport(
        posture: SecurityPostureResult,
        riskyApps: List<AppInfo>,
        firewallEvents: List<String> = emptyList(),
        recommendations: List<String> = posture.findings.map { it.recommendedAction },
        generatedAt: Date = Date()
    ): String {
        return buildString {
            appendLine("AURA DEFENSE - INFORME DE SEGURIDAD")
            appendLine("Fecha: ${formatDate(generatedAt)}")
            appendLine("Score: ${posture.score}/100")
            appendLine("VPN activa: ${posture.telemetry.vpnStatus}")
            appendLine("DNS privado activo: ${posture.telemetry.privateDnsStatus}")
            appendLine()
            appendLine("HALLAZGOS")
            posture.findings.forEach { appendFinding(this, it) }
            appendLine("APLICACIONES DE RIESGO")
            riskyApps.forEach { app ->
                appendLine("- ${app.name} (${app.packageName}) - ${app.versionName}")
            }
            appendLine("EVENTOS DE FIREWALL")
            firewallEvents.forEach { appendLine("- $it") }
            appendLine("RECOMENDACIONES")
            recommendations.distinct().forEach { appendLine("- $it") }
        }
    }

    fun generateJsonReport(
        posture: SecurityPostureResult,
        riskyApps: List<AppInfo>,
        firewallEvents: List<String> = emptyList(),
        recommendations: List<String> = posture.findings.map { it.recommendedAction },
        generatedAt: Date = Date()
    ): String {
        val root = JSONObject()
            .put("generatedAt", formatDate(generatedAt))
            .put("score", posture.score)
            .put("vpnActive", posture.telemetry.vpnStatus)
            .put("privateDnsActive", posture.telemetry.privateDnsStatus)
            .put("firewallEvents", JSONArray(firewallEvents))
            .put("recommendations", JSONArray(recommendations.distinct()))
            .put("findings", JSONArray(posture.findings.map(::findingJson)))
            .put("riskyApps", JSONArray(riskyApps.map { app ->
                JSONObject()
                    .put("name", app.name)
                    .put("packageName", app.packageName)
                    .put("versionName", app.versionName)
                    .put("installer", app.installer)
                    .put("isDebuggable", app.isDebuggable)
                    .put("sensitivePermissions", app.requestedPermissions.filter { permission ->
                        isSensitiveReportPermission(permission)
                    })
            }))
        return root.toString(2)
    }

    fun exportTxtReport(context: Context, report: String): File = export(context, "aura-defense-report.txt", report)

    fun exportJsonReport(context: Context, report: String): File = export(context, "aura-defense-report.json", report)

    private fun export(context: Context, filename: String, content: String): File {
        return File(context.filesDir, filename).also { file -> file.writeText(content, Charsets.UTF_8) }
    }

    private fun appendFinding(builder: StringBuilder, finding: SecurityFinding) {
        builder.appendLine("- [${finding.severity}] ${finding.title}")
        builder.appendLine("  Evidencia: ${finding.technicalEvidence}")
        builder.appendLine("  Explicación: ${finding.simpleExplanation}")
        builder.appendLine("  Acción: ${finding.recommendedAction}")
    }

    private fun findingJson(finding: SecurityFinding): JSONObject {
        return JSONObject()
            .put("title", finding.title)
            .put("severity", finding.severity)
            .put("technicalEvidence", finding.technicalEvidence)
            .put("simpleExplanation", finding.simpleExplanation)
            .put("recommendedAction", finding.recommendedAction)
            .put("settingsIntent", finding.settingsIntent)
    }

    private fun formatDate(date: Date): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(date)

    private fun isSensitiveReportPermission(permission: String): Boolean {
        return permission.contains("SMS") || permission.contains("CONTACTS") ||
            permission.contains("LOCATION") || permission.contains("CAMERA") ||
            permission.contains("RECORD_AUDIO") || permission.contains("PHONE") ||
            permission.contains("ALERT_WINDOW") || permission.contains("NOTIFICATION") ||
            permission.contains("USAGE_STATS") || permission.contains("INSTALL_PACKAGES")
    }
}
