package com.screenrest.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenrest.app.domain.model.ThemeMode
import com.screenrest.app.domain.model.TrackingMode
import com.screenrest.app.presentation.settings.components.TrackingModeSelector

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
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            BreakConfigurationSection(
                breakConfig = uiState.breakConfig,
                onThresholdSecondsChange = { viewModel.updateThresholdSeconds(it) },
                onDurationChange = { viewModel.updateDuration(it) },
                onTrackingModeChange = { viewModel.updateTrackingMode(it) }
            )
            
            HorizontalDivider()
            
            MessagesSection(
                quranMessagesEnabled = uiState.breakConfig.quranMessagesEnabled,
                onQuranMessagesToggle = { viewModel.updateQuranMessagesEnabled(it) },
                onNavigateToCustomMessages = onNavigateToCustomMessages
            )
            
            HorizontalDivider()
            
            AppearanceSection(
                currentTheme = uiState.themeMode,
                onThemeChange = { viewModel.updateTheme(it) }
            )
            
            HorizontalDivider()
            
            AboutSection()
        }
    }
}

@Composable
private fun BreakConfigurationSection(
    breakConfig: com.screenrest.app.domain.model.BreakConfig,
    onThresholdSecondsChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onTrackingModeChange: (TrackingMode) -> Unit
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
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Break Configuration",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Usage Threshold (trigger break after)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
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
                label = { Text("Min") },
                modifier = Modifier.weight(1f),
                singleLine = true
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
                label = { Text("Sec") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Break Duration (block screen for)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
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
                label = { Text("Min") },
                modifier = Modifier.weight(1f),
                singleLine = true
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
                label = { Text("Sec") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TrackingModeSelector(
            selectedMode = breakConfig.trackingMode,
            onModeSelected = onTrackingModeChange
        )
    }
}

@Composable
private fun MessagesSection(
    quranMessagesEnabled: Boolean,
    onQuranMessagesToggle: (Boolean) -> Unit,
    onNavigateToCustomMessages: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Break Messages",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Quranic Verses",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (quranMessagesEnabled) 
                                "Quranic verses will be shown during breaks" 
                            else 
                                "Only custom messages will be shown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = quranMessagesEnabled,
                        onCheckedChange = onQuranMessagesToggle
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider()
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "During breaks, you'll see:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (quranMessagesEnabled)
                        "• Custom messages (if you add any)\n• Quranic verses from API (with fallback)\n• Built-in verses (always available)"
                    else
                        "• Custom messages only\n• Add messages below to personalize your breaks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onNavigateToCustomMessages,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Custom Messages")
                }
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = currentTheme == mode,
                        onClick = { onThemeChange(mode) }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mode.name.replace('_', ' ').lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Text(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> "Follow system settings"
                                ThemeMode.LIGHT -> "Always use light theme"
                                ThemeMode.DARK -> "Always use dark theme"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "About",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AboutItem("Version", "1.0.0")
                AboutItem("License", "MIT")
                AboutItem("Source Code", "github.com/screenrest/app")
            }
        }
    }
}

@Composable
private fun AboutItem(label: String, value: String) {
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
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatDuration(seconds: Int): String {
    return when {
        seconds < 60 -> "$seconds seconds"
        seconds % 60 == 0 -> "${seconds / 60} minutes"
        else -> {
            val min = seconds / 60
            val sec = seconds % 60
            "$min min $sec sec"
        }
    }
}
