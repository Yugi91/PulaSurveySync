package com.pula.survey.sync.sync

import com.pula.survey.sync.domain.model.SyncError
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps raw exceptions to [SyncError] with retryable/early-stop classification.
 * HTTP errors are represented via [HttpException].
 */
class NetworkClassifier {

    fun classify(throwable: Throwable): SyncError = when (throwable) {
        is UnknownHostException,
        is ConnectException,
        is NoRouteToHostException -> SyncError.ConnectivityError(throwable)

        is SocketTimeoutException -> SyncError.Timeout(throwable)

        is HttpException -> {
            if (throwable.code in 400..499) {
                SyncError.ClientError(throwable.code, throwable.message)
            } else {
                SyncError.ServerError(throwable.code, throwable.message)
            }
        }

        else -> SyncError.Unknown(throwable)
    }
}

/** Represents an HTTP error response from the API. */
class HttpException(val code: Int, message: String?) : Exception(message)
