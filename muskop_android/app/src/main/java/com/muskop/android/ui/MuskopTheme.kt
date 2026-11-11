package com.muskop.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColors = lightColorScheme(
    primary = Color(0xFFB96B09),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFC95E37),
    background = Color(0xFFF5F1E8),
    surface = Color(0xFFFFFBF5),
    onBackground = Color(0xFF1F2430),
    onSurface = Color(0xFF1F2430),
    primaryContainer = Color(0xFFFFDDB1),
    onPrimaryContainer = Color(0xFF2A1800),
    secondaryContainer = Color(0xFFFFDBD0),
    onSecondaryContainer = Color(0xFF3A0B00),
    surfaceVariant = Color(0xFFECE1D2),
    onSurfaceVariant = Color(0xFF4D463D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2A33C),
    secondary = Color(0xFFE8875B),
    background = Color(0xFF1F2430),
    surface = Color(0xFF2A3140),
    onBackground = Color(0xFFF3EEE6),
    onSurface = Color(0xFFF3EEE6),
    primaryContainer = Color(0xFF5B3700),
    onPrimaryContainer = Color(0xFFFFDDB1),
    secondaryContainer = Color(0xFF5D1F0D),
    onSecondaryContainer = Color(0xFFFFDBD0),
    surfaceVariant = Color(0xFF46403A),
    onSurfaceVariant = Color(0xFFD0C5B9),
)

private val MuskopTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
)

private val MuskopShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun MuskopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MuskopTypography,
        shapes = MuskopShapes,
        content = content,
    )
}
