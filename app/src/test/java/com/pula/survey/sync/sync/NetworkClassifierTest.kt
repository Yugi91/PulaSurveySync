package com.pula.survey.sync.sync

import com.pula.survey.sync.domain.model.SyncError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkClassifierTest {

    private lateinit var classifier: NetworkClassifier

    @Before
    fun setup() {
        classifier = NetworkClassifier()
    }

    @Test
    fun `classify ConnectException returns ConnectivityError and triggersEarlyStop`() {
        val error = classifier.classify(ConnectException("refused"))
        assertTrue(error is SyncError.ConnectivityError)
        assertTrue(error.triggersEarlyStop)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `classify UnknownHostException returns ConnectivityError and triggersEarlyStop`() {
        val error = classifier.classify(UnknownHostException("no host"))
        assertTrue(error is SyncError.ConnectivityError)
        assertTrue(error.triggersEarlyStop)
    }

    @Test
    fun `classify NoRouteToHostException returns ConnectivityError and triggersEarlyStop`() {
        val error = classifier.classify(NoRouteToHostException("no route"))
        assertTrue(error is SyncError.ConnectivityError)
        assertTrue(error.triggersEarlyStop)
    }

    @Test
    fun `classify SocketTimeoutException returns Timeout and triggersEarlyStop`() {
        val error = classifier.classify(SocketTimeoutException("timeout"))
        assertTrue(error is SyncError.Timeout)
        assertTrue(error.triggersEarlyStop)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `classify HttpException 400 returns ClientError and noEarlyStop`() {
        val error = classifier.classify(HttpException(400, "Bad Request"))
        assertTrue(error is SyncError.ClientError)
        assertFalse(error.triggersEarlyStop)
        assertFalse(error.isRetryable)
    }

    @Test
    fun `classify HttpException 500 returns ServerError and noEarlyStop`() {
        val error = classifier.classify(HttpException(500, "Internal"))
        assertTrue(error is SyncError.ServerError)
        assertFalse(error.triggersEarlyStop)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `classify unexpected exception returns Unknown and noEarlyStop`() {
        val error = classifier.classify(IllegalStateException("weird"))
        assertTrue(error is SyncError.Unknown)
        assertFalse(error.triggersEarlyStop)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `isRetryable true for connectivity timeout server unknown, false for client`() {
        assertTrue(classifier.classify(ConnectException()).isRetryable)
        assertTrue(classifier.classify(SocketTimeoutException()).isRetryable)
        assertTrue(classifier.classify(HttpException(502, "")).isRetryable)
        assertTrue(classifier.classify(RuntimeException()).isRetryable)
        assertFalse(classifier.classify(HttpException(422, "")).isRetryable)
    }
}
