package com.pula.survey.sync.sync

import com.pula.survey.sync.data.remote.FakeSurveyApiService
import com.pula.survey.sync.domain.model.Attachment
import com.pula.survey.sync.domain.model.SurveyAnswer
import com.pula.survey.sync.domain.model.SurveyResponse
import com.pula.survey.sync.domain.model.SyncStatus
import com.pula.survey.sync.domain.model.TerminationReason
import com.pula.survey.sync.domain.repository.SurveyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.UUID

class SyncEngineTest {

    private lateinit var repository: SurveyRepository
    private lateinit var apiService: FakeSurveyApiService
    private lateinit var classifier: NetworkClassifier
    private lateinit var engine: SyncEngine

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        apiService = FakeSurveyApiService().delayMs(0)
        classifier = NetworkClassifier()
        engine = SyncEngine(repository, apiService, classifier, earlyStopThreshold = 2)
    }

    private fun responses(count: Int): List<SurveyResponse> =
        (1..count).map { i ->
            SurveyResponse(
                id = UUID.nameUUIDFromBytes("r$i".toByteArray()),
                farmerId = "farmer$i",
                surveyId = "survey1",
                answers = listOf(SurveyAnswer.Single("q1", "val$i")),
                attachments = emptyList(),
                status = SyncStatus.PENDING,
                createdAt = 1000L + i,
                updatedAt = 1000L + i,
                retryCount = 0,
                lastErrorType = null
            )
        }

    private fun stubEligible(list: List<SurveyResponse>) {
        coEvery { repository.getSyncEligibleOnce() } returns list
        coEvery { repository.claimForSync(any(), any()) } returns 1
    }

    @Test
    fun `sync allSucceed allMarkedSynced`() = runTest {
        val items = responses(5)
        stubEligible(items)

        val result = engine.sync()

        assertEquals(5, result.successful.size)
        assertEquals(0, result.failed.size)
        assertEquals(0, result.pending.size)
        assertEquals(TerminationReason.ALL_COMPLETED, result.terminationReason)
        coVerify(exactly = 5) { repository.markSynced(any()) }
    }

    @Test
    fun `sync partialFailure serverError marksRetryableAndContinues`() = runTest {
        val items = responses(8)
        stubEligible(items)
        apiService.failWithSequence(
            listOf(null, null, null, null, null, HttpException(500, "Internal"), null, null)
        )

        val result = engine.sync()

        assertEquals(7, result.successful.size)
        assertEquals(1, result.failed.size)
        assertTrue(result.failed.values.first().isRetryable)
        assertEquals(TerminationReason.ALL_COMPLETED, result.terminationReason)
    }

    @Test
    fun `sync partialFailure clientError marksFatalAndContinues`() = runTest {
        val items = responses(5)
        stubEligible(items)
        apiService.failWithSequence(listOf(null, null, HttpException(400, "Bad Request"), null, null))

        val result = engine.sync()

        assertEquals(4, result.successful.size)
        assertEquals(1, result.failed.size)
        val error = result.failed.values.first()
        assertTrue(!error.isRetryable)
        assertEquals(TerminationReason.ALL_COMPLETED, result.terminationReason)
    }

    @Test
    fun `sync networkDegradation stopsAfterConsecutiveConnectivityFailures`() = runTest {
        val items = responses(10)
        stubEligible(items)
        apiService.failWithSequence(
            listOf(null, null, null, ConnectException("fail"), ConnectException("fail"))
            // items 6-10 not in sequence -> would succeed, but early-stop should prevent them
        )

        val result = engine.sync()

        assertEquals(3, result.successful.size)
        assertEquals(2, result.failed.size)
        assertEquals(5, result.pending.size)
        assertEquals(TerminationReason.NETWORK_UNAVAILABLE, result.terminationReason)
    }

    @Test
    fun `sync networkDegradation timeoutThenSuccess resetsCounter`() = runTest {
        val items = responses(6)
        stubEligible(items)
        apiService.failWithSequence(
            listOf(null, null, null, SocketTimeoutException("timeout"), null, null)
        )

        val result = engine.sync()

        assertEquals(5, result.successful.size)
        assertEquals(1, result.failed.size)
        assertEquals(0, result.pending.size)
        assertEquals(TerminationReason.ALL_COMPLETED, result.terminationReason)
    }

    @Test
    fun `sync networkDegradation mixedErrors onlyConnectivityCountsForEarlyStop`() = runTest {
        val items = responses(6)
        stubEligible(items)
        // timeout, then server error (resets counter), then timeout again -> only 1 consecutive
        apiService.failWithSequence(
            listOf(null, SocketTimeoutException("t"), HttpException(500, "err"), SocketTimeoutException("t"), null, null)
        )

        val result = engine.sync()

        assertEquals(3, result.successful.size)
        assertEquals(3, result.failed.size)
        assertEquals(0, result.pending.size)
        assertEquals(TerminationReason.ALL_COMPLETED, result.terminationReason)
    }

    @Test
    fun `sync emptyQueue returnsEmptyQueueImmediately`() = runTest {
        coEvery { repository.getSyncEligibleOnce() } returns emptyList()

        val result = engine.sync()

        assertEquals(TerminationReason.EMPTY_QUEUE, result.terminationReason)
        assertEquals(0, result.successful.size)
    }

    @Test
    fun `sync concurrentCalls secondReturnsAlreadyInProgress`() = runTest {
        val items = responses(5)
        stubEligible(items)
        apiService.delayMs(200)

        val first = async { engine.sync() }
        delay(50) // let the first sync start
        val second = engine.sync()

        assertEquals(TerminationReason.ALREADY_IN_PROGRESS, second.terminationReason)

        val firstResult = first.await()
        assertEquals(TerminationReason.ALL_COMPLETED, firstResult.terminationReason)
    }

    @Test
    fun `sync onlyProcessesPendingAndFailedRetryable`() = runTest {
        val pending = responses(2).map { it.copy(status = SyncStatus.PENDING) }
        stubEligible(pending)

        val result = engine.sync()

        assertEquals(2, result.successful.size)
    }

    @Test
    fun `sync claimReturnsZero skipsItem`() = runTest {
        val items = responses(3)
        coEvery { repository.getSyncEligibleOnce() } returns items
        val ids = items.map { it.id.toString() }
        coEvery { repository.claimForSync(ids[0], any()) } returns 1
        coEvery { repository.claimForSync(ids[1], any()) } returns 0 // already claimed
        coEvery { repository.claimForSync(ids[2], any()) } returns 1

        val result = engine.sync()

        assertEquals(2, result.successful.size)
        assertEquals(2, apiService.callCount)
    }

    @Test
    fun `sync crashRecovery noStuckSyncingItems`() = runTest {
        val items = responses(3)
        stubEligible(items)

        engine.sync()

        coVerify(exactly = 0) { repository.markFailed(any(), eq("SYNCING"), any()) }
        coVerify(exactly = 3) { repository.markSynced(any()) }
    }
}
