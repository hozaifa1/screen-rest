package com.screenrest.app.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenrest.app.presentation.main.components.ConfigSummaryCard
import com.screenrest.app.presentation.main.components.StatusCard
import com.screenrest.app.presentation.components.PermissionWarningCard
import com.screenrest.app.service.UsageTrackingService
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Local timer state that updates every second
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // Update currentTimeMs every second while this composable is active
    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            delay(1000L)
        }
    }
    
    val thresholdMs = uiState.breakConfig.usageThresholdSeconds * 1000L
    val usageMs = UsageTrackingService.currentUsageMs
    val remainingMs = (thresholdMs - usageMs).coerceAtLeast(0L)
    
    val usedMinutes = (usageMs / 60000).toInt()
    val usedSeconds = ((usageMs % 60000) / 1000).toInt()
    val remainingMinutes = (remainingMs / 60000).toInt()
    val remainingSeconds = ((remainingMs % 60000) / 1000).toInt()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScreenRest") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                isServiceRunning = uiState.isServiceRunning,
                enforcementLevel = uiState.enforcementLevel,
                onToggleService = { viewModel.toggleService() }
            )
            
            CountdownTimerCard(
                usedMinutes = usedMinutes,
                usedSeconds = usedSeconds,
                remainingMinutes = remainingMinutes,
                remainingSeconds = remainingSeconds,
                thresholdSeconds = uiState.breakConfig.usageThresholdSeconds,
                isTracking = uiState.isServiceRunning
            )
            
            ConfigSummaryCard(
                breakConfig = uiState.breakConfig,
                onEditClick = onNavigateToSettings
            )
            
            if (!uiState.permissionStatus.usageStats) {
                PermissionWarningCard(
                    title = "Usage Access Required",
                    description = "Cannot track screen time without this permission",
                    permissionType = "usageStats"
                )
            }
            
            if (!uiState.permissionStatus.overlay) {
                PermissionWarningCard(
                    title = "Overlay Permission Required",
                    description = "Breaks will only show as notifications without this permission",
                    permissionType = "overlay"
                )
            }
            
            if (!uiState.permissionStatus.accessibility && uiState.permissionStatus.usageStats && uiState.permissionStatus.overlay) {
                PermissionWarningCard(
                    title = "Accessibility Service Optional",
                    description = "Break screen can be bypassed with home button. Enable for stricter enforcement.",
                    permissionType = "accessibility"
                )
            }
        }
    }
}

@Composable
private fun CountdownTimerCard(
    usedMinutes: Int,
    usedSeconds: Int,
    remainingMinutes: Int,
    remainingSeconds: Int,
    thresholdSeconds: Int,
    isTracking: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTracking) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isTracking) "Time Until Next Break" else "Tracking Paused",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isTracking) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = String.format("%d:%02d", remainingMinutes, remainingSeconds),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "remaining",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%d:%02d", usedMinutes, usedSeconds),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatThreshold(thresholdSeconds),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "threshold",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun formatThreshold(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds % 60 == 0 -> "${seconds / 60}m"
        else -> "${seconds / 60}m ${seconds % 60}s"
    }
}
