package com.pula.survey.sync.domain.model

/**
 * Live progress events during sync. Emitted via SharedFlow for UI.
 */
sealed class SyncProgress {
    data class Starting(val totalCount: Int) : SyncProgress()
    data class Uploading(
        val current: Int,
        val total: Int,
        val responseId: String,
        val farmerName: String
    ) : SyncProgress()
    data class ItemCompleted(
        val current: Int,
        val total: Int,
        val responseId: String,
        val success: Boolean,
        val error: SyncError?
    ) : SyncProgress()
    data class Finished(val result: SyncResult) : SyncProgress()
}
