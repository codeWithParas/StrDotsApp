package com.xyz.strapp.presentation.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.navigation.compose.hiltViewModel
import com.xyz.strapp.R
import com.xyz.strapp.domain.model.ProfileResponse
import com.xyz.strapp.presentation.userlogin.LoginUiState
import com.xyz.strapp.presentation.userlogin.LoginViewModel
import com.xyz.strapp.utils.Constants
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

@Preview
@Composable
fun ProfileScreenPreview(){
    ProfileScreen()
}

data class InfoItem(
    val key: String,
    val value: String,
    val icon: ImageVector
)

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = hiltViewModel(),
){
    val profileUIState by profileViewModel.profileUiState.collectAsState()
//    val scope = rememberCoroutineScope()
//    val snackbarHostState = remember { SnackbarHostState() }

//    LaunchedEffect(profileUIState) {
//        if (profileUIState is ProfileUiState.Error) {
//            val errorState = profileUIState as ProfileUiState.Error
//            scope.launch {
//                snackbarHostState.showSnackbar(
//                    message = "Failed to fetch profile: ${errorState.message}",
//                    duration = androidx.compose.material3.SnackbarDuration.Long
//                )
//            }
//            // Optionally reset state in ViewModel to allow retry
//            profileViewModel.resetState()
//        }
//    }

    LaunchedEffect(key1 = Unit) {
        profileViewModel.LoadProdile()
    }

    Surface(
        color = Color.LightGray,
        modifier = Modifier.fillMaxSize(),
    ) {
        // --- CHANGE 1: Handle different UI states ---
        // Use a 'when' block to decide what UI to show based on the current state.
        when (val state = profileUIState) {
            is ProfileUiState.Success -> {
                // When data is successfully loaded, show the profile content.
                ProfileContent(profile = state.profileResponse)
            }

            is ProfileUiState.Error -> {
                // On error, you could show a retry button or an error message.
                // For now, we'll show the error message. The Snackbar also appears.
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = state.message)
                }
            }

            is ProfileUiState.Loading, is ProfileUiState.Idle -> {
                // Show a loading indicator while fetching data or in the initial state.
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun ProfileContent(profile: ProfileResponse, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pass the dynamic image data
        ImageCard(profile = profile, modifier = Modifier)
        Spacer(modifier = Modifier.height(10.dp))

        // Build the list of personal info
        val personalInfo = listOf(
            InfoItem("Name", profile.name ?: "N/A", Icons.Default.Person),
            InfoItem("Email", profile.email ?: "N/A", Icons.Default.Email),
            InfoItem("Phone", profile.mobileNo ?: "N/A", Icons.Default.Phone)
        )


        PersonalInfoCard(
            cardTitle = "Personal Information",
            infoList = personalInfo,
            modifier = Modifier
        )

        Spacer(modifier = Modifier.height(10.dp))
        val workInfo = listOf(
            InfoItem("Circle", profile.circle ?: "N/A", Icons.Default.Person),
            InfoItem("Division", profile.division ?: "N/A", Icons.Default.Email),
            InfoItem("Range", profile.range ?: "N/A", Icons.Default.Phone),
            InfoItem("Section", profile.section ?: "N/A", Icons.Default.Phone),
            InfoItem("Beat", profile.beat ?: "N/A", Icons.Default.Phone),
            InfoItem("Work Hours", "${profile.startTime} - ${profile.endTime}", Icons.Default.Phone),
            InfoItem("Agency Name", profile.agencyName ?: "N/A", Icons.Default.Phone)
        )

        PersonalInfoCard(
            cardTitle = "Work Information",
            infoList = workInfo,
            modifier = Modifier
        )


        // You can add more cards here for other information
        // Example: ContactInfoCard(...)
    }
}


@Composable
fun ImageCard(profile: ProfileResponse, modifier: Modifier){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(6.dp))
            .height(250.dp),
        contentAlignment = Alignment.Center,
    ){
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Only show the image if the data is not null or blank
            if ((profile.image ?: "").isNotBlank()) {
                ProfileImage(base64Image = profile.image!!, modifier = Modifier)
            } else {
                // Optional: Show a placeholder if there is no image
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground), // Add a placeholder icon
                    contentDescription = "No Profile Image",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            }
            Text(
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.headlineMedium,
                text = profile.name ?: ""
            )
            Text(
                modifier = Modifier.padding(top = 5.dp),
                text = profile.employeeType ?: "",
                style = MaterialTheme.typography.bodyMedium
                )
            Text(modifier = Modifier.padding(top = 5.dp),text = profile.code ?: "")
        }

    }
}

@Composable
fun PersonalInfoCard(
    cardTitle: String,
    infoList: List<InfoItem>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(6.dp))
    ) {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = cardTitle,
                color = Color(0xFF13467A),
                style = MaterialTheme.typography.titleLarge,
                modifier = modifier.padding(top = 10.dp, start = 10.dp, bottom = 6.dp)
            )
            infoList.forEach { item ->
                InformationView(
                    title = item.key,
                    value = item.value,
                    icon = item.icon,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
fun InformationView(title:String, value:String,icon: ImageVector, modifier: Modifier) {
    Row(modifier = modifier.padding(10.dp)){
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color(0xFFD6EAF8)) // 👈 light blue background
                .border(1.dp, Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                imageVector = icon,
                contentDescription = "Profile Icon",
                modifier = Modifier.size(28.dp), // keep padding inside
                contentScale = ContentScale.Fit
            )
        }
        Column(
            modifier = modifier
                .height(50.dp)
                .fillMaxWidth()
                .padding(start = 10.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = title,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = value,
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
@Composable
fun ProfileImage(
    base64Image: String,
    modifier: Modifier = Modifier,
    maxDimension: Int = 400, // max width/height to prevent memory issues
    fallbackRotation: Float = 0f
) {

    val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)

    // Decode only bounds first to calculate scaling
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

    // Calculate scale factor
    var scale = 1
    val maxDim = maxOf(options.outWidth, options.outHeight)
    if (maxDim > maxDimension) {
        scale = maxDim / maxDimension
    }

    // Decode bitmap with scaling
    val bitmapOptions = BitmapFactory.Options().apply { inSampleSize = scale }
    var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bitmapOptions)

    // Fix rotation using EXIF if available
    try {
        val exif = ExifInterface(ByteArrayInputStream(imageBytes))
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        if (!matrix.isIdentity) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else if (fallbackRotation != 0f) {
            val fallbackMatrix = Matrix().apply { postRotate(fallbackRotation) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, fallbackMatrix, true)
        }
    } catch (_: Exception) {
        if (fallbackRotation != 0f) {
            val fallbackMatrix = Matrix().apply { postRotate(fallbackRotation) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, fallbackMatrix, true)
        }
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Profile Image",
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
            .border(2.dp, Color(0xFFD6EAF8), CircleShape),
        contentScale = ContentScale.Crop
    )
}