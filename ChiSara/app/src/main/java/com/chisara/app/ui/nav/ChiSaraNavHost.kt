package com.chisara.app.ui.nav

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chisara.app.ui.guess.GuessScreen
import com.chisara.app.ui.home.HomeScreen
import com.chisara.app.ui.onboarding.OnboardingScreen
import com.chisara.app.ui.reveal.RevealScreen
import com.chisara.app.ui.settings.SettingsScreen
import com.chisara.app.ui.stats.StatsScreen
import com.chisara.app.viewmodel.GameViewModel

@androidx.compose.runtime.Composable
fun ChiSaraNavHost(
    navController: NavHostController,
    viewModel: GameViewModel,
    startDestination: String,
    deepLinkEventId: Long?,
    onDeepLinkConsumed: () -> Unit
) {
    // Tapping a blind notification opens the app straight onto its guess screen.
    LaunchedEffect(deepLinkEventId) {
        val id = deepLinkEventId ?: return@LaunchedEffect
        navController.navigate(Routes.guess(id))
        onDeepLinkConsumed()
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                viewModel = viewModel,
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onGuess = { eventId -> navController.navigate(Routes.guess(eventId)) },
                onStats = { navController.navigate(Routes.STATS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onOnboarding = { navController.navigate(Routes.ONBOARDING) }
            )
        }

        composable(
            route = Routes.GUESS,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: -1L
            GuessScreen(
                viewModel = viewModel,
                eventId = eventId,
                onRevealReady = {
                    navController.navigate(Routes.REVEAL) {
                        popUpTo(Routes.GUESS) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.REVEAL) {
            RevealScreen(
                viewModel = viewModel,
                onDone = {
                    viewModel.clearOutcome()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.STATS) {
            StatsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
