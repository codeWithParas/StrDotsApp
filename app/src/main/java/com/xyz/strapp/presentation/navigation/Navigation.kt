package com.xyz.strapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xyz.strapp.presentation.StartScreen
import com.xyz.strapp.presentation.homescreen.HomeScreen
import com.xyz.strapp.presentation.strliveliness.LivelinessScreen
import com.xyz.strapp.presentation.userlogin.LoginScreen
import com.xyz.strapp.presentation.userlogin.LoginViewModel

@Composable
fun Navigation(
    navController: NavHostController,
    authStatusViewModel: AuthStatusViewModel = hiltViewModel()
) {

    val isUserLoggedIn by authStatusViewModel.isUserLoggedIn.collectAsState()
    val isLoading by authStatusViewModel.isLoading.collectAsState()

    NavHost(navController = navController, startDestination = Screen.StartScreen.route){
        composable(Screen.StartScreen.route){
            StartScreen(nextScreen = {
                when{
                    isUserLoggedIn == true -> {
                        navController.navigate(Screen.HomeScreen.route){
                            popUpTo(Screen.LoginScreen.route){
                                inclusive = true
                            }
                            popUpTo(Screen.StartScreen.route){
                                inclusive = true
                            }
                        }
                    }
                    else -> {
                        authStatusViewModel.logout()
                        navController.navigate(Screen.LoginScreen.route){
                            popUpTo(Screen.StartScreen.route){
                                inclusive = true
                            }
                        }
                    }
                }
            })
        }
        composable(Screen.LoginScreen.route){
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.HomeScreen.route){
                    popUpTo(Screen.LoginScreen.route){
                        inclusive = true
                    }
                }
            }, onNavigateToRegister = {})
        }
        composable(Screen.HomeScreen.route){
            HomeScreen(onNavigateToFaceLiveness = {
                navController.navigate(Screen.LivelinessScreen.route)
            },
                onLogout = {
                    navController.navigate(Screen.LoginScreen.route) {
                        popUpTo(0) { // clear entire back stack
                            inclusive = true
                        }
                    }
                })
        }
        composable(Screen.LivelinessScreen.route){
            LivelinessScreen(
                onNavigateBack = { navController.popBackStack(Screen.HomeScreen.route, false) },
            )
        }
    }

}

sealed class Screen(val route:String){
    object StartScreen:Screen("splash_screen")
    object LoginScreen:Screen("login_screen")
    object RegisterScreen:Screen("register_screen")
    object HomeScreen:Screen("home_screen")
    object ProfileScreen:Screen("profile_screen")
    object LivelinessScreen:Screen("str_liveliness_screen")
}