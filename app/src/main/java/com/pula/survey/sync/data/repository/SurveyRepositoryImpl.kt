package com.pula.survey.sync.data.repository

import com.pula.survey.sync.data.local.dao.AttachmentDao
import com.pula.survey.sync.data.local.dao.SurveyResponseDao
import com.pula.survey.sync.data.mapper.SurveyMapper
import com.pula.survey.sync.domain.model.Attachment
import com.pula.survey.sync.domain.model.SurveyResponse
import com.pula.survey.sync.domain.model.SyncStatus
import com.pula.survey.sync.domain.repository.StorageStats
import com.pula.survey.sync.domain.repository.SurveyRepository
import com.pula.survey.sync.domain.repository.SurveyResponseSummary
import com.pula.survey.sync.domain.repository.SurveyResponseWithAttachments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SurveyRepositoryImpl(
    private val responseDao: SurveyResponseDao,
    private val attachmentDao: AttachmentDao,
    private val mapper: SurveyMapper
) : SurveyRepository {

    override suspend fun saveResponse(response: SurveyResponse, attachments: List<Attachment>) {
        responseDao.insert(mapper.toEntity(response))
        if (attachments.isNotEmpty()) {
            attachmentDao.insertAll(attachments.map { mapper.toAttachmentEntity(it) })
        }
    }

    override fun getSyncEligible(): Flow<List<SurveyResponse>> =
        responseDao.getSyncEligible().map { entities ->
            entities.map { mapper.toDomain(it) }
        }

    override suspend fun getSyncEligibleOnce(): List<SurveyResponse> =
        responseDao.getSyncEligibleOnce().map { mapper.toDomain(it) }

    override suspend fun claimForSync(id: String, expectedStatus: SyncStatus): Int =
        responseDao.claimForSync(id, expectedStatus, System.currentTimeMillis())

    override suspend fun markSynced(id: String) {
        responseDao.markSynced(id, System.currentTimeMillis())
    }

    override suspend fun markFailed(id: String, errorType: String, isRetryable: Boolean) {
        val newStatus = if (isRetryable) SyncStatus.FAILED_RETRYABLE else SyncStatus.FAILED_FATAL
        responseDao.markFailed(id, newStatus, errorType, System.currentTimeMillis())
    }

    override fun getResponseWithAttachments(id: String): Flow<SurveyResponseWithAttachments?> =
        combine(
            responseDao.getResponseById(id),
            attachmentDao.getByResponseId(id)
        ) { entity, attachmentEntities ->
            entity?.let {
                val attachments = attachmentEntities.map { a -> mapper.toAttachmentDomain(a) }
                SurveyResponseWithAttachments(
                    response = mapper.toDomain(it, attachments),
                    attachments = attachments
                )
            }
        }

    override fun getAllResponsesSummary(): Flow<List<SurveyResponseSummary>> =
        responseDao.getAllResponses().map { entities ->
            entities.map { entity -> toSummary(entity) }
        }

    override fun getResponsesSummaryByStatus(status: SyncStatus?): Flow<List<SurveyResponseSummary>> =
        if (status == null) {
            getAllResponsesSummary()
        } else {
            responseDao.getResponsesByStatus(status).map { entities ->
                entities.map { entity -> toSummary(entity) }
            }
        }

    override fun getStorageStats(): Flow<StorageStats> {
        val cleanupThreshold = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        return combine(
            responseDao.getTotalCount(),
            responseDao.getCountByStatus(SyncStatus.PENDING),
            responseDao.getCountByStatus(SyncStatus.SYNCED),
            responseDao.getCountByStatus(SyncStatus.FAILED_RETRYABLE),
            responseDao.getCountByStatus(SyncStatus.FAILED_FATAL),
            attachmentDao.getTotalSize()
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            StorageStats(
                totalResponses = values[0] as Int,
                pendingCount = values[1] as Int,
                syncedCount = values[2] as Int,
                failedRetryableCount = values[3] as Int,
                failedFatalCount = values[4] as Int,
                totalAttachmentSizeBytes = values[5] as Long,
                syncedOlderThanDaysCount = 0
            )
        }
    }

    override suspend fun getSyncedResponsesOlderThan(timestamp: Long): List<SurveyResponse> =
        responseDao.getSyncedResponsesOlderThan(timestamp).map { mapper.toDomain(it) }

    override suspend fun deleteResponseAndAttachments(id: String) {
        responseDao.deleteById(id)
    }

    private suspend fun toSummary(entity: com.pula.survey.sync.data.local.entity.SurveyResponseEntity): SurveyResponseSummary {
        val attachmentCount = attachmentDao.getCountForResponse(entity.id)
        val answers = try {
            mapper.toDomain(entity).answers
        } catch (_: Exception) {
            emptyList()
        }
        return SurveyResponseSummary(
            id = entity.id,
            farmerId = entity.farmerId,
            surveyId = entity.surveyId,
            status = entity.status,
            createdAt = entity.createdAt,
            attachmentCount = attachmentCount,
            repetitionCount = mapper.countRepetitions(answers)
        )
    }
}
