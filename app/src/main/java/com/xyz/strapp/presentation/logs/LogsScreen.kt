package com.xyz.strapp.presentation.logs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.xyz.strapp.R
import com.xyz.strapp.domain.model.AttendanceLogModel
import com.xyz.strapp.domain.model.entity.FaceImageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    viewModel: LogsViewModel = hiltViewModel()
) {
    val currentContext = LocalContext.current
    val applicationContext = currentContext.applicationContext
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { viewModel.setSelectedTab(0) },
                text = { Text(stringResource(R.string.logs_online)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { viewModel.setSelectedTab(1) },
                text = { Text(stringResource(R.string.logs_offline)) }
            )
        }
        
        Box(modifier = Modifier.weight(1f)) {
            when (val state = uiState) {
                is LogsUiState.Loading -> {
                    LoadingState()
                }
                is LogsUiState.Empty -> {
                    EmptyState()
                }
                is LogsUiState.EmptyPendingUploads -> {
                    EmptyPendingUploadsState()
                }
                is LogsUiState.Success -> {
                    SuccessState(logs = state.logs, isOffline = false)
                }
                is LogsUiState.PendingUploads -> {
                    PendingUploadsState(state.isUploading, pendingUploads = state.pendingUploads, startOfflineSync = {
                        viewModel.uploadPendingLogs(context = applicationContext)
                    })
                }
                is LogsUiState.Error -> {
                    ErrorState(message = state.message)
                }
            }

            Row(modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
            ) {
                // Add refresh button
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) {
                            viewModel.loadLogs()
                        } else {
                            viewModel.loadPendingUploads()
                        }
                    },
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }

                /*if (selectedTab == 1){
                    val isUploading = when (val state = uiState) {
                        is LogsUiState.Success -> true
                        is LogsUiState.PendingUploads -> state.isUploading
                        else -> false
                    }
                    ProgressFloatingActionButton(
                        modifier = Modifier.padding(start = 10.dp),
                        isUploading = isUploading,
                        hasUploadedSuccessfully = !isUploading,
                        onClick = { viewModel.uploadPendingLogs(context = applicationContext) }
                    )
                }*/
            }

        }
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.height(80.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No attendance logs found",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your attendance history will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun SuccessState(logs: List<AttendanceLogModel>, isOffline: Boolean = false) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.logs_attendance_history),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = if (isOffline) 4.dp else 16.dp)
                )
                
                if (isOffline) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFFFAB91).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFE64A19),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Offline mode - showing cached data",
                            color = Color(0xFFE64A19),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        items(logs) { log ->
            AttendanceLogItem(log = log)
        }
    }
}

@Composable
fun AttendanceLogItem(log: AttendanceLogModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Date with calendar icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = log.getFormattedDate(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Action indicator (CheckIn/CheckOut)
                val (actionColor, actionIcon) = when (log.action.lowercase()) {
                    "checkin" -> Color(0xFF4CAF50) to Icons.Default.CheckCircle
                    "checkout" -> Color(0xFF2196F3) to Icons.Default.CheckCircle
                    else -> Color(0xFFFF9800) to Icons.Default.Error
                }
                
                Box(
                    modifier = Modifier
                        .background(
                            color = actionColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = log.action,
                            color = actionColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row (
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = log.imagePath, // your image URL
                    contentDescription = "Sample image",
                    modifier = Modifier
                        .size(75.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Column (modifier = Modifier.padding(start = 10.dp)){
                    // Employee info with code
                    LogInfoRow(
                        //icon = Icons.Default.Person,
                        icon = null,
                        label = "Employee",
                        value = if (log.employeeName != "NotFound")
                            "${log.employeeName} - ${log.employeeCode}"
                        else
                            "N/A"
                    )

                    // Time
                    Spacer(modifier = Modifier.height(8.dp))
                    LogInfoRow(
                        //icon = Icons.Default.Schedule,
                        icon = null,
                        label = "Time",
                        value = log.getFormattedTime()
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Location
            Spacer(modifier = Modifier.height(8.dp))
            LogInfoRow(
                icon = Icons.Default.LocationOn,
                label = "Location",
                value = log.getLocationString()
            )
            
            // Message (if not "No message")
            if (log.message != "No message") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.logs_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun LogInfoRow(icon: ImageVector?, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if(icon != null)
        {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Column () {
//            Text(
//                text = label,
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ErrorState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.height(80.dp),
                tint = Color.Red.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.logs_error_loading_logs),
                style = MaterialTheme.typography.titleLarge,
                color = Color.Red.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun EmptyPendingUploadsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Upload,
                contentDescription = null,
                modifier = Modifier.height(80.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.logs_no_pending_uploads),
                style = MaterialTheme.typography.titleLarge,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.logs_no_data_is_pending_to_upload),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun PendingUploadsState(isUploading: Boolean, pendingUploads: List<FaceImageEntity>, startOfflineSync: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 6.dp, start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = stringResource(R.string.logs_pending_uploads),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                    Text(
                        text = "Count : ${pendingUploads.size}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                // Upload status indicator
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFFF9800).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable(
                            onClick = {
                                startOfflineSync()
                            }
                        )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if(isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = "Sync Offline Entries",
                            color = Color(0xFFFF9800),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
        
        items(pendingUploads) { upload ->
            PendingUploadItem(upload = upload)
        }
    }
}

@Composable
fun PendingUploadItem(upload: FaceImageEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Date with calendar icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatTimestamp(upload.timestamp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Upload status indicator
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFFF9800).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        Text(
                            text = "Pending ${if(upload.isCheckIn) "CheckIn" else "CheckOut"}",
                            color = Color(0xFFFF9800),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Image ID
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "Image ID",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "#${upload.id}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Time
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.logs_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(upload.timestamp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Image size
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.logs_image_size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${upload.imageData.size / 1024} KB",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressFloatingActionButton(
    modifier: Modifier = Modifier,
    isUploading: Boolean,
    hasUploadedSuccessfully: Boolean, // New state for success indication
    onClick: () -> Unit,
    textIdle: String = "Upload",
    textUploading: String = "Uploading...",
    textSuccess: String = "Done"
) {
    // Using ExtendedFloatingActionButton for text + icon
    ExtendedFloatingActionButton(
        modifier = modifier,
        onClick = {
            if (!isUploading && !hasUploadedSuccessfully) { // Only allow click if not uploading or already successful
                onClick()
            }
        },
        containerColor = when {
            hasUploadedSuccessfully -> MaterialTheme.colorScheme.tertiaryContainer
            isUploading -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = when {
            hasUploadedSuccessfully -> MaterialTheme.colorScheme.onTertiaryContainer
            isUploading -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        }
    ) {
        // AnimatedContent to switch between states smoothly
        AnimatedContent(
            targetState = when {
                hasUploadedSuccessfully -> "SUCCESS"
                isUploading -> "UPLOADING"
                else -> "IDLE"
            },
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "fab_content_animation"
        ) { targetState ->
            when (targetState) {
                "SUCCESS" -> {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Upload Successful",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(textSuccess)
                }
                "UPLOADING" -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        strokeCap = StrokeCap.Round,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(textUploading)
                }
                else -> { // IDLE
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = "Start Upload",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(textIdle)
                }
            }
        }
    }
}

// Helper function to format timestamp
private fun formatTimestamp(timestamp: String): String {
    val timestampLong = timestamp.toLongOrNull() ?: System.currentTimeMillis()
    val date = Date(timestampLong)
    val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return format.format(date)
}

// Helper function to format time
private fun formatTime(timestamp: String): String {
    val timestampLong = timestamp.toLongOrNull() ?: System.currentTimeMillis()
    val date = Date(timestampLong)
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return format.format(date)
}

//@Preview(showBackground = true)
//@Composable
//fun LogsScreenSuccessPreview() {
//    StrAppTheme {
//        SuccessState(logs = LogsPreviewData.mockLogs)
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun LogsScreenEmptyPreview() {
//    StrAppTheme {
//        EmptyState()
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun LogsScreenLoadingPreview() {
//    StrAppTheme {
//        LoadingState()
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun LogsScreenErrorPreview() {
//    StrAppTheme {
//        ErrorState(message = "Failed to connect to server")
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun EmptyPendingUploadsStatePreview() {
//    StrAppTheme {
//        EmptyPendingUploadsState()
//    }
//}

@Preview(showBackground = true)
@Composable
fun  AttendanceUploadItemPreview() {
    AttendanceLogItem(
        log = AttendanceLogModel(
            employeeName = "asd",
            employeeCode = "asd",
            latitude = 12.3,
            longitude = 12.4,
            dateTime = "asd",
            message = "asd",
            imagePath = "asd",
            action = "asd",
        )
    )
}


//@Preview(showBackground = true)
//@Composable
//fun PendingUploadItemPreview() {
//    StrAppTheme {
//        PendingUploadItem(
//            upload = FaceImageEntity(
//                id = 1,
//                imageData = ByteArray(1024 * 50), // 50KB mock data
//                timestamp = System.currentTimeMillis().toString(),
//                isUploaded = false
//            )
//        )
//    }
//}




//@Preview(showBackground = true)
//@Composable
//fun AttendanceLogItemPreview() {
//    StrAppTheme {
//        AttendanceLogItem(log = LogsPreviewData.mockLogs.first())
//    }
//}