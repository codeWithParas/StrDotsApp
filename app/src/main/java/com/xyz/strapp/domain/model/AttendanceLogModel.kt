package com.xyz.strapp.domain.model

import com.google.gson.annotations.SerializedName
import com.xyz.strapp.domain.model.entity.AttendanceLogEntity
import com.xyz.strapp.utils.Utils
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Locale

data class AttendanceLogModel(
    @SerializedName("employeeName")
    val employeeName: String,
    
    @SerializedName("employeeCode")
    val employeeCode: String,
    
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("dateTime")
    val dateTime: String,
    
    @SerializedName("message")
    val message: String,

    @SerializedName("imagePath")
    val imagePath: String,

    @SerializedName("action")
    val action: String,
) {
    // Helper functions to format data for display
    fun getFormattedDate(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateTime)
            date?.let {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
            } ?: dateTime
        } catch (e: Exception) {
            dateTime
        }
    }
    
    fun getFormattedTime(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateTime)
            date?.let {
                SimpleDateFormat("HH:mm a", Locale.getDefault()).format(it)
            } ?: dateTime
        } catch (e: Exception) {
            dateTime
        }
    }
    
    fun getLocationString(): String {
        return "Lat: $latitude, Long: $longitude"
    }
    
    /**
     * Converts AttendanceLogModel to AttendanceLogEntity for database storage
     */
    fun toEntity(): AttendanceLogEntity {
        return AttendanceLogEntity(
            employeeName = employeeName,
            employeeCode = employeeCode,
            latitude = latitude,
            longitude = longitude,
            dateTime = dateTime,
            message = message,
            imagePath = imagePath,
            action = action
        )
    }
}

/**
 * Extension function to convert AttendanceLogEntity to AttendanceLogModel
 */
fun AttendanceLogEntity.toModel(): AttendanceLogModel {
    return AttendanceLogModel(
        employeeName = employeeName,
        employeeCode = employeeCode,
        latitude = latitude,
        longitude = longitude,
        dateTime = dateTime,
        message = message,
        imagePath = imagePath,
        action = action
    )
}

// The API returns a direct array, not wrapped in an object
typealias AttendanceLogsResponse = List<AttendanceLogModel>