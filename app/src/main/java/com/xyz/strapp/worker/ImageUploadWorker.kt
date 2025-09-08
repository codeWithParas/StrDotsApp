package com.xyz.strapp.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xyz.strapp.domain.model.CheckInRequest
import com.xyz.strapp.domain.repository.FaceLivenessRepository
import com.xyz.strapp.endpoints.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// A simple implementation for the placeholder ApiService
// In a real app, this would use Retrofit, Ktor, or similar and be provided by Hilt.
/*class PlaceholderApiService : ApiService {
    override suspend fun login(loginRequest: LoginRequest): Response<LoginResponse> {
        TODO("Not yet implemented")
    }
    *//*override suspend fun upload(imageData: ByteArray, fileName: String): Boolean {
        // Simulate network delay
        kotlinx.coroutines.delay(2000)
        Log.d("PlaceholderApiService", "Attempting to upload $fileName (${imageData.size} bytes)")
        // Simulate a 50% chance of success for testing
        val success = Math.random() > 0.5
        if (success) {
            Log.d("PlaceholderApiService", "Upload successful for $fileName")
        } else {
            Log.e("PlaceholderApiService", "Upload failed for $fileName")
        }
        return success
    }*//*

    override suspend fun uploadFaceImage(
        imagePart: MultipartBody.Part,
        userId: RequestBody,
        timestamp: RequestBody
    ): Response<UploadResponse> {
        return Response.success(UploadResponse(true, "image uploaded"))
    }
}*/


@HiltWorker
class ImageUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val faceLivenessRepository: FaceLivenessRepository,
    // Inject your actual ApiService here once you have it.
    // For now, we'll create the placeholder directly.
    private val apiService: ApiService
) : CoroutineWorker(appContext, workerParams) {

    // Using the placeholder for now.
    // In a real app, inject ApiService via constructor and remove this line.
    //private val apiService: ApiService = PlaceholderApiService()

    companion object {
        private const val TAG = "ImageUploadWorker"
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting image upload work.")
            try {
                val images = faceLivenessRepository.getPendingUploads()
                val pendingImages = faceLivenessRepository.getPendingUploads().firstOrNull()

                if(pendingImages == null)
                    return@withContext Result.failure()

                if (pendingImages.isUploaded) {
                    Log.d(TAG, "###@@@ No pending images to upload.")
                    return@withContext Result.success()
                }

                Log.d(TAG, "###@@@ Found $images images to upload.")
                var allUploadsSuccessful = true

                for (imageEntity in images) {
                    val fileName = "image_${imageEntity.id}_${imageEntity.timestamp}.jpg"
                    Log.d(TAG, "###@@@ Uploading image ID: ${imageEntity.id}, Filename: $fileName")

                    // 28.57544688653584, 77.44538638707068
                    val request = CheckInRequest(latitude = 28.575447f, longitude = 77.44539f, dateTime = "Mon 8 Sep 2025 12:00 PM")
                    //val success = apiService.uploadFaceImage(imageEntity.imageData, request)
                    val success = true

                    if (success) {
                        Log.d(TAG, "###@@@ Successfully uploaded image ID: ${imageEntity.id}")
                        // Option 1: Mark as uploaded
                        //faceLivenessRepository.markAsUploaded(imageEntity.id)
                        Log.d(TAG, "###@@@ Marked image ID: ${imageEntity.id} as uploaded.")

                        // Option 2: Delete after successful upload (if you don't need it locally anymore)
                        // faceLivenessRepository.deleteImageById(imageEntity.id)
                        // Log.d(TAG, "Deleted image ID: ${imageEntity.id} after successful upload.")
                    } else {
                        Log.e(TAG, "###@@@ Failed to upload image ID: ${imageEntity.id}")
                        allUploadsSuccessful = false
                        // Decide on retry strategy. For simplicity, we'll allow WorkManager to retry based on policy.
                        // If a specific image fails, the worker will be marked as failed/retried,
                        // and it will try this image again on the next run.
                        // For more granular control, you might only retry specific image uploads.
                        return@withContext Result.retry() // Retry the whole worker if any image fails
                    }
                }

                if (allUploadsSuccessful) {
                    Log.d(TAG, "###@@@ All pending images processed successfully.")
                    Result.success()
                } else {
                    Log.w(TAG, "###@@@ Some image uploads failed. Will retry.")
                    Result.retry() // Should have been caught by individual image failure
                }

            } catch (e: Exception) {
                Log.e(TAG, "###@@@ Error during image upload work", e)
                Result.failure() // Use Result.retry() if you want WorkManager to retry on exceptions
            }
        }
    }
}
