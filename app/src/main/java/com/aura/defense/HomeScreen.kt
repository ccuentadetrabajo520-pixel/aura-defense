package com.aura.defense

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ActivityNotFoundException
import android.content.Intent
import com.aura.defense.data.SecurityFinding
import com.aura.defense.data.SecurityPostureEngine

@Composable
internal fun HomeScreen(modifier: Modifier = Modifier, onSettingsClick: () -> Unit) {
    var auraId by remember { mutableStateOf("AURA-001") }
    val context = LocalContext.current
    val posture = remember(context) { SecurityPostureEngine.evaluate(context) }
    val topFindings = posture.findings.sortedBy { severityRank(it.severity) }.take(3)

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF102126), modifier = Modifier.weight(1f)) {
                    TextField(
                        value = auraId,
                        onValueChange = { auraId = it },
                        singleLine = true,
                        label = { Text("ID Aura") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, contentDescription = "Centro Aura", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Text("AURA DEFENSE", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
            Text("SIN ESCANEO", color = MaterialTheme.colorScheme.secondary, letterSpacing = 2.sp)
        }
        item { RadarPlaceholder(Modifier.size(220.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AURA SCORE", color = MaterialTheme.colorScheme.secondary, letterSpacing = 2.sp)
                    Text("${posture.score}", color = MaterialTheme.colorScheme.primary, fontSize = 56.sp)
                    Text(postureStatus(posture.score), color = statusColor(posture.score), fontSize = 18.sp)
                }
            }
        }
        item { TelemetryCard(posture.telemetry) }
        if (topFindings.isNotEmpty()) {
            item { Text("HALLAZGOS PRIORITARIOS", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxWidth()) }
            items(topFindings, key = { finding -> finding.title }) { finding ->
                FindingCard(finding) { intentAction ->
                    try {
                        context.startActivity(Intent(intentAction))
                    } catch (_: ActivityNotFoundException) {
                    }
                }
            }
        }
        item {
            HomeActionButton("ESCANEAR AHORA")
            Spacer(Modifier.height(2.dp))
            HomeActionButton("ACTIVAR DEFENSA VPN")
            Spacer(Modifier.height(2.dp))
            HomeActionButton("MODO EMERGENCIA")
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun TelemetryCard(telemetry: com.aura.defense.data.DeviceTelemetry) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp)) {
            Text("TELEMETRÍA DEL DISPOSITIVO", color = MaterialTheme.colorScheme.secondary)
            Text("${telemetry.manufacturer} ${telemetry.model}", color = MaterialTheme.colorScheme.onSurface)
            Text("Android ${telemetry.androidVersion} (API ${telemetry.apiLevel})", color = MaterialTheme.colorScheme.onSurface)
            Text("RAM: ${formatRam(telemetry.availableRam)} disponible / ${formatRam(telemetry.totalRam)} total", color = MaterialTheme.colorScheme.onSurface)
            Text("Batería: ${telemetry.batteryPercentage}% | Red: ${telemetry.networkType}", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun FindingCard(finding: SecurityFinding, onOpenSettings: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(finding.severity.uppercase(), color = severityColor(finding.severity), modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { finding.settingsIntent?.let(onOpenSettings) },
                    enabled = finding.settingsIntent != null
                ) { Text("Abrir Ajustes") }
            }
            Text(finding.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp)
        }
    }
}

private fun severityRank(severity: String): Int = when (severity) {
    "Critical" -> 0
    "High" -> 1
    "Medium" -> 2
    else -> 3
}

private fun severityColor(severity: String): Color = when (severity) {
    "Critical" -> Color(0xFFFF5C5C)
    "High" -> Color(0xFFFFA45C)
    "Medium" -> Color(0xFFFFD166)
    else -> Color(0xFF25F4D0)
}

private fun statusColor(score: Int): Color = when {
    score >= 85 -> Color(0xFFB8F72E)
    score >= 60 -> Color(0xFFFFD166)
    else -> Color(0xFFFF5C5C)
}

private fun postureStatus(score: Int): String = when {
    score >= 85 -> "PROTEGIDO"
    score >= 60 -> "PROTECCIÓN PARCIAL"
    else -> "RIESGO ALTO"
}

private fun formatRam(bytes: Long): String = "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))

@Composable
private fun HomeActionButton(label: String) {
    Button(
        onClick = { },
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color(0xFF031311))
    ) { Text(label, fontSize = 16.sp) }
}

@Composable
private fun RadarPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "radarRotation")
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 8.dp.toPx()
        for (scale in listOf(1f, .72f, .44f)) {
            drawCircle(Color(0xFF1B4D4C), radius * scale, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }
        drawLine(Color(0xFF25F4D0), center, Offset(center.x, center.y - radius), 3.dp.toPx(), StrokeCap.Round)
        rotate(rotation, center) { drawLine(Color(0x9925F4D0), center, Offset(center.x, center.y - radius), 8.dp.toPx(), StrokeCap.Round) }
        drawCircle(Color(0xFFB8F72E), 5.dp.toPx(), center)
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    AuraDefenseTheme {
        HomeScreen(onSettingsClick = {})
    }
}
