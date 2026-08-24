package com.aura.defense.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aura.defense.services.ScanWorker
import java.util.concurrent.TimeUnit

enum class ScanSchedule(val label: String, val intervalDays: Long) {
    DAILY("Diario", 1),
    WEEKLY("Semanal", 7),
    DISABLED("Desactivado", 0)
}

object ScanScheduler {
    private const val WORK_NAME = "aura_scheduled_app_scan"
    private const val PREFERENCES = "scan_scheduler"
    private const val SCHEDULE_KEY = "schedule"

    fun getSchedule(context: Context): ScanSchedule {
        val value = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(SCHEDULE_KEY, ScanSchedule.DISABLED.name)
        return runCatching { ScanSchedule.valueOf(value.orEmpty()) }.getOrDefault(ScanSchedule.DISABLED)
    }

    fun setSchedule(context: Context, schedule: ScanSchedule) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit().putString(SCHEDULE_KEY, schedule.name).apply()
        val workManager = WorkManager.getInstance(context)
        if (schedule == ScanSchedule.DISABLED) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<ScanWorker>(schedule.intervalDays, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
