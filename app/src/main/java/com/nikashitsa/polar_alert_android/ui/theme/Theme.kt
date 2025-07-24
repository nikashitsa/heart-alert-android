package com.nikashitsa.polar_alert_android.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme()

@Composable
fun HeartAlertTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides Colors.White,
        LocalTextStyle provides Fonts.textMd
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}