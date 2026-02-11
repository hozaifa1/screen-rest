package com.screenrest.app.presentation.block

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.screenrest.app.domain.model.DisplayMessage
import com.screenrest.app.presentation.theme.LocalThemeColorPalette

@Composable
fun BlockScreen(
    remainingSeconds: Int,
    displayMessage: DisplayMessage?,
    isLoading: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val palette = LocalThemeColorPalette.current
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    val gradientTop = if (isDark) palette.gradientTopDark else palette.gradientTopLight
    val gradientBottom = if (isDark) palette.gradientBottomDark else palette.gradientBottomLight
    val timerColor = if (isDark) palette.primaryLight else palette.primaryVariant
    val messageColor = if (isDark) palette.textOnDark else palette.textPrimary
    val referenceColor = if (isDark) palette.secondary else palette.primary
    val dividerColor = if (isDark) palette.dividerDark else palette.dividerLight
    val subtitleColor = if (isDark) palette.textMuted else palette.textSecondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(gradientTop, gradientBottom))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = timerColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = String.format("%d:%02d", minutes, seconds),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = timerColor,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Take a break",
                fontSize = 14.sp,
                color = subtitleColor,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.3f),
                color = dividerColor,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (isLoading) {
                CircularProgressIndicator(color = timerColor)
            } else {
                when (displayMessage) {
                    is DisplayMessage.QuranAyah -> {
                        Text(
                            text = displayMessage.ayah.englishTranslation,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Normal,
                            color = messageColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "\u2014 Surah ${displayMessage.ayah.surahName} (${displayMessage.ayah.surahNumber}:${displayMessage.ayah.ayahNumber})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Italic,
                            color = referenceColor
                        )
                    }
                    is DisplayMessage.IslamicReminder -> {
                        Text(
                            text = displayMessage.text,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Normal,
                            color = messageColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    is DisplayMessage.Custom -> {
                        Text(
                            text = displayMessage.text,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Normal,
                            color = messageColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    null -> {}
                }
            }
        }
    }
}
