package com.xyz.strapp.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class GlobalSuccessDialogContent(
    val title: String? = "Success!",
    val message: String = "",
    val icon: ImageVector = Icons.Filled.CheckCircle,
    // You can add more customization like custom dismiss actions if needed
)

@HiltViewModel
class GlobalFeedbackViewModel @Inject constructor() : ViewModel() {
    init {
        //println("GlobalFeedbackViewModel instance created: ${this.hashCode()}")
    }
    private val _showGlobalSuccessDialog = MutableStateFlow(false)
    val showGlobalSuccessDialog: StateFlow<Boolean> = _showGlobalSuccessDialog.asStateFlow()

    private val _showNoInternetDialog = MutableStateFlow(false)
    val showNoInternetDialog: StateFlow<Boolean> = _showNoInternetDialog.asStateFlow()

    private val _globalSuccessDialogContent = MutableStateFlow(GlobalSuccessDialogContent())
    val globalSuccessDialogContent: StateFlow<GlobalSuccessDialogContent> = _globalSuccessDialogContent.asStateFlow()

    fun showGlobalSuccess(message: String, title: String? = "Success!", icon: ImageVector = Icons.Filled.CheckCircle) {
        _globalSuccessDialogContent.value = GlobalSuccessDialogContent(title, message, icon)
        _showGlobalSuccessDialog.value = true
    }

    fun dismissGlobalSuccessDialog() {
        _showGlobalSuccessDialog.value = false
    }

    fun showNoInternetMsgDialog(feedback: GlobalSuccessDialogContent) {
        _globalSuccessDialogContent.value = feedback
        _showNoInternetDialog.value = true
    }

    fun dismissNoInternetDialog() {
        _showNoInternetDialog.value = false
    }
}