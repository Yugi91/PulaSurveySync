package com.pula.survey.sync.data.remote

import com.pula.survey.sync.domain.model.SurveyResponse

interface SurveyApiService {
    suspend fun uploadResponse(response: SurveyResponse): ApiResult
}

sealed class ApiResult {
    data class Success(val responseId: String) : ApiResult()
    data class Error(val code: Int, val message: String?) : ApiResult()
}
