package com.example.soundbox.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object SoundBoxColors {
    val Background = Color(0xFF181818)
    val TopBar = Color(0xFF202020)
    val Card = Color(0xFF303030)
    val CardPressed = Color(0xFF3A3A3A)
    val Border = Color(0xFF555555)
    val TextPrimary = Color(0xFFEEEEEE)
    val TextSecondary = Color(0xFF999999)
    val Accent = Color(0xFF6F9FB5)
    val Danger = Color(0xFFB85C5C)
    val Divider = Color(0xFF3D3D3D)
}

private val SoundBoxColorScheme = darkColorScheme(
    primary = SoundBoxColors.Accent,
    onPrimary = Color(0xFF10191D),
    secondary = SoundBoxColors.Accent,
    onSecondary = Color(0xFF10191D),
    background = SoundBoxColors.Background,
    onBackground = SoundBoxColors.TextPrimary,
    surface = SoundBoxColors.Card,
    onSurface = SoundBoxColors.TextPrimary,
    surfaceVariant = SoundBoxColors.CardPressed,
    onSurfaceVariant = SoundBoxColors.TextSecondary,
    outline = SoundBoxColors.Border,
    error = SoundBoxColors.Danger,
    onError = Color.White
)

private val SoundBoxTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun SoundBoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SoundBoxColorScheme,
        typography = SoundBoxTypography,
        content = content
    )
}