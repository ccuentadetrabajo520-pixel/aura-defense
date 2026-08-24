package com.aura.defense.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.tools.NotificationGuardService

@Composable
internal fun NotificationGuardScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var isActive by remember { mutableStateOf(NotificationGuardService.isConnected) }

    DisposableEffect(Unit) {
        isActive = NotificationGuardService.isConnected
        onDispose { isActive = false }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Notification Guard", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp)
        Text(
            "Aura Defense analiza localmente el texto de tus notificaciones para detectar enlaces sospechosos. El contenido no se guarda ni se envía a servidores. Al activarlo, Android concede acceso de lectura a este servicio."
        )
        Text(
            if (isActive) "Estado: Active" else "Estado: Inactive",
            color = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            fontSize = 18.sp
        )
        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (_: ActivityNotFoundException) {
                    isActive = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Activar Acceso a Notificaciones")
        }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar")
        }
    }
}
