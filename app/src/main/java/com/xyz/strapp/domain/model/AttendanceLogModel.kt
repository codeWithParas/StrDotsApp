package com.xyz.strapp.domain.model

data class AttendanceLogModel(
    val id: Int,
    val date: String,
    val checkInTime: String,
    val checkOutTime: String?,
    val status: String,
    val location: String
)

// Response wrapper for API
data class AttendanceLogsResponse(
    val success: Boolean,
    val message: String,
    val logs: List<AttendanceLogModel>
)
