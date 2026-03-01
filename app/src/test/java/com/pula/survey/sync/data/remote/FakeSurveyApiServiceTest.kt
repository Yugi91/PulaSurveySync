package com.pula.survey.sync.data.remote

import com.pula.survey.sync.domain.model.SurveyAnswer
import com.pula.survey.sync.domain.model.SurveyResponse
import com.pula.survey.sync.domain.model.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.net.ConnectException
import java.util.UUID

class FakeSurveyApiServiceTest {

    private lateinit var api: FakeSurveyApiService

    @Before
    fun setup() {
        api = FakeSurveyApiService().delayMs(0)
    }

    private fun response(id: String = UUID.randomUUID().toString()) = SurveyResponse(
        id = UUID.fromString(id.padStart(36, '0').takeLast(36).let {
            UUID.nameUUIDFromBytes(id.toByteArray()).toString()
        }),
        farmerId = "f1", surveyId = "s1",
        answers = listOf(SurveyAnswer.Single("q1", "v1")),
        attachments = emptyList(), status = SyncStatus.PENDING,
        createdAt = 1000, updatedAt = 1000, retryCount = 0, lastErrorType = null
    )

    @Test
    fun `failOnNthCall fails at correct position`() = runTest {
        api.failOnNthCall(3, ConnectException("fail"))

        api.uploadResponse(response("a"))
        api.uploadResponse(response("b"))

        try {
            api.uploadResponse(response("c"))
            throw AssertionError("Should have thrown")
        } catch (e: ConnectException) {
            assertEquals("fail", e.message)
        }

        assertEquals(3, api.callCount)
    }

    @Test
    fun `failAfterNSuccesses behaves correctly`() = runTest {
        api.failAfterNSuccesses(2, ConnectException("done"))

        api.uploadResponse(response("a"))
        api.uploadResponse(response("b"))

        try {
            api.uploadResponse(response("c"))
            throw AssertionError("Should have thrown")
        } catch (_: ConnectException) { }

        assertEquals(3, api.callCount)
    }

    @Test
    fun `failWithSequence matches exact pattern`() = runTest {
        api.failWithSequence(listOf(null, ConnectException("x"), null))

        api.uploadResponse(response("a")) // success
        try {
            api.uploadResponse(response("b")) // fail
            throw AssertionError("Should have thrown")
        } catch (_: ConnectException) { }
        api.uploadResponse(response("c")) // success

        assertEquals(3, api.callCount)
    }
}
