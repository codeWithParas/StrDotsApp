package com.xyz.strapp.domain.repository

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.xyz.strapp.R
import com.xyz.strapp.data.dao.FaceImageDao
import com.xyz.strapp.domain.model.AttendanceLogModel
import com.xyz.strapp.domain.model.CheckInResponse
import com.xyz.strapp.domain.model.entity.FaceImageEntity
import com.xyz.strapp.endpoints.ApiService
import com.xyz.strapp.presentation.strliveliness.LivenessScreenUiState
import com.xyz.strapp.utils.Constants
import com.xyz.strapp.utils.NetworkUtils
import com.xyz.strapp.utils.Utils.getCurrentDateTimeInIsoFormatTruncatedToSecond
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Repositories are often singletons managed by Hilt
class FaceLivenessRepository @Inject constructor(
    private val faceImageDao: FaceImageDao,
    private val apiService: ApiService,
    private val token: String?,
    private val networkUtils: NetworkUtils,
) {

    /**
     * Converts a Bitmap to ByteArray, creates a FaceImageEntity,
     * and inserts it into the Room database via the DAO.
     *
     * @param bitmap The bitmap of the live face to save.
     * @param timestamp The time the image was captured, defaults to current time.
     * @return The row ID of the newly inserted face image, or null on failure.
     */
    suspend fun saveFaceImageToRoomDatabase(
        bitmap: Bitmap,
        latitude: Float,
        longitude: Float,
        isCheckInFlow: Boolean
    ): Pair<Long, ByteArray>? {
        return withContext(Dispatchers.IO) { // Perform database and bitmap operations off the main thread
            try {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    outputStream
                ) // Compress to JPEG, adjust quality as needed
                val imageByteArray = outputStream.toByteArray()

                if (imageByteArray.isEmpty()) {
                    Log.e("FaceLivenessRepository", "Converted ByteArray is empty.")
                    return@withContext null
                }

                val formattedDateTimeTruncated = getCurrentDateTimeInIsoFormatTruncatedToSecond()
                val faceImageEntity = FaceImageEntity(
                    id = 0,
                    imageData = imageByteArray,
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = formattedDateTimeTruncated,
                    isUploaded = false, // Initially, the image is not uploaded
                    isCheckIn = isCheckInFlow
                )

                val insertedId = faceImageDao.insertFaceImage(faceImageEntity)
                Log.d(
                    "FaceLivenessRepository",
                    "###@@@ Face image saved with ID: $insertedId, Size: ${imageByteArray.size} bytes"
                )
                Pair(insertedId, imageByteArray)
            } catch (e: Exception) {
                Log.e("FaceLivenessRepository", "###@@@ Error saving face image", e)
                null // Return null or throw a custom domain exception
            }
        }
    }

    /**
     * Placeholder for fetching images that need to be uploaded.
     * This would typically fetch entities where isUploaded is false.
     */
    suspend fun getPendingUploads(): List<FaceImageEntity> {
        return withContext(Dispatchers.IO) {
            try {
                faceImageDao.getPendingUploads() // Assuming FaceImageDao has this method
            } catch (e: Exception) {
                Log.e("FaceLivenessRepository", "###@@@ Error fetching pending uploads", e)
                emptyList()
            }
        }
    }

    /**
     * Placeholder for marking an image as uploaded.
     * This would update the isUploaded flag in the database.
     *
     * @param imageId The ID of the image that has been successfully uploaded.
     */
    suspend fun markImageAsUploaded(imageId: Long) {
        withContext(Dispatchers.IO) {
            try {
                faceImageDao.markAsUploaded(imageId) // Assuming FaceImageDao has this method
                Log.d("FaceLivenessRepository", "Image with ID $imageId marked as uploaded.")
            } catch (e: Exception) {
                Log.e("FaceLivenessRepository", "Error marking image $imageId as uploaded", e)
            }
        }
    }

    /**
     * This function would:
     * 1. Get pending images using getPendingUploads().
     * 2. For each image:
     *    a. Attempt to upload it using a Retrofit service (e.g., faceApiService).
     *    b. If upload is successful, call markImageAsUploaded().
     *    c. Handle errors, retries, etc.
     * This should ideally be managed by WorkManager for robust background execution.
     */
    fun uploadPendingLogsToServer(context: Context): Flow<Result<List<FaceImageEntity>>> =
        channelFlow {
            if (networkUtils.isNetworkAvailable()) {
                try {
                    val pendingImages = getPendingUploads()
                    delay(200)
                    for (imageEntity in pendingImages) {
                        try {
                            if(imageEntity.isCheckIn) {
                                startCheckIn(
                                    context,
                                    imageEntity.id,
                                    imageEntity.imageData,
                                    imageEntity.latitude,
                                    imageEntity.longitude
                                )
                            } else {
                                startCheckOut(
                                    context,
                                    imageEntity.id,
                                    imageEntity.imageData,
                                    imageEntity.latitude,
                                    imageEntity.longitude
                                )
                            }.collectLatest { result ->
                                result.fold(
                                    onSuccess = {
                                        Log.e("ImageUploader", "Image Uploaded: ${imageEntity.id}")
                                        //emit(Result.success(message))
                                        // Fetch remaining uploads
                                        val updatedPendingLogs = getPendingUploads()
                                        send(Result.success(updatedPendingLogs))
                                    },
                                    onFailure = { error ->
                                        // Show error dialog
                                        Log.e("ImageUploader", "Image Uploaded Failed: ${imageEntity.id}")
                                        if(pendingImages.last() == imageEntity) {
                                            //emit(Result.success(pendingImages))
                                            send(Result.success(pendingImages))
                                        }
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("ImageUploader", "Image Uploaded Failed: ${imageEntity.id} - ${e.message}")
                            if(pendingImages.last() == imageEntity) {
                                //emit(Result.success(pendingImages))
                                send(Result.success(pendingImages))
                            }
                        }
                        delay(200)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during log upload API call: ${e.message}", e)
                    // Use cached data if available
                    send(Result.failure(Exception("Network error: ${e.message} and no cached data available")))
                }
            } else {
                // No internet - use cached data immediately
                send(Result.failure(Exception("No internet connection and no cached data available")))
            }
        }

    fun getImagesUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    private fun saveImageToGallery(context: Context, imageData: ByteArray, fileName: String): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp") // Folder name
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val collection = getImagesUri()
            val imageUri = resolver.insert(collection, contentValues)

            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(imageData)
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            Log.d("GallerySave", "Image saved to gallery: $imageUri")
            imageUri
        } catch (e: Exception) {
            Log.e("GallerySave", "Failed to save image: ${e.message}", e)
            null
        }
    }

    fun startCheckIn(
        context: Context,
        imageId: Long,
        imageByteArray: ByteArray,
        latitude: Float,
        longitude: Float
    ): Flow<Result<String>> = flow {
        if (networkUtils.isNetworkAvailable()) {
            try {
                if (imageByteArray.isEmpty()) {
                    emit(Result.failure(Exception("Image data not available.")))
                }
                val imageRequestBody = imageByteArray.toRequestBody(
                    "image/jpeg".toMediaTypeOrNull(), // Or "image/png" depending on your image format
                    0, // offset
                    imageByteArray.size // size
                )
                val imagePart = MultipartBody.Part.createFormData(
                    "Image", // Name "Image" to match curl command
                    "image_${imageId}.jpg", // This is the filename sent to the server
                    imageRequestBody
                )
                val formattedDateTimeTruncated = getCurrentDateTimeInIsoFormatTruncatedToSecond()
                val response = apiService.startCheckIn(
                    imagePart = imagePart,
                    latitude = latitude,
                    longitude = longitude,
                    dateTime = formattedDateTimeTruncated
                )
                if (response.isSuccessful) {
                    val data = response.body()
                    markImageAsUploaded(imageId)
                    //emit(Result.success(data))
                    emit(Result.success("Check in successful!"))
                } else {
                    emit(Result.failure(Exception("Check in failed!")))
                }
                if (Constants.savePhotoToGallery) {
                    saveImageToGallery(
                        context,
                        imageByteArray,
                        "checkIn_image_${imageId}.jpg"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during check-in API call: ${e.message}", e)
                emit(Result.failure(Exception("Check In Network error: ${e.message}")))
            }
        } else {
            // No internet - use cached data immediately
            emit(Result.failure(Exception("No internet connection and no cached data available")))
        }
    }

    fun startCheckOut(
        context: Context,
        imageId: Long,
        imageByteArray: ByteArray,
        latitude: Float,
        longitude: Float
    ): Flow<Result<String?>> = flow {
        if (networkUtils.isNetworkAvailable()) {
            try {
                if (imageByteArray.isEmpty()) {
                    emit(Result.failure(Exception("Image data not available for check out.")))
                }
                val imageRequestBody = imageByteArray.toRequestBody(
                    "image/jpeg".toMediaTypeOrNull(), // Or "image/png" depending on your image format
                    0, // offset
                    imageByteArray.size // size
                )
                val imagePart = MultipartBody.Part.createFormData(
                    "Image", // Name "Image" to match curl command
                    "image_${imageId}.jpg", // This is the filename sent to the server
                    imageRequestBody
                )
                if (Constants.savePhotoToGallery) {
                    //save this image to gallery
                    saveImageToGallery(
                        context,
                        imageByteArray,
                        "checkIn_image_${imageId}.jpg"
                    )
                }
                val formattedDateTimeTruncated = getCurrentDateTimeInIsoFormatTruncatedToSecond()
                val response = apiService.startCheckOut(
                    imagePart = imagePart,
                    latitude = latitude,
                    longitude = longitude,
                    dateTime = formattedDateTimeTruncated
                )
                if (response.isSuccessful) {
                    val data = response.body()
                    markImageAsUploaded(imageId)
                    //emit(Result.success(data))
                    emit(Result.success("Check out successful!"))
                } else {
                    emit(Result.failure(Exception("Check out failed!")))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during check-in API call: ${e.message}", e)
                emit(Result.failure(Exception("Check In Network error: ${e.message}")))
            }
        } else {
            // No internet - use cached data immediately
            emit(Result.failure(Exception("No internet connection and no cached data available")))
        }
    }

    // You might also need a function to delete images after successful upload or after a certain age
    suspend fun deleteUploadedImage(imageId: Long) {
        withContext(Dispatchers.IO) {
            try {
                faceImageDao.deleteImageById(imageId) // Assuming FaceImageDao has this method
                Log.d("FaceLivenessRepository", "Image with ID $imageId deleted from local DB.")
            } catch (e: Exception) {
                Log.e("FaceLivenessRepository", "Error deleting image $imageId from local DB", e)
            }
        }
    }
}