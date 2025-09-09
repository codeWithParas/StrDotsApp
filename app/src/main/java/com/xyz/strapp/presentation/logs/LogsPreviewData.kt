package com.xyz.strapp.presentation.logs

import com.xyz.strapp.domain.model.AttendanceLogModel

/**
 * Mock data for previewing and testing the Logs screen
 */
object LogsPreviewData {
    val mockLogs = listOf(
        AttendanceLogModel(
            employeeName = "John Doe",
            employeeCode = "EMP001",
            latitude = 28.575447,
            longitude = 77.44539,
            dateTime = "2025-09-08T09:30:00",
            message = "No message",
            action = "CheckIn"
        ),
        AttendanceLogModel(
            employeeName = "John Doe",
            employeeCode = "EMP001",
            latitude = 28.575447,
            longitude = 77.44539,
            dateTime = "2025-09-08T17:45:00",
            message = "No message",
            action = "CheckOut"
        ),
        AttendanceLogModel(
            employeeName = "John Doe",
            employeeCode = "EMP001",
            latitude = 28.575447,
            longitude = 77.44539,
            dateTime = "2025-09-09T09:15:00",
            message = "Working from office",
            action = "CheckIn"
        ),
        AttendanceLogModel(
            employeeName = "NotFound",
            employeeCode = "NotFound",
            latitude = 28.575447,
            longitude = 77.44539,
            dateTime = "2025-09-09T18:00:00",
            message = "No message",
            action = "CheckOut"
        )
    )
}
