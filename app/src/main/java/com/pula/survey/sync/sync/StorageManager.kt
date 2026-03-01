package com.pula.survey.sync.sync

import com.pula.survey.sync.data.local.dao.AttachmentDao
import com.pula.survey.sync.data.local.dao.SurveyResponseDao

/**
 * Storage cleanup policies: age-based deletion, post-sync file cleanup, and low-storage guard stub.
 */
class StorageManager(
    private val responseDao: SurveyResponseDao,
    private val attachmentDao: AttachmentDao
) {
    /** Delete synced responses (and cascade attachments) older than [days]. Returns count deleted. */
    suspend fun deleteSyncedOlderThan(days: Int = 7): Int {
        val threshold = System.currentTimeMillis() - days.toLong() * 24 * 60 * 60 * 1000
        val old = responseDao.getSyncedResponsesOlderThan(threshold)
        old.forEach { responseDao.deleteById(it.id) }
        return old.size
    }

    /** Null out file paths for a response's attachments (post-sync file cleanup). */
    suspend fun clearAttachmentFiles(responseId: String) {
        attachmentDao.clearFilePathsForResponse(responseId)
    }

    /** Stub: returns true if device has enough storage. Threshold tuning is device-dependent. */
    @Suppress("unused")
    fun hasEnoughStorage(path: String = "", thresholdBytes: Long = 100 * 1024 * 1024): Boolean {
        return true
    }
}
