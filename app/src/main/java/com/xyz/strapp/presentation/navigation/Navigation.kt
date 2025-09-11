package com.xyz.strapp.presentation.navigation

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.xyz.strapp.presentation.LanguageScreen.LanguageScreen
import com.xyz.strapp.presentation.StartScreen
import com.xyz.strapp.presentation.components.GlobalFeedbackViewModel
import com.xyz.strapp.presentation.components.SuccessMessageDialog
import com.xyz.strapp.presentation.homescreen.HomeScreen
import com.xyz.strapp.presentation.strliveliness.LivelinessScreen
import com.xyz.strapp.presentation.userlogin.LoginScreen
import com.xyz.strapp.utils.restartApp
import kotlinx.coroutines.delay

@Composable
fun Navigation(
    navController: NavHostController,
    authStatusViewModel: AuthStatusViewModel = hiltViewModel(),
    globalFeedbackViewModel: GlobalFeedbackViewModel = hiltViewModel(),
) {

    val isUserLoggedIn by authStatusViewModel.isUserLoggedIn.collectAsState()
    val isLanguageSelected by authStatusViewModel.isLanguageSelected.collectAsState()
    val isLoading by authStatusViewModel.isLoading.collectAsState()

    NavHost(navController = navController, startDestination = Screen.StartScreen.route) {
        composable(Screen.StartScreen.route) {
            StartScreen(nextScreen = {
                when {
                    isLanguageSelected == false -> {
                        navController.navigate(Screen.LanguageScreen.route) {
                            popUpTo(Screen.StartScreen.route) {
                                inclusive = true
                            }
                        }
                    }
                    isUserLoggedIn == true -> {
                        navController.navigate(Screen.HomeScreen.route) {
                            popUpTo(Screen.LoginScreen.route) {
                                inclusive = true
                            }
                            popUpTo(Screen.StartScreen.route) {
                                inclusive = true
                            }
                        }
                    }

                    else -> {
                        authStatusViewModel.logout()
                        navController.navigate(Screen.LoginScreen.route) {
                            popUpTo(Screen.StartScreen.route) {
                                inclusive = true
                            }
                        }
                    }
                }
            })
        }
        composable(Screen.LanguageScreen.route) {
            LanguageScreen(onLanguageSelection = { languageCode ->
                authStatusViewModel.setLanguage(languageCode)
            })
        }
        composable(Screen.LoginScreen.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.HomeScreen.route) {
                    popUpTo(Screen.LoginScreen.route) {
                        inclusive = true
                    }
                }
            }, onNavigateToRegister = {})
        }
        composable(Screen.HomeScreen.route) {
            HomeScreen(
                onNavigateToFaceLiveness = { isCheckInFlow, latitude, longitude ->
                    navController.navigate(Screen.LivelinessScreen.createRoute(isCheckInFlow, latitude.toFloat(), longitude.toFloat()))
                },
                onLogout = {
                    navController.navigate(Screen.LoginScreen.route) {
                        popUpTo(0) { // clear entire back stack
                            inclusive = true
                        }
                    }
                })
        }
        composable(
            Screen.LivelinessScreen.route,
            arguments = listOf(
                navArgument("isCheckInFlow") { type = NavType.BoolType },
                navArgument("latitude") { type = NavType.FloatType },
                navArgument("longitude") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val isCheckInFlow = backStackEntry.arguments?.getBoolean("isCheckInFlow") ?: false
            val latitude = backStackEntry.arguments?.getFloat("latitude") ?: 0.0f
            val longitude = backStackEntry.arguments?.getFloat("longitude") ?: 0.0f

            Log.d("Navigation", "###@@@ isCheckInFlow: $isCheckInFlow")
            LivelinessScreen(
                onNavigateBack = { strMessage ->
                    if(strMessage.isNotBlank()) {
                        globalFeedbackViewModel.showGlobalSuccess(strMessage)
                    }
                    navController.popBackStack(Screen.HomeScreen.route, false)
                },
                isCheckInFlow = isCheckInFlow,
                latitude = latitude,
                longitude = longitude
            )
        }
    }

}

sealed class Screen(val route: String) {
    object StartScreen : Screen("splash_screen")
    object LoginScreen : Screen("login_screen")
    object  LanguageScreen : Screen(route = "language_screen")
    object RegisterScreen : Screen("register_screen")
    object HomeScreen : Screen("home_screen")
    object ProfileScreen : Screen("profile_screen")
    object LogsScreen:Screen("logs_screen")
    object LivelinessScreen : Screen("str_liveliness_screen/{isCheckInFlow}/{latitude}/{longitude}") {
        fun createRoute(isCheckInFlow: Boolean, latitude: Float, longitude: Float): String {
            return "str_liveliness_screen/$isCheckInFlow/$latitude/$longitude"
        }
    }
}