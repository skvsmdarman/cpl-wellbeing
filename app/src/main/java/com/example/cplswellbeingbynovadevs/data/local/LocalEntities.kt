package com.example.cplswellbeingbynovadevs.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_profiles")
data class LocalProfileEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: String,
    val accountStatus: String,
    val lastLoginAt: String?
)

@Entity(tableName = "local_devices")
data class LocalDeviceEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val deviceName: String,
    val androidVersion: String,
    val appVersion: String,
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val sharingEnabled: Boolean,
    val backgroundStatus: String,
    val lastSeenAt: String?,
    val lastSyncAt: String?
)

@Entity(tableName = "local_usage_daily")
data class LocalUsageDailyEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val appId: String,
    val appName: String,
    val usageDate: String,
    val totalSeconds: Long,
    val sessionCount: Int,
    val firstUsedAt: String?,
    val lastUsedAt: String?
)

@Entity(tableName = "local_usage_sessions")
data class LocalUsageSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val appId: String,
    val appName: String,
    val startedAt: String,
    val endedAt: String,
    val durationSeconds: Long
)

@Entity(tableName = "local_connections")
data class LocalConnectionEntity(
    @PrimaryKey val id: String,
    val partnerId: String,
    val partnerEmail: String,
    val partnerDisplayName: String,
    val partnerAvatarUrl: String?,
    val status: String,
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val lastSeenAt: String?,
    val lastSyncAt: String?,
    val sharingEnabled: Boolean
)
