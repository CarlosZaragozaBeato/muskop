package com.muskop.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFB96B09),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFC95E37),
    background = Color(0xFFF5F1E8),
    surface = Color(0xFFFFFBF5),
    onBackground = Color(0xFF1F2430),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2A33C),
    secondary = Color(0xFFE8875B),
    background = Color(0xFF1F2430),
    surface = Color(0xFF2A3140),
)

@Composable
fun MuskopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
