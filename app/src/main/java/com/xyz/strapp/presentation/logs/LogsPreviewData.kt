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
            action = "CheckIn",
            imagePath = "http://103.186.230.15:7401/api/AttendanceRegister/DownloadLogs?id=5e6f884e-5c37-42fd-8e3a-0ef2b0b8c7f7"
        ),
        AttendanceLogModel(
            employeeName = "John Doe",
            employeeCode = "EMP001",
            latitude = 28.575447,
            longitude = 77.44539,
            dateTime = "2025-09-08T17:45:00",
            message = "No message",
            action = "CheckOut",
            imagePath = "http://103.186.230.15:7401/api/AttendanceRegister/DownloadLogs?id=5e6f884e-5c37-42fd-8e3a-0ef2b0b8c7f7"
        ),
        AttendanceLogModel(
            employeeName = "John Doe",
            employeeCode = "EMP001",
            latitude = 28.575447,
            longitude = 77.44539,
            dateTime = "2025-09-09T09:15:00",
            message = "Working from office",
            action = "CheckIn",
            imagePath = "http://103.186.230.15:7401/api/AttendanceRegister/DownloadLogs?id=5e6f884e-5c37-42fd-8e3a-0ef2b0b8c7f7"
        ),
        AttendanceLogModel(
            employeeName = "NotFound",
            employeeCode = "NotFound",
            latitude = 28.575447,
            longitude = 77.44539,
            dateTime = "2025-09-09T18:00:00",
            message = "No message",
            action = "CheckOut",
            imagePath = "http://103.186.230.15:7401/api/AttendanceRegister/DownloadLogs?id=5e6f884e-5c37-42fd-8e3a-0ef2b0b8c7f7"
        )
    )
}
