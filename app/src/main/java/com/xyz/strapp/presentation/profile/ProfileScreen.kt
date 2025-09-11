package com.xyz.strapp.presentation.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.navigation.compose.hiltViewModel
import com.xyz.strapp.R
import com.xyz.strapp.domain.model.ProfileResponse
import com.xyz.strapp.presentation.homescreen.HomeViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

@Preview
@Composable
fun ProfileScreenPreview(){
}

data class InfoItem(
    val key: String,
    val value: String,
    val icon: ImageVector
)

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = hiltViewModel(),
    onLogout: () -> Unit,
){
    val profileUIState by profileViewModel.profileUiState.collectAsState()
    val scope = rememberCoroutineScope()
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
        profileViewModel.loadProfile()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        // --- CHANGE 1: Handle different UI states ---
        // Use a 'when' block to decide what UI to show based on the current state.
        when (val state = profileUIState) {
            is ProfileUiState.Success -> {
                // When data is successfully loaded, show the profile content.
                ProfileContent(profile = state.profileResponse, {
                    scope.launch {
                        profileViewModel.Logout()
                        onLogout()
                    }
                })
            }

            is ProfileUiState.Error -> {
                // On error, you could show a retry button or an error message.
                // For now, we'll show the error message. The Snackbar also appears.
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error loading profile",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is ProfileUiState.Loading, is ProfileUiState.Idle -> {
                // Show a loading indicator while fetching data or in the initial state.
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading profile...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileContent(profile: ProfileResponse,onLogout: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pass the dynamic image data
        ImageCard(profile = profile, modifier = Modifier)
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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

            Spacer(modifier = Modifier.height(16.dp))
            
            val workInfo = listOf(
                InfoItem("Circle", profile.circle ?: "N/A", Icons.Default.AccountBox),
                InfoItem("Division", profile.division ?: "N/A", Icons.Default.Business),
                InfoItem("Range", profile.range ?: "N/A", Icons.Default.LocationOn),
                InfoItem("Section", profile.section ?: "N/A", Icons.Default.Work),
                InfoItem("Beat", profile.beat ?: "N/A", Icons.Default.Badge),
                InfoItem("Work Hours", "${profile.startTime} - ${profile.endTime}", Icons.Default.AccessTime),
                InfoItem("Agency Name", profile.agencyName ?: "N/A", Icons.Default.BusinessCenter)
            )

            PersonalInfoCard(
                cardTitle = "Work Information",
                infoList = workInfo,
                modifier = Modifier
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = {
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Logout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Composable
fun ImageCard(profile: ProfileResponse, modifier: Modifier){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .height(300.dp),
        contentAlignment = Alignment.Center,
    ){
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Only show the image if the data is not null or blank
            if ((profile.image ?: "").isNotBlank()) {
                ProfileImage(base64Image = profile.image!!, modifier = Modifier)
            } else {
                // Optional: Show a placeholder if there is no image
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "No Profile Image",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = profile.name ?: "Unknown",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = profile.employeeType ?: "Employee",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp)
            )
            
            if (!profile.code.isNullOrBlank()) {
                Text(
                    text = "ID: ${profile.code}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PersonalInfoCard(
    cardTitle: String,
    infoList: List<InfoItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = cardTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            infoList.forEachIndexed { index, item ->
                InformationView(
                    title = item.key,
                    value = item.value,
                    icon = item.icon,
                    modifier = Modifier
                )
                
                if (index < infoList.size - 1) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun InformationView(title:String, value:String,icon: ImageVector, modifier: Modifier) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$title Icon",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal
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
            .border(
                width = 4.dp, 
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            ),
        contentScale = ContentScale.Crop
    )
}