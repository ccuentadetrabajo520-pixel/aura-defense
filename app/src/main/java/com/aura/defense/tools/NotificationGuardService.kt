package com.aura.defense.tools

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationGuardService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        isConnected = false
        super.onListenerDisconnected()
        sendAlert("Acceso a Notification Guard no concedido o desconectado.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val text = listOf(
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_TITLE)
        ).filterNotNull().joinToString(" ").trim()

        if (text.isNotBlank()) {
            val verdict = analyzeUrl(text)
            if (verdict.startsWith("Peligroso") || verdict.startsWith("Sospechoso")) {
                sendAlert("Posible riesgo detectado: $verdict")
            }
        }
    }

    private fun sendAlert(message: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Alertas de Notification Guard",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Aura Notification Guard")
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(message.hashCode(), notification) }
    }

    companion object {
        private const val CHANNEL_ID = "notification_guard_alerts"

        @Volatile
        var isConnected: Boolean = false
            private set
    }
}
