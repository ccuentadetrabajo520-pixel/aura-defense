package com.anonymous.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileDescriptor
import java.nio.ByteBuffer

class AuraVpnService : VpnService() {
    private var tunInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        tunInterface?.close()
        tunInterface = null
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = super.onBind(intent)

    fun establishTunnel(): Boolean {
        val builder = Builder()
        builder.setSession("AuraDefense")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)

        tunInterface = builder.establish()
        return tunInterface != null
    }

    private fun buildForegroundNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AuraDefense protection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("AuraDefense active")
            .setContentText("VPN protection and telemetry monitoring are active.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "aura_defense_vpn"
        const val NOTIFICATION_ID = 42
    }
}
