package com.xyz.strapp.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a Task from the API response
 */
data class TaskModel(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("dateTime")
    val dateTime: String
)

/**
 * Response wrapper for the Tasks API call
 */
data class TasksResponse(
    val tasks: List<TaskModel>
)
