package com.aura.defense

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
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
    }

    companion object {
        @Volatile
        var isConnected: Boolean = false
            private set
    }
}
