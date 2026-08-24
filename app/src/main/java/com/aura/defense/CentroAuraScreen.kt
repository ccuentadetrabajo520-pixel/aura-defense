package com.aura.defense

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.ui.screens.LinkAnalyzerScreen
import com.aura.defense.ui.screens.PasswordAuditorScreen
import com.aura.defense.ui.screens.ReportsScreen

private data class CentroAuraItem(val label: String, val icon: ImageVector)

private val centroAuraItems = listOf(
    CentroAuraItem("ID de usuario", Icons.Filled.Person),
    CentroAuraItem("Permisos", Icons.Filled.Security),
    CentroAuraItem("Términos y Condiciones", Icons.Filled.Description),
    CentroAuraItem("Privacidad", Icons.Filled.Lock),
    CentroAuraItem("Analizador de Enlaces", Icons.Filled.Link),
    CentroAuraItem("QR Anti-Phishing", Icons.Filled.QrCode),
    CentroAuraItem("Auditor de Contraseñas", Icons.Filled.Password),
    CentroAuraItem("Notification Guard", Icons.Filled.Notifications),
    CentroAuraItem("Share Scanner", Icons.Filled.Share),
    CentroAuraItem("Reportes", Icons.Filled.Assessment),
    CentroAuraItem("Vault Cifrado", Icons.Filled.Lock),
    CentroAuraItem("Modo Emergencia", Icons.Filled.Warning),
    CentroAuraItem("WorkManager", Icons.Filled.Work),
    CentroAuraItem("Límites Reales de Android", Icons.Filled.Security)
)

@Composable
internal fun CentroAuraScreen() {
    val context = LocalContext.current
    var selectedTool by remember { mutableStateOf<String?>(null) }

    when (selectedTool) {
        "Analizador de Enlaces" -> LinkAnalyzerScreen { selectedTool = null }
        "Auditor de Contraseñas" -> PasswordAuditorScreen { selectedTool = null }
        "Reportes" -> ReportsScreen { selectedTool = null }
        else -> CentroAuraMenu(
            onItemClick = { item ->
                if (item.label in setOf("Analizador de Enlaces", "Auditor de Contraseñas", "Reportes")) {
                    selectedTool = item.label
                } else if (item.label == "Notification Guard") {
                    try {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "Ajustes no disponibles en este dispositivo", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Función en desarrollo", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun CentroAuraMenu(onItemClick: (CentroAuraItem) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text("Centro Aura", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = 14.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(centroAuraItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onItemClick(item) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Text(item.label, modifier = Modifier.padding(start = 14.dp), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}