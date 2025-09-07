package com.xyz.strapp.presentation.strliveliness

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.face.Face

// Removed: import androidx.activity.result.launch // Not directly used in this snippet anymore
@Composable
fun LivelinessScreen(viewModel: LivenessViewModel = hiltViewModel()) { // Renamed for clarity
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionGranted()
        } else {
            // Handle permission denial: show a message, etc.
            Log.d("StrLivelinessScreen", "Camera permission denied")
            // You might want to show a persistent message on screen here
        }
    }

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(key1 = lifecycleOwner) {
        permissionState.launch(Manifest.permission.CAMERA)
    }

    val isCameraReady by viewModel.isCameraReady.collectAsState()
    val livenessResults by viewModel.livenessResults.collectAsState()

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
            // This is the animated placeholder overlay
            AnimatedFaceGuideOverlay(modifier = Modifier.fillMaxSize())
        } else {
            Text(
                text = "Requesting camera permission or initializing camera...",
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Display Liveness Status
        LivenessStatusOverlay(
            livenessResults = livenessResults,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
fun LivenessStatusOverlay(
    livenessResults: Map<Face, Boolean>,
    modifier: Modifier = Modifier
) {
    val statusText = if (livenessResults.isEmpty()) {
        "No face detected. Please position your face in the camera."
    } else {
        // For simplicity, checking if any face is live.
        // You could iterate and display info for multiple faces if needed.
        val liveFaces = livenessResults.count { it.value }
        val spoofFaces = livenessResults.count { !it.value }

        when {
            //liveFaces == 0 -> "Live face(s) detected: $liveFaces"
            liveFaces > 0 -> "Live face(s) detected: $liveFaces"
            spoofFaces > 0 -> "Spoof attempt detected. Please ensure it's a real person."
            else -> "Processing..." // Should not happen if livenessResults is not empty and all are processed
        }
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier
            .padding(8.dp)
// Add a background to make text more readable, e.g.
// .background(Color.Black.copy(alpha = 0.5f))
// .padding(8.dp)
    )
}

@Composable
fun AnimatedFaceGuideOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseStrength by infiniteTransition.animateFloat(
        initialValue = 0.9f, // Start slightly scaled down or less opaque
        targetValue = 1.1f, // Scale up or more opaque
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseStrength"
    )
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, delayMillis = 100),
            repeatMode = RepeatMode.Reverse
        ), label = "animatedAlpha"
    )


    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Define the oval size and position (centered)
        // Make it slightly smaller than full width, and portrait-oriented
        val ovalWidth = canvasWidth * 0.90f  // 0.75f
        val ovalHeight = ovalWidth * 1.3f // Adjust aspect ratio as needed

        val ovalLeft = (canvasWidth - ovalWidth) / 2
        val ovalTop = (canvasHeight - ovalHeight) / 2

        val ovalRect = Rect(ovalLeft, ovalTop, ovalLeft + ovalWidth, ovalTop + ovalHeight)
        val ovalPath = Path().apply { addOval(ovalRect) }

        // 1. Semi-transparent scrim around the oval (cut-out effect)
        clipPath(ovalPath) {
            // This clear will make the oval area transparent if drawn on a colored background.
            // However, since we are overlaying on camera, we might not need this exact effect
            // if we are just drawing borders.
            // For a "cut-out" effect from a solid color overlay:
             //drawRect(color = Color.Transparent.copy(alpha = 0.5f), blendMode = BlendMode.SrcOut)
             drawRect(color = Color.Transparent.copy(alpha = 0.1f), blendMode = BlendMode.SrcOut)
        }
        // Instead of a full scrim, draw a dark overlay and then "clear" the oval area.
        // This is a common way to create a "cutout"
        val overlayColor = Color.Black.copy(alpha = 0.4f)
        val fullCanvasRect = Path().apply { addRect(Rect(0f,0f, canvasWidth, canvasHeight))}
        /*val cutoutPath = Path.combine(
            operation = Path.Op.Difference, // Subtract oval from full rect
            path1 = fullCanvasRect,
            path2 = ovalPath
        )
        drawPath(cutoutPath, color = overlayColor)*/


        // 2. Static inner guideline (optional, could be dashed)
        drawOval(
            color = Color.White.copy(alpha = 0.6f),
            topLeft = Offset(ovalLeft, ovalTop),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(width = 2.dp.toPx())
        )

        // 3. Animated pulsing outer border
        drawOval(
            color = Color.Green.copy(alpha = animatedAlpha), // Pulsing alpha
            topLeft = Offset(
                ovalLeft - (ovalWidth * (pulseStrength -1) / 2) , // Center the pulse expansion
                ovalTop - (ovalHeight * (pulseStrength -1) /2)
            ),
            size = Size(ovalWidth * pulseStrength, ovalHeight * pulseStrength),
            style = Stroke(width = 3.dp.toPx())
        )

        // Text instruction (optional)
        // You might want to use Text Composable outside Canvas for better text rendering
    }
}


/*
import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CameraPreviewScreen(viewModel: LivenessViewModel = hiltViewModel()) { // Assuming LivenessViewModel
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionGranted()
        } else {
            // Handle permission denial: show a message, etc.
            Log.d("CameraPreviewScreen", "Camera permission denied")
        }
    }

    // This will hold the PreviewView
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(key1 = lifecycleOwner) { // Use lifecycleOwner or true if you only want to launch once
        permissionState.launch(Manifest.permission.CAMERA)
    }

    // Observe a state from ViewModel, e.g., if permission is granted
    val isCameraReady by viewModel.isCameraReady.collectAsState()

    if (isCameraReady) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                viewModel.startCamera(
                    lifecycleOwner = lifecycleOwner,
                    surfaceProvider = previewView.surfaceProvider
                )
            }
        )
    } else {
        // Show a placeholder, loading indicator, or permission request message
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Requesting camera permission or initializing camera...")
        }
    }
}*/
