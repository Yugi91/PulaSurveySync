package com.pula.survey.sync.ui.surveylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pula.survey.sync.domain.model.SyncStatus
import com.pula.survey.sync.domain.repository.SurveyRepository
import com.pula.survey.sync.domain.repository.SurveyResponseSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class SurveyListViewModel(
    private val repository: SurveyRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<SyncStatus?>(null)
    val selectedFilter: StateFlow<SyncStatus?> = _selectedFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val responses: StateFlow<List<SurveyResponseSummary>> = _selectedFilter
        .flatMapLatest { status -> repository.getResponsesSummaryByStatus(status) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(status: SyncStatus?) {
        _selectedFilter.value = status
    }

    class Factory(
        private val repository: SurveyRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SurveyListViewModel(repository) as T
    }
}
