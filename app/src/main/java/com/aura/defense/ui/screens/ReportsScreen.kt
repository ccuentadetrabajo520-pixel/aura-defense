package com.aura.defense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.defense.data.AuraSecurityEngine
import com.aura.defense.tools.ReportGenerator
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun ReportsScreen() {
    val context = LocalContext.current
    var reportText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Reportes de Seguridad", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val result = AuraSecurityEngine.evaluate(context)
                reportText = ReportGenerator.generateTxtReport(result)
                Toast.makeText(context, "Reporte generado", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generar Reporte TXT")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (reportText.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = reportText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
