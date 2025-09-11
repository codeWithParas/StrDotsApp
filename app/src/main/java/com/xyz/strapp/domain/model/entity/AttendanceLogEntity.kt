package com.xyz.strapp.domain.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_logs")
data class AttendanceLogEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    @ColumnInfo(name = "employee_name")
    val employeeName: String,
    
    @ColumnInfo(name = "employee_code")
    val employeeCode: String,
    
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    
    @ColumnInfo(name = "date_time")
    val dateTime: String,
    
    @ColumnInfo(name = "message")
    val message: String,
    
    @ColumnInfo(name = "action")
    val action: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
