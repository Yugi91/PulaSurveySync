package com.pula.survey.sync.data.local.converter

import androidx.room.TypeConverter
import com.pula.survey.sync.domain.model.SyncStatus

class Converters {

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
