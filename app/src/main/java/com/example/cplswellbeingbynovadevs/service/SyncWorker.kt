package com.example.cplswellbeingbynovadevs.service

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cplswellbeingbynovadevs.data.local.AppDatabase
import com.example.cplswellbeingbynovadevs.data.local.LocalDeviceEntity
import com.example.cplswellbeingbynovadevs.data.local.LocalUsageDailyEntity
import com.example.cplswellbeingbynovadevs.data.local.LocalUsageSessionEntity
import com.example.cplswellbeingbynovadevs.data.model.DeviceInfo
import com.example.cplswellbeingbynovadevs.data.remote.SupabaseClient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker starting background synchronization...")
        val userId = SupabaseClient.currentUserId ?: return Result.success()
        val db = AppDatabase.getDatabase(context)
        val dao = db.appDao()

        return try {
            val prefs = context.getSharedPreferences("cpl_prefs", Context.MODE_PRIVATE)
            val isPauseSharing = prefs.getBoolean("is_pause_sharing", false)

            val batteryInfo = BatteryHelper.getBatteryInfo(context)
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val nowStr = isoFormat.format(Date())

            val deviceId = "device_${userId.take(8)}_${Build.MODEL.replace(" ", "_")}"
            val deviceInfo = DeviceInfo(
                id = deviceId,
                userId = userId,
                deviceName = Build.MODEL ?: "Android Device",
                androidVersion = Build.VERSION.RELEASE ?: "Android",
                appVersion = "1.0.0",
                batteryPercentage = batteryInfo.percentage,
                isCharging = batteryInfo.isCharging,
                sharingEnabled = !isPauseSharing,
                backgroundStatus = if (isPauseSharing) "PAUSED" else "ACTIVE",
                lastSeenAt = nowStr,
                lastSyncAt = nowStr
            )

            dao.insertDevice(
                LocalDeviceEntity(
                    id = deviceId,
                    userId = userId,
                    deviceName = deviceInfo.deviceName,
                    androidVersion = deviceInfo.androidVersion,
                    appVersion = deviceInfo.appVersion,
                    batteryPercentage = deviceInfo.batteryPercentage,
                    isCharging = deviceInfo.isCharging,
                    sharingEnabled = deviceInfo.sharingEnabled,
                    backgroundStatus = deviceInfo.backgroundStatus,
                    lastSeenAt = deviceInfo.lastSeenAt,
                    lastSyncAt = deviceInfo.lastSyncAt
                )
            )
            SupabaseClient.syncDevice(deviceInfo)

            val (todayDaily, todaySessions) = UsageStatsHelper.collectUsageData(context, userId, deviceId, daysAgo = 0)
            val (yesterdayDaily, yesterdaySessions) = UsageStatsHelper.collectUsageData(context, userId, deviceId, daysAgo = 1)

            val allDaily = todayDaily + yesterdayDaily
            val allSessions = todaySessions + yesterdaySessions

            if (allDaily.isNotEmpty()) {
                dao.insertUsageDaily(allDaily.map {
                    LocalUsageDailyEntity(
                        id = it.id,
                        userId = it.userId,
                        appId = it.appId,
                        appName = it.appName,
                        usageDate = it.usageDate,
                        totalSeconds = it.totalSeconds,
                        sessionCount = it.sessionCount,
                        firstUsedAt = it.firstUsedAt,
                        lastUsedAt = it.lastUsedAt
                    )
                })
                if (!isPauseSharing) {
                    SupabaseClient.uploadUsageDaily(allDaily)
                }
            }

            if (allSessions.isNotEmpty()) {
                dao.insertUsageSessions(allSessions.map {
                    LocalUsageSessionEntity(
                        id = it.id,
                        userId = it.userId,
                        appId = it.appId,
                        appName = it.appName,
                        startedAt = it.startedAt,
                        endedAt = it.endedAt,
                        durationSeconds = it.durationSeconds
                    )
                })
                if (!isPauseSharing) {
                    SupabaseClient.uploadUsageSessions(allSessions)
                }
            }

            val cutoffCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cutoffDateStr = dateFormat.format(cutoffCal.time)
            val cutoffIsoStr = isoFormat.format(cutoffCal.time)

            dao.pruneOldUsageDaily(cutoffDateStr)
            dao.pruneOldUsageSessions(cutoffIsoStr)

            Log.d(TAG, "SyncWorker background synchronization completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker error", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "CplWellbeingSyncWorker"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
