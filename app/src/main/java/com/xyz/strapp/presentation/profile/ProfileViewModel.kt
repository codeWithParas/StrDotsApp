package com.xyz.strapp.presentation.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xyz.strapp.domain.model.ProfileResponse
import com.xyz.strapp.domain.repository.LoginRepository
import com.xyz.strapp.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Idle : ProfileUiState // Initial state
    data object Loading : ProfileUiState // Login attempt in progress
    data class Success(val profileResponse: ProfileResponse) : ProfileUiState// Login successful
    data class Error(val message: String) : ProfileUiState // Login failed
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _UiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val profileUiState: StateFlow<ProfileUiState> = _UiState.asStateFlow()
    fun loadProfile(){
        viewModelScope.launch {
            val result = profileRepository.getUserProfile()
            result.fold(
                onSuccess = { response ->
                    _UiState.value = ProfileUiState.Success(response)
                },
                onFailure = { exception ->
                    _UiState.value = ProfileUiState.Error(exception.message ?: "An unknown error occurred")
                    Log.e("LoginViewModel", "Login Failed", exception)
                }
            )
        }

    }

    fun setLanguage(language:String){
        viewModelScope.launch {
            loginRepository.setPreferredLanguage(language)
        }
    }

    suspend fun Logout(){
        loginRepository.clearAllUserData()
    }

    fun resetState() {
        _UiState.value = ProfileUiState.Idle
    }
}