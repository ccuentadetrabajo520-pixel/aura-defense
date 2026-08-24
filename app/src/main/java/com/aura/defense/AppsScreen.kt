package com.aura.defense

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.data.AppInfo
import com.aura.defense.data.AppScannerRepository

@Composable
internal fun AppsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val apps = remember(context) { AppScannerRepository(context.applicationContext).scanVisibleApps() }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Aplicaciones instaladas", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp)
            Text("${apps.size} aplicaciones visibles", color = MaterialTheme.colorScheme.secondary)
        }
        items(apps, key = { app -> app.packageName }) { app ->
            AppCard(app)
        }
    }
}

@Composable
private fun AppCard(app: AppInfo) {
    val context = LocalContext.current
    val hasWarning = app.hasSensitivePermissions || app.isDebuggable
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(app.name, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp)
                if (hasWarning) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Permisos sensibles o aplicación depurable",
                        tint = Color(0xFFFFA45C)
                    )
                }
            }
            Text(app.packageName, color = MaterialTheme.colorScheme.onSurface)
            Text("Versión ${app.versionName}", color = MaterialTheme.colorScheme.secondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { openAppDetails(context, app.packageName) }, modifier = Modifier.weight(1f)) {
                    Text("Ver Detalles", fontSize = 11.sp)
                }
                Button(onClick = { openAppDetails(context, app.packageName) }, modifier = Modifier.weight(1f)) {
                    Text("Ver Permisos", fontSize = 11.sp)
                }
                Button(onClick = { requestUninstall(context, app.packageName) }, modifier = Modifier.weight(1f)) {
                    Text("Solicitar Desinstalación", fontSize = 10.sp)
                }
            }
        }
    }
}

private fun openAppDetails(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }
    startActivitySafely(context, intent)
}

private fun requestUninstall(context: Context, packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE).apply {
        data = Uri.parse("package:$packageName")
    }
    startActivitySafely(context, intent)
}

private fun startActivitySafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
