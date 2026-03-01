package com.pula.survey.sync.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pula.survey.sync.domain.model.SyncStatus

@Entity(
    tableName = "survey_response",
    indices = [Index(value = ["status"]), Index(value = ["createdAt"])]
)
data class SurveyResponseEntity(
    @PrimaryKey
    val id: String,
    val farmerId: String,
    val surveyId: String,
    val answersJson: String,
    val status: SyncStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val retryCount: Int,
    val lastErrorType: String?
)
