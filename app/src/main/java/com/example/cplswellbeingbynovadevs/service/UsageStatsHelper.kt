package com.example.cplswellbeingbynovadevs.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.example.cplswellbeingbynovadevs.data.model.UsageDaily
import com.example.cplswellbeingbynovadevs.data.model.UsageSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object UsageStatsHelper {

    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    suspend fun collectUsageData(
        context: Context,
        userId: String,
        deviceId: String,
        daysAgo: Int = 0
    ): Pair<List<UsageDaily>, List<UsageSession>> {
        if (!hasUsagePermission(context)) return Pair(emptyList(), emptyList())

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val dayEndTime = calendar.timeInMillis
        val endTime = if (daysAgo == 0) System.currentTimeMillis() else dayEndTime

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val usageDateStr = dateFormat.format(Date(startTime))

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

        val activeSessions = mutableMapOf<String, Long>()
        val sessionsList = mutableListOf<UsageSession>()
        val totalSecondsMap = mutableMapOf<String, Long>()
        val sessionCountMap = mutableMapOf<String, Int>()
        val firstUsedMap = mutableMapOf<String, Long>()
        val lastUsedMap = mutableMapOf<String, Long>()

        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val pkg = event.packageName ?: continue

            if (pkg == context.packageName || pkg == "com.android.systemui") continue

            val eventTime = event.timeStamp
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    activeSessions[pkg] = eventTime
                    sessionCountMap[pkg] = (sessionCountMap[pkg] ?: 0) + 1
                    if (!firstUsedMap.containsKey(pkg)) {
                        firstUsedMap[pkg] = eventTime
                    }
                    lastUsedMap[pkg] = eventTime
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = activeSessions.remove(pkg)
                    if (start != null && eventTime > start) {
                        val durationMs = eventTime - start
                        val durationSec = durationMs / 1000
                        if (durationSec > 0) {
                            totalSecondsMap[pkg] = (totalSecondsMap[pkg] ?: 0L) + durationSec

                            val appName = getAppName(context, pkg)
                            sessionsList.add(
                                UsageSession(
                                    id = UUID.randomUUID().toString(),
                                    userId = userId,
                                    deviceId = deviceId,
                                    appId = pkg,
                                    appName = appName,
                                    startedAt = isoFormat.format(Date(start)),
                                    endedAt = isoFormat.format(Date(eventTime)),
                                    durationSeconds = durationSec
                                )
                            )
                        }
                    }
                }
            }
        }

        for ((pkg, start) in activeSessions) {
            val durationSec = (endTime - start) / 1000
            if (durationSec > 0) {
                totalSecondsMap[pkg] = (totalSecondsMap[pkg] ?: 0L) + durationSec
                val appName = getAppName(context, pkg)
                sessionsList.add(
                    UsageSession(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        deviceId = deviceId,
                        appId = pkg,
                        appName = appName,
                        startedAt = isoFormat.format(Date(start)),
                        endedAt = isoFormat.format(Date(endTime)),
                        durationSeconds = durationSec
                    )
                )
            }
        }

        try {
            val statsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime)
            if (!statsList.isNullOrEmpty()) {
                for (stat in statsList) {
                    val pkg = stat.packageName ?: continue
                    if (pkg == context.packageName || pkg == "com.android.systemui") continue
                    val sec = stat.totalTimeInForeground / 1000
                    if (sec > 0) {
                        val existing = totalSecondsMap[pkg] ?: 0L
                        if (sec > existing) {
                            totalSecondsMap[pkg] = sec
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback
        }

        val dailyList = mutableListOf<UsageDaily>()
        for ((pkg, totalSec) in totalSecondsMap) {
            if (totalSec <= 0) continue
            val appName = getAppName(context, pkg)
            val firstSec = firstUsedMap[pkg]
            val lastSec = lastUsedMap[pkg]
            dailyList.add(
                UsageDaily(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    deviceId = deviceId,
                    appId = pkg,
                    appName = appName,
                    usageDate = usageDateStr,
                    totalSeconds = totalSec,
                    sessionCount = sessionCountMap[pkg] ?: 1,
                    firstUsedAt = firstSec?.let { isoFormat.format(Date(it)) },
                    lastUsedAt = lastSec?.let { isoFormat.format(Date(it)) }
                )
            )
        }

        return Pair(dailyList, sessionsList)
    }
}
