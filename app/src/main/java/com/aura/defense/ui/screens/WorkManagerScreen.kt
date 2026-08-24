package com.aura.defense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.defense.data.ScanSchedule
import com.aura.defense.data.ScanScheduler

@Composable
internal fun WorkManagerScreen(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selected by remember { mutableStateOf(ScanScheduler.getSchedule(context)) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Escaneos programados", style = MaterialTheme.typography.headlineMedium)
        Text("El análisis se ejecuta localmente y guarda un resumen cifrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ScanSchedule.values().forEach { schedule ->
            Button(
                onClick = {
                    selected = schedule
                    ScanScheduler.setSchedule(context, schedule)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selected == schedule) "✓ ${schedule.label}" else schedule.label)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text(
                "Configuración actual: ${selected.label}",
                modifier = Modifier.padding(16.dp)
            )
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
    }
}
