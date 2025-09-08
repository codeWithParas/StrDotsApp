package com.xyz.strapp.domain.model

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName


data class ProfileResponse(
    @SerializedName(value = "name")
    val name: String?,
    @SerializedName(value = "code")
    val code: String?,
    @SerializedName(value =  "gender")
    val gender: String?,
    @SerializedName(value =  "employeeType")
    val employeeType: String?,
    @SerializedName(value =  "circle")
    val circle: String?,
    @SerializedName(value =  "division")
    val division: String?,
    @SerializedName(value =  "range")
    val range: String?,
    @SerializedName(value =  "section")
    val section: String?,
    @SerializedName(value =  "beat")
    val beat: String?,
    @SerializedName(value =  "shift")
    val shift: String?,
    @SerializedName(value =  "startTime")
    val startTime: String?,
    @SerializedName(value =  "endTime")
    val endTime: String?,
    @SerializedName(value =  "image")
    val image: String?,
    @SerializedName(value =  "email")
    val email: String?,
    @SerializedName(value =  "mobileNo")
    val mobileNo: String?,
    @SerializedName(value =  "agencyName")
    val agencyName: String?,
    @SerializedName(value =  "faceImageRequried")
    val faceImageRequried: Boolean
)


