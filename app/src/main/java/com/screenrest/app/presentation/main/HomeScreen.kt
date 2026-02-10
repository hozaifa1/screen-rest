package com.screenrest.app.presentation.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenrest.app.presentation.components.PermissionWarningCard
import com.screenrest.app.service.UsageTrackingService
import kotlinx.coroutines.delay

private val quotes = listOf(
    "\"Verily, with hardship comes ease.\" — Quran 94:6",
    "\"Take benefit of five before five: your youth before your old age.\" — Hadith",
    "\"The best of you are those who are best to their bodies.\"",
    "\"Rest is not idleness; it is the key to greater productivity.\"",
    "\"Your eyes are an amanah (trust). Guard them well.\"",
    "\"He who has no rest, has no worship.\"",
    "\"Balance is the essence of a good life.\"",
    "\"Step away from the screen. Step closer to yourself.\""
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var quoteIndex by remember { mutableIntStateOf((System.currentTimeMillis() / 86400000).toInt() % quotes.size) }

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
    val progress = if (thresholdMs > 0) (usageMs.toFloat() / thresholdMs).coerceIn(0f, 1f) else 0f

    val usedMinutes = (usageMs / 60000).toInt()
    val usedSeconds = ((usageMs % 60000) / 1000).toInt()
    val remainingMinutes = (remainingMs / 60000).toInt()
    val remainingSeconds = ((remainingMs % 60000) / 1000).toInt()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar: minimal with just settings icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Quote card
            QuoteCard(
                quote = quotes[quoteIndex],
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Circular timer
            CircularTimerSection(
                remainingMinutes = remainingMinutes,
                remainingSeconds = remainingSeconds,
                usedMinutes = usedMinutes,
                usedSeconds = usedSeconds,
                thresholdSeconds = uiState.breakConfig.usageThresholdSeconds,
                progress = progress,
                isTracking = uiState.isServiceRunning,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tracking toggle + config row
            TrackingControlRow(
                isRunning = uiState.isServiceRunning,
                enforcementLevel = uiState.enforcementLevel,
                breakAfter = formatDuration(uiState.breakConfig.usageThresholdSeconds),
                breakDuration = formatDuration(uiState.breakConfig.blockDurationSeconds),
                onToggle = { viewModel.toggleService() },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Permission warnings (compact)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                        description = "Breaks will only show as notifications without this",
                        permissionType = "overlay"
                    )
                }

                if (!uiState.permissionStatus.accessibility && uiState.permissionStatus.usageStats && uiState.permissionStatus.overlay) {
                    PermissionWarningCard(
                        title = "Accessibility Service Optional",
                        description = "Without this, the break screen can be bypassed via the notification bar or home button. Enable for full protection.",
                        permissionType = "accessibility"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuoteCard(
    quote: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = quote,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun CircularTimerSection(
    remainingMinutes: Int,
    remainingSeconds: Int,
    usedMinutes: Int,
    usedSeconds: Int,
    thresholdSeconds: Int,
    progress: Float,
    isTracking: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val strokeWidth = 8.dp.toPx()
                val padding = strokeWidth / 2
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(padding, padding)

                // Background track
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress arc
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%d:%02d", remainingMinutes, remainingSeconds),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isTracking) "remaining" else "paused",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = String.format("%d:%02d", usedMinutes, usedSeconds),
                label = "used"
            )
            StatItem(
                value = formatThreshold(thresholdSeconds),
                label = "threshold"
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrackingControlRow(
    isRunning: Boolean,
    enforcementLevel: com.screenrest.app.domain.model.EnforcementLevel,
    breakAfter: String,
    breakDuration: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRunning) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "Tracking" else "Paused",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = enforcementLevel.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onToggle,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (isRunning) "Pause" else "Start",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Config summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Break after",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = breakAfter,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = breakDuration,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
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

private fun formatDuration(seconds: Int): String {
    return when {
        seconds < 60 -> "$seconds sec"
        seconds % 60 == 0 -> "${seconds / 60} min"
        else -> "${seconds / 60}m ${seconds % 60}s"
    }
}
