package com.aura.defense.services

import android.content.Intent
import android.net.VpnService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.aura.defense.MainActivity

class AuraTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (VpnService.prepare(this) != null) {
            startActivityAndCollapse(Intent(this, MainActivity::class.java))
            return
        }

        if (AuraVpnService.isRunning) {
            stopService(Intent(this, AuraVpnService::class.java))
        } else {
            startService(Intent(this, AuraVpnService::class.java))
        }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.let { tile ->
            val active = AuraVpnService.isRunning
            tile.label = if (active) "Aura Defensa activa" else "Aura Defensa"
            tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }
}
