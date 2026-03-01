package com.pula.survey.sync.domain.model

/**
 * Persisted sync status. No SYNCING in DB — SYNCING is in-memory only during sync to avoid stuck state after crash.
 * Sync-eligible: PENDING, FAILED_RETRYABLE.
 */
enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED_RETRYABLE,
    FAILED_FATAL
}
