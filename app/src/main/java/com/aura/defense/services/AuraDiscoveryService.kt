package com.aura.defense.services

import android.content.Context
import android.net.wifi.WifiManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
data class DiscoveredAura(
    val id: String,
    val status: String,
    val timestamp: Long,
    val lastSeen: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
)

class AuraDiscoveryService(context: Context) {
    companion object {
        private const val DISCOVERY_PORT = 45454
        private const val MULTICAST_ADDRESS = "239.255.42.99"
        private const val PACKET_BUFFER_SIZE = 2048
        private const val EXPIRY_MS = 30_000L
    }

    private val appContext = context.applicationContext
    private val running = AtomicBoolean(false)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val discovered = LinkedHashMap<String, DiscoveredAura>()
    private val lock = Any()
    private val listeners = mutableSetOf<(List<DiscoveredAura>) -> Unit>()
    private var multicastSocket: MulticastSocket? = null
    private var sendSocket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var shareLocation = false

    fun addListener(listener: (List<DiscoveredAura>) -> Unit) {
        synchronized(lock) { listeners += listener }
        listener(discoveredAuras())
    }

    fun removeListener(listener: (List<DiscoveredAura>) -> Unit) {
        synchronized(lock) { listeners -= listener }
    }

    fun setShareLocation(enabled: Boolean) {
        shareLocation = enabled
        if (enabled && running.get()) {
            executor.execute { sendAnnouncement() }
        }
    }

    fun discoveredAuras(): List<DiscoveredAura> = synchronized(lock) {
        discovered.values.sortedByDescending { it.lastSeen }
    }

    fun startDiscovery() {
        if (!running.compareAndSet(false, true)) return
        acquireMulticastLock()
        executor.execute {
            try {
                openSockets()
                sendAnnouncement()
                listenForAnnouncements()
            } finally {
                closeSockets()
                releaseMulticastLock()
            }
        }
    }

    fun stopDiscovery() {
        if (!running.compareAndSet(true, false)) return
        multicastSocket?.close()
        sendSocket?.close()
        synchronized(lock) { discovered.clear() }
        notifyListeners()
    }

    fun close() {
        stopDiscovery()
        executor.shutdownNow()
    }

    private fun openSockets() {
        sendSocket = DatagramSocket()
        multicastSocket = MulticastSocket(DISCOVERY_PORT).apply {
            reuseAddress = true
            joinGroup(InetSocketAddress(InetAddress.getByName(MULTICAST_ADDRESS), DISCOVERY_PORT), null)
            soTimeout = 1_000
        }
    }

    private fun sendAnnouncement() {
        val payload = buildPayload().toByteArray(Charsets.UTF_8)
        val address = InetAddress.getByName(MULTICAST_ADDRESS)
        sendSocket?.send(DatagramPacket(payload, payload.size, address, DISCOVERY_PORT))
    }

    private fun listenForAnnouncements() {
        val buffer = ByteArray(PACKET_BUFFER_SIZE)
        while (running.get()) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                multicastSocket?.receive(packet)
                parsePayload(String(packet.data, packet.offset, packet.length, Charsets.UTF_8))?.let { aura ->
                    if (aura.id != localAuraId()) {
                        synchronized(lock) { discovered[aura.id] = aura }
                        notifyListeners()
                    }
                }
                removeExpired()
            } catch (_: java.net.SocketTimeoutException) {
                removeExpired()
            } catch (_: java.net.SocketException) {
                if (running.get()) continue
            }
        }
    }

    private fun buildPayload(): String {
        val id = localAuraId()
        val timestamp = System.currentTimeMillis()
        val location = if (shareLocation) ",\"LATITUDE\":0.0,\"LONGITUDE\":0.0" else ""
        return "{\"AURA_ID\":\"$id\",\"AURA_STATUS\":\"VIVO\",\"TIMESTAMP\":$timestamp$location}"
    }

    private fun parsePayload(payload: String): DiscoveredAura? {
        val id = field(payload, "AURA_ID") ?: return null
        val status = field(payload, "AURA_STATUS") ?: return null
        val timestamp = field(payload, "TIMESTAMP")?.toLongOrNull() ?: return null
        val latitude = field(payload, "LATITUDE")?.toDoubleOrNull()
        val longitude = field(payload, "LONGITUDE")?.toDoubleOrNull()
        return DiscoveredAura(id, status, timestamp, System.currentTimeMillis(), latitude, longitude)
    }

    private fun field(payload: String, name: String): String? {
        val quoted = Regex("\\\"$name\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").find(payload)?.groupValues?.get(1)
        return quoted ?: Regex("\\\"$name\\\"\\s*:\\s*([-+0-9.eE]+)").find(payload)?.groupValues?.get(1)
    }

    private fun removeExpired() {
        val now = System.currentTimeMillis()
        synchronized(lock) { discovered.entries.removeIf { now - it.value.lastSeen > EXPIRY_MS } }
        notifyListeners()
    }

    private fun notifyListeners() {
        val snapshot = discoveredAuras()
        synchronized(lock) { listeners.toList() }.forEach { it(snapshot) }
    }

    private fun localAuraId(): String = appContext.getSharedPreferences("aura_discovery", Context.MODE_PRIVATE)
        .getString("aura_id", null)
        ?: "AURA-${UUID.randomUUID().toString().take(8).uppercase()}".also {
            appContext.getSharedPreferences("aura_discovery", Context.MODE_PRIVATE).edit().putString("aura_id", it).apply()
        }

    private fun acquireMulticastLock() {
        val manager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = manager.createMulticastLock("aura-discovery").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.takeIf { it.isHeld }?.release()
        multicastLock = null
    }

    private fun closeSockets() {
        multicastSocket?.close()
        sendSocket?.close()
        multicastSocket = null
        sendSocket = null
    }
}
