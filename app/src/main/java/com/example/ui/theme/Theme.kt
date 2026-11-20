package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private data class ThemeAccents(
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val onPrimaryContainer: Color
)

fun getDynamicColorScheme(themeColor: String): ColorScheme {
    val accent = when (themeColor.lowercase()) {
        "blue" -> ThemeAccents(Color(0xFF0284C7), Color(0xFFBAE6FD), NeoWhite, NeoBlack)
        "green" -> ThemeAccents(Color(0xFF059669), Color(0xFFA7F3D0), NeoWhite, NeoBlack)
        "orange" -> ThemeAccents(Color(0xFFEA580C), Color(0xFFFFEDD5), NeoWhite, NeoBlack)
        "pink" -> ThemeAccents(Color(0xFFE11D48), Color(0xFFFFE4E6), NeoWhite, NeoBlack)
        "teal" -> ThemeAccents(Color(0xFF0D9488), Color(0xFFCCFBF1), NeoWhite, NeoBlack)
        "red" -> ThemeAccents(NeoRed, NeoRedLight, NeoWhite, NeoBlack)
        else -> { // "yellow" or default
            ThemeAccents(NeoYellow, NeoYellowLight, NeoBlack, NeoBlack)
        }
    }

    return LightColorScheme.copy(
        primary = accent.primary,
        primaryContainer = accent.primaryContainer,
        onPrimary = accent.onPrimary,
        onPrimaryContainer = accent.onPrimaryContainer
    )
}

private val LightColorScheme = lightColorScheme(
    primary = NeoYellow,
    primaryContainer = NeoYellow,
    onPrimary = NeoBlack,
    secondary = NeoRed,
    onSecondary = NeoWhite,
    tertiary = NeoCyan,
    background = NeoCream,
    surface = NeoWhite,
    onBackground = NeoBlack,
    onSurface = NeoBlack,
    onSurfaceVariant = NeoNearBlack,
    outline = NeoBlack,
    error = NeoRed,
    onError = NeoWhite
)

@Composable
fun MyApplicationTheme(
    themeColor: String = "yellow",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        else -> getDynamicColorScheme(themeColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

