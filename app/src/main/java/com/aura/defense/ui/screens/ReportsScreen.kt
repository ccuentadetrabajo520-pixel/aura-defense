package com.aura.defense.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.defense.data.AppScannerRepository
import com.aura.defense.data.AuraSecurityEngine
import com.aura.defense.data.DeviceTelemetry
import com.aura.defense.data.SecurityFinding
import com.aura.defense.data.SecurityPostureResult
import com.aura.defense.tools.ReportGenerator

@Composable
internal fun ReportsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val posture = remember(context) { AuraSecurityEngine.evaluate(context) }
    val apps = remember(context) { AppScannerRepository(context.applicationContext).scanVisibleApps() }
    val riskyApps = apps.filter { it.hasSensitivePermissions || it.isDebuggable || it.requestsInstallPackages }
    val txtReport = remember(posture, riskyApps) { ReportGenerator.generateTxtReport(posture, riskyApps) }
    val jsonReport = remember(posture, riskyApps) { ReportGenerator.generateJsonReport(posture, riskyApps) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Reportes")
        Text("Informe actual: score ${posture.score}/100, ${riskyApps.size} aplicaciones de riesgo.")
        Button(
            onClick = {
                val file = ReportGenerator.exportTxtReport(context, txtReport)
                Toast.makeText(context, "TXT guardado en ${file.name}", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Exportar TXT") }
        Button(
            onClick = {
                val file = ReportGenerator.exportJsonReport(context, jsonReport)
                Toast.makeText(context, "JSON guardado en ${file.name}", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Exportar JSON") }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar")
        }
    }
}
