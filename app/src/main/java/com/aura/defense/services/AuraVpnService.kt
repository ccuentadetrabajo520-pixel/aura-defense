package com.aura.defense.services

import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.aura.defense.data.ThreatRepository

class AuraVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var blockedDomains: List<String> = emptyList()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        blockedDomains = ThreatRepository.loadBlockedDomains(this)
        if (vpnInterface == null) {
            vpnInterface = Builder()
                .setSession("Aura Defense")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .establish()
        }
        isRunning = vpnInterface != null
        return START_STICKY
    }

    override fun onDestroy() {
        vpnInterface?.close()
        vpnInterface = null
        blockedDomains = emptyList()
        isRunning = false
        super.onDestroy()
    }

    fun shouldBlockDomain(domain: String): Boolean {
        return ThreatRepository.isBlockedDomain(domain, blockedDomains)
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
