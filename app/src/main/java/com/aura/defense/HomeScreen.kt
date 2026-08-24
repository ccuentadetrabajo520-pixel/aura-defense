package com.aura.defense.ui.screens

import android.app.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.defense.data.AuraSecurityEngine
import com.aura.defense.data.DeviceTelemetry
import com.aura.defense.data.SecurityFinding

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onSettingsClick: () -> Unit = {}) {
    var auraId by remember { mutableStateOf("AURA-001") }
    val posture by produceState(initialValue = AuraSecurityEngine.evaluate(LocalContext.current)) {
        value = AuraSecurityEngine.evaluate(LocalContext.current)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título y Chip ID
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AURA DEFENSE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = auraId,
                onValueChange = { auraId = it },
                label = { Text("ID Aura") },
                singleLine = true,
                modifier = Modifier.width(150.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Score y Estado
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Aura Score", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = posture.score.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (posture.score >= 85) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = when {
                        posture.score >= 85 -> "PROTEGIDO"
                        posture.score >= 60 -> "PROTECCIÓN PARCIAL"
                        else -> "RIESGO ALTO"
                    },
                    color = if (posture.score >= 85) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón de Escanear
        Button(
            onClick = { /* Re-evaluar */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ESCANEAR AHORA")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hallazgos Críticos
        Text("Hallazgos Críticos", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(posture.findings.filter { it.severity == "Critical" || it.severity == "High" }) { finding ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (finding.severity == "Critical")
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(finding.title, fontWeight = FontWeight.Bold)
                        Text(finding.simpleExplanation, style = MaterialTheme.typography.bodySmall)
                        finding.settingsIntent?.let { intentAction ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    try {
                                        LocalContext.current.startActivity(Intent(intentAction))
                                    } catch (_: ActivityNotFoundException) {
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Abrir Ajustes")
                            }
                        }
                    }
                }
            }
        }
    }
}
