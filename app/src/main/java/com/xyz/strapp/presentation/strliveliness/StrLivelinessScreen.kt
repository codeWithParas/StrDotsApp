
package com.xyz.strapp.presentation.strliveliness

import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column // Added for centering messages
import androidx.compose.foundation.layout.Spacer // Added
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height // Added
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size // Added for icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons // Added
import androidx.compose.material.icons.filled.CheckCircle // Added
import androidx.compose.material.icons.filled.Error // Added
import androidx.compose.material3.CircularProgressIndicator // Added
import androidx.compose.material3.Icon // Added
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LivelinessScreen(viewModel: LivenessViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionGranted()
        } else {
            Log.d("StrLivelinessScreen", "Camera permission denied")
            // Consider updating UI state here via ViewModel to show a persistent message
        }
    }

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(key1 = lifecycleOwner) {
        permissionState.launch(Manifest.permission.CAMERA)
    }

    val isCameraReady by viewModel.isCameraReady.collectAsState()
    val livenessResults by viewModel.livenessResults.collectAsState() // For general face info
    val uiState by viewModel.uiState.collectAsState()
    val countdownValue by viewModel.countdownValue.collectAsState()
    val isTimerVisible by viewModel.isTimerVisible.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCameraReady) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
                update = {
                    viewModel.startCamera(
                        lifecycleOwner = lifecycleOwner,
                        surfaceProvider = previewView.surfaceProvider
                    )
                }
            )
            if (uiState is LivenessScreenUiState.Detecting || uiState is LivenessScreenUiState.CountdownRunning) {
                 AnimatedFaceGuideOverlay(modifier = Modifier.fillMaxSize())
            }

        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text(
                    text = "Requesting camera permission or initializing camera...",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        val statusText = if (livenessResults.isEmpty()) {
            "No face detected."
        } else {
            val liveFaces = livenessResults.count { it.value.isLive }
            val spoofFaces = livenessResults.count { !it.value.isLive }
            when {
                //liveFaces == 0 -> "Live face(s) detected: $liveFaces"
                liveFaces > 0 -> "Face detected."
                spoofFaces > 0 -> "Spoof detected."
                else -> "Processing..." // Should not happen if livenessResults is not empty and all are processed
            }
        }

        // Display UI based on LivenessScreenUiState
        when (val state = uiState) {
            is LivenessScreenUiState.Detecting -> {
                LivenessStatusMessageOverlay(
                    message = "Position your face in the Oval and blick to start detection.\n $statusText",
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            is LivenessScreenUiState.CountdownRunning -> {
                if (isTimerVisible && countdownValue > 0) {
                    val countMsg = when(countdownValue){
                        5, 4 -> "Position your face in the guided frame."
                        else -> "Please try and remain still."
                    }
                    CountdownOverlayTopMsg(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
                        /*if(statusText.contains("Spoof")) statusText else*/ countMsg
                    )
                    CountdownOverlay(countdownValue = countdownValue, statusText = statusText)
                }
            }
            is LivenessScreenUiState.ProcessingCapture -> {
                ProcessingOverlay(message = "Processing capture...")
            }
            is LivenessScreenUiState.CaptureSuccess -> {
                SuccessErrorOverlay(message = state.message, isError = false)
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
            is LivenessScreenUiState.CaptureError -> {
                SuccessErrorOverlay(message = state.message, isError = true)
            }
            LivenessScreenUiState.Idle -> {
                // Nothing specific for idle, or could be initial instructions
            }
        }
    }
}

@Composable
fun LivenessStatusMessageOverlay(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = modifier.padding(bottom = 50.dp),
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CountdownOverlayTopMsg(modifier: Modifier = Modifier, statusText: String) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter // 👈 keeps things centered horizontally, at the top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), shape = CircleShape)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun CountdownOverlay(countdownValue: Int, modifier: Modifier = Modifier, statusText: String) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = countdownValue.toString(),
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), shape = CircleShape)
                    .padding(horizontal = 30.dp, vertical = 10.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = statusText, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }

    }
}

@Composable
fun ProcessingOverlay(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun SuccessErrorOverlay(message: String, isError: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isError) Icons.Filled.Error else Icons.Filled.CheckCircle,
                contentDescription = if (isError) "Error" else "Success",
                tint = if (isError) Color.Red else Color.Green,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}


@Composable
fun AnimatedFaceGuideOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_overlay")
    val pulseStrength by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse_strength_overlay"
    )
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, delayMillis = 100),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha_overlay"
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val ovalWidth = canvasWidth * 0.90f //0.75f
        val ovalHeight = ovalWidth * 1.3f //1.3f
        val ovalLeft = (canvasWidth - ovalWidth) / 2
        val ovalTop = (canvasHeight - ovalHeight) * 0.4f // Position slightly higher than true center

        val ovalRect = Rect(ovalLeft, ovalTop, ovalLeft + ovalWidth, ovalTop + ovalHeight)
        val ovalPath = Path().apply { addOval(ovalRect) }

        val overlayColor = Color.Black.copy(alpha = 0.5f)
        val fullCanvasRectPath = Path().apply { addRect(Rect(0f, 0f, canvasWidth, canvasHeight)) }
        val cutoutPath = Path.combine(
            operation = PathOperation.Difference,
            path1 = fullCanvasRectPath,
            path2 = ovalPath
        )
        drawPath(cutoutPath, color = overlayColor)

        drawOval(
            color = Color.White.copy(alpha = 0.7f),
            topLeft = Offset(ovalLeft, ovalTop),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(width = 2.dp.toPx())
        )

        drawOval(
            color = Color.Green.copy(alpha = animatedAlpha),
            topLeft = Offset(
                ovalLeft - (ovalWidth * (pulseStrength - 1) / 2),
                ovalTop - (ovalHeight * (pulseStrength - 1) / 2)
            ),
            size = Size(ovalWidth * pulseStrength, ovalHeight * pulseStrength),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
