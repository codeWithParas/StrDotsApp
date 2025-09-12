package com.xyz.strapp.domain.repository

import com.xyz.strapp.domain.model.TaskModel
import com.xyz.strapp.endpoints.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling Tasks API operations
 */
@Singleton
class TasksRepository @Inject constructor(
    private val apiService: ApiService
) {
    
    /**
     * Fetches all tasks from the API
     * @return Flow of Result containing List of TaskModel
     */
    suspend fun getAllTasks(): Flow<Result<List<TaskModel>>> = flow {
        try {
            val response: Response<List<TaskModel>> = apiService.getAllTasks()
            if (response.isSuccessful) {
                val tasks = response.body() ?: emptyList()
                emit(Result.success(tasks))
            } else {
                emit(Result.failure(Exception("Failed to fetch tasks: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
