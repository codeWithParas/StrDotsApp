package com.xyz.strapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xyz.strapp.di.dataStore
import com.xyz.strapp.domain.repository.LoginRepository
import com.xyz.strapp.presentation.components.GlobalFeedbackViewModel
import com.xyz.strapp.presentation.components.SuccessMessageDialog
import com.xyz.strapp.presentation.navigation.Navigation
import com.xyz.strapp.ui.theme.StrAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val lanCode =  runBlocking {
            newBase.dataStore.data.first()[LoginRepository.PreferencesKeys.PREFERRED_LANGUAGE] ?: "EN"
        }
        val context = LocaleHelper.wrapContext(newBase, lanCode)
        super.attachBaseContext(context)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StrAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }
}


@Composable
fun AppRoot(
    globalFeedbackViewModel: GlobalFeedbackViewModel = hiltViewModel()
) {
    val showDialog by globalFeedbackViewModel.showGlobalSuccessDialog.collectAsState()
    val dialogContent by globalFeedbackViewModel.globalSuccessDialogContent.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Your main application navigation
        AppNavigation(globalFeedbackViewModel) // Pass ViewModel if screens need to trigger dialog

        // The SuccessMessageDialog is placed here, outside the NavHost's screens
        // It will overlay the content of AppNavigation when showDialog is true
        SuccessMessageDialog(
            showDialog = showDialog,
            onDismissRequest = { globalFeedbackViewModel.dismissGlobalSuccessDialog() },
            title = dialogContent.title,
            message = dialogContent.message,
            successIcon = dialogContent.icon
        )
    }
}

@Composable
fun AppNavigation(globalFeedbackViewModel: GlobalFeedbackViewModel) {
    val navController = rememberNavController()
    Navigation(navController)
}