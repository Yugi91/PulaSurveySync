package com.pula.survey.sync.ui.surveydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pula.survey.sync.domain.repository.SurveyRepository
import com.pula.survey.sync.domain.repository.SurveyResponseWithAttachments
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SurveyDetailViewModel(
    repository: SurveyRepository,
    responseId: String
) : ViewModel() {

    val data: StateFlow<SurveyResponseWithAttachments?> =
        repository.getResponseWithAttachments(responseId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    class Factory(
        private val repository: SurveyRepository,
        private val responseId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SurveyDetailViewModel(repository, responseId) as T
    }
}
