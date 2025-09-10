package com.xyz.strapp.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xyz.strapp.domain.repository.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// This could be a new ViewModel or logic added to an existing one
// like MainViewModel or even LoginViewModel if it handles initial checks.
@HiltViewModel
class AuthStatusViewModel @Inject constructor(
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _isUserLoggedIn = MutableStateFlow<Boolean?>(null) // Null initially, then true/false
    val isUserLoggedIn: StateFlow<Boolean?> = _isUserLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(true) // To show loading while checking
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    var isCheckInFlow = true

    init {
        checkLoginStatus()
    }

    fun checkLoginStatus() {
        _isLoading.value = true
        viewModelScope.launch {
            val loggedIn = loginRepository.isLoggedIn() // Call the suspend function
            _isUserLoggedIn.value = loggedIn
            _isLoading.value = false
        }
    }

    // You might also have a logout function that uses loginRepository.clearAuthToken()
    fun logout() {
        viewModelScope.launch {
            loginRepository.clearAllUserData()
            _isUserLoggedIn.value = false // Update state after clearing token
        }
    }
}
