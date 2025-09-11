package com.xyz.strapp.presentation.faceupload

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xyz.strapp.domain.repository.LoginRepository
import com.xyz.strapp.endpoints.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Represents the different poses/angles for multi-angle face capture
 */
enum class CaptureAngle(val instruction: String, val description: String) {
    STRAIGHT("Look straight ahead", "Position your face directly towards the camera"),
    LEFT("Look left", "Turn your head to look left while keeping face visible"),
    RIGHT("Look right", "Turn your head to look right while keeping face visible"),
    UP("Look up", "Tilt your head up while keeping face visible"),
    DOWN("Look down", "Tilt your head down while keeping face visible")
}


/**
 * Data class to track captured images and their upload status
 */
data class CapturedImageData(
    val angle: CaptureAngle,
    val bitmap: Bitmap,
    val isUploaded: Boolean = false,
    val uploadError: String? = null
)

// Sealed interface to represent the UI state for multi-angle face image upload
sealed interface FaceImageUploadUiState {
    data object Idle : FaceImageUploadUiState // Initial state
    data object Processing : FaceImageUploadUiState // Processing captured image
    data class Error(val message: String) : FaceImageUploadUiState // Upload failed
    
    // Multi-angle specific states
    data class ShowingInstructions(val currentAngle: CaptureAngle) : FaceImageUploadUiState
    data class CapturingAngle(val currentAngle: CaptureAngle) : FaceImageUploadUiState
    data class ImageCaptured(
        val currentAngle: CaptureAngle,
        val capturedBitmap: Bitmap
    ) : FaceImageUploadUiState
    data class UploadingImage(
        val currentAngle: CaptureAngle,
        val imageNumber: Int
    ) : FaceImageUploadUiState
    data class ImageUploaded(
        val uploadedCount: Int,
        val totalImages: Int,
        val nextAngle: CaptureAngle?
    ) : FaceImageUploadUiState
    data class AllImagesCompleted(val totalUploaded: Int) : FaceImageUploadUiState
}

@HiltViewModel
class FaceImageUploadViewModel @Inject constructor(
    private val apiService: ApiService,
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FaceImageUploadUiState>(FaceImageUploadUiState.Idle)
    val uiState: StateFlow<FaceImageUploadUiState> = _uiState.asStateFlow()

    private val _capturedImages = MutableStateFlow<List<CapturedImageData>>(emptyList())
    val capturedImages: StateFlow<List<CapturedImageData>> = _capturedImages.asStateFlow()

    private val angles = listOf(
        CaptureAngle.STRAIGHT,
        CaptureAngle.LEFT,
        CaptureAngle.RIGHT,
        CaptureAngle.UP,
        CaptureAngle.DOWN
    )

    private var currentAngleIndex = 0

    companion object {
        private const val TAG = "FaceImageUploadViewModel"
    }

    /**
     * Start the multi-angle capture process (entry point)
     */
    fun startCapture() {
        currentAngleIndex = 0
        _capturedImages.value = emptyList()
        showInstructionsForCurrentAngle()
    }

    /**
     * Show instructions for the current angle
     */
    private fun showInstructionsForCurrentAngle() {
        if (currentAngleIndex < angles.size) {
            val currentAngle = angles[currentAngleIndex]
            _uiState.value = FaceImageUploadUiState.ShowingInstructions(currentAngle)
        } else {
            _uiState.value = FaceImageUploadUiState.AllImagesCompleted(
                totalUploaded = _capturedImages.value.count { it.isUploaded }
            )
        }
    }

    /**
     * User is ready to capture the current angle
     */
    fun startCaptureForCurrentAngle() {
        if (currentAngleIndex < angles.size) {
            val currentAngle = angles[currentAngleIndex]
            _uiState.value = FaceImageUploadUiState.CapturingAngle(currentAngle)
        }
    }

    /**
     * Called when a face image is captured from camera
     */
    fun onImageCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = FaceImageUploadUiState.Processing
            
            if (currentAngleIndex < angles.size) {
                val currentAngle = angles[currentAngleIndex]
                _uiState.value = FaceImageUploadUiState.ImageCaptured(
                    currentAngle = currentAngle,
                    capturedBitmap = bitmap
                )
            }
        }
    }

    /**
     * User confirms the captured image and wants to upload it
     */
    fun confirmAndUploadImage(bitmap: Bitmap) {
        if (currentAngleIndex < angles.size) {
            val currentAngle = angles[currentAngleIndex]
            _uiState.value = FaceImageUploadUiState.UploadingImage(
                currentAngle = currentAngle,
                imageNumber = currentAngleIndex + 1
            )
            uploadImage(bitmap, currentAngle)
        }
    }

    /**
     * User wants to retake the current image
     */
    fun retakeCurrentImage() {
        if (currentAngleIndex < angles.size) {
            val currentAngle = angles[currentAngleIndex]
            _uiState.value = FaceImageUploadUiState.CapturingAngle(currentAngle)
        }
    }

    /**
     * Proceed to next angle after successful upload
     */
    fun proceedToNextAngle() {
        showInstructionsForCurrentAngle()
    }

    /**
     * Upload the captured image to server
     */
    private fun uploadImage(bitmap: Bitmap, angle: CaptureAngle) {
        viewModelScope.launch {
            try {
                val result = uploadBitmapToAttendanceServer(bitmap)
                
                if (result.isSuccess) {
                    // Add to captured images list
                    val newCapturedImage = CapturedImageData(
                        angle = angle,
                        bitmap = bitmap,
                        isUploaded = true
                    )
                    _capturedImages.value = _capturedImages.value + newCapturedImage
                    
                    val uploadedCount = currentAngleIndex + 1
                    val totalImages = angles.size
                    
                    // Move to next angle
                    currentAngleIndex++
                    
                    if (currentAngleIndex < angles.size) {
                        val nextAngle = angles[currentAngleIndex]
                        _uiState.value = FaceImageUploadUiState.ImageUploaded(
                            uploadedCount = uploadedCount,
                            totalImages = totalImages,
                            nextAngle = nextAngle
                        )
                    } else {
                        _uiState.value = FaceImageUploadUiState.AllImagesCompleted(
                            totalUploaded = uploadedCount
                        )
                    }
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Upload failed"
                    _uiState.value = FaceImageUploadUiState.Error(errorMessage)
                    Log.e(TAG, "Upload failed for $angle: $errorMessage")
                }
            } catch (e: Exception) {
                _uiState.value = FaceImageUploadUiState.Error("Upload error: ${e.message}")
                Log.e(TAG, "Upload exception for $angle", e)
            }
        }
    }

    /**
     * Convert bitmap to multipart and upload to attendance server using /api/AttendanceRegister/UploadImage
     */
    private suspend fun uploadBitmapToAttendanceServer(bitmap: Bitmap): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Convert bitmap to byte array with compression
                val outputStream = ByteArrayOutputStream()
                // Use 70% quality for good balance between file size and image quality
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val imageByteArray = outputStream.toByteArray()
                
                Log.d(TAG, "Compressed image size: ${imageByteArray.size} bytes (${imageByteArray.size / 1024}KB)")

                if (imageByteArray.isEmpty()) {
                    return@withContext Result.failure(Exception("Failed to convert image to byte array"))
                }

                // Create multipart body part for the image
                val imageRequestBody = imageByteArray.toRequestBody(
                    "image/jpeg".toMediaTypeOrNull(),
                    0,
                    imageByteArray.size
                )

                val imagePart = MultipartBody.Part.createFormData(
                    "Image", // Use "Image" as the field name to match API documentation
                    "face_image_${System.currentTimeMillis()}.jpg",
                    imageRequestBody
                )

                // Make API call
                val response = apiService.uploadImage(imagePart)

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (!responseBody.isNullOrEmpty()) {
                        // Check if response indicates success
                        val isSuccess = responseBody.contains("success", ignoreCase = true) || 
                                       responseBody.contains("saved", ignoreCase = true) ||
                                       response.code() == 200
                        
                        if (isSuccess) {
                            Log.d(TAG, "Image uploaded successfully: $responseBody")
                            Result.success(Unit)
                        } else {
                            Result.failure(Exception("Upload failed: $responseBody"))
                        }
                    } else {
                        // Empty response but HTTP 200 - consider success
                        if (response.code() == 200) {
                            Log.d(TAG, "Image uploaded successfully (empty response)")
                            Result.success(Unit)
                        } else {
                            Result.failure(Exception("Empty response"))
                        }
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading image", e)
                Result.failure(e)
            }
        }
    }


    /**
     * Gets auth token from repository/datastore
     */
    private suspend fun getAuthToken(): String? {
        return try {
            loginRepository.getToken()
        } catch (e: Exception) {
            Log.e("FaceImageUploadViewModel", "Error getting auth token", e)
            null
        }
    }

    /**
     * Resets the UI state back to Idle
     */
    fun resetState() {
        _uiState.value = FaceImageUploadUiState.Idle
    }

    /**
     * Reset the entire capture process
     */
    fun resetCapture() {
        currentAngleIndex = 0
        _capturedImages.value = emptyList()
        _uiState.value = FaceImageUploadUiState.Idle
    }

    /**
     * Retry current operation
     */
    fun retry() {
        when (val currentState = _uiState.value) {
            is FaceImageUploadUiState.Error -> {
                showInstructionsForCurrentAngle()
            }
            else -> {
                showInstructionsForCurrentAngle()
            }
        }
    }
}
