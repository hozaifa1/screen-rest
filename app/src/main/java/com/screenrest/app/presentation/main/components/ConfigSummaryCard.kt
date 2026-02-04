package com.screenrest.app.presentation.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.screenrest.app.domain.model.BreakConfig
import com.screenrest.app.domain.model.TrackingMode

@Composable
fun ConfigSummaryCard(
    breakConfig: BreakConfig,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Break Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Settings"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ConfigItem(
                label = "Break After",
                value = "${formatDuration(breakConfig.usageThresholdSeconds)} of ${
                    when (breakConfig.trackingMode) {
                        TrackingMode.CONTINUOUS -> "continuous"
                        TrackingMode.CUMULATIVE_DAILY -> "cumulative"
                    }
                } usage"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ConfigItem(
                label = "Break Duration",
                value = formatDuration(breakConfig.blockDurationSeconds)
            )
        }
    }
}

@Composable
private fun ConfigItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDuration(seconds: Int): String {
    return when {
        seconds < 60 -> "$seconds sec"
        seconds % 60 == 0 -> "${seconds / 60} min"
        else -> {
            val min = seconds / 60
            val sec = seconds % 60
            "$min min $sec sec"
        }
    }
}
