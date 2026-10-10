package com.example.cplswellbeingbynovadevs.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String = "",
    val email: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val role: String = ROLE_USER, // "USER" or "ADMIN"
    @SerialName("account_status") val accountStatus: String = STATUS_ACTIVE, // "ACTIVE" or "DISABLED"
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("last_login_at") val lastLoginAt: String? = null
) {
    companion object {
        const val ROLE_USER = "USER"
        const val ROLE_ADMIN = "ADMIN"
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_DISABLED = "DISABLED"
    }
}

@Serializable
data class DeviceInfo(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("device_name") val deviceName: String = "",
    @SerialName("android_version") val androidVersion: String = "",
    @SerialName("app_version") val appVersion: String = "",
    @SerialName("battery_percentage") val batteryPercentage: Int = 100,
    @SerialName("is_charging") val isCharging: Boolean = false,
    @SerialName("sharing_enabled") val sharingEnabled: Boolean = true,
    @SerialName("background_status") val backgroundStatus: String = "ACTIVE",
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("last_sync_at") val lastSyncAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ConnectionRecord(
    val id: String = "",
    @SerialName("requester_user_id") val requesterUserId: String = "",
    @SerialName("receiver_user_id") val receiverUserId: String = "",
    val status: String = "ACTIVE", // "ACTIVE" or "DISCONNECTED"
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("accepted_at") val acceptedAt: String? = null,
    @SerialName("disconnected_at") val disconnectedAt: String? = null,
    @SerialName("partner_profile") val partnerProfile: UserProfile? = null,
    @SerialName("partner_device") val partnerDevice: DeviceInfo? = null
)

@Serializable
data class ConnectionCode(
    val id: String = "",
    @SerialName("creator_user_id") val creatorUserId: String = "",
    val code: String = "",
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("used_at") val usedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class UsageDaily(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("app_id") val appId: String = "", // package_name
    @SerialName("app_name") val appName: String = "",
    @SerialName("usage_date") val usageDate: String = "", // YYYY-MM-DD
    @SerialName("total_seconds") val totalSeconds: Long = 0,
    @SerialName("session_count") val sessionCount: Int = 0,
    @SerialName("first_used_at") val firstUsedAt: String? = null,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class UsageSession(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("app_id") val appId: String = "", // package_name
    @SerialName("app_name") val appName: String = "",
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("ended_at") val endedAt: String = "",
    @SerialName("duration_seconds") val durationSeconds: Long = 0,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class UpdateManifest(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val mandatory: Boolean = false,
    val title: String = "Update Available",
    val message: String = "A new update is available for CPL Wellbeing.",
    val releaseUrl: String = "",
    val apkUrl: String = ""
)

data class ConnectedPartner(
    val connectionId: String,
    val partnerId: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val statusText: String,
    val lastSyncAt: String?,
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val sharingEnabled: Boolean
)
