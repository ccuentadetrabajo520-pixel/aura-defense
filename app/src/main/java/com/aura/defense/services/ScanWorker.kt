package com.aura.defense.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aura.defense.data.AppScannerRepository
import com.aura.defense.security.EncryptedVault

class ScanWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        return runCatching {
            val apps = AppScannerRepository(applicationContext).scanVisibleApps()
            val report = buildString {
                append("Escaneo de aplicaciones: ")
                append(apps.size)
                append(" aplicaciones. ")
                append(apps.count { it.hasSensitivePermissions })
                append(" con permisos sensibles.")
            }
            EncryptedVault(applicationContext).saveEncryptedData(
                "last_scan_log",
                "${System.currentTimeMillis()}|$report"
            )
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
