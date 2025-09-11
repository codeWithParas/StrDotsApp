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
        // Always try to get cached data first as fallback
        val cachedLogs = getCachedLogs()
        Log.d(TAG, "Found ${cachedLogs.size} cached logs")
        
        // If we have internet connection, try to fetch from API
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
                        
                        // Use cached data if available
                        if (cachedLogs.isNotEmpty()) {
                            Log.d(TAG, "API returned null, using cached data, got ${cachedLogs.size} logs")
                            emit(Result.success(cachedLogs))
                        } else {
                            emit(Result.failure(Exception("No logs data received and no cached data available")))
                        }
                    }
                } else {
                    Log.e(TAG, "API error: ${response.code()}")
                    
                    // Use cached data if available
                    if (cachedLogs.isNotEmpty()) {
                        Log.d(TAG, "API failed, using cached data, got ${cachedLogs.size} logs")
                        emit(Result.success(cachedLogs))
                    } else {
                        emit(Result.failure(Exception("Failed to fetch logs: ${response.code()} and no cached data available")))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during API call: ${e.message}", e)
                
                // Use cached data if available
                if (cachedLogs.isNotEmpty()) {
                    Log.d(TAG, "API exception, using cached data, got ${cachedLogs.size} logs")
                    emit(Result.success(cachedLogs))
                } else {
                    Log.e(TAG, "No cached data available for fallback")
                    emit(Result.failure(Exception("Network error: ${e.message} and no cached data available")))
                }
            }
        } else {
            // No internet - use cached data immediately
            Log.d(TAG, "No internet connection, using cached data")
            if (cachedLogs.isNotEmpty()) {
                Log.d(TAG, "Found cached data, returning ${cachedLogs.size} logs")
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
            val entities = attendanceLogDao.getAllAttendanceLogsSync()
            entities.map { it.toModel() }
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
    
    /**
     * Debug method to check how many logs are stored locally
     */
    suspend fun getLocalLogsCount(): Int {
        return try {
            val count = attendanceLogDao.getAttendanceLogsCount()
            Log.d(TAG, "Local logs count: $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local logs count", e)
            0
        }
    }
}