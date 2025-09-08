package com.xyz.strapp.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_table")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "code")
    val code: String,
    @ColumnInfo(name = "gender")
    val gender: String,
    @ColumnInfo(name = "employeeType")
    val employeeType: String,
    @ColumnInfo(name = "circle")
    val circle: String,
    @ColumnInfo(name = "division")
    val division: String,
    @ColumnInfo(name = "range")
    val range: String,
    @ColumnInfo(name = "section")
    val section: String,
    @ColumnInfo(name = "beat")
    val beat: String,
    @ColumnInfo(name = "shift")
    val shift: String,
    @ColumnInfo(name = "startTime")
    val startTime: String,
    @ColumnInfo(name = "endTime")
    val endTime: String,
    @ColumnInfo(name = "image")
    val image: String,
    @ColumnInfo(name = "email")
    val email: String,
    @ColumnInfo(name = "mobileNo")
    val mobileNo: String,
    @ColumnInfo(name = "agencyName")
    val agencyName: String,
    @ColumnInfo(name = "faceImageRequried")
    val faceImageRequried: Boolean,
)