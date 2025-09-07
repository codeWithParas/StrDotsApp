package com.xyz.strapp.presentation.strliveliness

import android.app.Application
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.face.Face
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class LivenessViewModel @Inject constructor(
    private val application: Application // Application context
) : ViewModel() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceAnalyzer: FaceLivenessAnalyzer

    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady

    // Holds the liveness result for each detected face
    private val _livenessResults = MutableStateFlow<Map<Face, Boolean>>(emptyMap())
    val livenessResults: StateFlow<Map<Face, Boolean>> = _livenessResults

    // Optional: If you still want to separately track all detected faces' bounding boxes
    // private val _detectedFaces = MutableStateFlow<List<Face>>(emptyList())
    // val detectedFaces: StateFlow<List<Face>> = _detectedFaces

    init {
        // ViewModel initialization
    }

    fun onPermissionGranted() {
        _isCameraReady.value = true
        Log.d("LivenessViewModel", "Camera permission granted by user.")
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(application)
        cameraExecutor = Executors.newSingleThreadExecutor()


        // Initialize FaceLivenessAnalyzer with Context and the new callback
        faceAnalyzer = FaceLivenessAnalyzer(
            context = application, // Pass application context
            onLivenessResults = { results ->
                _livenessResults.value = results
                if (results.isNotEmpty()) {
                    results.forEach { (face, isLive) ->
                        Log.d("LivenessViewModel", "Face BBox: ${face.boundingBox}, Is Live: $isLive")
                        // TODO: Implement UI logic based on liveness (e.g., enable capture, show messages)
                        // Example: if (isLive) { /* enable capture button */ }
                    }
                } else {
                    Log.d("LivenessViewModel", "No faces detected or processed for liveness.")
                }
            },
            onError = { exception ->
                Log.e("LivenessViewModel", "Liveness Analysis Error", exception)
                // TODO: Handle errors (e.g., show error message to user)
            }
        )

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreviewAndAnalysis(cameraProvider, lifecycleOwner, surfaceProvider)
        }, ContextCompat.getMainExecutor(application))
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
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT) // Or your preferred lens
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            Log.d("LivenessViewModel", "CameraX Use cases bound to lifecycle")
        } catch (exc: Exception) {
            Log.e("LivenessViewModel", "Use case binding failed", exc)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
        if (::faceAnalyzer.isInitialized) {
            faceAnalyzer.stop() // Release TFLite Interpreter and ML Kit resources
        }
    }
}
