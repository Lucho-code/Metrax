package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAmber,
    onPrimary = Color.Black,
    primaryContainer = PrimaryAmberDark,
    onPrimaryContainer = Color.White,
    secondary = SecondaryCyan,
    onSecondary = Color.White,
    tertiary = AccentEmerald,
    background = DarkBackground,
    onBackground = Slate100,
    surface = DarkSurface,
    onSurface = Slate100,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Slate400,
    error = AccentRose
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAmber,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFFF7ED),
    onPrimaryContainer = PrimaryAmberDark,
    secondary = SecondaryCyan,
    onSecondary = Color.White,
    tertiary = AccentEmerald,
    background = LightBackground,
    onBackground = Slate900,
    surface = LightSurface,
    onSurface = Slate900,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Slate600,
    error = AccentRose
)

@Composable
fun MetrajeInstanteTheme(
    darkTheme: Boolean = true, // Default to sleek dark AR theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
