package com.xyz.strapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.xyz.strapp.di.dataStore
import com.xyz.strapp.domain.repository.LoginRepository
import com.xyz.strapp.presentation.components.GlobalFeedbackViewModel
import com.xyz.strapp.presentation.components.NoInternetDialog
import com.xyz.strapp.presentation.components.SuccessMessageDialog
import com.xyz.strapp.presentation.navigation.Navigation
import com.xyz.strapp.ui.theme.StrAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val lanCode = runBlocking {
            newBase.dataStore.data.first()[LoginRepository.PreferencesKeys.PREFERRED_LANGUAGE]
                ?: "EN"
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
    globalFeedbackViewModel: GlobalFeedbackViewModel = hiltViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity)
) {
    val showNoInternetDialog by globalFeedbackViewModel.showNoInternetDialog.collectAsState()
    val showDialog by globalFeedbackViewModel.showGlobalSuccessDialog.collectAsState()
    val dialogContent by globalFeedbackViewModel.globalSuccessDialogContent.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        AppNavigation()

        SuccessMessageDialog(
            showDialog = showDialog,
            onDismissRequest = { globalFeedbackViewModel.dismissGlobalSuccessDialog() },
            title = dialogContent.title,
            message = dialogContent.message,
            successIcon = dialogContent.icon
        )

        NoInternetDialog(
            showDialog = showNoInternetDialog,
            onDismiss = {
                globalFeedbackViewModel.dismissNoInternetDialog()
            },
            message = dialogContent.message
        )
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    Navigation(navController)
}