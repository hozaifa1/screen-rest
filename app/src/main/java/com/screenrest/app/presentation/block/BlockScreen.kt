package com.screenrest.app.presentation.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenrest.app.domain.model.DisplayMessage
import com.screenrest.app.presentation.components.AyahDisplay

@Composable
fun BlockScreen(
    remainingSeconds: Int,
    displayMessage: DisplayMessage?,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = formatTime(remainingSeconds),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                when (displayMessage) {
                    is DisplayMessage.QuranAyah -> {
                        AyahDisplay(
                            ayah = displayMessage.ayah,
                            modifier = Modifier
                        )
                    }
                    is DisplayMessage.Custom -> {
                        Text(
                            text = displayMessage.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                    null -> {
                        // Empty state
                    }
                }
            }
            
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}
