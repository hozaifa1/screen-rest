package com.screenrest.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenrest.app.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCustomMessages: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showLongDurationWarning) {
        LongDurationWarningDialog(
            onDismiss = { viewModel.dismissLongDurationWarning() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Break timing section
            SectionHeader("Break Timing")
            BreakTimingCard(
                breakConfig = uiState.breakConfig,
                onThresholdSecondsChange = { viewModel.updateThresholdSeconds(it) },
                onDurationChange = { viewModel.updateDuration(it) }
            )

            // Messages section
            SectionHeader("Messages")
            MessagesCard(
                quranMessagesEnabled = uiState.breakConfig.quranMessagesEnabled,
                onQuranMessagesToggle = { viewModel.updateQuranMessagesEnabled(it) },
                onNavigateToCustomMessages = onNavigateToCustomMessages
            )

            // Appearance section
            SectionHeader("Appearance")
            ThemeCard(
                currentTheme = uiState.themeMode,
                onThemeChange = { viewModel.updateTheme(it) }
            )

            // About
            SectionHeader("About")
            AboutCard()

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun BreakTimingCard(
    breakConfig: com.screenrest.app.domain.model.BreakConfig,
    onThresholdSecondsChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit
) {
    var thresholdMinutesText by remember(breakConfig.usageThresholdSeconds) {
        mutableStateOf((breakConfig.usageThresholdSeconds / 60).toString())
    }
    var thresholdSecondsText by remember(breakConfig.usageThresholdSeconds) {
        mutableStateOf((breakConfig.usageThresholdSeconds % 60).toString())
    }
    var durationMinutesText by remember(breakConfig.blockDurationSeconds) {
        mutableStateOf((breakConfig.blockDurationSeconds / 60).toString())
    }
    var durationSecondsText by remember(breakConfig.blockDurationSeconds) {
        mutableStateOf((breakConfig.blockDurationSeconds % 60).toString())
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Threshold
            Text(
                text = "Trigger break after",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = thresholdMinutesText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 4) {
                            thresholdMinutesText = value
                            val minutes = value.toIntOrNull() ?: 0
                            val seconds = thresholdSecondsText.toIntOrNull() ?: 0
                            val totalSeconds = (minutes * 60 + seconds).coerceIn(1, 86400)
                            onThresholdSecondsChange(totalSeconds)
                        }
                    },
                    label = { Text("Min", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = thresholdSecondsText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 2) {
                            thresholdSecondsText = value
                            val minutes = thresholdMinutesText.toIntOrNull() ?: 0
                            val seconds = (value.toIntOrNull() ?: 0).coerceIn(0, 59)
                            val totalSeconds = (minutes * 60 + seconds).coerceIn(1, 86400)
                            onThresholdSecondsChange(totalSeconds)
                        }
                    },
                    label = { Text("Sec", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Duration
            Text(
                text = "Block screen for",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = durationMinutesText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 3) {
                            durationMinutesText = value
                            val minutes = value.toIntOrNull() ?: 0
                            val seconds = durationSecondsText.toIntOrNull() ?: 0
                            val totalSeconds = (minutes * 60 + seconds).coerceIn(1, 7200)
                            onDurationChange(totalSeconds)
                        }
                    },
                    label = { Text("Min", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = durationSecondsText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 2) {
                            durationSecondsText = value
                            val minutes = durationMinutesText.toIntOrNull() ?: 0
                            val seconds = (value.toIntOrNull() ?: 0).coerceIn(0, 59)
                            val totalSeconds = (minutes * 60 + seconds).coerceIn(1, 7200)
                            onDurationChange(totalSeconds)
                        }
                    },
                    label = { Text("Sec", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MessagesCard(
    quranMessagesEnabled: Boolean,
    onQuranMessagesToggle: (Boolean) -> Unit,
    onNavigateToCustomMessages: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quranic Verses",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (quranMessagesEnabled) "Shown during breaks" else "Disabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = quranMessagesEnabled,
                    onCheckedChange = onQuranMessagesToggle
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCustomMessages() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Messages",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                val selected = currentTheme == mode
                FilterChip(
                    selected = selected,
                    onClick = { onThemeChange(mode) },
                    label = {
                        Text(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> "Auto"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AboutCard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AboutRow("Version", "1.0.0")
            AboutRow("License", "MIT")
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
