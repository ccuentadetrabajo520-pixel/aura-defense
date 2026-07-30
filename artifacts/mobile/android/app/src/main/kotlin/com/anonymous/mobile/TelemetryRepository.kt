package com.anonymous.mobile

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class TelemetryRepository(private val context: Context) {
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            _batteryState.value = BatteryState(
                level = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100f).toInt() else null,
                temperatureC = if (temp >= 0) temp / 10f else null,
                voltageMv = if (voltage >= 0) voltage else null
            )
        }
    }

    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState

    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun readMemoryInfo(): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryClass()
        activityManager.getMemoryInfo(memoryInfo)
        return MemoryInfo(
            totalRamMb = memoryInfo.totalMem / (1024L * 1024L),
            availableRamMb = memoryInfo.availMem / (1024L * 1024L),
            lowMemory = memoryInfo.lowMemory
        )
    }

    fun readInternalStorage(): StorageInfo {
        val path = Environment.getDataDirectory()
        val statFs = StatFs(path.absolutePath)
        val availableBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            statFs.availableBytes
        } else {
            statFs.availableBlocks.toLong() * statFs.blockSize.toLong()
        }
        val totalBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            statFs.blockCountLong * statFs.blockSizeLong
        } else {
            statFs.blockCount.toLong() * statFs.blockSize.toLong()
        }
        return StorageInfo(
            totalBytes = totalBytes,
            availableBytes = availableBytes
        )
    }

    fun close() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver already unregistered.
        }
    }
}

data class BatteryState(
    val level: Int? = null,
    val temperatureC: Float? = null,
    val voltageMv: Int? = null
)

data class MemoryInfo(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val lowMemory: Boolean
)

data class StorageInfo(
    val totalBytes: Long,
    val availableBytes: Long
)