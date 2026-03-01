package com.pula.survey.sync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pula.survey.sync.data.local.entity.SurveyResponseEntity
import com.pula.survey.sync.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyResponseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SurveyResponseEntity)

    @Query("SELECT * FROM survey_response WHERE status IN ('PENDING', 'FAILED_RETRYABLE') ORDER BY createdAt ASC")
    fun getSyncEligible(): Flow<List<SurveyResponseEntity>>

    @Query("SELECT * FROM survey_response WHERE status IN ('PENDING', 'FAILED_RETRYABLE') ORDER BY createdAt ASC")
    suspend fun getSyncEligibleOnce(): List<SurveyResponseEntity>

    /**
     * Optimistic lock: only updates if current status matches [expectedStatus].
     * Returns rows affected (0 = already claimed / status changed, 1 = claimed).
     */
    @Query("UPDATE survey_response SET updatedAt = :now WHERE id = :id AND status = :expectedStatus")
    suspend fun claimForSync(id: String, expectedStatus: SyncStatus, now: Long): Int

    @Query("UPDATE survey_response SET status = 'SYNCED', updatedAt = :now WHERE id = :id")
    suspend fun markSynced(id: String, now: Long): Int

    @Query(
        """UPDATE survey_response 
           SET status = :newStatus, lastErrorType = :errorType, retryCount = retryCount + 1, updatedAt = :now 
           WHERE id = :id"""
    )
    suspend fun markFailed(id: String, newStatus: SyncStatus, errorType: String, now: Long)

    @Query("SELECT * FROM survey_response WHERE id = :id")
    fun getResponseById(id: String): Flow<SurveyResponseEntity?>

    @Query("SELECT * FROM survey_response WHERE id = :id")
    suspend fun getResponseByIdOnce(id: String): SurveyResponseEntity?

    @Query("SELECT * FROM survey_response ORDER BY createdAt DESC")
    fun getAllResponses(): Flow<List<SurveyResponseEntity>>

    @Query("SELECT * FROM survey_response WHERE status = :status ORDER BY createdAt DESC")
    fun getResponsesByStatus(status: SyncStatus): Flow<List<SurveyResponseEntity>>

    @Query("SELECT * FROM survey_response WHERE status = 'SYNCED' AND updatedAt < :timestamp")
    suspend fun getSyncedResponsesOlderThan(timestamp: Long): List<SurveyResponseEntity>

    @Query("DELETE FROM survey_response WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM survey_response")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM survey_response WHERE status = :status")
    fun getCountByStatus(status: SyncStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM survey_response WHERE status = :status")
    suspend fun getCountByStatusOnce(status: SyncStatus): Int

    @Query("SELECT COUNT(*) FROM survey_response WHERE status = 'SYNCED' AND updatedAt < :timestamp")
    suspend fun getSyncedOlderThanCount(timestamp: Long): Int
}
