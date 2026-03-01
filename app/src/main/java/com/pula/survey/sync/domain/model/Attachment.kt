package com.pula.survey.sync.domain.model

import java.util.UUID

data class Attachment(
    val id: UUID,
    val responseId: UUID,
    val filePath: String?,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long
)
