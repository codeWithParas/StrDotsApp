package com.xyz.strapp.domain.model

import com.google.gson.annotations.SerializedName

data class CheckInRequest(
    @SerializedName("Latitude")
    val latitude: Float,

    @SerializedName("Longitude")
    val longitude: Float,

    @SerializedName("dateTime")
    val dateTime: String
)

data class UploadImageRequest(
    @SerializedName("Image")
    val image: String
)

