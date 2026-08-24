package com.aura.defense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aura.defense.services.AuraDiscoveryService
import com.aura.defense.services.DiscoveredAura
import java.text.DateFormat
import java.util.Date

@Composable
internal fun AurasScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val discoveryService = remember(context) { AuraDiscoveryService(context) }
    var shareLocation by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var discoveredAuras by remember { mutableStateOf(emptyList<DiscoveredAura>()) }
    val listener: (List<DiscoveredAura>) -> Unit = { discoveredAuras = it }

    DisposableEffect(discoveryService) {
        discoveryService.addListener(listener)
        onDispose {
            discoveryService.removeListener(listener)
            discoveryService.close()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Auras en Red", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Compartir Mi Ubicación")
                Switch(
                    checked = shareLocation,
                    onCheckedChange = {
                        shareLocation = it
                        discoveryService.setShareLocation(it)
                    }
                )
            }
        }
        item {
            Button(
                onClick = {
                    isSearching = true
                    discoveryService.startDiscovery()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSearching
            ) {
                Text(if (isSearching) "Buscando Auras..." else "Buscar Auras en Red")
            }
        }
        if (discoveredAuras.isEmpty()) {
            item {
                Text(
                    "No se detectaron Auras en la red local.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(discoveredAuras, key = { it.id }) { aura ->
                AuraCard(aura)
            }
        }
    }
}

@Composable
private fun AuraCard(aura: DiscoveredAura) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(aura.id, style = MaterialTheme.typography.titleMedium)
            Text("Estado: ${aura.status}")
            Text("Última vez visto: ${formatLastSeen(aura.lastSeen)}")
        }
    }
}

private fun formatLastSeen(lastSeen: Long): String = DateFormat.getDateTimeInstance(
    DateFormat.SHORT,
    DateFormat.SHORT
).format(Date(lastSeen))
