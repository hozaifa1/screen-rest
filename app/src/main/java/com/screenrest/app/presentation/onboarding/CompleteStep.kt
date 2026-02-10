package com.screenrest.app.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.screenrest.app.domain.model.EnforcementLevel
import com.screenrest.app.domain.model.PermissionStatus

@Composable
fun CompleteStep(
    permissionStatus: PermissionStatus,
    enforcementLevel: EnforcementLevel,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "All Set!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Permission summary",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = when (enforcementLevel) {
                EnforcementLevel.FULL -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                EnforcementLevel.STANDARD -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                EnforcementLevel.BASIC -> MaterialTheme.colorScheme.surfaceVariant
                EnforcementLevel.NONE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enforcement",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = enforcementLevel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (enforcementLevel) {
                        EnforcementLevel.FULL -> "Maximum protection"
                        EnforcementLevel.STANDARD -> "Good protection"
                        EnforcementLevel.BASIC -> "Basic protection"
                        EnforcementLevel.NONE -> "Insufficient permissions"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PermissionCheckItem(name = "Usage Stats", isGranted = permissionStatus.usageStats)
            PermissionCheckItem(name = "Display Over Apps", isGranted = permissionStatus.overlay)
            PermissionCheckItem(name = "Notifications", isGranted = permissionStatus.notification)
            PermissionCheckItem(name = "Accessibility", isGranted = permissionStatus.accessibility)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = permissionStatus.usageStats && permissionStatus.overlay
        ) {
            Text("Start Using ScreenRest", style = MaterialTheme.typography.labelLarge)
        }

        if (!permissionStatus.usageStats || !permissionStatus.overlay) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Grant required permissions to continue",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onBack) {
            Text("Back", style = MaterialTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun PermissionCheckItem(
    name: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isGranted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isGranted) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
