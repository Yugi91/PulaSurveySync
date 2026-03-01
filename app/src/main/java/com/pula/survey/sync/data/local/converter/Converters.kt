package com.pula.survey.sync.data.local.converter

import androidx.room.TypeConverter
import com.pula.survey.sync.domain.model.SurveyAnswer
import com.pula.survey.sync.domain.model.SyncStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromSurveyAnswers(answers: List<SurveyAnswer>): String =
        json.encodeToString(answers)

    @TypeConverter
    fun toSurveyAnswers(value: String): List<SurveyAnswer> =
        json.decodeFromString(value)
}
