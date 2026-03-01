package com.pula.survey.sync.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pula.survey.sync.data.local.SurveyDatabase
import com.pula.survey.sync.data.local.entity.AttachmentEntity
import com.pula.survey.sync.data.local.entity.SurveyResponseEntity
import com.pula.survey.sync.domain.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SurveyResponseDaoTest {

    private lateinit var db: SurveyDatabase
    private lateinit var responseDao: SurveyResponseDao
    private lateinit var attachmentDao: AttachmentDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SurveyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        responseDao = db.surveyResponseDao()
        attachmentDao = db.attachmentDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun entity(
        id: String = "r1",
        status: SyncStatus = SyncStatus.PENDING,
        createdAt: Long = 1000L,
        answersJson: String = "[]"
    ) = SurveyResponseEntity(
        id = id, farmerId = "farmer1", surveyId = "survey1",
        answersJson = answersJson, status = status,
        createdAt = createdAt, updatedAt = createdAt,
        retryCount = 0, lastErrorType = null
    )

    @Test
    fun insertAndRetrieve_surveyResponse_withNestedRepeatingAnswers() = runTest {
        val json = """[{"type":"com.pula.survey.sync.domain.model.SurveyAnswer.Single","questionId":"q1","value":"Maize"}]"""
        val e = entity(answersJson = json)
        responseDao.insert(e)
        val loaded = responseDao.getResponseByIdOnce("r1")
        assertNotNull(loaded)
        assertEquals(json, loaded!!.answersJson)
    }

    @Test
    fun getSyncEligible_returnsOnlyPendingAndFailedRetryable() = runTest {
        responseDao.insert(entity("r1", SyncStatus.PENDING))
        responseDao.insert(entity("r2", SyncStatus.FAILED_RETRYABLE))
        responseDao.insert(entity("r3", SyncStatus.SYNCED))
        responseDao.insert(entity("r4", SyncStatus.FAILED_FATAL))

        val eligible = responseDao.getSyncEligibleOnce()
        assertEquals(2, eligible.size)
        assertEquals(setOf("r1", "r2"), eligible.map { it.id }.toSet())
    }

    @Test
    fun getSyncEligible_excludesSyncedAndFailedFatal() = runTest {
        responseDao.insert(entity("r1", SyncStatus.SYNCED))
        responseDao.insert(entity("r2", SyncStatus.FAILED_FATAL))

        val eligible = responseDao.getSyncEligibleOnce()
        assertEquals(0, eligible.size)
    }

    @Test
    fun claimForSync_returnsOneWhenStatusMatches() = runTest {
        responseDao.insert(entity("r1", SyncStatus.PENDING))
        val rows = responseDao.claimForSync("r1", SyncStatus.PENDING, System.currentTimeMillis())
        assertEquals(1, rows)
    }

    @Test
    fun claimForSync_returnsZeroWhenStatusAlreadyChanged() = runTest {
        responseDao.insert(entity("r1", SyncStatus.SYNCED))
        val rows = responseDao.claimForSync("r1", SyncStatus.PENDING, System.currentTimeMillis())
        assertEquals(0, rows)
    }

    @Test
    fun markSynced_updatesStatusCorrectly() = runTest {
        responseDao.insert(entity("r1", SyncStatus.PENDING))
        responseDao.markSynced("r1", System.currentTimeMillis())
        val loaded = responseDao.getResponseByIdOnce("r1")
        assertEquals(SyncStatus.SYNCED, loaded!!.status)
    }

    @Test
    fun markFailed_setsCorrectStatusAndIncrementsRetryCount() = runTest {
        responseDao.insert(entity("r1", SyncStatus.PENDING))
        responseDao.markFailed("r1", SyncStatus.FAILED_RETRYABLE, "ServerError", System.currentTimeMillis())
        val loaded = responseDao.getResponseByIdOnce("r1")
        assertEquals(SyncStatus.FAILED_RETRYABLE, loaded!!.status)
        assertEquals(1, loaded.retryCount)
        assertEquals("ServerError", loaded.lastErrorType)
    }

    @Test
    fun getResponseWithAttachments_returnsLinkedAttachments() = runTest {
        responseDao.insert(entity("r1"))
        attachmentDao.insertAll(
            listOf(
                AttachmentEntity("a1", "r1", "/path/1.jpg", "1.jpg", "image/jpeg", 1024, 1000L),
                AttachmentEntity("a2", "r1", "/path/2.jpg", "2.jpg", "image/jpeg", 2048, 1000L)
            )
        )
        val attachments = attachmentDao.getByResponseIdOnce("r1")
        assertEquals(2, attachments.size)
    }

    @Test
    fun deleteSyncedOlderThan_removesOnlyOldSynced() = runTest {
        val oldTime = 1000L
        val recentTime = System.currentTimeMillis()
        responseDao.insert(entity("r1", SyncStatus.SYNCED, oldTime).copy(updatedAt = oldTime))
        responseDao.insert(entity("r2", SyncStatus.SYNCED, recentTime).copy(updatedAt = recentTime))
        responseDao.insert(entity("r3", SyncStatus.PENDING, oldTime).copy(updatedAt = oldTime))

        val cutoff = recentTime - 1
        val old = responseDao.getSyncedResponsesOlderThan(cutoff)
        assertEquals(1, old.size)
        assertEquals("r1", old.first().id)
    }

    @Test
    fun getStorageStats_returnsAccurateCounts() = runTest {
        responseDao.insert(entity("r1", SyncStatus.PENDING))
        responseDao.insert(entity("r2", SyncStatus.SYNCED))
        responseDao.insert(entity("r3", SyncStatus.FAILED_RETRYABLE))

        val total = responseDao.getTotalCount().first()
        assertEquals(3, total)

        val pendingCount = responseDao.getCountByStatusOnce(SyncStatus.PENDING)
        assertEquals(1, pendingCount)
    }
}
