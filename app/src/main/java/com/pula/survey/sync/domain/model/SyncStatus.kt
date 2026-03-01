package com.pula.survey.sync.domain.model

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED_RETRYABLE,
    FAILED_FATAL
}
