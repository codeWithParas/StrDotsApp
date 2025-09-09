package com.xyz.strapp.presentation.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xyz.strapp.domain.model.AttendanceLogModel
import com.xyz.strapp.domain.repository.AttendanceLogsRepository
import com.xyz.strapp.domain.repository.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val attendanceLogsRepository: AttendanceLogsRepository,
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LogsUiState>(LogsUiState.Loading)
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
    }

    fun loadLogs() {
        viewModelScope.launch {
            _uiState.value = LogsUiState.Loading
            
            // Get the auth token
            val authToken = loginRepository.getToken()
            if (authToken.isNullOrEmpty()) {
                _uiState.value = LogsUiState.Error("Not logged in")
                return@launch
            }
            
            attendanceLogsRepository.getAttendanceLogs(authToken).collectLatest { result ->
                _uiState.value = result.fold(
                    onSuccess = { logs ->
                        if (logs.isEmpty()) {
                            LogsUiState.Empty
                        } else {
                            LogsUiState.Success(logs)
                        }
                    },
                    onFailure = { error ->
                        LogsUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }
}

sealed class LogsUiState {
    object Loading : LogsUiState()
    object Empty : LogsUiState()
    data class Success(val logs: List<AttendanceLogModel>) : LogsUiState()
    data class Error(val message: String) : LogsUiState()
}
