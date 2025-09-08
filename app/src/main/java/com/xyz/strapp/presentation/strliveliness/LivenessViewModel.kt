package com.xyz.strapp.presentation.strliveliness

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.face.Face
import com.xyz.strapp.domain.repository.FaceLivenessRepository
import com.xyz.strapp.worker.ImageUploadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

// Define UI States
sealed interface LivenessScreenUiState {
    object Idle : LivenessScreenUiState
    object Detecting : LivenessScreenUiState // Camera is running, actively looking for faces
    object CountdownRunning : LivenessScreenUiState
    object ProcessingCapture : LivenessScreenUiState
    data class CaptureSuccess(val message: String) : LivenessScreenUiState
    data class CaptureError(val message: String) : LivenessScreenUiState
}

@HiltViewModel
class LivenessViewModel @Inject constructor(
    private val application: Application, // Application context
    private val faceLivenessRepository: FaceLivenessRepository,
    //private val workManager: CheckInWorkManager
) : ViewModel() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceAnalyzer: FaceLivenessAnalyzer

    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    private val _livenessResults = MutableStateFlow<Map<Face, FaceLivenessData>>(emptyMap())
    val livenessResults: StateFlow<Map<Face, FaceLivenessData>> = _livenessResults.asStateFlow()

    private val _uiState = MutableStateFlow<LivenessScreenUiState>(LivenessScreenUiState.Idle)
    val uiState: StateFlow<LivenessScreenUiState> = _uiState.asStateFlow()

    private val _countdownValue = MutableStateFlow(0) // 0 means timer not running or finished
    val countdownValue: StateFlow<Int> = _countdownValue.asStateFlow()

    private val _isTimerVisible = MutableStateFlow(false)
    val isTimerVisible: StateFlow<Boolean> = _isTimerVisible.asStateFlow()

    private var capturedImageForProcessing: Bitmap? = null
    private var countdownJob: Job? = null
    private var lastTrackedFaceIdForCountdown: Int? = null

    companion object {
        private const val TAG = "LivenessViewModel"
        private const val COUNTDOWN_SECONDS = 5
        private const val UPLOAD_WORK_TAG = "ImageUploadWork"
    }

    init {
        _uiState.value = LivenessScreenUiState.Detecting
        // Optionally, prune any previous work if it makes sense for your app logic
        // workManager.pruneWork()
    }

    fun onPermissionGranted() {
        _isCameraReady.value = true
        Log.d(TAG, "Camera permission granted by user.")
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(application)
        cameraExecutor = Executors.newSingleThreadExecutor()

        faceAnalyzer = FaceLivenessAnalyzer(
            context = application,
            onLivenessResults = { results ->
                _livenessResults.value = results
                if (countdownJob?.isActive == true && lastTrackedFaceIdForCountdown != null) {
                    val trackedFaceResult = results.entries.find { it.key.trackingId == lastTrackedFaceIdForCountdown }
                    if (trackedFaceResult == null || !trackedFaceResult.value.isLive) {
                        Log.d(TAG, "###@@@ Face for countdown lost or became spoof. Cancelling timer.")
                        viewModelScope.launch {
                            //delay(3000)
                            cancelCountdown()
                        }
                    }
                }
            },
            onLiveFaceDetectedForCountdown = { faceBitmap, results ->
                if (countdownJob == null || countdownJob?.isCompleted == true) {
                    Log.d(TAG, "###@@@ Live face detected, starting countdown.")
                    capturedImageForProcessing = faceBitmap
                    // Check if face entry already made for same face tracking id
                    val faceEntry = _livenessResults.value.entries.firstOrNull { it.value.isLive && it.value.faceBitmap == faceBitmap && it.key.trackingId == lastTrackedFaceIdForCountdown }
                    //lastTrackedFaceIdForCountdown = faceEntry?.key?.trackingId
                    Log.d(TAG, "###@@@ Scanned Tracking Id -- ${faceEntry?.key?.trackingId}")
                    if(faceEntry == null) {
                        Log.d(TAG, "###@@@ Countdown Started.")
                        lastTrackedFaceIdForCountdown = results.entries.first().key.trackingId
                        Log.d(TAG, "###@@@ LastTrackedFaceIdForCountdown -- $lastTrackedFaceIdForCountdown")
                        _livenessResults.value = results
                        startCountdown()
                    }
                }
            },
            onError = { exception ->
                Log.e(TAG, "###@@@ Liveness Analysis Error", exception)
                _uiState.value = LivenessScreenUiState.CaptureError("Analysis Error: ${exception.message}")
                cancelCountdown()
            }
        )

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreviewAndAnalysis(cameraProvider, lifecycleOwner, surfaceProvider)
        }, ContextCompat.getMainExecutor(application))
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _isTimerVisible.value = true
            _uiState.value = LivenessScreenUiState.CountdownRunning
            for (i in COUNTDOWN_SECONDS downTo 1) {
                _countdownValue.value = i
                Log.d(TAG, "Countdown: $i")
                delay(900)
            }
            _countdownValue.value = 0
            _isTimerVisible.value = false
            Log.d(TAG, "###@@@ Countdown finished.")
            processImageCapture()
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _isTimerVisible.value = false
        _countdownValue.value = 0
        capturedImageForProcessing = null
        lastTrackedFaceIdForCountdown = null
        if (_uiState.value is LivenessScreenUiState.CountdownRunning) {
            _uiState.value = LivenessScreenUiState.Detecting
        }
        Log.d(TAG, "###@@@ Countdown cancelled.")
    }

    private fun processImageCapture() {
        val bitmapToSave = capturedImageForProcessing
        if (bitmapToSave == null) {
            Log.e(TAG, "###@@@ Bitmap for processing is null after countdown.")
            _uiState.value = LivenessScreenUiState.CaptureError("Error: No image to capture.")
            return
        }

        val finalCheckFaceResult = _livenessResults.value.entries.find { it.key.trackingId == lastTrackedFaceIdForCountdown }
        if (finalCheckFaceResult == null || !finalCheckFaceResult.value.isLive) {
            Log.w(TAG, "###@@@ Face for capture no longer live or available at the moment of capture.")
            _uiState.value = LivenessScreenUiState.CaptureError("Face lost before capture completed.")
            resetCaptureState()
            return
        }

        _uiState.value = LivenessScreenUiState.ProcessingCapture
        viewModelScope.launch {
            try {
                Log.d(TAG, "###@@@ Attempting to save image to repository.")
                // Changed to use the correct repository method name
                val imageId = faceLivenessRepository.saveFaceImage(bitmapToSave)
                if (imageId?.first != null) {
                    _uiState.value = LivenessScreenUiState.CaptureSuccess("Attendance Marked Successfully!")
                    Log.d(TAG, "###@@@ Image saved successfully with ID: $imageId. Start Enqueuing upload worker.")
                    //enqueueImageUploadWorker()
                    faceLivenessRepository.startCheckIn(imageId.first, imageId.second)
                } else {
                    _uiState.value = LivenessScreenUiState.CaptureError("Failed to save image locally.")
                    Log.e(TAG, "###@@@ Failed to save image to repository (returned null ID).")
                }
            } catch (e: Exception) {
                Log.e(TAG, "###@@@ Error saving image to repository", e)
                _uiState.value = LivenessScreenUiState.CaptureError("###@@@ Failed to save image: ${e.message}")
            }
            resetCaptureState()
        }
    }

    private fun enqueueImageUploadWorker() {
        // Initialize WorkManager
        val workManager = WorkManager.getInstance(application)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<ImageUploadWorker>()
            .setConstraints(constraints)
            // You can add a tag to observe or cancel the work by this tag if needed
            .addTag(UPLOAD_WORK_TAG)
            .build()

        //workManager.enqueue(uploadWorkRequest)
        // For unique work, if you only want one instance of this worker running:
        workManager.enqueueUniqueWork(
            UPLOAD_WORK_TAG,
            ExistingWorkPolicy.REPLACE, // or KEEP, or APPEND
            uploadWorkRequest
        )
        Log.d(TAG, "###@@@ ImageUploadWorker enqueued.")
    }

    private fun resetCaptureState() {
        capturedImageForProcessing = null
        lastTrackedFaceIdForCountdown = null
        viewModelScope.launch {
            delay(3000) 
            if (_uiState.value is LivenessScreenUiState.CaptureSuccess || _uiState.value is LivenessScreenUiState.CaptureError) {
                 _uiState.value = LivenessScreenUiState.Detecting
            }
        }
    }

    private fun bindPreviewAndAnalysis(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider
    ) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(surfaceProvider)
        }
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, faceAnalyzer)
            }
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            _uiState.value = LivenessScreenUiState.Detecting
            Log.d(TAG, "CameraX Use cases bound to lifecycle")
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            _uiState.value = LivenessScreenUiState.CaptureError("Camera setup failed: ${exc.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
        if (::faceAnalyzer.isInitialized) {
            faceAnalyzer.stop()
        }
        countdownJob?.cancel()
    }
}

