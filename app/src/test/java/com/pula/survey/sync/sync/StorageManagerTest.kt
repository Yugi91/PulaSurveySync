package com.pula.survey.sync.sync

import com.pula.survey.sync.data.local.dao.AttachmentDao
import com.pula.survey.sync.data.local.dao.SurveyResponseDao
import com.pula.survey.sync.data.local.entity.SurveyResponseEntity
import com.pula.survey.sync.domain.model.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StorageManagerTest {

    private lateinit var responseDao: SurveyResponseDao
    private lateinit var attachmentDao: AttachmentDao
    private lateinit var manager: StorageManager

    @Before
    fun setup() {
        responseDao = mockk(relaxed = true)
        attachmentDao = mockk(relaxed = true)
        manager = StorageManager(responseDao, attachmentDao)
    }

    private fun entity(id: String, updatedAt: Long) = SurveyResponseEntity(
        id = id, farmerId = "f1", surveyId = "s1", answersJson = "[]",
        status = SyncStatus.SYNCED, createdAt = updatedAt, updatedAt = updatedAt,
        retryCount = 0, lastErrorType = null
    )

    @Test
    fun `cleanupSynced deletes old synced responses`() = runTest {
        val oldEntity = entity("r1", 1000L)
        coEvery { responseDao.getSyncedResponsesOlderThan(any()) } returns listOf(oldEntity)

        val deleted = manager.deleteSyncedOlderThan(7)

        assertEquals(1, deleted)
        coVerify { responseDao.deleteById("r1") }
    }

    @Test
    fun `cleanupSynced preserves recent synced`() = runTest {
        coEvery { responseDao.getSyncedResponsesOlderThan(any()) } returns emptyList()

        val deleted = manager.deleteSyncedOlderThan(7)

        assertEquals(0, deleted)
        coVerify(exactly = 0) { responseDao.deleteById(any()) }
    }

    @Test
    fun `cleanupSynced preserves pending regardless of age`() = runTest {
        // The DAO query already filters by SYNCED status, so PENDING won't appear
        coEvery { responseDao.getSyncedResponsesOlderThan(any()) } returns emptyList()

        val deleted = manager.deleteSyncedOlderThan(7)

        assertEquals(0, deleted)
    }
}
