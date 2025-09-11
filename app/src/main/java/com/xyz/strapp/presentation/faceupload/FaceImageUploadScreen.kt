package com.xyz.strapp.presentation.faceupload

import android.Manifest
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.xyz.strapp.ui.theme.StrAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun FaceImageUploadScreen(
    viewModel: FaceImageUploadViewModel = hiltViewModel(),
    onUploadSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val capturedImages by viewModel.capturedImages.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Camera permission
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is FaceImageUploadUiState.AllImagesCompleted -> {
                scope.launch {
                    snackbarHostState.showSnackbar("All ${state.totalUploaded} images uploaded successfully!")
                }
                delay(2000) // Show message for 2 seconds
                onUploadSuccess()
            }
            is FaceImageUploadUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Error: ${state.message}")
                }
                viewModel.resetState()
            }
            else -> {
                // Other states handled in UI
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Angle Capture") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !cameraPermissionState.status.isGranted -> {
                    // Show permission request UI
                    PermissionRequestContent(
                        showRationale = cameraPermissionState.status.shouldShowRationale,
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                }
                
                else -> {
                    when (val state = uiState) {
                        is FaceImageUploadUiState.ShowingInstructions -> {
                            // Show instructions for current angle
                            InstructionsContent(
                                angle = state.currentAngle,
                                capturedCount = capturedImages.size,
                                totalCount = 5,
                                onProceed = { viewModel.startCaptureForCurrentAngle() }
                            )
                        }
                        
                        is FaceImageUploadUiState.CapturingAngle -> {
                            // Show camera preview for specific angle
                            AngleCameraContent(
                                angle = state.currentAngle,
                                onImageCaptured = { bitmap -> viewModel.onImageCaptured(bitmap) }
                            )
                        }
                        
                        is FaceImageUploadUiState.ImageCaptured -> {
                            // Show captured image with options to retake or upload
                            MultiAngleImagePreviewContent(
                                angle = state.currentAngle,
                                bitmap = state.capturedBitmap,
                                onConfirm = { viewModel.confirmAndUploadImage(state.capturedBitmap) },
                                onRetake = { viewModel.retakeCurrentImage() }
                            )
                        }
                        
                        is FaceImageUploadUiState.UploadingImage -> {
                            // Show uploading progress
                            UploadingContent(
                                angle = state.currentAngle,
                                imageNumber = state.imageNumber
                            )
                        }
                        
                        is FaceImageUploadUiState.ImageUploaded -> {
                            // Show upload success with next angle info
                            UploadSuccessContent(
                                uploadedCount = state.uploadedCount,
                                totalImages = state.totalImages,
                                nextAngle = state.nextAngle,
                                onProceed = { viewModel.proceedToNextAngle() }
                            )
                        }
                        
                        is FaceImageUploadUiState.AllImagesCompleted -> {
                            // Show completion screen
                            CompletionContent(totalUploaded = state.totalUploaded)
                        }
                        
                        else -> {
                            // Show initial instruction screen
                            InstructionContent(
                                onStartCapture = { viewModel.startCapture() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestContent(
    showRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (showRationale) {
                "Camera access is needed to capture your face image for identity verification. This helps secure your account."
            } else {
                "To upload your face image, we need permission to access your camera."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Camera Permission")
        }
    }
}

@Composable
private fun InstructionContent(
    onStartCapture: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Multi-Angle Face Capture",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "We'll capture 5 images from different angles for enhanced security. Make sure:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InstructionItem("• Face is clearly visible")
                    InstructionItem("• Good lighting conditions")
                    InstructionItem("• Follow each angle instruction")
                    InstructionItem("• Remove sunglasses or hats")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onStartCapture,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start Multi-Angle Capture",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InstructionItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun CameraPreviewContent(
    onImageCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { preview ->
                    previewView = preview
                    
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val previewUseCase = CameraPreview.Builder().build().also {
                            it.setSurfaceProvider(preview.surfaceProvider)
                        }
                        
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        
                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                        
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                previewUseCase,
                                imageAnalysis
                            )
                        } catch (exc: Exception) {
                            Log.e("CameraPreview", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Face oval overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radiusX = size.width * 0.35f
            val radiusY = size.height * 0.25f
            
            drawOval(
                color = Color.White,
                topLeft = Offset(center.x - radiusX, center.y - radiusY),
                size = androidx.compose.ui.geometry.Size(radiusX * 2, radiusY * 2),
                style = Stroke(width = 4.dp.toPx())
            )
        }
        
        // Instructions overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Position your face within the oval",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        
        // Capture button
        FloatingActionButton(
            onClick = {
                // Take a snapshot of the preview
                previewView?.let { preview ->
                    // This is a simplified capture - in a real app you'd use ImageCapture use case
                    // For now, we'll simulate with a placeholder
                    captureImageFromPreview(preview, onImageCaptured)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
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
            // Cleanup camera resources if needed
        }
    }
}

// Simplified image capture - in a real implementation you'd use ImageCapture use case
private fun captureImageFromPreview(previewView: PreviewView, onImageCaptured: (Bitmap) -> Unit) {
    // This is a placeholder implementation
    // In a real app, you'd implement proper image capture using CameraX ImageCapture use case
    // For now, creating a dummy bitmap
    val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(android.graphics.Color.GRAY)
    
    onImageCaptured(bitmap)
}

@Composable
private fun InstructionsContent(
    angle: CaptureAngle,
    capturedCount: Int,
    totalCount: Int,
    onProceed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Progress indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$capturedCount of $totalCount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = angle.instruction,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = angle.description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Ready to Capture",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AngleCameraContent(
    angle: CaptureAngle,
    onImageCaptured: (Bitmap) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Instruction overlay
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = angle.instruction,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Camera preview
        Box(
            modifier = Modifier.weight(1f)
        ) {
            CameraPreviewContent(
                onImageCaptured = onImageCaptured
            )
        }
    }
}

@Composable
private fun MultiAngleImagePreviewContent(
    angle: CaptureAngle,
    bitmap: Bitmap,
    onConfirm: () -> Unit,
    onRetake: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Preview - ${angle.instruction}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Captured image",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(12.dp))
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retake")
            }
            
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm")
            }
        }
    }
}

@Composable
private fun UploadingContent(
    angle: CaptureAngle,
    imageNumber: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Uploading Image $imageNumber",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Uploading ${angle.instruction.lowercase()}...",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UploadSuccessContent(
    uploadedCount: Int,
    totalImages: Int,
    nextAngle: CaptureAngle?,
    onProceed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .background(
                    Color.Green.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .padding(16.dp),
            tint = Color.Green
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "$uploadedCount image${if (uploadedCount > 1) "s" else ""} uploaded successfully!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Green
        )
        
        if (nextAngle != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Next: ${nextAngle.instruction}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onProceed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun CompletionContent(
    totalUploaded: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .background(
                    Color.Green.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .padding(20.dp),
            tint = Color.Green
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "All Done!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Green
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "$totalUploaded images uploaded successfully for attendance registration.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FaceImageUploadScreenPreview() {
    StrAppTheme {
        FaceImageUploadScreen(
            onUploadSuccess = { },
            onNavigateBack = { }
        )
    }
}
