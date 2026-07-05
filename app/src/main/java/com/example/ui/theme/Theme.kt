package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ProDarkColorScheme = darkColorScheme(
    primary = AccentTeal,
    secondary = AccentAmber,
    tertiary = AccentRed,
    background = CarbonDark,
    surface = CarbonGray,
    onPrimary = BlackPure,
    onSecondary = BlackPure,
    onTertiary = WhitePure,
    onBackground = WhitePure,
    onSurface = WhitePure,
    outline = BorderGray
)

private val ProLightColorScheme = lightColorScheme(
    primary = AccentTealDark,
    secondary = AccentAmber,
    tertiary = AccentRed,
    background = CarbonDark, // Always dark theme for professional DSLR camera interface
    surface = CarbonGray,
    onPrimary = BlackPure,
    onSecondary = BlackPure,
    onTertiary = WhitePure,
    onBackground = WhitePure,
    onSurface = WhitePure,
    outline = BorderGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default for DSLR experience
    dynamicColor: Boolean = false, // Disable dynamic colors to keep our premium carbon branding consistent
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) ProDarkColorScheme else ProLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
