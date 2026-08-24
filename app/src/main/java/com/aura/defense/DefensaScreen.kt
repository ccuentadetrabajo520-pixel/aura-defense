package com.aura.defense

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.defense.data.ThreatRepository
import com.aura.defense.services.AuraVpnService
import kotlinx.coroutines.delay

@Composable
internal fun DefensaScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val blockedDomainCount = remember(context) {
        ThreatRepository.loadBlockedDomains(context.applicationContext).size
    }
    var vpnEnabled by remember { mutableStateOf(isVpnActive(context)) }
    var privateDnsEnabled by remember { mutableStateOf(isPrivateDnsActive(context)) }
    var consentDenied by remember { mutableStateOf(false) }
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpn(context)
            consentDenied = false
        } else {
            vpnEnabled = false
            consentDenied = true
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            vpnEnabled = isVpnActive(context)
            privateDnsEnabled = isPrivateDnsActive(context)
            delay(1_000)
        }
    }

    val protectionActive = vpnEnabled && privateDnsEnabled

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Defensa VPN", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp)
        Icon(
            imageVector = if (protectionActive) Icons.Filled.Shield else Icons.Outlined.Shield,
            contentDescription = if (protectionActive) "Protección completa activa" else "Protección incompleta",
            tint = if (protectionActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(72.dp)
        )
        Button(
            onClick = {
                if (vpnEnabled) {
                    stopVpn(context)
                    vpnEnabled = false
                } else {
                    val prepareIntent = VpnService.prepare(context)
                    if (prepareIntent == null) {
                        startVpn(context)
                        consentDenied = false
                    } else {
                        consentLauncher.launch(prepareIntent)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vpnEnabled) "Desactivar Defensa VPN" else "Activar Defensa VPN")
        }
        Switch(
            checked = vpnEnabled,
            onCheckedChange = { enabled ->
                if (enabled) {
                    val prepareIntent = VpnService.prepare(context)
                    if (prepareIntent == null) {
                        startVpn(context)
                    } else {
                        consentLauncher.launch(prepareIntent)
                    }
                } else {
                    stopVpn(context)
                    vpnEnabled = false
                }
            }
        )
        Text(
            if (protectionActive) "PROTECCIÓN ACTIVA" else "PROTECCIÓN INCOMPLETA",
            color = if (protectionActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            fontSize = 20.sp
        )
        Text("Lista de Amenazas", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
        Text("$blockedDomainCount dominios en la lista local", color = MaterialTheme.colorScheme.onSurface)
        if (consentDenied) {
            Text("El permiso de VPN es necesario para crear el túnel local. No se inició ninguna conexión porque rechazaste el consentimiento.")
        }
    }
}

private fun startVpn(context: Context) {
    context.startService(Intent(context, AuraVpnService::class.java))
}

private fun stopVpn(context: Context) {
    context.stopService(Intent(context, AuraVpnService::class.java))
}

private fun isVpnActive(context: Context): Boolean {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val capabilities = connectivityManager?.activeNetwork?.let(connectivityManager::getNetworkCapabilities)
    return capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true
}

private fun isPrivateDnsActive(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
    return Settings.Global.getString(context.contentResolver, "private_dns_mode") in setOf("hostname", "opportunistic")
}
