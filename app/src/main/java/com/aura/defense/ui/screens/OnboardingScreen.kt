package com.aura.defense.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PermissionStep(
    val title: String,
    val explanation: String,
    val icon: ImageVector
)

private val permissionSteps = listOf(
    PermissionStep(
        title = "Permiso de VPN",
        explanation = "Aura Defense necesita este permiso para analizar conexiones y bloquear amenazas antes de que lleguen a tus aplicaciones.",
        icon = Icons.Filled.Security
    ),
    PermissionStep(
        title = "Permiso de ubicación",
        explanation = "La ubicación ayuda a detectar redes cercanas y a evaluar si una conexión Wi-Fi puede ser peligrosa.",
        icon = Icons.Filled.LocationOn
    ),
    PermissionStep(
        title = "Permiso de notificaciones",
        explanation = "Las notificaciones permiten avisarte de inmediato cuando se detecta una amenaza o cambia el estado de tu protección.",
        icon = Icons.Filled.Notifications
    )
)

@Composable
internal fun OnboardingScreen(onComplete: () -> Unit = {}) {
    var currentStep by remember { mutableIntStateOf(0) }
    val step = permissionSteps[currentStep]

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Configura tu protección", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp)
        Spacer(Modifier.height(28.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(18.dp))
                Text(step.title, color = MaterialTheme.colorScheme.secondary, fontSize = 21.sp)
                Spacer(Modifier.height(12.dp))
                Text(step.explanation, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            permissionSteps.indices.forEach { index ->
                Text(
                    text = if (index == currentStep) "●" else "○",
                    color = if (index == currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (currentStep == permissionSteps.lastIndex) onComplete() else currentStep++
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Activar")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Omitir")
        }
    }
}
