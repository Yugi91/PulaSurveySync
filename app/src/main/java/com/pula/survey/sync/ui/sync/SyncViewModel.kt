package com.pula.survey.sync.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pula.survey.sync.domain.model.SyncError
import com.pula.survey.sync.domain.model.SyncProgress
import com.pula.survey.sync.domain.model.SyncResult
import com.pula.survey.sync.sync.SyncEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SyncItemUi(
    val responseId: String,
    val label: String,
    val state: SyncItemState
)

enum class SyncItemState { WAITING, UPLOADING, SUCCESS, FAILED }

sealed class SyncUiState {
    data object Idle : SyncUiState()
    data class InProgress(
        val current: Int,
        val total: Int,
        val items: List<SyncItemUi>
    ) : SyncUiState()
    data class Result(val result: SyncResult) : SyncUiState()
}

class SyncViewModel(
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private val items = mutableListOf<SyncItemUi>()

    init {
        viewModelScope.launch {
            syncEngine.progress.collect { event ->
                when (event) {
                    is SyncProgress.Starting -> {
                        items.clear()
                        _uiState.value = SyncUiState.InProgress(0, event.totalCount, emptyList())
                    }
                    is SyncProgress.Uploading -> {
                        val existing = items.find { it.responseId == event.responseId }
                        if (existing == null) {
                            items.add(SyncItemUi(event.responseId, event.farmerName, SyncItemState.UPLOADING))
                        }
                        _uiState.value = SyncUiState.InProgress(event.current, event.total, items.toList())
                    }
                    is SyncProgress.ItemCompleted -> {
                        val idx = items.indexOfFirst { it.responseId == event.responseId }
                        if (idx >= 0) {
                            items[idx] = items[idx].copy(
                                state = if (event.success) SyncItemState.SUCCESS else SyncItemState.FAILED
                            )
                        }
                        _uiState.value = SyncUiState.InProgress(event.current, event.total, items.toList())
                    }
                    is SyncProgress.Finished -> {
                        _uiState.value = SyncUiState.Result(event.result)
                    }
                }
            }
        }
    }

    fun startSync() {
        viewModelScope.launch {
            syncEngine.sync()
        }
    }

    fun reset() {
        items.clear()
        _uiState.value = SyncUiState.Idle
    }

    class Factory(
        private val syncEngine: SyncEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SyncViewModel(syncEngine) as T
    }
}
