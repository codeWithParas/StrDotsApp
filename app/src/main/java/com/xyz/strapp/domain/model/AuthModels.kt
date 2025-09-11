package com.xyz.strapp.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Data class for the login request body.
 */
data class LoginRequest(
    @SerializedName("phoneNumber") // Ensure this matches your API's expected field name for email
    val phone: String,

    @SerializedName("password") // Ensure this matches your API's expected field name for password
    val password: String
)

/**
 * Data class for the login response from the server.
 * Customize this based on what your API returns upon successful login.
 */

data class LoginResponse(
    @SerializedName("isSuccess")
    val isSuccess: Boolean,

    @SerializedName("token") // Example: if your API returns an access token
    val token: String?,

    @SerializedName("userId") // Example: if your API returns a user ID
    val userId: String?,

    @SerializedName("userName") // Example: if your API returns a user name
    val userName: String?,

    @SerializedName("userImageUrl") // Example: if your API returns a user name
    val userImageUrl: String?,

    @SerializedName("errorMessage") // Example: if your API returns a user name
    val errorMessage: String?,

    @SerializedName("tenentId") // Example: if your API returns a user name
    val tenentId: String?,

    @SerializedName("faceImageRequried") // Boolean flag to indicate if face image is required
    val faceImageRequried: Boolean?
    // Add other fields your API might return (e.g., refresh_token, user_role, etc.)
)

/**
 * A generic error response structure if your API returns errors in a common format.
 * This is optional and highly dependent on your API design.
 */
/*
* {
  "isSuccess": false,
  "token": null,
  "userName": null,
  "userImageUrl": null,
  "errorMessage": "Login failed!",
  "tenentId": null,
  "faceImageRequried": true
}*/
data class ApiErrorResponse(
    @SerializedName("status_code")
    val statusCode: Int?,
    @SerializedName("error")
    val error: String?,
    @SerializedName("message")
    val message: String?
)


