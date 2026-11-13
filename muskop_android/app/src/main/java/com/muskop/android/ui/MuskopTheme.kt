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
    primary = Color(0xFFF2A33C),
    onPrimary = Color(0xFF111111),
    secondary = Color(0xFF111111),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111),
    primaryContainer = Color(0xFFFFE7C6),
    onPrimaryContainer = Color(0xFF111111),
    secondaryContainer = Color(0xFFEEEEEE),
    onSecondaryContainer = Color(0xFF111111),
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF595959),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F8F8),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEEEEEE),
    surfaceContainerHighest = Color(0xFFE8E8E8),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE8E8E8),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFFDEDEDE),
    inverseSurface = Color(0xFF252525),
    inverseOnSurface = Color(0xFFF5F5F5),
    inversePrimary = Color(0xFFF2A33C),
    tertiary = Color(0xFF111111),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEEEEEE),
    onTertiaryContainer = Color(0xFF111111),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2A33C),
    onPrimary = Color(0xFF111111),
    secondary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF111111),
    background = Color(0xFF111111),
    surface = Color(0xFF111111),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF49331B),
    onPrimaryContainer = Color(0xFFFFE7C6),
    secondaryContainer = Color(0xFF252525),
    onSecondaryContainer = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF252525),
    onSurfaceVariant = Color(0xFFB8B8B8),
    surfaceContainerLowest = Color(0xFF111111),
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainer = Color(0xFF1C1C1C),
    surfaceContainerHigh = Color(0xFF252525),
    surfaceContainerHighest = Color(0xFF303030),
    surfaceBright = Color(0xFF383838),
    surfaceDim = Color(0xFF111111),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF969696),
    outlineVariant = Color(0xFF383838),
    inverseSurface = Color(0xFFF5F5F5),
    inverseOnSurface = Color(0xFF111111),
    inversePrimary = Color(0xFF704300),
    tertiary = Color(0xFFF5F5F5),
    onTertiary = Color(0xFF111111),
    tertiaryContainer = Color(0xFF252525),
    onTertiaryContainer = Color(0xFFF5F5F5),
)

private val MuskopTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
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
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
    ),
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
