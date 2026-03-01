package com.pula.survey.sync.domain.model

/**
 * Outcome of a sync session.
 * - [successful]: response IDs that were uploaded successfully.
 * - [failed]: response ID -> classified error (retryable vs fatal).
 * - [pending]: response IDs not attempted (e.g. early-stop or empty queue).
 * - [terminationReason]: why the sync run ended.
 */
data class SyncResult(
    val successful: List<String>,
    val failed: Map<String, SyncError>,
    val pending: List<String>,
    val terminationReason: TerminationReason
)

/** Reason the sync session terminated. */
enum class TerminationReason {
    /** All eligible items were processed (success or failure). */
    ALL_COMPLETED,
    /** Stopped early after consecutive connectivity/timeout failures. */
    NETWORK_UNAVAILABLE,
    /** Another sync was already in progress; this call did nothing. */
    ALREADY_IN_PROGRESS,
    /** Sync was cancelled by the caller. */
    CANCELLED,
    /** No sync-eligible items (PENDING / FAILED_RETRYABLE). */
    EMPTY_QUEUE
}
