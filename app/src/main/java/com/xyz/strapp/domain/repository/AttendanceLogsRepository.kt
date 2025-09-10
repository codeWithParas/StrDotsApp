package com.xyz.strapp.domain.repository

import android.util.Log
import com.xyz.strapp.domain.model.AttendanceLogModel
import com.xyz.strapp.endpoints.ApiService
import com.xyz.strapp.utils.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AttendanceLogsRepository @Inject constructor(
    private val apiService: ApiService,
    private val networkUtils: NetworkUtils
) {
    private val TAG = "AttendanceLogsRepository"
    
    /**
     * Fetches attendance logs for the current user
     * @param authToken The authentication token
     * @return A Flow emitting a Result containing a list of AttendanceLogModel
     */
    fun getAttendanceLogs(authToken: String): Flow<Result<List<AttendanceLogModel>>> = flow {
        // If we have internet connection, fetch from API
        if (networkUtils.isNetworkAvailable()) {
            try {
                Log.d(TAG, "Internet available, fetching from API")
                val response = apiService.getAttendanceLogs("Bearer $authToken")
                if (response.isSuccessful) {
                    val logs = response.body()
                    if (logs != null) {
                        Log.d(TAG, "API fetch successful, got ${logs.size} logs")
                        emit(Result.success(logs))
                    } else {
                        Log.e(TAG, "API returned null body")
                        emit(Result.failure(Exception("No logs data received")))
                    }
                } else {
                    Log.e(TAG, "API error: ${response.code()}")
                    emit(Result.failure(Exception("Failed to fetch logs: ${response.code()}")))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during API call", e)
                emit(Result.failure(e))
            }
        } else {
            // No internet
            Log.d(TAG, "No internet connection")
            emit(Result.failure(Exception("No internet connection")))
        }
    }
}