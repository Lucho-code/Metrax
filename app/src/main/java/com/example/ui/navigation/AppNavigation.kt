package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.MeasurementMode
import com.example.ui.screens.ArMeasureScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MeasureScreen
import com.example.ui.viewmodel.MeasurementViewModel

object Routes {
    const val HOME = "home"
    const val MEASURE = "measure"
    const val AR_VOLUME = "ar_volume"
    const val HISTORY = "history"
}

@Composable
fun AppNavigation(
    viewModel: MeasurementViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToMeasure = { mode ->
                    viewModel.setMode(mode)
                    navController.navigate(Routes.MEASURE)
                },
                onNavigateToArVolume = {
                    navController.navigate(Routes.AR_VOLUME)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.HISTORY)
                }
            )
        }

        composable(Routes.MEASURE) {
            MeasureScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.AR_VOLUME) {
            ArMeasureScreen(
                sharedViewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
