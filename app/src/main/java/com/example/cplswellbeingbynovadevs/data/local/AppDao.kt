package com.example.cplswellbeingbynovadevs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: LocalProfileEntity)

    @Query("SELECT * FROM local_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String): LocalProfileEntity?

    @Query("SELECT * FROM local_profiles WHERE id = :id LIMIT 1")
    fun observeProfile(id: String): Flow<LocalProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: LocalDeviceEntity)

    @Query("SELECT * FROM local_devices WHERE userId = :userId LIMIT 1")
    suspend fun getDevice(userId: String): LocalDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageDaily(items: List<LocalUsageDailyEntity>)

    @Query("SELECT * FROM local_usage_daily WHERE userId = :userId AND usageDate = :date ORDER BY totalSeconds DESC")
    suspend fun getUsageDaily(userId: String, date: String): List<LocalUsageDailyEntity>

    @Query("SELECT * FROM local_usage_daily WHERE userId = :userId AND usageDate = :date ORDER BY totalSeconds DESC")
    fun observeUsageDaily(userId: String, date: String): Flow<List<LocalUsageDailyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageSessions(items: List<LocalUsageSessionEntity>)

    @Query("SELECT * FROM local_usage_sessions WHERE userId = :userId AND startedAt LIKE :datePrefix || '%' ORDER BY startedAt DESC")
    suspend fun getUsageSessions(userId: String, datePrefix: String): List<LocalUsageSessionEntity>

    @Query("DELETE FROM local_usage_daily WHERE usageDate < :cutoffDate")
    suspend fun pruneOldUsageDaily(cutoffDate: String)

    @Query("DELETE FROM local_usage_sessions WHERE startedAt < :cutoffDate")
    suspend fun pruneOldUsageSessions(cutoffDate: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnections(connections: List<LocalConnectionEntity>)

    @Query("SELECT * FROM local_connections WHERE status = 'ACTIVE'")
    fun observeActiveConnections(): Flow<List<LocalConnectionEntity>>

    @Query("SELECT * FROM local_connections WHERE status = 'ACTIVE'")
    suspend fun getActiveConnections(): List<LocalConnectionEntity>

    @Query("DELETE FROM local_connections WHERE id = :connectionId")
    suspend fun deleteConnection(connectionId: String)
}
