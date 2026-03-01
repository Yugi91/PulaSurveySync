package com.pula.survey.sync.data.repository

import com.pula.survey.sync.data.local.dao.AttachmentDao
import com.pula.survey.sync.data.local.dao.SurveyResponseDao
import com.pula.survey.sync.data.local.entity.SurveyResponseEntity
import com.pula.survey.sync.data.mapper.SurveyMapper
import com.pula.survey.sync.domain.model.Attachment
import com.pula.survey.sync.domain.model.SurveyAnswer
import com.pula.survey.sync.domain.model.SurveyResponse
import com.pula.survey.sync.domain.model.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class SurveyRepositoryTest {

    private lateinit var responseDao: SurveyResponseDao
    private lateinit var attachmentDao: AttachmentDao
    private lateinit var mapper: SurveyMapper
    private lateinit var repository: SurveyRepositoryImpl

    @Before
    fun setup() {
        responseDao = mockk(relaxed = true)
        attachmentDao = mockk(relaxed = true)
        mapper = SurveyMapper()
        repository = SurveyRepositoryImpl(responseDao, attachmentDao, mapper)
    }

    private fun response(id: UUID = UUID.randomUUID()) = SurveyResponse(
        id = id,
        farmerId = "farmer1",
        surveyId = "survey1",
        answers = listOf(SurveyAnswer.Single("q1", "Maize")),
        attachments = emptyList(),
        status = SyncStatus.PENDING,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        retryCount = 0,
        lastErrorType = null
    )

    @Test
    fun `saveResponse persists with PENDING status`() = runTest {
        val slot = slot<SurveyResponseEntity>()
        coEvery { responseDao.insert(capture(slot)) } returns Unit

        val resp = response()
        repository.saveResponse(resp, emptyList())

        coVerify(exactly = 1) { responseDao.insert(any()) }
        assertEquals(SyncStatus.PENDING, slot.captured.status)
    }

    @Test
    fun `saveResponse with attachments links correctly`() = runTest {
        val respId = UUID.randomUUID()
        val attachments = listOf(
            Attachment(UUID.randomUUID(), respId, "/path/1.jpg", "1.jpg", "image/jpeg", 1024, System.currentTimeMillis()),
            Attachment(UUID.randomUUID(), respId, "/path/2.jpg", "2.jpg", "image/jpeg", 2048, System.currentTimeMillis())
        )

        repository.saveResponse(response(respId), attachments)

        coVerify(exactly = 1) { responseDao.insert(any()) }
        coVerify(exactly = 1) { attachmentDao.insertAll(match { it.size == 2 }) }
    }

    @Test
    fun `markFailed sets FAILED_RETRYABLE for retryable errors`() = runTest {
        repository.markFailed("r1", "ServerError", isRetryable = true)
        coVerify { responseDao.markFailed("r1", SyncStatus.FAILED_RETRYABLE, "ServerError", any()) }
    }

    @Test
    fun `markFailed sets FAILED_FATAL for non-retryable errors`() = runTest {
        repository.markFailed("r1", "ClientError", isRetryable = false)
        coVerify { responseDao.markFailed("r1", SyncStatus.FAILED_FATAL, "ClientError", any()) }
    }
}
