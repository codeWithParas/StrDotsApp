package com.xyz.strapp.endpoints

import com.xyz.strapp.domain.model.LoginRequest
import com.xyz.strapp.domain.model.LoginResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    /**
     * Attempts to log in a user.
     * @param loginRequest The login credentials (e.g., email and password).
     * @return A Retrofit Response wrapping a LoginResponse.
     */
    @POST("api/Auth/Login") // IMPORTANT: Replace with your actual login endpoint
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    // You can add other auth-related calls here later, e.g.:
    // @POST("api/v1/auth/register")
    // suspend fun register(@Body registrationRequest: RegistrationRequest): Response<RegistrationResponse>
    //
    // @POST("api/v1/auth/refresh_token")
    // suspend fun refreshToken(@Body refreshTokenRequest: RefreshTokenRequest): Response<LoginResponse>
    //
    // @POST("api/v1/auth/logout")
    // suspend fun logout(): Response<Unit> // Or whatever your logout API returns




    /**
     * Uploads a face image to the server.
     * This example uses a multipart request, which is common for file uploads.
     *
     * @param imagePart The image file itself, as a MultipartBody.Part.
     * @param userId A RequestBody part for additional data, like a user ID. (Example)
     * @param timestamp A RequestBody part for the capture timestamp. (Example)
     * @return A Retrofit Response wrapping a ServerResponse data class.
     */
    @Multipart
    @POST("api/v1/upload_face_image") // Replace with your actual endpoint
    suspend fun uploadFaceImage(
        @Part imagePart: MultipartBody.Part,
        @Part("user_id") userId: RequestBody, // Example: "user_id" is the name of the part in the multipart request
        @Part("timestamp") timestamp: RequestBody // Example: "timestamp"
    ): Response<UploadResponse> // Replace UploadResponse with your actual server response model

    // You can add other API calls here, for example:
    // @GET("api/v1/user_status/{userId}")
    // suspend fun getUserStatus(@Path("userId") userId: String): Response<UserStatusResponse>
}

/**
 * Example data class for a server response after an upload.
 * Define this according to your actual API's JSON response.
 */
data class UploadResponse(
    val success: Boolean,
    val message: String,
    val imageUrl: String? = null, // Optional: URL of the uploaded image on the server
    val imageIdServer: String? = null // Optional: ID of the image on the server
)