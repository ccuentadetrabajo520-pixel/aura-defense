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
import com.aura.defense.security.EncryptedVault

@Composable
internal fun VaultScreen(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var history by remember { mutableStateOf(readHistory(EncryptedVault(context))) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Vault Cifrado", style = MaterialTheme.typography.headlineMedium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Historial protegido", color = MaterialTheme.colorScheme.primary)
                history.forEach { entry -> Text(entry) }
                if (history.isEmpty()) Text("No hay datos cifrados guardados.")
            }
        }
        Button(
            onClick = {
                EncryptedVault(context).clearHistory()
                history = emptyList()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Borrar todo el historial") }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
    }
}

private fun readHistory(vault: EncryptedVault): List<String> = listOfNotNull(
    vault.readDecryptedData("last_report"),
    vault.readDecryptedData("last_scan_log"),
    vault.readDecryptedData("firewall_events")
)
