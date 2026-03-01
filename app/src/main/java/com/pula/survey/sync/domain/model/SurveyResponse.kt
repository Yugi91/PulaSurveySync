package com.pula.survey.sync.domain.model

import java.util.UUID

/**
 * Domain model for a single survey response.
 * Stored locally with [SyncStatus]; answers may include nested [SurveyAnswer.RepeatingSection].
 */
data class SurveyResponse(
    val id: UUID,
    val farmerId: String,
    val surveyId: String,
    val answers: List<SurveyAnswer>,
    val attachments: List<Attachment>,
    val status: SyncStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val retryCount: Int,
    val lastErrorType: String?
)
