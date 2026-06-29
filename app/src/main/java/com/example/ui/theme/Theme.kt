package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = if (ThemeState.isDark) {
        darkColorScheme(
            primary = GoldenGold,
            onPrimary = Color.White,
            primaryContainer = HighDensityPrimaryContainer,
            onPrimaryContainer = HighDensityOnPrimaryContainer,
            secondary = HighDensitySecondaryContainer,
            onSecondary = HighDensityOnSecondaryContainer,
            secondaryContainer = HighDensitySecondaryContainer,
            onSecondaryContainer = HighDensityOnSecondaryContainer,
            tertiary = InfoTeal,
            onTertiary = Color.White,
            background = CharcoalNavy,
            onBackground = TextWhite,
            surface = DarkSlate,
            onSurface = TextWhite,
            surfaceVariant = LightSlate,
            onSurfaceVariant = TextWhite,
            error = AlertRed,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = GoldenGold,
            onPrimary = Color.White,
            primaryContainer = HighDensityPrimaryContainer,
            onPrimaryContainer = HighDensityOnPrimaryContainer,
            secondary = HighDensitySecondaryContainer,
            onSecondary = HighDensityOnSecondaryContainer,
            secondaryContainer = HighDensitySecondaryContainer,
            onSecondaryContainer = HighDensityOnSecondaryContainer,
            tertiary = InfoTeal,
            onTertiary = Color.White,
            background = CharcoalNavy,
            onBackground = TextWhite,
            surface = DarkSlate,
            onSurface = TextWhite,
            surfaceVariant = LightSlate,
            onSurfaceVariant = TextWhite,
            error = AlertRed,
            onError = Color.White
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

