package com.pula.survey.sync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pula.survey.sync.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AttachmentEntity>)

    @Query("SELECT * FROM attachment WHERE responseId = :responseId")
    fun getByResponseId(responseId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachment WHERE responseId = :responseId")
    suspend fun getByResponseIdOnce(responseId: String): List<AttachmentEntity>

    @Query("SELECT COUNT(*) FROM attachment WHERE responseId = :responseId")
    suspend fun getCountForResponse(responseId: String): Int

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM attachment")
    fun getTotalSize(): Flow<Long>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM attachment")
    suspend fun getTotalSizeOnce(): Long

    @Query("UPDATE attachment SET filePath = NULL WHERE responseId = :responseId")
    suspend fun clearFilePathsForResponse(responseId: String)

    @Query("DELETE FROM attachment WHERE responseId = :responseId")
    suspend fun deleteByResponseId(responseId: String)
}
