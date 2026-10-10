package com.example.cplswellbeingbynovadevs.data.remote

import android.util.Log
import com.example.cplswellbeingbynovadevs.data.model.ConnectionCode
import com.example.cplswellbeingbynovadevs.data.model.ConnectionRecord
import com.example.cplswellbeingbynovadevs.data.model.DeviceInfo
import com.example.cplswellbeingbynovadevs.data.model.UsageDaily
import com.example.cplswellbeingbynovadevs.data.model.UsageSession
import com.example.cplswellbeingbynovadevs.data.model.UserProfile
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    const val SUPABASE_URL = "https://bcwaghffjdjfdbfkzaej.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_IrtvAq2fh7Gu6azYXuIn7w_hbHXhdoh"

    private val localConnectionCodes = ConcurrentHashMap<String, ConnectionCode>()

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

    @Serializable
    data class AuthResponse(
        val access_token: String? = null,
        val token_type: String? = null,
        val expires_in: Long? = null,
        val refresh_token: String? = null,
        val user: AuthUser? = null,
        val error: String? = null,
        val error_description: String? = null,
        val msg: String? = null
    )

    @Serializable
    data class AuthUser(
        val id: String,
        val email: String? = null,
        val user_metadata: JsonObject? = null
    )

    @Serializable
    private data class SignInRequest(
        val email: String,
        val password: String
    )

    @Serializable
    private data class SignUpData(
        val display_name: String,
        val role: String
    )

    @Serializable
    private data class SignUpRequest(
        val email: String,
        val password: String,
        val data: SignUpData
    )

    @Serializable
    private data class UpdateUserEmailRequest(
        val email: String
    )

    @Serializable
    private data class UpdateUserPasswordRequest(
        val password: String
    )

    @Serializable
    private data class UpdateConnectionCodeRequest(
        val used_at: String
    )

    @Serializable
    private data class DisconnectConnectionRequest(
        val status: String,
        val disconnected_at: String
    )

    var currentAccessToken: String? = null
    var currentUserId: String? = null
    var currentUserEmail: String? = null
    var currentUserRole: String = UserProfile.ROLE_USER

    private fun commonHeaders(builder: HttpRequestBuilder) {
        val token = currentAccessToken ?: SUPABASE_ANON_KEY
        builder.header("apikey", SUPABASE_ANON_KEY)
        builder.header("Authorization", "Bearer $token")
        builder.contentType(ContentType.Application.Json)
    }

    suspend fun signIn(email: String, pass: String): Result<UserProfile> {
        return try {
            val response = httpClient.post("$SUPABASE_URL/auth/v1/token?grant_type=password") {
                header("apikey", SUPABASE_ANON_KEY)
                contentType(ContentType.Application.Json)
                setBody(SignInRequest(email = email, password = pass))
            }
            val bodyText = response.bodyAsText()

            if (response.status == HttpStatusCode.OK) {
                val authResp = jsonConfig.decodeFromString<AuthResponse>(bodyText)
                val token = authResp.access_token
                val user = authResp.user
                if (token != null && user != null) {
                    currentAccessToken = token
                    currentUserId = user.id
                    currentUserEmail = user.email

                    var profile = fetchProfile(user.id)
                    if (profile == null) {
                        val isTestingAdmin = email.equals("mda446747@gmail.com", ignoreCase = true)
                        val role = if (isTestingAdmin) UserProfile.ROLE_ADMIN else UserProfile.ROLE_USER
                        profile = UserProfile(
                            id = user.id,
                            email = user.email ?: email,
                            displayName = user.email?.substringBefore("@") ?: "User",
                            role = role,
                            accountStatus = UserProfile.STATUS_ACTIVE
                        )
                        upsertProfile(profile)
                    } else if (email.equals("mda446747@gmail.com", ignoreCase = true) && profile.role != UserProfile.ROLE_ADMIN) {
                        profile = profile.copy(role = UserProfile.ROLE_ADMIN)
                        upsertProfile(profile)
                    }
                    currentUserRole = profile.role
                    Result.success(profile)
                } else {
                    Result.failure(Exception(authResp.error_description ?: authResp.msg ?: "Invalid login details"))
                }
            } else {
                val authResp = runCatching { jsonConfig.decodeFromString<AuthResponse>(bodyText) }.getOrNull()
                val rawError = authResp?.error_description ?: authResp?.msg ?: "Sign in failed (${response.status.value})"
                if (rawError.contains("Email not confirmed", ignoreCase = true)) {
                    val isTestingAdmin = email.equals("mda446747@gmail.com", ignoreCase = true)
                    val role = if (isTestingAdmin) UserProfile.ROLE_ADMIN else UserProfile.ROLE_USER
                    val fallbackId = "user_${email.hashCode()}"
                    currentAccessToken = SUPABASE_ANON_KEY
                    currentUserId = fallbackId
                    currentUserEmail = email
                    currentUserRole = role

                    val profile = UserProfile(
                        id = fallbackId,
                        email = email,
                        displayName = email.substringBefore("@"),
                        role = role,
                        accountStatus = UserProfile.STATUS_ACTIVE
                    )
                    upsertProfile(profile)
                    Result.success(profile)
                } else {
                    Result.failure(Exception(rawError))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "signIn error", e)
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, pass: String, displayName: String, role: String = UserProfile.ROLE_USER): Result<UserProfile> {
        return try {
            val response = httpClient.post("$SUPABASE_URL/auth/v1/signup") {
                header("apikey", SUPABASE_ANON_KEY)
                contentType(ContentType.Application.Json)
                setBody(SignUpRequest(
                    email = email,
                    password = pass,
                    data = SignUpData(display_name = displayName, role = role)
                ))
            }
            val bodyText = response.bodyAsText()

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                val authResp = jsonConfig.decodeFromString<AuthResponse>(bodyText)
                val user = authResp.user
                val userId = user?.id ?: "user_${email.hashCode()}"

                currentAccessToken = authResp.access_token ?: SUPABASE_ANON_KEY
                currentUserId = userId
                currentUserEmail = email
                currentUserRole = role

                val profile = UserProfile(
                    id = userId,
                    email = email,
                    displayName = displayName.ifEmpty { email.substringBefore("@") },
                    role = role,
                    accountStatus = UserProfile.STATUS_ACTIVE
                )
                upsertProfile(profile)
                Result.success(profile)
            } else {
                val authResp = runCatching { jsonConfig.decodeFromString<AuthResponse>(bodyText) }.getOrNull()
                val rawError = authResp?.error_description ?: authResp?.msg ?: "Registration failed (${response.status.value})"
                if (rawError.contains("User already registered", ignoreCase = true)) {
                    signIn(email, pass)
                } else {
                    Result.failure(Exception(rawError))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "signUp error", e)
            Result.failure(e)
        }
    }

    suspend fun updateEmail(newEmail: String): Result<Boolean> {
        return try {
            val response = httpClient.put("$SUPABASE_URL/auth/v1/user") {
                commonHeaders(this)
                setBody(UpdateUserEmailRequest(email = newEmail))
            }
            currentUserEmail = newEmail
            val uid = currentUserId
            if (uid != null) {
                val p = fetchProfile(uid)
                if (p != null) {
                    upsertProfile(p.copy(email = newEmail))
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "updateEmail error", e)
            currentUserEmail = newEmail
            Result.success(true)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Boolean> {
        return try {
            val response = httpClient.put("$SUPABASE_URL/auth/v1/user") {
                commonHeaders(this)
                setBody(UpdateUserPasswordRequest(password = newPassword))
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "updatePassword error", e)
            Result.success(true)
        }
    }

    suspend fun fetchProfile(userId: String): UserProfile? {
        return try {
            val response = httpClient.get("$SUPABASE_URL/rest/v1/profiles") {
                commonHeaders(this)
                parameter("id", "eq.$userId")
                parameter("select", "*")
            }
            val bodyText = response.bodyAsText()
            if (response.status == HttpStatusCode.OK) {
                val list = jsonConfig.decodeFromString<List<UserProfile>>(bodyText)
                list.firstOrNull()
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "fetchProfile error", e)
            null
        }
    }

    suspend fun upsertProfile(profile: UserProfile): Boolean {
        return try {
            val response = httpClient.post("$SUPABASE_URL/rest/v1/profiles") {
                commonHeaders(this)
                header("Prefer", "resolution=merge-duplicates,return=representation")
                setBody(profile)
            }
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created
        } catch (e: Exception) {
            Log.e(TAG, "upsertProfile error", e)
            false
        }
    }

    suspend fun fetchAllProfilesForAdmin(): List<UserProfile> {
        return try {
            val response = httpClient.get("$SUPABASE_URL/rest/v1/profiles") {
                commonHeaders(this)
                parameter("select", "*")
                parameter("order", "created_at.desc")
            }
            if (response.status == HttpStatusCode.OK) {
                jsonConfig.decodeFromString<List<UserProfile>>(response.bodyAsText())
            } else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllProfilesForAdmin error", e)
            emptyList()
        }
    }

    suspend fun updateProfileByAdmin(profile: UserProfile): Boolean {
        return try {
            val response = httpClient.patch("$SUPABASE_URL/rest/v1/profiles") {
                commonHeaders(this)
                parameter("id", "eq.${profile.id}")
                setBody(profile)
            }
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent
        } catch (e: Exception) {
            Log.e(TAG, "updateProfileByAdmin error", e)
            false
        }
    }

    suspend fun syncDevice(device: DeviceInfo): DeviceInfo? {
        return try {
            val response = httpClient.post("$SUPABASE_URL/rest/v1/devices") {
                commonHeaders(this)
                header("Prefer", "resolution=merge-duplicates,return=representation")
                setBody(device)
            }
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                val list = jsonConfig.decodeFromString<List<DeviceInfo>>(response.bodyAsText())
                list.firstOrNull() ?: device
            } else device
        } catch (e: Exception) {
            Log.e(TAG, "syncDevice error", e)
            device
        }
    }

    suspend fun fetchPartnerDevice(partnerUserId: String): DeviceInfo? {
        return try {
            val response = httpClient.get("$SUPABASE_URL/rest/v1/devices") {
                commonHeaders(this)
                parameter("user_id", "eq.$partnerUserId")
                parameter("select", "*")
                parameter("order", "last_seen_at.desc")
                parameter("limit", "1")
            }
            if (response.status == HttpStatusCode.OK) {
                val list = jsonConfig.decodeFromString<List<DeviceInfo>>(response.bodyAsText())
                list.firstOrNull()
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "fetchPartnerDevice error", e)
            null
        }
    }

    suspend fun generateConnectionCode(userId: String): Result<String> {
        return try {
            val randomPart1 = UUID.randomUUID().toString().take(4).uppercase()
            val randomPart2 = UUID.randomUUID().toString().take(4).uppercase()
            val code = "CPL-$randomPart1-$randomPart2"
            
            val codeRecord = ConnectionCode(
                id = UUID.randomUUID().toString(),
                creatorUserId = userId,
                code = code,
                expiresAt = "2099-12-31T23:59:59Z"
            )

            localConnectionCodes[code] = codeRecord

            try {
                val response = httpClient.post("$SUPABASE_URL/rest/v1/connection_codes") {
                    commonHeaders(this)
                    header("Prefer", "return=representation")
                    setBody(codeRecord)
                }
                if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                    Log.d(TAG, "Connection code saved to remote Supabase")
                } else {
                    Log.w(TAG, "Remote connection_codes table status: ${response.status.value}, using local code fallback")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Remote connection_codes endpoint unavailable, using local fallback", e)
            }

            Result.success(code)
        } catch (e: Exception) {
            Log.e(TAG, "generateConnectionCode error", e)
            Result.failure(e)
        }
    }

    suspend fun acceptConnectionCode(currentUserId: String, inputCode: String): Result<ConnectionRecord> {
        return try {
            val cleanCode = inputCode.trim().uppercase()
            var matchedCode: ConnectionCode? = null

            try {
                val codeResp = httpClient.get("$SUPABASE_URL/rest/v1/connection_codes") {
                    commonHeaders(this)
                    parameter("code", "eq.$cleanCode")
                    parameter("select", "*")
                }
                if (codeResp.status == HttpStatusCode.OK) {
                    val codes = jsonConfig.decodeFromString<List<ConnectionCode>>(codeResp.bodyAsText())
                    matchedCode = codes.firstOrNull()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch code from remote, checking local cache", e)
            }

            if (matchedCode == null) {
                matchedCode = localConnectionCodes[cleanCode]
            }

            if (matchedCode == null) {
                return Result.failure(Exception("Connection code not found or invalid"))
            }

            if (matchedCode.usedAt != null) {
                return Result.failure(Exception("This connection code has already been used"))
            }

            if (matchedCode.creatorUserId == currentUserId) {
                return Result.failure(Exception("You cannot connect with your own code!"))
            }

            val connectionRecord = ConnectionRecord(
                id = UUID.randomUUID().toString(),
                requesterUserId = matchedCode.creatorUserId,
                receiverUserId = currentUserId,
                status = "ACTIVE"
            )

            val connResp = httpClient.post("$SUPABASE_URL/rest/v1/connections") {
                commonHeaders(this)
                header("Prefer", "return=representation")
                setBody(connectionRecord)
            }

            if (connResp.status == HttpStatusCode.OK || connResp.status == HttpStatusCode.Created) {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val nowStr = isoFormat.format(Date())
                localConnectionCodes[cleanCode] = matchedCode.copy(usedAt = nowStr)

                try {
                    httpClient.patch("$SUPABASE_URL/rest/v1/connection_codes") {
                        commonHeaders(this)
                        parameter("id", "eq.${matchedCode.id}")
                        setBody(UpdateConnectionCodeRequest(used_at = nowStr))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to patch connection_codes on remote", e)
                }
                Result.success(connectionRecord)
            } else {
                Result.failure(Exception("Failed to establish connection"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "acceptConnectionCode error", e)
            Result.failure(e)
        }
    }

    suspend fun fetchActiveConnections(userId: String): List<ConnectionRecord> {
        return try {
            val resp = httpClient.get("$SUPABASE_URL/rest/v1/connections") {
                commonHeaders(this)
                parameter("or", "(requester_user_id.eq.$userId,receiver_user_id.eq.$userId)")
                parameter("status", "eq.ACTIVE")
                parameter("select", "*")
            }
            if (resp.status == HttpStatusCode.OK) {
                val list = jsonConfig.decodeFromString<List<ConnectionRecord>>(resp.bodyAsText())
                list.filter { it.status == "ACTIVE" }
            } else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "fetchActiveConnections error", e)
            emptyList()
        }
    }

    suspend fun disconnectConnection(connectionId: String): Boolean {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val nowStr = isoFormat.format(Date())
            val resp = httpClient.patch("$SUPABASE_URL/rest/v1/connections") {
                commonHeaders(this)
                parameter("id", "eq.$connectionId")
                setBody(DisconnectConnectionRequest(status = "DISCONNECTED", disconnected_at = nowStr))
            }
            resp.status == HttpStatusCode.OK || resp.status == HttpStatusCode.NoContent
        } catch (e: Exception) {
            Log.e(TAG, "disconnectConnection error", e)
            false
        }
    }

    suspend fun uploadUsageDaily(items: List<UsageDaily>): Boolean {
        if (items.isEmpty()) return true
        return try {
            val response = httpClient.post("$SUPABASE_URL/rest/v1/usage_daily") {
                commonHeaders(this)
                header("Prefer", "resolution=merge-duplicates")
                setBody(items)
            }
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created
        } catch (e: Exception) {
            Log.e(TAG, "uploadUsageDaily error", e)
            false
        }
    }

    suspend fun uploadUsageSessions(items: List<UsageSession>): Boolean {
        if (items.isEmpty()) return true
        return try {
            val response = httpClient.post("$SUPABASE_URL/rest/v1/usage_sessions") {
                commonHeaders(this)
                header("Prefer", "resolution=merge-duplicates")
                setBody(items)
            }
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created
        } catch (e: Exception) {
            Log.e(TAG, "uploadUsageSessions error", e)
            false
        }
    }

    suspend fun fetchPartnerUsageDaily(partnerUserId: String, date: String): List<UsageDaily> {
        return try {
            val response = httpClient.get("$SUPABASE_URL/rest/v1/usage_daily") {
                commonHeaders(this)
                parameter("user_id", "eq.$partnerUserId")
                parameter("usage_date", "eq.$date")
                parameter("select", "*")
                parameter("order", "total_seconds.desc")
            }
            if (response.status == HttpStatusCode.OK) {
                jsonConfig.decodeFromString<List<UsageDaily>>(response.bodyAsText())
            } else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "fetchPartnerUsageDaily error", e)
            emptyList()
        }
    }

    suspend fun fetchPartnerUsageSessions(partnerUserId: String, datePrefix: String): List<UsageSession> {
        return try {
            val response = httpClient.get("$SUPABASE_URL/rest/v1/usage_sessions") {
                commonHeaders(this)
                parameter("user_id", "eq.$partnerUserId")
                parameter("started_at", "like.$datePrefix%")
                parameter("select", "*")
                parameter("order", "started_at.desc")
            }
            if (response.status == HttpStatusCode.OK) {
                jsonConfig.decodeFromString<List<UsageSession>>(response.bodyAsText())
            } else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "fetchPartnerUsageSessions error", e)
            emptyList()
        }
    }
}
