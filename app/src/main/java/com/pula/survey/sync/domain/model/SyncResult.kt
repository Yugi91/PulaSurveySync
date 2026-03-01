package com.pula.survey.sync.domain.model

/**
 * Outcome of a sync session.
 */
data class SyncResult(
    val successful: List<String>,
    val failed: Map<String, SyncError>,
    val pending: List<String>,
    val terminationReason: TerminationReason
)

enum class TerminationReason {
    ALL_COMPLETED,
    NETWORK_UNAVAILABLE,
    ALREADY_IN_PROGRESS,
    CANCELLED,
    EMPTY_QUEUE
}
