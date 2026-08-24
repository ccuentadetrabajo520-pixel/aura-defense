package com.aura.defense.tools

import com.aura.defense.data.DeviceTelemetry
import com.aura.defense.data.SecurityFinding
import com.aura.defense.data.SecurityPostureResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportGenerator {
    fun generateTxtReport(result: SecurityPostureResult): String {
        val sb = StringBuilder()
        sb.appendLine("=== AURA DEFENSE REPORT ===")
        sb.appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        sb.appendLine("Score: ${result.score}")
        sb.appendLine("")
        sb.appendLine("--- Findings ---")
        result.findings.forEach { finding ->
            sb.appendLine("• ${finding.title} (${finding.severity})")
            sb.appendLine("   ${finding.simpleExplanation}")
            sb.appendLine("   Action: ${finding.recommendedAction}")
            sb.appendLine()
        }
        sb.appendLine("--- Telemetry ---")
        val t = result.telemetry
        sb.appendLine("Device: ${t.manufacturer} ${t.model}")
        sb.appendLine("Android: ${t.androidVersion} (API ${t.apiLevel})")
        sb.appendLine("Battery: ${t.batteryPercentage}%")
        sb.appendLine("Network: ${t.networkType}")
        sb.appendLine("RAM: ${t.availableRam / (1024 * 1024)} MB / ${t.totalRam / (1024 * 1024)} MB")
        return sb.toString()
    }

    fun generateJsonReport(result: SecurityPostureResult): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"score\": ${result.score},")
        sb.appendLine("  \"findings\": [")
        result.findings.forEachIndexed { index, finding ->
            sb.appendLine("    {")
            sb.appendLine("      \"title\": \"${finding.title}\",")
            sb.appendLine("      \"severity\": \"${finding.severity}\",")
            sb.appendLine("      \"recommendedAction\": \"${finding.recommendedAction}\"")
            sb.append(if (index == result.findings.lastIndex) "    }" else "    },")
            sb.appendLine()
        }
        sb.appendLine("  ]")
        sb.appendLine("}")
        return sb.toString()
    }
}
