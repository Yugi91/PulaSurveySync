package com.pula.survey.sync.domain.model

import java.util.UUID

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
