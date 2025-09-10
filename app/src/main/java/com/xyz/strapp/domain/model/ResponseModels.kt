package com.xyz.strapp.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Data class for the login response from the server.
 */
data class CheckInResponse(
    @SerializedName("statusCode")
    val statusCode: Int,

    @SerializedName("value")
    val value: CheckInProps,
)

data class CheckInProps(
    @SerializedName("latitude")
    val latitude: Int,

    @SerializedName("longitude")
    val longitude: Int,

    @SerializedName("image")
    val image: String?,

    @SerializedName("action")
    val action: String?,

    @SerializedName("employeeCode")
    val employeeCode: String?,

    @SerializedName("employeeName")
    val employeeName: String?,

    @SerializedName("designation")
    val designation: String?,

    @SerializedName("department")
    val department: String?,

    @SerializedName("employeeType")
    val employeeType: String?,

    @SerializedName("gender")
    val gender: String?,

    @SerializedName("erorrMessage")
    val erorrMessage: String?,

    @SerializedName("imageUrl")
    val imageUrl: String?,
)