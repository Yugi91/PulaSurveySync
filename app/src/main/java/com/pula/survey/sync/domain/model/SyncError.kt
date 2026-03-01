package com.pula.survey.sync.domain.model

/**
 * Classified sync error. Distinguishes retryable vs non-retryable and
 * whether the error triggers early-stop (consecutive connectivity failures).
 */
sealed class SyncError {
    /** UnknownHostException, ConnectException, NoRouteToHostException. Triggers early-stop. */
    data class ConnectivityError(val cause: Throwable) : SyncError()

    /** SocketTimeoutException (connect timeout). Triggers early-stop. */
    data class Timeout(val cause: Throwable) : SyncError()

    /** HTTP 5xx. Retryable, does NOT trigger early-stop. */
    data class ServerError(val code: Int, val message: String?) : SyncError()

    /** HTTP 4xx. Non-retryable, does NOT trigger early-stop. */
    data class ClientError(val code: Int, val message: String?) : SyncError()

    /** Catch-all. Retryable with caution, does NOT trigger early-stop. */
    data class Unknown(val cause: Throwable) : SyncError()

    val isRetryable: Boolean
        get() = when (this) {
            is ConnectivityError, is Timeout, is ServerError, is Unknown -> true
            is ClientError -> false
        }

    val triggersEarlyStop: Boolean
        get() = when (this) {
            is ConnectivityError, is Timeout -> true
            is ServerError, is ClientError, is Unknown -> false
        }
}
