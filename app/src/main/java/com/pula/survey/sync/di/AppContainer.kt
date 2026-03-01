package com.pula.survey.sync.di

import android.content.Context
import com.pula.survey.sync.data.local.SurveyDatabase
import com.pula.survey.sync.data.mapper.SurveyMapper
import com.pula.survey.sync.data.remote.FakeSurveyApiService
import com.pula.survey.sync.data.remote.SurveyApiService
import com.pula.survey.sync.data.repository.SurveyRepositoryImpl
import com.pula.survey.sync.domain.repository.SurveyRepository
import com.pula.survey.sync.sync.NetworkClassifier
import com.pula.survey.sync.sync.StorageManager
import com.pula.survey.sync.sync.SyncEngine
import com.pula.survey.sync.testdata.TestDataGenerator

class AppContainer(context: Context) {

    val database by lazy { SurveyDatabase.create(context) }
    val surveyResponseDao by lazy { database.surveyResponseDao() }
    val attachmentDao by lazy { database.attachmentDao() }

    val mapper by lazy { SurveyMapper() }

    val apiService: SurveyApiService by lazy {
        FakeSurveyApiService().delayMs(300)
    }

    val repository: SurveyRepository by lazy {
        SurveyRepositoryImpl(surveyResponseDao, attachmentDao, mapper)
    }

    val networkClassifier by lazy { NetworkClassifier() }

    val syncEngine by lazy {
        SyncEngine(repository, apiService, networkClassifier)
    }

    val storageManager by lazy {
        StorageManager(surveyResponseDao, attachmentDao)
    }

    val testDataGenerator by lazy {
        TestDataGenerator(repository)
    }
}
