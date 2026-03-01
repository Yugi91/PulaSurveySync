package com.pula.survey.sync

import android.app.Application
import com.pula.survey.sync.di.AppContainer

class PulaSurveyApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
