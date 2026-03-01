package com.pula.survey.sync.data.mapper

import com.pula.survey.sync.data.local.entity.AttachmentEntity
import com.pula.survey.sync.data.local.entity.SurveyResponseEntity
import com.pula.survey.sync.domain.model.Attachment
import com.pula.survey.sync.domain.model.SurveyAnswer
import com.pula.survey.sync.domain.model.SurveyResponse
import com.pula.survey.sync.domain.model.SyncStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class SurveyMapper {

    private val json = Json { ignoreUnknownKeys = true }

    fun toEntity(domain: SurveyResponse): SurveyResponseEntity =
        SurveyResponseEntity(
            id = domain.id.toString(),
            farmerId = domain.farmerId,
            surveyId = domain.surveyId,
            answersJson = json.encodeToString(domain.answers),
            status = domain.status,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            retryCount = domain.retryCount,
            lastErrorType = domain.lastErrorType
        )

    fun toDomain(entity: SurveyResponseEntity, attachments: List<Attachment> = emptyList()): SurveyResponse =
        SurveyResponse(
            id = UUID.fromString(entity.id),
            farmerId = entity.farmerId,
            surveyId = entity.surveyId,
            answers = json.decodeFromString<List<SurveyAnswer>>(entity.answersJson),
            attachments = attachments,
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            retryCount = entity.retryCount,
            lastErrorType = entity.lastErrorType
        )

    fun toAttachmentEntity(domain: Attachment): AttachmentEntity =
        AttachmentEntity(
            id = domain.id.toString(),
            responseId = domain.responseId.toString(),
            filePath = domain.filePath,
            fileName = domain.fileName,
            mimeType = domain.mimeType,
            sizeBytes = domain.sizeBytes,
            createdAt = domain.createdAt
        )

    fun toAttachmentDomain(entity: AttachmentEntity): Attachment =
        Attachment(
            id = UUID.fromString(entity.id),
            responseId = UUID.fromString(entity.responseId),
            filePath = entity.filePath,
            fileName = entity.fileName,
            mimeType = entity.mimeType,
            sizeBytes = entity.sizeBytes,
            createdAt = entity.createdAt
        )

    fun countRepetitions(answers: List<SurveyAnswer>): Int =
        answers.filterIsInstance<SurveyAnswer.RepeatingSection>()
            .sumOf { it.repetitions.size }
}
