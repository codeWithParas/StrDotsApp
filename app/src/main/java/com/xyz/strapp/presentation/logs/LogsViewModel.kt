package com.xyz.strapp.presentation.logs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xyz.strapp.domain.model.AttendanceLogModel
import com.xyz.strapp.domain.model.entity.FaceImageEntity
import com.xyz.strapp.domain.repository.AttendanceLogsRepository
import com.xyz.strapp.domain.repository.FaceLivenessRepository
import com.xyz.strapp.domain.repository.LoginRepository
import com.xyz.strapp.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val attendanceLogsRepository: AttendanceLogsRepository,
    private val loginRepository: LoginRepository,
    private val faceLivenessRepository: FaceLivenessRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow<LogsUiState>(LogsUiState.Loading)
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()
    
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        loadLogs()
        observeNetworkConnectivity()
    }
    
    private fun observeNetworkConnectivity() {
        viewModelScope.launch {
            networkUtils.observeNetworkConnectivity().collect { isConnected ->
                _isOnline.value = isConnected
                
                // If we just came back online and we're on the logs tab, refresh data
                if (isConnected && _selectedTab.value == 0) {
                    loadLogs()
                }
            }
        }
    }
    
    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
        if (index == 0) {
            loadLogs()
        } else {
            loadPendingUploads()
        }
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
    
    fun loadPendingUploads() {
        viewModelScope.launch {
            _uiState.value = LogsUiState.Loading
            try {
                val pendingUploads = faceLivenessRepository.getPendingUploads()
                if (pendingUploads.isEmpty()) {
                    _uiState.value = LogsUiState.EmptyPendingUploads
                } else {
                    _uiState.value = LogsUiState.PendingUploads(false, pendingUploads)
                }
            } catch (e: Exception) {
                _uiState.value = LogsUiState.Error(e.message ?: "Unknown error loading pending uploads")
            }
        }
    }

    fun uploadPendingLogs(context: Context) {
        viewModelScope.launch {
            _uiState.value = LogsUiState.PendingUploads(true, faceLivenessRepository.getPendingUploads())
            //_uiState.value = LogsUiState.PendingLogsLoader
            // Get the auth token
            val authToken = loginRepository.getToken()
            if (authToken.isNullOrEmpty()) {
                _uiState.value = LogsUiState.Error("Not logged in")
                return@launch
            }

            faceLivenessRepository.uploadPendingLogsToServer(context = context).collectLatest { result ->
                /*_uiState.value = r*/result.fold(
                    onSuccess = { pendingUploads ->
                        if (pendingUploads.isEmpty()) {
                            _uiState.value = LogsUiState.EmptyPendingUploads
                        } else {
                            //LogsUiState.PendingUploads(pendingUploads)
                            _uiState.value = LogsUiState.PendingUploads(true, pendingUploads)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = LogsUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }
}

sealed class LogsUiState {
    object Loading : LogsUiState()
    object Empty : LogsUiState()
    object EmptyPendingUploads : LogsUiState()
    data class Success(val logs: List<AttendanceLogModel>) : LogsUiState()
    data class PendingUploads(val isUploading: Boolean, val pendingUploads: List<FaceImageEntity>) : LogsUiState()
    data class Error(val message: String) : LogsUiState()
}
