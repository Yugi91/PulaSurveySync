package com.pula.survey.sync.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachment",
    foreignKeys = [
        ForeignKey(
            entity = SurveyResponseEntity::class,
            parentColumns = ["id"],
            childColumns = ["responseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["responseId"])]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String,
    val responseId: String,
    val filePath: String?,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long
)
