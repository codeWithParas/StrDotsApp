package com.xyz.strapp.presentation.profile

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xyz.strapp.R
import com.xyz.strapp.domain.model.ProfileResponse
import com.xyz.strapp.utils.restartApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val onLanguageSelected = { lang : String ->

    }
    val profileUIState by profileViewModel.profileUiState.collectAsState()
    val scope = rememberCoroutineScope()

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
                            text = stringResource(R.string.profile_error_loading_profile),
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
                            text = stringResource(R.string.profile_loading_profile),
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
fun ProfileContent(
    profile: ProfileResponse,onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
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
                InfoItem(stringResource(R.string.profile_name), profile.name ?: "N/A", Icons.Default.Person),
                InfoItem(stringResource(R.string.profile_email), profile.email ?: "N/A", Icons.Default.Email),
                InfoItem(stringResource(R.string.profile_phone), profile.mobileNo ?: "N/A", Icons.Default.Phone)
            )

            PersonalInfoCard(
                cardTitle = stringResource(R.string.profile_personal_information),
                infoList = personalInfo,
                modifier = Modifier
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            val workInfo = listOf(
                InfoItem(stringResource(R.string.profile_circle), profile.circle ?: "N/A", Icons.Default.AccountBox),
                InfoItem(stringResource(R.string.profile_division), profile.division ?: "N/A", Icons.Default.Business),
                InfoItem(stringResource(R.string.profile_range), profile.range ?: "N/A", Icons.Default.LocationOn),
                InfoItem(stringResource(R.string.profile_section), profile.section ?: "N/A", Icons.Default.Work),
                InfoItem(stringResource(R.string.profile_beat), profile.beat ?: "N/A", Icons.Default.Badge),
                InfoItem(stringResource(R.string.profile_work_hours), "${profile.startTime} - ${profile.endTime}", Icons.Default.AccessTime),
                InfoItem(stringResource(R.string.profile_agency_name), profile.agencyName ?: "N/A", Icons.Default.BusinessCenter)
            )

            PersonalInfoCard(
                cardTitle = stringResource(R.string.profile_work_information),
                infoList = workInfo,
                modifier = Modifier
            )

            Spacer(modifier = Modifier.height(16.dp))

            LanguageCard(
                modifier = Modifier,
                onLanguageSelected = { lang ->
                    scope.launch {
                        profileViewModel.setLanguage(lang)
                        delay(1000)
                        restartApp(context)
                    }
                    val language = if(lang == "en"){
                        "English"
                    } else {
                        "Tamil"
                    }
                    Toast.makeText(context, "Please wait, updating Language to $language", Toast.LENGTH_SHORT).show()
                }
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
                    text = stringResource(R.string.profile_logout),
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
                ProfileImage(imagePath = profile.image!!, modifier = Modifier)
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
                text = profile.name ?: stringResource(R.string.profile_unknown),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = profile.employeeType ?: stringResource(R.string.profile_employee),
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
fun LanguageCard(
    modifier: Modifier = Modifier,
    onLanguageSelected: (String) -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Language",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onLanguageSelected("en") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("English")
                }
                Button(
                    onClick = { onLanguageSelected("ta") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tamil")
                }
            }
        }
    }
}

@Composable
fun ProfileImage(
    imagePath: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = imagePath,
        contentDescription = "Sample image",
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}