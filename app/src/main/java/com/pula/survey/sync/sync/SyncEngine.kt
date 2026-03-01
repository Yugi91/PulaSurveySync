package com.pula.survey.sync.sync

import com.pula.survey.sync.data.remote.SurveyApiService
import com.pula.survey.sync.domain.model.SyncError
import com.pula.survey.sync.domain.model.SyncProgress
import com.pula.survey.sync.domain.model.SyncResult
import com.pula.survey.sync.domain.model.SyncStatus
import com.pula.survey.sync.domain.model.TerminationReason
import com.pula.survey.sync.domain.repository.SurveyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

class SyncEngine(
    private val repository: SurveyRepository,
    private val apiService: SurveyApiService,
    private val networkClassifier: NetworkClassifier,
    private val earlyStopThreshold: Int = 2
) {
    private val mutex = Mutex()

    private val _progress = MutableSharedFlow<SyncProgress>(extraBufferCapacity = 64)
    val progress: SharedFlow<SyncProgress> = _progress.asSharedFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /**
     * Runs a full sync session. Returns immediately with [TerminationReason.ALREADY_IN_PROGRESS]
     * if another sync is active (Mutex.tryLock).
     */
    suspend fun sync(): SyncResult {
        if (!mutex.tryLock()) {
            return SyncResult(
                successful = emptyList(),
                failed = emptyMap(),
                pending = emptyList(),
                terminationReason = TerminationReason.ALREADY_IN_PROGRESS
            )
        }

        _isSyncing.value = true
        try {
            return doSync()
        } finally {
            _isSyncing.value = false
            mutex.unlock()
        }
    }

    private suspend fun doSync(): SyncResult {
        val eligible = repository.getSyncEligibleOnce()
        if (eligible.isEmpty()) {
            val result = SyncResult(emptyList(), emptyMap(), emptyList(), TerminationReason.EMPTY_QUEUE)
            _progress.emit(SyncProgress.Finished(result))
            return result
        }

        _progress.emit(SyncProgress.Starting(eligible.size))

        val successful = mutableListOf<String>()
        val failed = mutableMapOf<String, SyncError>()
        val pending = mutableListOf<String>()
        var consecutiveConnFailures = 0
        var earlyStop = false

        for ((index, response) in eligible.withIndex()) {
            val id = response.id.toString()

            if (earlyStop) {
                pending.add(id)
                continue
            }

            val claimed = repository.claimForSync(id, response.status)
            if (claimed == 0) continue

            _progress.emit(
                SyncProgress.Uploading(
                    current = index + 1,
                    total = eligible.size,
                    responseId = id,
                    farmerName = response.farmerId
                )
            )

            try {
                apiService.uploadResponse(response)
                repository.markSynced(id)
                successful.add(id)
                consecutiveConnFailures = 0

                _progress.emit(
                    SyncProgress.ItemCompleted(index + 1, eligible.size, id, success = true, error = null)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val syncError = networkClassifier.classify(e)
                repository.markFailed(id, syncError::class.simpleName ?: "Unknown", syncError.isRetryable)
                failed[id] = syncError

                if (syncError.triggersEarlyStop) {
                    consecutiveConnFailures++
                    if (consecutiveConnFailures >= earlyStopThreshold) {
                        earlyStop = true
                    }
                } else {
                    consecutiveConnFailures = 0
                }

                _progress.emit(
                    SyncProgress.ItemCompleted(index + 1, eligible.size, id, success = false, error = syncError)
                )
            }
        }

        val reason = when {
            earlyStop -> TerminationReason.NETWORK_UNAVAILABLE
            else -> TerminationReason.ALL_COMPLETED
        }

        val result = SyncResult(successful, failed, pending, reason)
        _progress.emit(SyncProgress.Finished(result))
        return result
    }
}
