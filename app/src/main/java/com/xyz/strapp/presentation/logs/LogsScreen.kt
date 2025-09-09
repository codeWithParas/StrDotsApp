package com.xyz.strapp.presentation.logs

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xyz.strapp.domain.model.AttendanceLogModel
import com.xyz.strapp.presentation.logs.LogsPreviewData
import com.xyz.strapp.ui.theme.StrAppTheme

@Composable
fun LogsScreen(
    viewModel: LogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is LogsUiState.Loading -> {
                LoadingState()
            }
            is LogsUiState.Empty -> {
                EmptyState()
            }
            is LogsUiState.Success -> {
                SuccessState(logs = state.logs)
            }
            is LogsUiState.Error -> {
                ErrorState(message = state.message)
            }
        }
        
        // Add refresh button
        FloatingActionButton(
            onClick = { viewModel.loadLogs() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
fun SuccessState(logs: List<AttendanceLogModel>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Attendance History",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
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
            
            // Employee info with code
            LogInfoRow(
                icon = Icons.Default.Person,
                label = "Employee",
                value = if (log.employeeName != "NotFound") 
                    "${log.employeeName} - ${log.employeeCode}" 
                else 
                    "N/A"
            )
            
            // Time
            Spacer(modifier = Modifier.height(8.dp))
            LogInfoRow(
                icon = Icons.Default.Schedule,
                label = "Time",
                value = log.getFormattedTime()
            )
            
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
                    text = "Message:",
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
fun LogInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                text = "Error Loading Logs",
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

@Preview(showBackground = true)
@Composable
fun LogsScreenSuccessPreview() {
    StrAppTheme {
        SuccessState(logs = LogsPreviewData.mockLogs)
    }
}

@Preview(showBackground = true)
@Composable
fun LogsScreenEmptyPreview() {
    StrAppTheme {
        EmptyState()
    }
}

@Preview(showBackground = true)
@Composable
fun LogsScreenLoadingPreview() {
    StrAppTheme {
        LoadingState()
    }
}

@Preview(showBackground = true)
@Composable
fun LogsScreenErrorPreview() {
    StrAppTheme {
        ErrorState(message = "Failed to connect to server")
    }
}

@Preview(showBackground = true)
@Composable
fun AttendanceLogItemPreview() {
    StrAppTheme {
        AttendanceLogItem(log = LogsPreviewData.mockLogs.first())
    }
}