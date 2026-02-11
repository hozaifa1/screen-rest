package com.screenrest.app.presentation.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.screenrest.app.domain.model.ThemeColor
import com.screenrest.app.domain.model.ThemeMode
import com.screenrest.app.presentation.theme.getThemeColorPalette
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCustomMessages: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTimerDialog by remember { mutableStateOf(false) }

    if (uiState.showLongDurationWarning) {
        LongDurationWarningDialog(
            onDismiss = { viewModel.dismissLongDurationWarning() }
        )
    }

    if (uiState.showShortThresholdWarning) {
        ShortThresholdWarningDialog(
            onDismiss = { viewModel.dismissShortThresholdWarning() }
        )
    }

    if (showTimerDialog) {
        TimerAdjustDialog(
            currentThresholdSeconds = uiState.breakConfig.usageThresholdSeconds,
            currentDurationSeconds = uiState.breakConfig.blockDurationSeconds,
            onSave = { threshold, duration ->
                viewModel.updateTimers(threshold, duration)
                showTimerDialog = false
            },
            onDismiss = { showTimerDialog = false }
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
            BreakTimingSummaryCard(
                breakConfig = uiState.breakConfig,
                onAdjustTimers = { showTimerDialog = true }
            )

            // Messages section
            SectionHeader("Messages")
            MessagesCard(
                quranMessagesEnabled = uiState.breakConfig.quranMessagesEnabled,
                islamicRemindersEnabled = uiState.breakConfig.islamicRemindersEnabled,
                onQuranMessagesToggle = { viewModel.updateQuranMessagesEnabled(it) },
                onIslamicRemindersToggle = { viewModel.updateIslamicRemindersEnabled(it) },
                onNavigateToCustomMessages = onNavigateToCustomMessages
            )

            // Appearance section
            SectionHeader("Appearance")
            ThemeCard(
                currentTheme = uiState.themeMode,
                onThemeChange = { viewModel.updateTheme(it) }
            )
            ThemeColorCard(
                currentColor = uiState.themeColor,
                onColorChange = { viewModel.updateThemeColor(it) }
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
private fun BreakTimingSummaryCard(
    breakConfig: com.screenrest.app.domain.model.BreakConfig,
    onAdjustTimers: () -> Unit
) {
    val thresholdMin = breakConfig.usageThresholdSeconds / 60
    val thresholdSec = breakConfig.usageThresholdSeconds % 60
    val durationMin = breakConfig.blockDurationSeconds / 60
    val durationSec = breakConfig.blockDurationSeconds % 60

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
                Column {
                    Text(
                        text = "Trigger break after",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimerDisplay(thresholdMin, thresholdSec),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Block screen for",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimerDisplay(durationMin, durationSec),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalButton(
                onClick = onAdjustTimers,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adjust Timers")
            }
        }
    }
}

private fun formatTimerDisplay(minutes: Int, seconds: Int): String {
    return when {
        minutes == 0 -> "${seconds}s"
        seconds == 0 -> "${minutes}m"
        else -> "${minutes}m ${seconds}s"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerAdjustDialog(
    currentThresholdSeconds: Int,
    currentDurationSeconds: Int,
    onSave: (thresholdSeconds: Int, durationSeconds: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var thresholdMin by remember { mutableIntStateOf(currentThresholdSeconds / 60) }
    var thresholdSec by remember { mutableIntStateOf(currentThresholdSeconds % 60) }
    var durationMin by remember { mutableIntStateOf(currentDurationSeconds / 60) }
    var durationSec by remember { mutableIntStateOf(currentDurationSeconds % 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Adjust Timers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Threshold picker
                Text(
                    text = "Trigger break after",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                TimeScrollPicker(
                    minutes = thresholdMin,
                    seconds = thresholdSec,
                    onMinutesChange = { thresholdMin = it },
                    onSecondsChange = { thresholdSec = it },
                    maxMinutes = 120
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // Duration picker
                Text(
                    text = "Block screen for",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                TimeScrollPicker(
                    minutes = durationMin,
                    seconds = durationSec,
                    onMinutesChange = { durationMin = it },
                    onSecondsChange = { durationSec = it },
                    maxMinutes = 30
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val threshold = (thresholdMin * 60 + thresholdSec).coerceIn(1, 7200)
                    val duration = (durationMin * 60 + durationSec).coerceIn(1, 1800)
                    onSave(threshold, duration)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TimeScrollPicker(
    minutes: Int,
    seconds: Int,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    maxMinutes: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minutes wheel
        ScrollWheelPicker(
            value = minutes,
            range = 0..maxMinutes,
            onValueChange = onMinutesChange,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "min",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Seconds wheel
        ScrollWheelPicker(
            value = seconds,
            range = 0..59,
            onValueChange = onSecondsChange,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "sec",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScrollWheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = range.toList()
    val visibleItems = 3
    val itemHeight = 40.dp
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(value).coerceAtLeast(0)
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex +
                    if (listState.firstVisibleItemScrollOffset > itemHeight.value * 0.5f) 1 else 0
            val clampedIndex = centerIndex.coerceIn(0, items.lastIndex)
            if (items[clampedIndex] != value) {
                onValueChange(items[clampedIndex])
            }
        }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        // Selection highlight
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {}

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItems),
            contentPadding = PaddingValues(vertical = itemHeight),
            flingBehavior = flingBehavior
        ) {
            items(items.size) { index ->
                val distanceFromCenter = abs(
                    index - (listState.firstVisibleItemIndex +
                            if (listState.firstVisibleItemScrollOffset > itemHeight.value * 0.5f) 1 else 0)
                )
                val alpha = if (distanceFromCenter == 0) 1f else 0.35f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", items[index]),
                        fontSize = if (distanceFromCenter == 0) 22.sp else 16.sp,
                        fontWeight = if (distanceFromCenter == 0) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortThresholdWarningDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text("\u26A0\uFE0F", style = MaterialTheme.typography.headlineMedium)
        },
        title = {
            Text("Very Short Threshold")
        },
        text = {
            Text("You\u2019ve set the usage threshold to less than 30 seconds. The block screen will trigger almost immediately. Make sure this is intentional.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun MessagesCard(
    quranMessagesEnabled: Boolean,
    islamicRemindersEnabled: Boolean,
    onQuranMessagesToggle: (Boolean) -> Unit,
    onIslamicRemindersToggle: (Boolean) -> Unit,
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Islamic Reminders",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (islamicRemindersEnabled) "Shown during breaks" else "Disabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = islamicRemindersEnabled,
                    onCheckedChange = onIslamicRemindersToggle
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

@Composable
private fun ThemeColorCard(
    currentColor: ThemeColor,
    onColorChange: (ThemeColor) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Theme Color",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            val colors = ThemeColor.entries
            val rows = colors.chunked(5)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { color ->
                            val palette = getThemeColorPalette(color)
                            val selected = currentColor == color

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (selected) Modifier.border(
                                            2.5.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape
                                        ) else Modifier
                                    )
                                    .clickable { onColorChange(color) },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(42.dp)) {
                                    // Light color - left half
                                    drawArc(
                                        color = palette.primary,
                                        startAngle = 90f,
                                        sweepAngle = 180f,
                                        useCenter = true,
                                        size = Size(size.width, size.height)
                                    )
                                    // Dark color - right half
                                    drawArc(
                                        color = palette.primaryLight,
                                        startAngle = 270f,
                                        sweepAngle = 180f,
                                        useCenter = true,
                                        size = Size(size.width, size.height)
                                    )
                                }

                                if (selected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier
                                                .padding(2.dp)
                                                .size(14.dp),
                                            tint = palette.primaryVariant
                                        )
                                    }
                                }
                            }
                        }
                        // Fill remaining space if row has fewer than 5 items
                        repeat(5 - row.size) {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }

            Text(
                text = colorDisplayName(currentColor),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun colorDisplayName(color: ThemeColor): String = when (color) {
    ThemeColor.TEAL -> "Teal"
    ThemeColor.BLUE -> "Blue"
    ThemeColor.INDIGO -> "Indigo"
    ThemeColor.PURPLE -> "Purple"
    ThemeColor.PINK -> "Pink"
    ThemeColor.RED -> "Red"
    ThemeColor.ORANGE -> "Orange"
    ThemeColor.AMBER -> "Amber"
    ThemeColor.GREEN -> "Green"
    ThemeColor.CYAN -> "Cyan"
}
