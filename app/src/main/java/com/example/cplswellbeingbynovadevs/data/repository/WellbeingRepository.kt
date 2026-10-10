package com.example.cplswellbeingbynovadevs.data.repository

import android.content.Context
import com.example.cplswellbeingbynovadevs.data.local.AppDatabase
import com.example.cplswellbeingbynovadevs.data.local.LocalConnectionEntity
import com.example.cplswellbeingbynovadevs.data.model.ConnectedPartner
import com.example.cplswellbeingbynovadevs.data.model.ConnectionRecord
import com.example.cplswellbeingbynovadevs.data.model.UpdateManifest
import com.example.cplswellbeingbynovadevs.data.model.UsageDaily
import com.example.cplswellbeingbynovadevs.data.model.UsageSession
import com.example.cplswellbeingbynovadevs.data.model.UserProfile
import com.example.cplswellbeingbynovadevs.data.remote.SupabaseClient
import com.example.cplswellbeingbynovadevs.data.remote.UpdateApi
import com.example.cplswellbeingbynovadevs.service.SyncWorker
import com.example.cplswellbeingbynovadevs.service.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class WellbeingRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.appDao()
    private val prefs = context.getSharedPreferences("cpl_prefs", Context.MODE_PRIVATE)

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private val _connectedPartners = MutableStateFlow<List<ConnectedPartner>>(emptyList())
    val connectedPartners: StateFlow<List<ConnectedPartner>> = _connectedPartners.asStateFlow()

    private val _selectedPartner = MutableStateFlow<ConnectedPartner?>(null)
    val selectedPartner: StateFlow<ConnectedPartner?> = _selectedPartner.asStateFlow()

    private val _isPauseSharing = MutableStateFlow(prefs.getBoolean("is_pause_sharing", false))
    val isPauseSharing: StateFlow<Boolean> = _isPauseSharing.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val token = prefs.getString("access_token", null)
        val userId = prefs.getString("user_id", null)
        val email = prefs.getString("user_email", null)
        val role = prefs.getString("user_role", UserProfile.ROLE_USER) ?: UserProfile.ROLE_USER

        if (token != null && userId != null) {
            SupabaseClient.currentAccessToken = token
            SupabaseClient.currentUserId = userId
            SupabaseClient.currentUserEmail = email
            SupabaseClient.currentUserRole = role

            _currentUserProfile.value = UserProfile(
                id = userId,
                email = email ?: "",
                displayName = email?.substringBefore("@") ?: "User",
                role = role
            )
            SyncWorker.schedulePeriodicSync(context)
        }
    }

    private fun saveSession(token: String?, userId: String?, email: String?, role: String) {
        prefs.edit()
            .putString("access_token", token)
            .putString("user_id", userId)
            .putString("user_email", email)
            .putString("user_role", role)
            .apply()
    }

    fun togglePauseSharing() {
        val newValue = !_isPauseSharing.value
        _isPauseSharing.value = newValue
        prefs.edit().putBoolean("is_pause_sharing", newValue).apply()
    }

    suspend fun signIn(email: String, pass: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            val result = SupabaseClient.signIn(email, pass)
            result.onSuccess { profile ->
                _currentUserProfile.value = profile
                saveSession(SupabaseClient.currentAccessToken, profile.id, profile.email, profile.role)
                SyncWorker.schedulePeriodicSync(context)
                refreshPartners()
            }
            result
        }
    }

    suspend fun signUp(email: String, pass: String, displayName: String, role: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            val result = SupabaseClient.signUp(email, pass, displayName, role)
            result.onSuccess { profile ->
                _currentUserProfile.value = profile
                saveSession(SupabaseClient.currentAccessToken, profile.id, profile.email, profile.role)
                SyncWorker.schedulePeriodicSync(context)
                refreshPartners()
            }
            result
        }
    }

    suspend fun updateEmail(newEmail: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            val res = SupabaseClient.updateEmail(newEmail)
            res.onSuccess {
                saveSession(SupabaseClient.currentAccessToken, SupabaseClient.currentUserId, newEmail, SupabaseClient.currentUserRole)
                _currentUserProfile.value = _currentUserProfile.value?.copy(email = newEmail)
            }
            res
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            SupabaseClient.updatePassword(newPassword)
        }
    }

    fun signOut() {
        SupabaseClient.currentAccessToken = null
        SupabaseClient.currentUserId = null
        SupabaseClient.currentUserEmail = null
        _currentUserProfile.value = null
        _connectedPartners.value = emptyList()
        _selectedPartner.value = null
        saveSession(null, null, null, UserProfile.ROLE_USER)
    }

    suspend fun refreshPartners(): List<ConnectedPartner> {
        return withContext(Dispatchers.IO) {
            val userId = SupabaseClient.currentUserId ?: return@withContext emptyList()
            val partnerList = mutableListOf<ConnectedPartner>()

            try {
                val connections = SupabaseClient.fetchActiveConnections(userId)
                for (conn in connections) {
                    val partnerId = if (conn.requesterUserId == userId) conn.receiverUserId else conn.requesterUserId
                    val partnerProfile = SupabaseClient.fetchProfile(partnerId)
                    val partnerDevice = SupabaseClient.fetchPartnerDevice(partnerId)

                    val displayName = partnerProfile?.displayName?.ifEmpty { "Partner" } ?: "Partner"
                    val email = partnerProfile?.email ?: ""
                    val avatarUrl = partnerProfile?.avatarUrl

                    val batteryPct = partnerDevice?.batteryPercentage ?: 100
                    val isCharging = partnerDevice?.isCharging ?: false
                    val lastSeenAt = partnerDevice?.lastSeenAt
                    val lastSyncAt = partnerDevice?.lastSyncAt
                    val sharingEnabled = partnerDevice?.sharingEnabled ?: true

                    val (isOnline, statusText) = computeOnlineStatus(lastSeenAt)

                    val partner = ConnectedPartner(
                        connectionId = conn.id,
                        partnerId = partnerId,
                        email = email,
                        displayName = displayName,
                        avatarUrl = avatarUrl,
                        isOnline = isOnline,
                        statusText = statusText,
                        lastSyncAt = lastSyncAt,
                        batteryPercentage = batteryPct,
                        isCharging = isCharging,
                        sharingEnabled = sharingEnabled
                    )
                    partnerList.add(partner)
                }

                if (partnerList.isNotEmpty()) {
                    dao.insertConnections(partnerList.map { p ->
                        LocalConnectionEntity(
                            id = p.connectionId,
                            partnerId = p.partnerId,
                            partnerEmail = p.email,
                            partnerDisplayName = p.displayName,
                            partnerAvatarUrl = p.avatarUrl,
                            status = "ACTIVE",
                            batteryPercentage = p.batteryPercentage,
                            isCharging = p.isCharging,
                            lastSeenAt = null,
                            lastSyncAt = p.lastSyncAt,
                            sharingEnabled = p.sharingEnabled
                        )
                    })
                }
            } catch (e: Exception) {
                val cached = dao.getActiveConnections()
                for (c in cached) {
                    partnerList.add(
                        ConnectedPartner(
                            connectionId = c.id,
                            partnerId = c.partnerId,
                            email = c.partnerEmail,
                            displayName = c.partnerDisplayName,
                            avatarUrl = c.partnerAvatarUrl,
                            isOnline = false,
                            statusText = "Cached offline data",
                            lastSyncAt = c.lastSyncAt,
                            batteryPercentage = c.batteryPercentage,
                            isCharging = c.isCharging,
                            sharingEnabled = c.sharingEnabled
                        )
                    )
                }
            }

            _connectedPartners.value = partnerList
            if (_selectedPartner.value == null && partnerList.isNotEmpty()) {
                _selectedPartner.value = partnerList.first()
            }
            partnerList
        }
    }

    fun selectPartner(partner: ConnectedPartner?) {
        _selectedPartner.value = partner
    }

    private fun computeOnlineStatus(lastSeenIso: String?): Pair<Boolean, String> {
        if (lastSeenIso == null) return Pair(false, "Offline / no recent sync")
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = isoFormat.parse(lastSeenIso) ?: return Pair(false, "Offline / no recent sync")
            val diffMs = System.currentTimeMillis() - date.time
            val diffMinutes = diffMs / (1000 * 60)

            when {
                diffMinutes < 5 -> Pair(true, "🟢 Active now")
                diffMinutes < 60 -> Pair(false, "🟡 Last seen $diffMinutes min ago")
                diffMinutes < 1440 -> Pair(false, "🔴 Last seen ${diffMinutes / 60}h ago")
                else -> Pair(false, "🔴 Offline / no recent sync")
            }
        } catch (e: Exception) {
            Pair(false, "Offline / no recent sync")
        }
    }

    suspend fun generateConnectionCode(): Result<String> {
        val userId = SupabaseClient.currentUserId ?: return Result.failure(Exception("User not authenticated"))
        return withContext(Dispatchers.IO) {
            SupabaseClient.generateConnectionCode(userId)
        }
    }

    suspend fun acceptConnectionCode(code: String): Result<ConnectionRecord> {
        val userId = SupabaseClient.currentUserId ?: return Result.failure(Exception("User not authenticated"))
        return withContext(Dispatchers.IO) {
            val res = SupabaseClient.acceptConnectionCode(userId, code)
            res.onSuccess {
                refreshPartners()
            }
            res
        }
    }

    suspend fun disconnectPartner(connectionId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val success = SupabaseClient.disconnectConnection(connectionId)
            if (success) {
                dao.deleteConnection(connectionId)
                refreshPartners()
                if (_selectedPartner.value?.connectionId == connectionId) {
                    _selectedPartner.value = _connectedPartners.value.firstOrNull()
                }
            }
            success
        }
    }

    suspend fun getUsageDailyForTarget(targetUserId: String, daysAgo: Int = 0): List<UsageDaily> {
        return withContext(Dispatchers.IO) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = dateFormat.format(cal.time)

            if (targetUserId == SupabaseClient.currentUserId) {
                val deviceId = "device_local"
                val (dailyList, _) = UsageStatsHelper.collectUsageData(context, targetUserId, deviceId, daysAgo)
                dailyList.sortedByDescending { it.totalSeconds }
            } else {
                SupabaseClient.fetchPartnerUsageDaily(targetUserId, dateStr)
            }
        }
    }

    suspend fun getUsageSessionsForTarget(targetUserId: String, daysAgo: Int = 0): List<UsageSession> {
        return withContext(Dispatchers.IO) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = dateFormat.format(cal.time)

            if (targetUserId == SupabaseClient.currentUserId) {
                val deviceId = "device_local"
                val (_, sessionsList) = UsageStatsHelper.collectUsageData(context, targetUserId, deviceId, daysAgo)
                sessionsList.sortedByDescending { it.startedAt }
            } else {
                SupabaseClient.fetchPartnerUsageSessions(targetUserId, dateStr)
            }
        }
    }

    suspend fun adminFetchAllUsers(): List<UserProfile> {
        return withContext(Dispatchers.IO) {
            SupabaseClient.fetchAllProfilesForAdmin()
        }
    }

    suspend fun adminUpdateUserProfile(profile: UserProfile): Boolean {
        return withContext(Dispatchers.IO) {
            SupabaseClient.updateProfileByAdmin(profile)
        }
    }

    suspend fun adminCreateUser(email: String, pass: String, displayName: String, role: String): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            SupabaseClient.signUp(email, pass, displayName, role)
        }
    }

    suspend fun checkForAppUpdate(): UpdateManifest? {
        return withContext(Dispatchers.IO) {
            UpdateApi.checkForUpdates()
        }
    }
}
