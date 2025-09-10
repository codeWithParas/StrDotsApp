package com.xyz.strapp.domain.repository

import android.util.Log
import com.xyz.strapp.data.dao.AttendanceLogDao
import com.xyz.strapp.domain.model.AttendanceLogModel
import com.xyz.strapp.domain.model.toModel
import com.xyz.strapp.endpoints.ApiService
import com.xyz.strapp.utils.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AttendanceLogsRepository @Inject constructor(
    private val apiService: ApiService,
    private val networkUtils: NetworkUtils,
    private val attendanceLogDao: AttendanceLogDao
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
                        
                        // Store the fetched logs locally
                        storeLogsLocally(logs)
                        
                        emit(Result.success(logs))
                    } else {
                        Log.e(TAG, "API returned null body")
                        
                        // Try to get cached data
                        val cachedLogs = getCachedLogs()
                        if (cachedLogs.isNotEmpty()) {
                            Log.d(TAG, "Using cached data, got ${cachedLogs.size} logs")
                            emit(Result.success(cachedLogs))
                        } else {
                            emit(Result.failure(Exception("No logs data received")))
                        }
                    }
                } else {
                    Log.e(TAG, "API error: ${response.code()}")
                    
                    // Try to get cached data
                    val cachedLogs = getCachedLogs()
                    if (cachedLogs.isNotEmpty()) {
                        Log.d(TAG, "API failed, using cached data, got ${cachedLogs.size} logs")
                        emit(Result.success(cachedLogs))
                    } else {
                        emit(Result.failure(Exception("Failed to fetch logs: ${response.code()}")))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during API call", e)
                
                // Try to get cached data
                val cachedLogs = getCachedLogs()
                if (cachedLogs.isNotEmpty()) {
                    Log.d(TAG, "API exception, using cached data, got ${cachedLogs.size} logs")
                    emit(Result.success(cachedLogs))
                } else {
                    emit(Result.failure(e))
                }
            }
        } else {
            // No internet - get cached data
            Log.d(TAG, "No internet connection, fetching from local database")
            val cachedLogs = getCachedLogs()
            if (cachedLogs.isNotEmpty()) {
                Log.d(TAG, "Using cached data, got ${cachedLogs.size} logs")
                emit(Result.success(cachedLogs))
            } else {
                Log.d(TAG, "No cached data available")
                emit(Result.failure(Exception("No internet connection and no cached data available")))
            }
        }
    }
    
    /**
     * Stores attendance logs locally in the database
     * @param logs List of AttendanceLogModel to store
     */
    private suspend fun storeLogsLocally(logs: List<AttendanceLogModel>) {
        try {
            Log.d(TAG, "Storing ${logs.size} logs locally")
            
            // Clear old logs before inserting new ones to avoid duplicates
            attendanceLogDao.deleteAllAttendanceLogs()
            
            // Convert to entities and insert
            val entities = logs.map { it.toEntity() }
            attendanceLogDao.insertAttendanceLogs(entities)
            
            Log.d(TAG, "Successfully stored ${logs.size} logs locally")
        } catch (e: Exception) {
            Log.e(TAG, "Error storing logs locally", e)
        }
    }
    
    /**
     * Retrieves cached attendance logs from local database
     * @return List of AttendanceLogModel from local storage
     */
    private suspend fun getCachedLogs(): List<AttendanceLogModel> {
        return try {
            // Get all logs from database and convert to models
            val entities = attendanceLogDao.getAllAttendanceLogs()
            // Since getAllAttendanceLogs returns Flow, we need to collect it first
            var result = listOf<AttendanceLogModel>()
            entities.collect { entityList ->
                result = entityList.map { it.toModel() }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached logs", e)
            emptyList()
        }
    }
    
    /**
     * Gets attendance logs from local database as Flow
     * @return Flow of list of AttendanceLogModel from local storage
     */
    fun getCachedLogsFlow(): Flow<List<AttendanceLogModel>> {
        return attendanceLogDao.getAllAttendanceLogs().map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    /**
     * Clears all locally stored attendance logs
     */
    suspend fun clearLocalLogs() {
        try {
            attendanceLogDao.deleteAllAttendanceLogs()
            Log.d(TAG, "Cleared all local attendance logs")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local logs", e)
        }
    }
}