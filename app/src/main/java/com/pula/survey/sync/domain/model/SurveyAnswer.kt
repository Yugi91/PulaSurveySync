package com.pula.survey.sync.domain.model

import kotlinx.serialization.Serializable

/** Single answer or repeating section; supports arbitrary nesting for JSON storage in Room. */
@Serializable
sealed class SurveyAnswer {
    @Serializable
    data class Single(
        val questionId: String,
        val value: String
    ) : SurveyAnswer()

    @Serializable
    data class RepeatingSection(
        val sectionId: String,
        val triggerQuestionId: String,
        val repetitions: List<List<SurveyAnswer>>
    ) : SurveyAnswer()
}
