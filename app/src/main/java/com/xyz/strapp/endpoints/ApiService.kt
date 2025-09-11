package com.xyz.strapp.endpoints

import com.xyz.strapp.domain.model.CheckInResponse
import com.xyz.strapp.domain.model.AttendanceLogsResponse
import com.xyz.strapp.domain.model.LoginRequest
import com.xyz.strapp.domain.model.LoginResponse
import com.xyz.strapp.domain.model.UploadImageRequest
import com.xyz.strapp.domain.model.ProfileResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.PartMap

interface ApiService {

    /**
     * Attempts to log in a user.
     * @param loginRequest The login credentials (e.g., email and password).
     * @return A Retrofit Response wrapping a LoginResponse.
     */
    @POST("api/Auth/LoginPhone") // IMPORTANT: Replace with your actual login endpoint
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @GET("api/AttendanceRegister/GetUserDetails") // IMPORTANT: Replace with your actual login endpoint
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<ProfileResponse>

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
    @POST("api/AttendanceRegister/CheckIn")
    suspend fun startCheckIn(
        //@Header("Authorization") authToken: String,
        @Part imagePart: MultipartBody.Part,
        @Query("Latitude") latitude: Float,
        @Query("Longitude") longitude: Float,
        @Query("dateTime") dateTime: String
    ): Response<CheckInResponse>

    @Multipart
    @POST("api/AttendanceRegister/CheckOut")
    suspend fun startCheckOut(
        //@Header("Authorization") authToken: String,
        @Part imagePart: MultipartBody.Part,
        @Query("Latitude") latitude: Float,
        @Query("Longitude") longitude: Float,
        @Query("dateTime") dateTime: String
    ): Response<CheckInResponse>


    @POST("api/AttendanceRegister/UploadImage")
    suspend fun uploadImage(
        @Body uploadData: UploadImageRequest
    ): Response<UploadResponse>

    // You can add other API calls here, for example:
    // @GET("api/v1/user_status/{userId}")
    // suspend fun getUserStatus(@Path("userId") userId: String): Response<UserStatusResponse>

    /**
     * Fetches attendance logs for the current user
     * @param authToken The authentication token
     * @return A Retrofit Response wrapping an AttendanceLogsResponse
     */
    @GET("api/AttendanceRegister/GetLogsTest?page=1")
    suspend fun getAttendanceLogs(
        @Header("Authorization") authToken: String
    ): Response<AttendanceLogsResponse>
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