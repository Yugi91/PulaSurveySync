package com.pula.survey.sync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pula.survey.sync.ui.navigation.AppNavigation
import com.pula.survey.sync.ui.theme.PulaSurveySyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as PulaSurveyApp).container

        setContent {
            PulaSurveySyncTheme {
                AppNavigation(container)
            }
        }
    }
}
