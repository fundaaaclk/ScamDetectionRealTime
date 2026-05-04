package com.example.scamdetect.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.scamdetect.ui.components.BottomNavBar
import com.example.scamdetect.ui.screens.HomeScreen
import com.example.scamdetect.ui.screens.ModeSelectionScreen
import com.example.scamdetect.ui.screens.ReportScreen
import com.example.scamdetect.ui.screens.ReportsListScreen
import com.example.scamdetect.ui.screens.SettingsScreen
import com.example.scamdetect.ui.screens.SimulationScreen
import com.example.scamdetect.ui.screens.AudioFileScreen
import com.example.scamdetect.ui.screens.MicrophoneScreen
import com.example.scamdetect.ui.theme.AppBackground
import com.example.scamdetect.ui.theme.TextPrimary
import com.example.scamdetect.ui.theme.TextSecondary

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object ModeSelection : Screen("mode_selection")
    data object Simulation : Screen("simulation")
    data object AudioFile : Screen("audio_file")
    data object Microphone : Screen("microphone")
    data object Report : Screen("report/{analysisId}") {
        fun createRoute(analysisId: String) = "report/$analysisId"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val showBottomBar = currentRoute in listOf("home", "reports", "settings")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "SE Shield",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menü",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onItemSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        containerColor = AppBackground
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController)
            }

            composable(Screen.ModeSelection.route) {
                ModeSelectionScreen(navController)
            }

            composable(Screen.Simulation.route) {
                SimulationScreen(navController)
            }

            composable(Screen.AudioFile.route) {
                AudioFileScreen(navController)
            }

            composable(Screen.Microphone.route) {
                MicrophoneScreen(navController)
            }

            composable(
                route = Screen.Report.route,
                arguments = listOf(navArgument("analysisId") { type = NavType.StringType })
            ) { backStackEntry ->
                val analysisId = backStackEntry.arguments?.getString("analysisId") ?: ""
                ReportScreen(navController, analysisId)
            }

            composable("reports") {
                ReportsListScreen()
            }

            composable("settings") {
                SettingsScreen()
            }
        }
    }
}