package com.aura.defense

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
internal fun DefensaScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val blockedDomainCount = remember(context) {
        ThreatRepository.loadBlockedDomains(context.applicationContext).size
    }
    var vpnEnabled by remember { mutableStateOf(AuraVpnService.isRunning) }
    var consentDenied by remember { mutableStateOf(false) }
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpn(context)
            vpnEnabled = true
            consentDenied = false
        } else {
            vpnEnabled = false
            consentDenied = true
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Defensa VPN", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp)
        Button(
            onClick = {
                if (vpnEnabled) {
                    stopVpn(context)
                    vpnEnabled = false
                } else {
                    val prepareIntent = VpnService.prepare(context)
                    if (prepareIntent == null) {
                        startVpn(context)
                        vpnEnabled = true
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
                        vpnEnabled = true
                    } else {
                        consentLauncher.launch(prepareIntent)
                    }
                } else {
                    stopVpn(context)
                    vpnEnabled = false
                }
            }
        )
        Text(if (vpnEnabled) "VPN ACTIVA" else "INACTIVA", color = if (vpnEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error, fontSize = 20.sp)
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
