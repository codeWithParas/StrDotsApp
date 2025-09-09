package com.xyz.strapp.domain.repository

import com.xyz.strapp.domain.model.AttendanceLogModel
import com.xyz.strapp.endpoints.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AttendanceLogsRepository @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * Fetches attendance logs for the current user
     * @param authToken The authentication token
     * @return A Flow emitting a Result containing a list of AttendanceLogModel
     */
    fun getAttendanceLogs(authToken: String): Flow<Result<List<AttendanceLogModel>>> = flow {
        try {
            val response = apiService.getAttendanceLogs("Bearer $authToken")
            if (response.isSuccessful) {
                val logsResponse = response.body()
                if (logsResponse != null && logsResponse.success) {
                    emit(Result.success(logsResponse.logs))
                } else {
                    emit(Result.failure(Exception(logsResponse?.message ?: "Unknown error")))
                }
            } else {
                emit(Result.failure(Exception("Failed to fetch logs: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
