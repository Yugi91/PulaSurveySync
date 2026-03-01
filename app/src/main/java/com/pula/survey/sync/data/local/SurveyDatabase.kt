package com.pula.survey.sync.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pula.survey.sync.data.local.converter.Converters
import com.pula.survey.sync.data.local.dao.AttachmentDao
import com.pula.survey.sync.data.local.dao.SurveyResponseDao
import com.pula.survey.sync.data.local.entity.AttachmentEntity
import com.pula.survey.sync.data.local.entity.SurveyResponseEntity

@Database(
    entities = [SurveyResponseEntity::class, AttachmentEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SurveyDatabase : RoomDatabase() {

    abstract fun surveyResponseDao(): SurveyResponseDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        @Volatile
        private var INSTANCE: SurveyDatabase? = null

        fun create(context: Context): SurveyDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SurveyDatabase::class.java,
                    "pula_survey_sync.db"
                ).build().also { INSTANCE = it }
            }
    }
}
