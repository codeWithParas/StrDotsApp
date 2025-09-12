package com.xyz.strapp.presentation.userlogin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xyz.strapp.domain.model.LoginRequest
import com.xyz.strapp.domain.model.LoginResponse
import com.xyz.strapp.domain.repository.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Sealed class to represent the UI state for the login screen
sealed interface LoginUiState {
    data object Idle : LoginUiState // Initial state
    data object Loading : LoginUiState // Login attempt in progress
    data class Success(val loginResponse: LoginResponse) : LoginUiState // Login successful, navigate to home
    data class FaceImageRequired(val loginResponse: LoginResponse) : LoginUiState // Login successful but face image upload required
    data class Error(val message: String) : LoginUiState // Login failed
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository, // Hilt provides this
    // Removed ApiService direct injection, repository should use it
) : ViewModel() {

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    // Function to be called from the UI when phone input changes
    fun onPhoneChange(newPhone: String) {
        _phone.update { newPhone }
    }

    // Function to be called from the UI when password input changes
    fun onPasswordChange(newPassword: String) {
        _password.update { newPassword }
    }

    // Function to be called from the UI when the login button is clicked
    fun onLoginClicked() {
        // Basic validation (can be more sophisticated)
        if (_phone.value.isBlank() || _password.value.isBlank()) {
            _loginUiState.value = LoginUiState.Error("Phone number and password cannot be empty.")
            return
        }
        // Could add phone format validation here too

        _loginUiState.value = LoginUiState.Loading // Set state to Loading

        viewModelScope.launch {
            val request = LoginRequest(phone = _phone.value.trim(), password = _password.value)
            val result = loginRepository.loginUserRemote(request)

            result.fold(
                onSuccess = { response ->
                    if(response.isSuccess) {
                        // TODO: FOR TESTING - Always redirect to face upload screen
                        // Remove this when testing is complete and use the actual API response
                        //_loginUiState.value = LoginUiState.FaceImageRequired(response)

//                         Original logic (commented out for testing):
//                         Check if face image is required
                         if (response.faceImageRequried == true) {
                             _loginUiState.value = LoginUiState.FaceImageRequired(response)
                         } else {
                             _loginUiState.value = LoginUiState.Success(response)
                         }
                    } else {
                        // Use error message from response or default message
                        val errorMsg = response.errorMessage?.takeIf { it.isNotBlank() }
                            ?: "Invalid Credentials"
                        _loginUiState.value = LoginUiState.Error(errorMsg)
                    }
                },
                onFailure = { exception ->
                    // Handle login failure
                    _loginUiState.value = LoginUiState.Error(exception.message ?: "An unknown error occurred")
                }
            )
        }
    }

    /**
     * Resets the UI state back to Idle, e.g., after an error message has been shown.
     */
    fun resetLoginState() {
        _loginUiState.value = LoginUiState.Idle
    }

    fun getLanguage() : String  {
        var language = "en"
        viewModelScope.launch {
            language = loginRepository.getPreferredLanguage()
        }
        Log.d("LoginVM", "language: ${language}")
        return language
    }
}