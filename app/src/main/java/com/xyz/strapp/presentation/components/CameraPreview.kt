package com.xyz.strapp.presentation.components

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    onImageCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    
    // Camera executor
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { preview ->
                    previewView = preview
                    
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val provider = cameraProviderFuture.get()
                            cameraProvider = provider
                            
                            val previewUseCase = Preview.Builder().build().also {
                                it.setSurfaceProvider(preview.surfaceProvider)
                            }
                            
                            val imageCaptureUseCase = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            
                            imageCapture = imageCaptureUseCase
                            
                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    previewUseCase,
                                    imageCaptureUseCase
                                )
                            } catch (exc: Exception) {
                                Log.e("CameraPreview", "Use case binding failed", exc)
                            }
                        } catch (exc: Exception) {
                            Log.e("CameraPreview", "Camera provider initialization failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Capture button
        FloatingActionButton(
            onClick = {
                captureImage(context, imageCapture, cameraExecutor, onImageCaptured)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capture",
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

private fun captureImage(
    context: android.content.Context,
    imageCapture: ImageCapture?,
    cameraExecutor: ExecutorService,
    onImageCaptured: (Bitmap) -> Unit
) {
    val imageCapture = imageCapture ?: return
    
    // Create time-stamped output file
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(System.currentTimeMillis())
    val outputFile = File(context.externalCacheDir, "$name.jpg")
    
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    
    imageCapture.takePicture(
        outputFileOptions,
        cameraExecutor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraPreview", "Photo capture failed: ${exception.message}", exception)
            }
            
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    // Convert the saved file to bitmap
                    val bitmap = android.graphics.BitmapFactory.decodeFile(outputFile.absolutePath)
                    if (bitmap != null) {
                        onImageCaptured(bitmap)
                    } else {
                        Log.e("CameraPreview", "Failed to decode bitmap from file")
                    }
                    
                    // Clean up the temporary file
                    outputFile.delete()
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Error processing captured image", e)
                }
            }
        }
    )
}
