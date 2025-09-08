package com.xyz.strapp.presentation.strliveliness

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min
import androidx.core.graphics.get
import kotlin.math.abs

/**
 * Data class to hold liveness status and the bitmap for a detected face.
 */
data class FaceLivenessData(
    val isLive: Boolean,
    val faceBitmap: Bitmap? // Bitmap of the cropped face
)

private data class FaceFrameSnapshot(
    val timestamp: Long,
    val boundingBoxCenterX: Int,
    val boundingBoxCenterY: Int,
    val headEulerAngleY: Float, // Yaw
    val headEulerAngleZ: Float, // Roll
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?
)

class FaceLivenessAnalyzer(
    context: Context,
    private val onLivenessResults: (results: Map<Face, FaceLivenessData>) -> Unit,
    private val onLiveFaceDetectedForCountdown: (faceBitmap: Bitmap, results: Map<Face, FaceLivenessData>) -> Unit, // New callback
    private val onError: (exception: Exception) -> Unit
) : ImageAnalysis.Analyzer {

    private var croppedBitmap: Bitmap
    private val faceDetector: FaceDetector
    private val tfLiteInterpreter: Interpreter
    private val tfLiteInputImageWidth: Int
    private val tfLiteInputImageHeight: Int

    // State for managing countdown trigger
    private var liveFaceCandidateReported = false
    private var lastTrackedLiveFaceId: Int? = null
    private val CROP_SIZE: Int = 1000
    private val TF_OD_API_INPUT_SIZE2: Int = 160

    private val MAX_HISTORY_SIZE: Int = 12
    private val MIN_FRAMES_FOR_DYNAMIC_CHECK: Int = 10
    private val STILLNESS_THRESHOLD_POSITION = 5 // Max allowed pixels change for X or Y
    // Min probability for an eye to be considered closed (i.e. if < threshold, then closed)
    private val BLINK_EYE_CLOSED_PROB_THRESHOLD = 0.5f
    // Max probability for an eye to be considered open (i.e. if > threshold, then open)
    private val BLINK_EYE_OPEN_PROB_THRESHOLD = 0.7f
    // Max allowed degrees change for Yaw or Roll
    private val STILLNESS_THRESHOLD_ANGLE = 3.0f


    private val faceSnapshotHistory = ArrayDeque<FaceFrameSnapshot>(MAX_HISTORY_SIZE)

    init {
        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .enableTracking() // Enable face tracking for stable IDs
            .build()

        faceDetector = FaceDetection.getClient(highAccuracyOpts)

        // Load TFLite model and get input image dimensions
        try {
            val modelBuffer = loadModelFile(context, MODEL_FILENAME)
            tfLiteInterpreter = Interpreter(modelBuffer)
            val inputTensor = tfLiteInterpreter.getInputTensor(0)
            tfLiteInputImageWidth = inputTensor.shape()[1] // Typically height
            tfLiteInputImageHeight = inputTensor.shape()[2] // Typically width
            Log.d(TAG, "TFLite model loaded. Input shape: ${inputTensor.shape().joinToString()}")

            croppedBitmap = createBitmap(CROP_SIZE, CROP_SIZE)

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TFLite interpreter", e)
            onError(e)
            throw e // Rethrow to signal critical failure
        }
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val originalBitmap = imageProxyToBitmap(imageProxy) // Get the full frame bitmap once

            faceDetector.process(inputImage)
                .addOnSuccessListener { detectedFaces ->
                    val resultsMap = mutableMapOf<Face, FaceLivenessData>()
                    var liveFaceFoundInCurrentFrame = false
                    var currentFrameTrackedLiveFaceId: Int? = null
                    var candidateBitmapForCountdown: Bitmap? = null

                    if (detectedFaces.isEmpty()) {
                        // No faces detected, reset countdown trigger if it was active
                        if (liveFaceCandidateReported) {
                            Log.d(TAG, "###@@@ No faces detected, resetting countdown trigger state.")
                            liveFaceCandidateReported = false
                            lastTrackedLiveFaceId = null
                        }
                        // No faces detected, no need to continue
                        onLivenessResults(emptyMap())
                    } else {

                        // Find the face with the largest bounding box area
                        val largestFace = detectedFaces.maxByOrNull {
                            val box = it.boundingBox
                            (box.width() * box.height()).toFloat() // Calculate area
                        } ?: detectedFaces.first()
                        if (largestFace == null) {
                            // Should not happen if detectedFaces is not empty, but handle defensively
                            if (liveFaceCandidateReported) {
                                Log.d(TAG, "###@@@ No largest face found (unexpected), resetting countdown trigger state.")
                                liveFaceCandidateReported = false
                                lastTrackedLiveFaceId = null
                            }
                            noFaceDetectedPrompt()
                            onLivenessResults(emptyMap())
                        } else {
                            val faceBitmap = cropFaceFromBitmap(originalBitmap, largestFace.boundingBox, imageProxy.imageInfo.rotationDegrees)
                            if (faceBitmap == null) {
                                Log.w(TAG, "###@@@ Could not crop face bitmap for face ID: ${largestFace.trackingId}")
                                resultsMap[largestFace] = FaceLivenessData(isLive = false, faceBitmap = null)
                                noFaceDetectedPrompt()
                            } else {
                                //19:17:24.987  D  ###@@@ Liveness score: {Face{boundingBox=Rect(171, 229 - 338, 396), trackingId=9, rightEyeOpenProbability=0.9991688, leftEyeOpenProbability=0.9992078, smileProbability=0.0114077125, eulerX=-6.1341376, eulerY=2.9608095, eulerZ=0.79010594, landmarks=Landmarks{landmark_0=FaceLandmark{type=0, position=PointF(256.87433, 357.89822)}, landmark_1=FaceLandmark{type=1, position=PointF(218.45177, 328.70364)}, landmark_3=FaceLandmark{type=3, position=PointF(192.00597, 310.40836)}, landmark_4=FaceLandmark{type=4, position=PointF(227.68192, 293.77405)}, landmark_5=FaceLandmark{type=5, position=PointF(235.17981, 348.45007)}, landmark_6=FaceLandmark{type=6, position=PointF(256.65527, 329.19897)}, landmark_7=FaceLandmark{type=7, position=PointF(293.1048, 327.46542)}, landmark_9=FaceLandmark{type=9, position=PointF(314.76703, 308.33157)}, landmark_10=FaceLandmark{type=10, position=PointF(283.5394, 291.61252)}, landmark_11=FaceLandmark{type=11, position=PointF(276.90454, 350.55127)}}, contours=Contours{Contour_1=FaceContour{type=1, points=[PointF(257.0, 240.0), PointF(268.0, 241.0), PointF(287.0, 243.0), PointF(299.0, 248.0), PointF(307.0, 256.0), PointF(312.0, 266.0), PointF(314.0, 276.0), PointF(314.0, 290.0), PointF(313.0, 304.0), PointF(311.0, 318.0), PointF(309.0, 332.0), PointF(304.0, 346.0), PointF(299.0, 359.0), PointF(293.0, 369.0), PointF(286.0, 378.0), PointF(280.0, 385.0), PointF(274.0, 390.0), PointF(264.0, 395.0), PointF(257.0, 396.0), PointF(249.0, 395.0), PointF(238.0, 391.0), PointF(230.0, 386.0), PointF(222.0, 380.0), PointF(214.0, 370.0), PointF(206.0, 360.0), PointF(199.0, 346.0), PointF(195.0, 331.0), PointF(192.0, 317.0), PointF(191.0, 303.0), PointF(191.0, 289.0), PointF(192.0, 275.0), PointF(196.0, 265.0), PointF(203.0, 255.0), PointF(212.0, 247.0), PointF(226.0, 243.0), PointF(246.0, 240.0)]}, Contour_2=FaceContour{type=2, points=[PointF(204.0, 277.0), PointF(210.0, 273.0), PointF(219.0, 271.0), PointF(231.0, 272.0), PointF(244.0, 273.0)]}, Contour_3=FaceContour{type=3, points=[PointF(208.0, 281.0), PointF(213.0, 278.0), PointF(221.0, 276.0), PointF(232.0, 277.0), PointF(245.0, 281.0)]}, Contour_4=FaceContour{type=4, points=[PointF(306.0, 277.0), PointF(302.0, 273.0), PointF(294.0, 271.0), PointF(283.0, 272.0), PointF(270.0, 273.0)]}, Contour_5=FaceContour{type=5, points=[PointF(304.0, 281.0), PointF(299.0, 278.0), PointF(292.0, 276.0), PointF(282.0, 277.0), PointF(268.0, 281.0)]}, Contour_6=FaceContour{type=6, points=[PointF(215.0, 292.0), PointF(216.0, 291.0), PointF(218.0, 290.0), PointF(221.0, 289.0), PointF(226.0, 288.0), PointF(231.0, 288.0), PointF(235.0, 290.0), PointF(238.0, 292.0), PointF(240.0, 294.0), PointF(239.0, 294.0), PointF(236.0, 294.0), PointF(232.0, 295.0), PointF(228.0, 296.0), PointF(223.0, 295.0), PointF(220.0, 294.0), PointF(217.0, 293.0)]}, Contour_7=FaceContour{type=7, points=[PointF(272.0, 293.0), PointF(273.0, 292.0), PointF(276.0, 290.0), PointF(281.0, 288.0), PointF(286.0, 288.0), PointF(291.0, 289.0), PointF(293.0, 290.0), PointF(295.0, 291.0), PointF(296.0, 291.0), PointF(294.0, 293.0), PointF(292.0, 294.0), PointF(288.0, 295.0), PointF(284.0, 296.0), PointF(279.0, 295.0), PointF(275.0, 294.0), PointF(273.0, 294.0)]}, Contour_8=FaceContour{type=8, points=[PointF(235.0, 358.0), PointF(237.0, 357.0), PointF(240.0, 356.0), PointF(244.0, 355.0), PointF(251.0, 354.0), PointF(257.0, 355.0), PointF(264.0, 354.0), PointF(270.0, 354.0), PointF(274.0, 356.0), PointF(277.0, 357.0), PointF(278.0, 358.0)]}, Contour_9=FaceContour{type=9, points=[PointF(238.0, 358.0), PointF(244.0, 358.0), PointF(248.0, 359.0), PointF(252.0, 359.0), PointF(257.0, 359.0), PointF(262.0, 359.0), PointF(266.0, 358.0), PointF(269.0, 358.0), PointF(275.0, 357.0)]}, Contour_10=FaceContour{type=10, points=[PointF(272.0, 358.0), PointF(270.0, 359.0), PointF(267.0, 359.0), PointF(263.0, 360.0), PointF(258.0, 360.0), PointF(252.0, 360.0), PointF(248.0, 359.0), PointF(244.0, 359.0), PointF(241.0, 358.0)]}, Contour_11=FaceContour{type=11, points=[PointF(276.0, 360.0), PointF(2
                                //19:18:29.371  D  ###@@@ Liveness score: {Face{boundingBox=Rect(197, 237 - 304, 344), trackingId=11, rightEyeOpenProbability=0.9991688, leftEyeOpenProbability=1.0, smileProbability=0.008071166, eulerX=-7.120268, eulerY=3.5157745, eulerZ=3.158121, landmarks=Landmarks{landmark_0=FaceLandmark{type=0, position=PointF(253.31195, 320.4077)}, landmark_1=FaceLandmark{type=1, position=PointF(228.66876, 302.06763)}, landmark_3=FaceLandmark{type=3, position=PointF(211.33502, 290.80765)}, landmark_4=FaceLandmark{type=4, position=PointF(233.3853, 278.85013)}, landmark_5=FaceLandmark{type=5, position=PointF(240.29094, 314.74643)}, landmark_6=FaceLandmark{type=6, position=PointF(252.39685, 301.73297)}, landmark_7=FaceLandmark{type=7, position=PointF(275.14163, 299.38083)}, landmark_9=FaceLandmark{type=9, position=PointF(288.22574, 285.6245)}, landmark_10=FaceLandmark{type=10, position=PointF(268.35632, 275.96805)}, landmark_11=FaceLandmark{type=11, position=PointF(265.0172, 314.68097)}}, contours=Contours{Contour_1=FaceContour{type=1, points=[PointF(250.0, 241.0), PointF(258.0, 241.0), PointF(271.0, 242.0), PointF(280.0, 244.0), PointF(286.0, 249.0), PointF(290.0, 255.0), PointF(292.0, 262.0), PointF(292.0, 270.0), PointF(292.0, 280.0), PointF(292.0, 290.0), PointF(290.0, 300.0), PointF(286.0, 309.0), PointF(282.0, 319.0), PointF(277.0, 326.0), PointF(273.0, 333.0), PointF(269.0, 338.0), PointF(265.0, 343.0), PointF(259.0, 347.0), PointF(254.0, 348.0), PointF(248.0, 347.0), PointF(242.0, 344.0), PointF(237.0, 340.0), PointF(232.0, 335.0), PointF(227.0, 329.0), PointF(222.0, 322.0), PointF(217.0, 312.0), PointF(213.0, 302.0), PointF(210.0, 293.0), PointF(209.0, 283.0), PointF(208.0, 273.0), PointF(208.0, 265.0), PointF(210.0, 257.0), PointF(214.0, 251.0), PointF(219.0, 246.0), PointF(228.0, 243.0), PointF(242.0, 242.0)]}, Contour_2=FaceContour{type=2, points=[PointF(215.0, 267.0), PointF(219.0, 265.0), PointF(224.0, 264.0), PointF(232.0, 264.0), PointF(241.0, 265.0)]}, Contour_3=FaceContour{type=3, points=[PointF(217.0, 270.0), PointF(221.0, 268.0), PointF(226.0, 267.0), PointF(233.0, 268.0), PointF(243.0, 271.0)]}, Contour_4=FaceContour{type=4, points=[PointF(286.0, 265.0), PointF(283.0, 262.0), PointF(277.0, 261.0), PointF(269.0, 263.0), PointF(260.0, 264.0)]}, Contour_5=FaceContour{type=5, points=[PointF(284.0, 267.0), PointF(281.0, 265.0), PointF(276.0, 265.0), PointF(269.0, 266.0), PointF(259.0, 270.0)]}, Contour_6=FaceContour{type=6, points=[PointF(222.0, 278.0), PointF(223.0, 278.0), PointF(224.0, 277.0), PointF(226.0, 276.0), PointF(230.0, 276.0), PointF(233.0, 276.0), PointF(237.0, 277.0), PointF(239.0, 278.0), PointF(240.0, 279.0), PointF(239.0, 279.0), PointF(238.0, 280.0), PointF(235.0, 281.0), PointF(232.0, 281.0), PointF(228.0, 281.0), PointF(226.0, 280.0), PointF(224.0, 279.0)]}, Contour_7=FaceContour{type=7, points=[PointF(262.0, 278.0), PointF(263.0, 277.0), PointF(266.0, 275.0), PointF(269.0, 274.0), PointF(272.0, 274.0), PointF(276.0, 274.0), PointF(278.0, 275.0), PointF(279.0, 275.0), PointF(280.0, 276.0), PointF(278.0, 277.0), PointF(276.0, 278.0), PointF(274.0, 279.0), PointF(271.0, 280.0), PointF(268.0, 279.0), PointF(265.0, 279.0), PointF(263.0, 278.0)]}, Contour_8=FaceContour{type=8, points=[PointF(237.0, 325.0), PointF(238.0, 324.0), PointF(241.0, 323.0), PointF(244.0, 322.0), PointF(248.0, 322.0), PointF(253.0, 323.0), PointF(257.0, 321.0), PointF(261.0, 322.0), PointF(264.0, 322.0), PointF(266.0, 323.0), PointF(267.0, 324.0)]}, Contour_9=FaceContour{type=9, points=[PointF(240.0, 325.0), PointF(244.0, 325.0), PointF(246.0, 326.0), PointF(249.0, 326.0), PointF(253.0, 326.0), PointF(256.0, 326.0), PointF(258.0, 325.0), PointF(261.0, 325.0), PointF(265.0, 324.0)]}, Contour_10=FaceContour{type=10, points=[PointF(263.0, 325.0), PointF(261.0, 326.0), PointF(259.0, 326.0), PointF(256.0, 327.0), PointF(253.0, 328.0), PointF(249.0, 328.0), PointF(246.0, 327.0), PointF(243.0, 326.0), PointF(242.0, 326.0)]}, Contour_11=FaceContour{type=11, points=[PointF(266.0, 326.0), PointF(264.0, 328.0

                                // Extra checks
                                createFaceSnapShotHistory(largestFace)

                                //val isFaceLive = isLive(faceBitmap)  // Perform liveness check
                                val isFaceLive = isLiveOld(faceBitmap)  // Perform liveness check - Working
                                var finalIsLive = isFaceLive
                                resultsMap[largestFace] = FaceLivenessData(isLive = isFaceLive, faceBitmap = faceBitmap)
                                Log.d(TAG, "###@@@ isFaceLive : $isFaceLive")
                                Log.d(TAG, "###@@@ Liveness score: $resultsMap")

                                if (isFaceLive) {
                                    liveFaceFoundInCurrentFrame = true
                                    currentFrameTrackedLiveFaceId = largestFace.trackingId
                                    candidateBitmapForCountdown = faceBitmap // Store the bitmap of the live face

                                    // Trigger countdown if:
                                    // 1. A live face is found.
                                    // 2. We haven't reported a candidate yet OR the current live face is a new one.
                                    Log.d(TAG, "###@@@ LargestFace.TrackingId: ${largestFace.trackingId}")
                                    // Note : Face Tracking Id will remain same if face remains in frame.

                                    if (faceSnapshotHistory.size >= MIN_FRAMES_FOR_DYNAMIC_CHECK) {

                                        val isTooStill = isFaceTooStill(faceSnapshotHistory.toList())
                                        val hasBlinked = hasBlinkedRecently(faceSnapshotHistory.toList())

                                        Log.d(TAG, "###@@@ TooStill: $isTooStill, HasBlinked: $hasBlinked")

                                        if (hasBlinked) {
                                            Log.d(TAG, "###@@@ Dynamic Checks: Face ID ${largestFace.trackingId} is still but blinked. Considered LIVE.")
                                            onFaceDetectedForCountdown(largestFace, faceBitmap, resultsMap)
                                        } else {
                                            Log.w(TAG, "###@@@ SPOOF DETECTED (OVERRIDE): Face ID ${largestFace.trackingId} was TFLite-live but failed dynamic checks (Too Still AND No Blink).")
                                            finalIsLive = false // Override TFLite's result
                                            onLivenessResults(resultsMap)
                                        }
                                    } else {
                                        Log.d(TAG, "###@@@ Face ID ${largestFace.trackingId}: Not enough history for dynamic checks (${faceSnapshotHistory.size}/$MIN_FRAMES_FOR_DYNAMIC_CHECK). Relying on TFLite result for now.")
                                    }
                                } else {
                                    //onLivenessResults(resultsMap)
                                }
                            }
                            //onLivenessResults(resultsMap)
                        }
                        // If the previously tracked live face is no longer live or no longer detected, reset.
                        if (liveFaceCandidateReported && lastTrackedLiveFaceId != null &&
                            (!liveFaceFoundInCurrentFrame || resultsMap.entries.none { it.key.trackingId == lastTrackedLiveFaceId && it.value.isLive })) {
                            Log.d(TAG, "###@@@ Previously tracked live face (ID: $lastTrackedLiveFaceId) lost or no longer live. Resetting trigger.")
                            liveFaceCandidateReported = false
                            lastTrackedLiveFaceId = null
                        }
                    }
                    //onLivenessResults(resultsMap) // Send all processed face results
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "###@@@ Face detection failed", e)
                    onError(e)
                }
                .addOnCompleteListener {
                    imageProxy.close() // Crucial to close the ImageProxy
                }
        } else {
            imageProxy.close() // Ensure proxy is closed if mediaImage is null
        }
    }

    fun onFaceDetectedForCountdown(
        largestFace: Face,
        faceBitmap: Bitmap,
        resultsMap: MutableMap<Face, FaceLivenessData>
    ) {
        if (largestFace.trackingId != null) { // Only consider faces with tracking IDs
            if (!liveFaceCandidateReported || lastTrackedLiveFaceId != largestFace.trackingId) {
                Log.d(TAG, "###@@@ Live face candidate detected (ID: ${largestFace.trackingId}). Signaling for countdown.")
                onLiveFaceDetectedForCountdown(faceBitmap, resultsMap)
                liveFaceCandidateReported = true
                lastTrackedLiveFaceId = largestFace.trackingId
            }
        }
    }

    fun createFaceSnapShotHistory(largestFace: Face) {
        Log.d(TAG, "###@@@ TFLite Liveness for face ID ${largestFace.trackingId}")

        // Create and update face snapshot history
        val snapshot = FaceFrameSnapshot(
            timestamp = System.currentTimeMillis(),
            boundingBoxCenterX = largestFace.boundingBox.centerX(),
            boundingBoxCenterY = largestFace.boundingBox.centerY(),
            headEulerAngleY = largestFace.headEulerAngleY,
            headEulerAngleZ = largestFace.headEulerAngleZ,
            leftEyeOpenProbability = largestFace.leftEyeOpenProbability,
            rightEyeOpenProbability = largestFace.rightEyeOpenProbability
        )
        updateFaceSnapshotHistory(snapshot, largestFace.trackingId)
    }

    private fun hasBlinkedRecently(history: List<FaceFrameSnapshot>): Boolean {
        if (history.size < MIN_FRAMES_FOR_DYNAMIC_CHECK) {
            return false // Not enough data
        }

        var foundClosedState = false
        var foundOpenStateAfterClosed = false // To ensure open state is not just the initial state before any blink

        // Simpler check: any frame with eyes closed and any frame with eyes open
        // More robust: look for a sequence or at least one frame distinctly closed
        // and one frame distinctly open.

        var closedTimestamp = -1L

        for (snapshot in history) {
            val leftEyeOpenProb = snapshot.leftEyeOpenProbability ?: 1.0f // Default to open if null
            val rightEyeOpenProb =
                snapshot.rightEyeOpenProbability ?: 1.0f // Default to open if null

            Log.d(TAG, "###@@@ EyeOpenProb leftEyeOpenProb: $leftEyeOpenProb, rightEyeOpenProb: $rightEyeOpenProb")

            if (leftEyeOpenProb < BLINK_EYE_CLOSED_PROB_THRESHOLD && rightEyeOpenProb < BLINK_EYE_CLOSED_PROB_THRESHOLD) {
                foundClosedState = true
                closedTimestamp = snapshot.timestamp // Mark when we saw a closed state
                //Log.d(TAG, "Blink Check: Found CLOSED state at ${snapshot.timestamp}")

                //EyeFoundClosedState: true --- leftEyeOpenProb: 0.056715727, rightEyeOpenProb: 0.022249281
            }

            // Check for an open state that occurs *after* or around the time of a detected closed state
            // This makes the check a bit more robust against just having eyes open initially.
            // For this simplified version, we just check if open state exists anywhere.
            // A better check would be: if (foundClosedState && snapshot.timestamp >= closedTimestamp)
            if (leftEyeOpenProb > BLINK_EYE_OPEN_PROB_THRESHOLD && rightEyeOpenProb > BLINK_EYE_OPEN_PROB_THRESHOLD) {
                if (foundClosedState) { // only count as open state if we previously saw a closed one
                    foundOpenStateAfterClosed = true
                    Log.d(TAG, "###@@@ BLINK_EYE_OPEN_PROB_THRESHOLD : true --- leftEyeOpenProb: $leftEyeOpenProb, rightEyeOpenProb: $rightEyeOpenProb")
                    // Log.d(TAG, "Blink Check: Found OPEN state AFTER closed at ${snapshot.timestamp}")
                }
            }
        }
        if (foundClosedState && foundOpenStateAfterClosed) {
            Log.d(TAG, "Blink Check: PASSED (found closed and subsequent open states in history)")
            return true
        }
        Log.d(
            TAG,
            "Blink Check: FAILED (foundClosedState: $foundClosedState, foundOpenStateAfterClosed: $foundOpenStateAfterClosed)"
        )
        return false
    }

    private fun isFaceTooStill(history: List<FaceFrameSnapshot>): Boolean {
        if (history.size < MIN_FRAMES_FOR_DYNAMIC_CHECK) {
            return false // Not enough data to make a judgment
        }

        val firstTimestamp = history.first().timestamp
        val lastTimestamp = history.last().timestamp
        // Consider a minimum duration for the history as well, e.g., 200-300ms
        if ((lastTimestamp - firstTimestamp) < 200L && history.size < MAX_HISTORY_SIZE) { // e.g. < 200ms
            //Log.d(TAG, "History duration too short for stillness check: ${lastTimestamp - firstTimestamp}ms")
            return false // Not enough time elapsed for a reliable stillness check unless history is full
        }


        val minX = history.minOf { it.boundingBoxCenterX }
        val maxX = history.maxOf { it.boundingBoxCenterX }
        val minY = history.minOf { it.boundingBoxCenterY }
        val maxY = history.maxOf { it.boundingBoxCenterY }

        val minYaw = history.minOf { it.headEulerAngleY }
        val maxYaw = history.maxOf { it.headEulerAngleY }
        val minRoll = history.minOf { it.headEulerAngleZ }
        val maxRoll = history.maxOf { it.headEulerAngleZ }

        val positionChangeX = maxX - minX
        val positionChangeY = maxY - minY
        val angleChangeYaw = maxYaw - minYaw
        val angleChangeRoll = maxRoll - minRoll

        val isTooStillPosition = positionChangeX < STILLNESS_THRESHOLD_POSITION &&
                positionChangeY < STILLNESS_THRESHOLD_POSITION
        val isTooStillAngle = abs(angleChangeYaw) < STILLNESS_THRESHOLD_ANGLE &&
                abs(angleChangeRoll) < STILLNESS_THRESHOLD_ANGLE

        if (isTooStillPosition && isTooStillAngle) {
            Log.d(
                TAG,
                "Face deemed TOO STILL. Positional delta (X:$positionChangeX, Y:$positionChangeY). Angular delta (Yaw:$angleChangeYaw, Roll:$angleChangeRoll)"
            )
            return true
        }
        // Log.d(TAG, "Face NOT too still. Positional delta (X:$positionChangeX, Y:$positionChangeY). Angular delta (Yaw:$angleChangeYaw, Roll:$angleChangeRoll)")
        return false
    }

    private fun updateFaceSnapshotHistory(
        snapshot: FaceFrameSnapshot,
        currentFaceTrackingId: Int?
    ) {
        if (currentFaceTrackingId == null) {
            // If there's no tracking ID, we can't reliably track history for a specific face.
            // Depending on desired behavior, clear history or handle differently.
            // For now, let's clear if we lose tracking.
            if (faceSnapshotHistory.isNotEmpty()) {
                faceSnapshotHistory.clear()
                Log.d(TAG, "No tracking ID for current face, history cleared.")
            }
            lastTrackedLiveFaceId = null // Reset the main tracked ID as well
            return
        }

        if (lastTrackedLiveFaceId != currentFaceTrackingId) {
            // New face detected or tracking ID changed, clear history for the old face
            faceSnapshotHistory.clear()
            Log.d(
                TAG,
                "New face tracked (ID: $currentFaceTrackingId), history cleared for old ID: $lastTrackedLiveFaceId"
            )
            lastTrackedLiveFaceId = currentFaceTrackingId // Update to the new tracking ID
        }

        // Add the new snapshot to the history
        if (faceSnapshotHistory.size >= MAX_HISTORY_SIZE) {
            faceSnapshotHistory.removeFirst() // Keep history size fixed
        }
        faceSnapshotHistory.addLast(snapshot)
    }

    fun noFaceDetectedPrompt(){
        Log.d(TAG, "###@@@ No faces detected, resetting countdown trigger state.")
        liveFaceCandidateReported = false
        lastTrackedLiveFaceId = null
    }

    fun isLiveOld(faceBitmap: Bitmap): Boolean {
        require(!(faceBitmap.width != INPUT_SIZE || faceBitmap.height != INPUT_SIZE)) {
            println("###@@@ Input bitmap must be 224x224")
        }

        // Prepare input tensor
        val input = Array<Array<Array<FloatArray?>?>?>(1) {
            Array<Array<FloatArray?>?>(INPUT_SIZE) {
                Array<FloatArray?>(INPUT_SIZE) { FloatArray(3) }
            }
        }

        for (y in 0..<INPUT_SIZE) {
            for (x in 0..<INPUT_SIZE) {
                val pixel = faceBitmap[x, y]
                // Extract RGB channels
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                input[0]!![y]!![x]!![0] = r
                input[0]!![y]!![x]!![1] = g
                input[0]!![y]!![x]!![2] = b
            }
        }

        // Prepare output tensor
        val output = Array<FloatArray?>(1) { FloatArray(1) }
        tfLiteInterpreter.run(input, output)

        val score = output[0]!![0]
        println("###@@@ Spoof score → $score")
        // Score value is lesser than it should be for a real face
        return score < LIVENESS_THRESHOLD
    }


    private fun isLive(faceBitmap: Bitmap): Boolean {
        // Preprocess the face bitmap
        val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, tfLiteInputImageWidth, tfLiteInputImageHeight, true)
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // Prepare output buffer (assuming 1 output tensor for liveness score)
        val outputBuffer = ByteBuffer.allocateDirect(1 * Float.SIZE_BYTES).apply {
            order(ByteOrder.nativeOrder())
        }

        try {
            tfLiteInterpreter.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()
            val livenessScore = outputBuffer.float // Get the single float output

            Log.d(TAG, "###@@@ Liveness score: $livenessScore")

            // Compare with threshold
            return livenessScore >= LIVENESS_THRESHOLD // Real (live) if score is >= threshold
        } catch (e: Exception) {
            Log.e(TAG, "###@@@ Error during TFLite inference for liveness", e)
            onError(e)
            return false // Default to not live on error
        }
    }


    private fun cropFaceFromBitmap(originalBitmap: Bitmap, boundingBox: Rect, rotationDegrees: Int): Bitmap? {
        // Adjust bounding box to be within image dimensions
        val left = max(0, boundingBox.left)
        val top = max(0, boundingBox.top)
        val right = min(originalBitmap.width, boundingBox.right)
        val bottom = min(originalBitmap.height, boundingBox.bottom)

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) {
            Log.w(TAG, "Invalid bounding box dimensions for cropping: $boundingBox")
            return null
        }


        //TODO crop the face
        val bounds: Rect = boundingBox
        if (bounds.top < 0) {
            bounds.top = 0
        }
        if (bounds.left < 0) {
            bounds.left = 0
        }
        if (bounds.left + bounds.width() > croppedBitmap.getWidth()) {
            bounds.right = croppedBitmap.getWidth() - 1
        }
        if (bounds.top + bounds.height() > croppedBitmap.getHeight()) {
            bounds.bottom = croppedBitmap.getHeight() - 1
        }

        // Create the cropped bitmap
        /*var croppedBitmap = Bitmap.createBitmap(
            originalBitmap,
            bounds.left,
            bounds.top,
            bounds.width(),
            bounds.height())*/
        var croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, width, height)
        croppedBitmap = croppedBitmap.scale(TF_OD_API_INPUT_SIZE2, TF_OD_API_INPUT_SIZE2)

        val face224 = croppedBitmap.scale(224, 224, false)
        // Handle rotation if necessary. The input to TFLite should be upright.
        // If the originalBitmap comes from imageProxyToBitmap and is already upright,
        // this explicit rotation might be redundant or might need adjustment.
        // However, ML Kit bounding boxes are relative to the *unrotated* frame if inputImage wasn't rotated.
        // Let's assume for now the crop from originalBitmap is sufficient.
        // If liveness model expects perfectly upright faces, further rotation based on face pose might be needed.
        // For now, we assume the crop from the full originalBitmap (already upright from imageProxyToBitmap) is what we need.

        //return croppedBitmap
        return face224
    }


    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        // Convert ImageProxy to Bitmap
        // This is a common conversion, ensure it's efficient and correct for your use case.
        val yBuffer = imageProxy.planes[0].buffer // Y
        val uBuffer = imageProxy.planes[1].buffer // U
        val vBuffer = imageProxy.planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        // Rotate bitmap if necessary to make it upright
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }


    private fun loadModelFile(context: Context, modelFilename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(
            1 * tfLiteInputImageWidth * tfLiteInputImageHeight * 3 * Float.SIZE_BYTES // Assuming float32 input
        ).apply {
            order(ByteOrder.nativeOrder())
        }
        inputBuffer.rewind()

        val pixels = IntArray(tfLiteInputImageWidth * tfLiteInputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in pixels) {
            // Assuming image is RGB and normalization is to [0,1] or [-1,1]
            // For [0,1] normalization:
            inputBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
            inputBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixelValue and 0xFF) / 255.0f)

            // If your model expects [-1,1] (e.g. MobileNet often does):
            // inputBuffer.putFloat((((pixelValue shr 16) and 0xFF) / 255.0f - IMAGE_MEAN) / IMAGE_STD)
            // inputBuffer.putFloat((((pixelValue shr 8) and 0xFF) / 255.0f - IMAGE_MEAN) / IMAGE_STD)
            // inputBuffer.putFloat(((pixelValue and 0xFF) / 255.0f - IMAGE_MEAN) / IMAGE_STD)
        }
        return inputBuffer
    }

    fun stop() {
        faceDetector.close()
        tfLiteInterpreter.close()
        Log.d(TAG, "FaceLivenessAnalyzer stopped and resources released.")
    }

    companion object {
        private const val TAG = "FaceLivenessAnalyzer"
        private const val MODEL_FILENAME = "liveness_model.tflite" // Your TFLite model file in assets

        // --- Model-specific Configuration (ADJUST THESE) ---
        // These values depend on your specific TFLite model.
        // Check your model's documentation or use a tool like Netron to inspect it.

        // For liveness score interpretation:
        // Example: If your model outputs a single float, where >= LIVENESS_THRESHOLD means "live".
        private const val LIVENESS_THRESHOLD = 0.3f // Adjust this based on your model's output and desired sensitivity.
        private const val INPUT_SIZE = 224

        // If your model outputs probabilities for multiple classes (e.g., ["spoof", "live"])
        // private const val NUM_OUTPUT_CLASSES = 2
        // private const val LIVE_CLASS_INDEX = 1 // Index of the "live" class in the output array
        // private const val SPOOF_THRESHOLD = 0.5f // Confidence threshold for classifying as spoof or live

        // For image normalization (if your model expects input in a specific range, e.g., [-1, 1] or [0, 1])
        // If your model expects [0,1] floats, normalization in convertBitmapToByteBuffer would be val/255.0f
        // If your model expects [-1,1] floats, it's often (val/255.0f - 0.5f) * 2.0f or similar.
        // private const val IMAGE_MEAN = 0.5f // Example for [-1,1] if normalized by (pixel-MEAN)/STD
        // private const val IMAGE_STD = 0.5f  // Example for [-1,1]
    }
}
