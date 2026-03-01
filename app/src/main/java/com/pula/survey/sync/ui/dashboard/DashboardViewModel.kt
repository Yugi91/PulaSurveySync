package com.pula.survey.sync.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pula.survey.sync.domain.model.SyncResult
import com.pula.survey.sync.domain.repository.StorageStats
import com.pula.survey.sync.domain.repository.SurveyRepository
import com.pula.survey.sync.sync.SyncEngine
import com.pula.survey.sync.testdata.TestDataGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: SurveyRepository,
    private val syncEngine: SyncEngine,
    private val testDataGenerator: TestDataGenerator
) : ViewModel() {

    val storageStats: StateFlow<StorageStats> = repository.getStorageStats()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            StorageStats(0, 0, 0, 0, 0, 0L, 0)
        )

    val isSyncing: StateFlow<Boolean> = syncEngine.isSyncing

    private val _lastSyncResult = MutableStateFlow<SyncResult?>(null)
    val lastSyncResult: StateFlow<SyncResult?> = _lastSyncResult.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun triggerSync() {
        viewModelScope.launch {
            val result = syncEngine.sync()
            _lastSyncResult.value = result
        }
    }

    fun generateTestData(count: Int = 10) {
        viewModelScope.launch {
            _isGenerating.value = true
            testDataGenerator.generate(count)
            _isGenerating.value = false
        }
    }

    class Factory(
        private val repository: SurveyRepository,
        private val syncEngine: SyncEngine,
        private val testDataGenerator: TestDataGenerator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(repository, syncEngine, testDataGenerator) as T
    }
}
