package com.pula.survey.sync.domain.repository

import com.pula.survey.sync.domain.model.SurveyResponse
import com.pula.survey.sync.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

data class SurveyResponseWithAttachments(
    val response: SurveyResponse,
    val attachments: List<com.pula.survey.sync.domain.model.Attachment>
)

data class StorageStats(
    val totalResponses: Int,
    val pendingCount: Int,
    val syncedCount: Int,
    val failedRetryableCount: Int,
    val failedFatalCount: Int,
    val totalAttachmentSizeBytes: Long,
    val syncedOlderThanDaysCount: Int
)

interface SurveyRepository {
    suspend fun saveResponse(response: SurveyResponse, attachments: List<com.pula.survey.sync.domain.model.Attachment>)
    fun getSyncEligible(): Flow<List<SurveyResponse>>
    suspend fun getSyncEligibleOnce(): List<SurveyResponse>
    suspend fun claimForSync(id: String, expectedStatus: SyncStatus): Int
    suspend fun markSynced(id: String)
    suspend fun markFailed(id: String, errorType: String, isRetryable: Boolean)
    fun getResponseWithAttachments(id: String): Flow<SurveyResponseWithAttachments?>
    fun getAllResponsesSummary(): Flow<List<SurveyResponseSummary>>
    fun getResponsesSummaryByStatus(status: SyncStatus?): Flow<List<SurveyResponseSummary>>
    fun getStorageStats(): Flow<StorageStats>
    suspend fun getSyncedResponsesOlderThan(timestamp: Long): List<SurveyResponse>
    suspend fun deleteResponseAndAttachments(id: String)
}

data class SurveyResponseSummary(
    val id: String,
    val farmerId: String,
    val surveyId: String,
    val status: SyncStatus,
    val createdAt: Long,
    val attachmentCount: Int,
    val repetitionCount: Int
)
