package com.pula.survey.sync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pula.survey.sync.di.AppContainer
import com.pula.survey.sync.ui.dashboard.DashboardScreen
import com.pula.survey.sync.ui.dashboard.DashboardViewModel
import com.pula.survey.sync.ui.surveydetail.SurveyDetailScreen
import com.pula.survey.sync.ui.surveydetail.SurveyDetailViewModel
import com.pula.survey.sync.ui.surveylist.SurveyListScreen
import com.pula.survey.sync.ui.surveylist.SurveyListViewModel
import com.pula.survey.sync.ui.sync.SyncBottomSheet
import com.pula.survey.sync.ui.sync.SyncViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val SURVEY_LIST = "survey_list"
    const val SURVEY_DETAIL = "survey_detail/{responseId}"

    fun surveyDetail(id: String) = "survey_detail/$id"
}

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()
    var showSyncSheet by remember { mutableStateOf(false) }

    val syncViewModel: SyncViewModel = viewModel(
        factory = SyncViewModel.Factory(container.syncEngine)
    )

    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            val vm: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(
                    container.repository,
                    container.syncEngine,
                    container.testDataGenerator
                )
            )
            DashboardScreen(
                viewModel = vm,
                onViewAll = { navController.navigate(Routes.SURVEY_LIST) },
                onSyncClick = { showSyncSheet = true }
            )
        }

        composable(Routes.SURVEY_LIST) {
            val vm: SurveyListViewModel = viewModel(
                factory = SurveyListViewModel.Factory(container.repository)
            )
            SurveyListScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSurveyClick = { id -> navController.navigate(Routes.surveyDetail(id)) }
            )
        }

        composable(
            Routes.SURVEY_DETAIL,
            arguments = listOf(navArgument("responseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val responseId = backStackEntry.arguments?.getString("responseId") ?: return@composable
            val vm: SurveyDetailViewModel = viewModel(
                factory = SurveyDetailViewModel.Factory(container.repository, responseId)
            )
            SurveyDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (showSyncSheet) {
        SyncBottomSheet(
            viewModel = syncViewModel,
            onDismiss = { showSyncSheet = false }
        )
    }
}
