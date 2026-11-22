package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =============================================================================
// Sada (صَدى) Brand Identity & Curated M3 Theme Presets
// Default: Sada Teal + Coral Pulse
// =============================================================================

data class SadaThemePresetMeta(
    val id: String,
    val label: String,
    val subtitle: String,
    val previewPrimaryLight: Color,
    val previewAccentLight: Color,
    val previewPrimaryDark: Color,
    val previewAccentDark: Color
)

val SadaBrandThemePresets: List<SadaThemePresetMeta> = listOf(
    SadaThemePresetMeta(
        id = "sada_teal",
        label = "Sada Teal",
        subtitle = "Default · Coral Pulse",
        previewPrimaryLight = Color(0xFF0F766E),
        previewAccentLight = Color(0xFFE05A47),
        previewPrimaryDark = Color(0xFF2DD4BF),
        previewAccentDark = Color(0xFFFB7185)
    ),
    SadaThemePresetMeta(
        id = "sada_indigo",
        label = "Folio Indigo",
        subtitle = "Mint Emerald",
        previewPrimaryLight = Color(0xFF4F46E5),
        previewAccentLight = Color(0xFF10B981),
        previewPrimaryDark = Color(0xFF818CF8),
        previewAccentDark = Color(0xFF34D399)
    ),
    SadaThemePresetMeta(
        id = "sada_ocean",
        label = "Deep Ocean",
        subtitle = "Warm Gold",
        previewPrimaryLight = Color(0xFF0E7490),
        previewAccentLight = Color(0xFFD97706),
        previewPrimaryDark = Color(0xFF22D3EE),
        previewAccentDark = Color(0xFFFBBF24)
    ),
    SadaThemePresetMeta(
        id = "lisan_oxford",
        label = "Lisan Oxford",
        subtitle = "Editorial Paper",
        previewPrimaryLight = Color(0xFF1E40AF),
        previewAccentLight = Color(0xFFD97706),
        previewPrimaryDark = Color(0xFF5B8CFF),
        previewAccentDark = Color(0xFFFBBF24)
    ),
    SadaThemePresetMeta(
        id = "qabas_amber",
        label = "Qabas Amber",
        subtitle = "Warm Linen",
        previewPrimaryLight = Color(0xFFC2590A),
        previewAccentLight = Color(0xFF0F766E),
        previewPrimaryDark = Color(0xFFFB923C),
        previewAccentDark = Color(0xFF2DD4BF)
    ),
    SadaThemePresetMeta(
        id = "blue",
        label = "Apple Blue",
        subtitle = "Classic",
        previewPrimaryLight = Color(0xFF007AFF),
        previewAccentLight = Color(0xFF5856D6),
        previewPrimaryDark = Color(0xFF0A84FF),
        previewAccentDark = Color(0xFF7D7AFF)
    ),
    SadaThemePresetMeta(
        id = "green",
        label = "Forest",
        subtitle = "Calm Green",
        previewPrimaryLight = Color(0xFF1D6B4A),
        previewAccentLight = Color(0xFF30D158),
        previewPrimaryDark = Color(0xFF8CD6B1),
        previewAccentDark = Color(0xFF30D158)
    ),
    SadaThemePresetMeta(
        id = "pink",
        label = "Rose",
        subtitle = "Warm Berry",
        previewPrimaryLight = Color(0xFFB32656),
        previewAccentLight = Color(0xFFFF6B8B),
        previewPrimaryDark = Color(0xFFFFB1C4),
        previewAccentDark = Color(0xFFFB7185)
    )
)

// =============================================================================
// Sada Teal + Coral (DEFAULT) — Light & Dark Schemes
// =============================================================================

val SadaTealLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5F5F0),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F7F4),
    onSecondaryContainer = Color(0xFF083A36),
    tertiary = Color(0xFFE05A47),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE7E3),
    onTertiaryContainer = Color(0xFF5C170D),
    error = Color(0xFFD70015),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE5E7),
    onErrorContainer = Color(0xFF690005),
    background = Color(0xFFF4F8F7),
    onBackground = Color(0xFF0F1F1D),
    surface = Color(0xFFF4F8F7),
    onSurface = Color(0xFF0F1F1D),
    surfaceVariant = Color(0xFFDCE6E3),
    onSurfaceVariant = Color(0xFF4F6662),
    surfaceDim = Color(0xFFE4ECEA),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEBF2F0),
    surfaceContainerHigh = Color(0xFFE0EAE7),
    surfaceContainerHighest = Color(0xFFD2E0DC),
    outline = Color(0xFF7B938E),
    outlineVariant = Color(0xFFCFDCD8),
    inverseSurface = Color(0xFF132422),
    inverseOnSurface = Color(0xFFF0F7F5),
    inversePrimary = Color(0xFF2DD4BF),
    scrim = Color(0xFF000000)
)

val SadaTealDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF032B27),
    primaryContainer = Color(0xFF113A36),
    onPrimaryContainer = Color(0xFF99F6E4),
    secondary = Color(0xFF14B8A6),
    onSecondary = Color(0xFF032B27),
    secondaryContainer = Color(0xFF143532),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFFB7185),
    onTertiary = Color(0xFF3E0813),
    tertiaryContainer = Color(0xFF421520),
    onTertiaryContainer = Color(0xFFFECDD3),
    error = Color(0xFFFF5A5F),
    onError = Color(0xFF3A0006),
    errorContainer = Color(0xFF451418),
    onErrorContainer = Color(0xFFFFB4AE),
    background = Color(0xFF081110),
    onBackground = Color(0xFFECF7F5),
    surface = Color(0xFF081110),
    onSurface = Color(0xFFECF7F5),
    surfaceVariant = Color(0xFF1D302E),
    onSurfaceVariant = Color(0xFF8CA8A3),
    surfaceDim = Color(0xFF060D0C),
    surfaceBright = Color(0xFF1E3331),
    surfaceContainerLowest = Color(0xFF050B0A),
    surfaceContainerLow = Color(0xFF111E1D),
    surfaceContainer = Color(0xFF162725),
    surfaceContainerHigh = Color(0xFF1D3230),
    surfaceContainerHighest = Color(0xFF26403D),
    outline = Color(0xFF53706B),
    outlineVariant = Color(0xFF223735),
    inverseSurface = Color(0xFFECF7F5),
    inverseOnSurface = Color(0xFF0F1F1D),
    inversePrimary = Color(0xFF0F766E),
    scrim = Color(0xFF000000)
)

// Legacy Apple Indigo base (preserved for existing references & classic blue/indigo option)
val RedeusLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF003A7A),
    secondary = Color(0xFF5856D6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECEBFF),
    onSecondaryContainer = Color(0xFF282678),
    tertiary = Color(0xFFC97A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFF1D6),
    onTertiaryContainer = Color(0xFF4A2A00),
    error = Color(0xFFD70015),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE5E7),
    onErrorContainer = Color(0xFF690005),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFD1D1D6),
    onSurfaceVariant = Color(0xFF636366),
    surfaceDim = Color(0xFFE5E5EA),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF2F2F7),
    surfaceContainerHigh = Color(0xFFE5E5EA),
    surfaceContainerHighest = Color(0xFFD1D1D6),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFD1D1D6),
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFF2F2F7),
    inversePrimary = Color(0xFF0A84FF),
    scrim = Color(0xFF000000)
)

val RedeusDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF102A4A),
    onPrimaryContainer = Color(0xFF8CC5FF),
    secondary = Color(0xFF7D7AFF),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF252538),
    onSecondaryContainer = Color(0xFFD7D6FF),
    tertiary = Color(0xFFFF9F0A),
    onTertiary = Color(0xFF1C1200),
    tertiaryContainer = Color(0xFF3A280A),
    onTertiaryContainer = Color(0xFFFFD89E),
    error = Color(0xFFFF453A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF3D1416),
    onErrorContainer = Color(0xFFFFB4AE),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF3A3A3C),
    onSurfaceVariant = Color(0xFF98989D),
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF2C2C2E),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF242426),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    outline = Color(0xFF636366),
    outlineVariant = Color(0xFF38383A),
    inverseSurface = Color(0xFFF5F5F7),
    inverseOnSurface = Color(0xFF1C1C1E),
    inversePrimary = Color(0xFF007AFF),
    scrim = Color(0xFF000000)
)

// =============================================================================
// Theme Preset Palettes (Light + Dark M3 mappings for user themeColor setting)
// =============================================================================

internal fun presetColorScheme(themeColor: String, darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) RedeusDarkColorScheme else RedeusLightColorScheme
    return when (themeColor.lowercase()) {
        "sada_teal", "teal", "default" -> if (darkTheme) SadaTealDarkColorScheme else SadaTealLightColorScheme

        "sada_indigo" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFF818CF8),
                onPrimary = Color(0xFF14124A),
                primaryContainer = Color(0xFF242254),
                onPrimaryContainer = Color(0xFFE0E7FF),
                secondary = Color(0xFF34D399),
                onSecondary = Color(0xFF032D1F),
                tertiary = Color(0xFF34D399),
                onTertiary = Color(0xFF032D1F),
                background = Color(0xFF0C0C14),
                surface = Color(0xFF0C0C14),
                surfaceContainerLow = Color(0xFF161624),
                surfaceContainer = Color(0xFF1E1E32),
                surfaceContainerHigh = Color(0xFF27273D),
                outlineVariant = Color(0xFF2B2B44)
            )
        } else {
            base.copy(
                primary = Color(0xFF4F46E5),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFEEF2FF),
                onPrimaryContainer = Color(0xFF1E1B4B),
                secondary = Color(0xFF10B981),
                onSecondary = Color(0xFFFFFFFF),
                tertiary = Color(0xFF059669),
                onTertiary = Color(0xFFFFFFFF),
                background = Color(0xFFF7F7FA),
                surface = Color(0xFFF7F7FA),
                surfaceContainerLow = Color(0xFFFFFFFF),
                surfaceContainer = Color(0xFFEEEEF6),
                outlineVariant = Color(0xFFDFDFE9)
            )
        }

        "sada_ocean" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFF22D3EE),
                onPrimary = Color(0xFF032A33),
                primaryContainer = Color(0xFF0E3746),
                onPrimaryContainer = Color(0xFFCFFAFE),
                secondary = Color(0xFF38BDF8),
                onSecondary = Color(0xFF04263B),
                tertiary = Color(0xFFFBBF24),
                onTertiary = Color(0xFF332000),
                background = Color(0xFF081018),
                surface = Color(0xFF081018),
                surfaceContainerLow = Color(0xFF101D29),
                surfaceContainer = Color(0xFF162635),
                surfaceContainerHigh = Color(0xFF1F3244),
                outlineVariant = Color(0xFF243A4E)
            )
        } else {
            base.copy(
                primary = Color(0xFF0E7490),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFE0F2FE),
                onPrimaryContainer = Color(0xFF0C4A6E),
                secondary = Color(0xFF0284C7),
                onSecondary = Color(0xFFFFFFFF),
                tertiary = Color(0xFFD97706),
                onTertiary = Color(0xFFFFFFFF),
                background = Color(0xFFF4F8FA),
                surface = Color(0xFFF4F8FA),
                surfaceContainerLow = Color(0xFFFFFFFF),
                surfaceContainer = Color(0xFFE8F1F5),
                outlineVariant = Color(0xFFD0DFE6)
            )
        }

        "lisan_oxford" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFF5B8CFF),
                onPrimary = Color(0xFF051A4D),
                primaryContainer = Color(0xFF1A2B52),
                onPrimaryContainer = Color(0xFFDBEAFE),
                secondary = Color(0xFF818CF8),
                tertiary = Color(0xFFFBBF24),
                onTertiary = Color(0xFF332000),
                background = Color(0xFF0B101B),
                surface = Color(0xFF0B101B),
                surfaceContainerLow = Color(0xFF141B2D),
                surfaceContainer = Color(0xFF1B243B),
                surfaceContainerHigh = Color(0xFF232F4C),
                outlineVariant = Color(0xFF263352)
            )
        } else {
            base.copy(
                primary = Color(0xFF1E40AF),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFEBF1FF),
                onPrimaryContainer = Color(0xFF172554),
                secondary = Color(0xFF3B82F6),
                tertiary = Color(0xFFD97706),
                onTertiary = Color(0xFFFFFFFF),
                background = Color(0xFFFAF8F4),
                surface = Color(0xFFFAF8F4),
                surfaceContainerLow = Color(0xFFFFFFFF),
                surfaceContainer = Color(0xFFF2EEE6),
                outlineVariant = Color(0xFFE5DFD3)
            )
        }

        "qabas_amber" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFFFB923C),
                onPrimary = Color(0xFF3A1602),
                primaryContainer = Color(0xFF42220B),
                onPrimaryContainer = Color(0xFFFFEDD5),
                secondary = Color(0xFF2DD4BF),
                tertiary = Color(0xFF2DD4BF),
                onTertiary = Color(0xFF032B27),
                background = Color(0xFF120E0B),
                surface = Color(0xFF120E0B),
                surfaceContainerLow = Color(0xFF1D1713),
                surfaceContainer = Color(0xFF261E19),
                surfaceContainerHigh = Color(0xFF322821),
                outlineVariant = Color(0xFF3B2E26)
            )
        } else {
            base.copy(
                primary = Color(0xFFC2590A),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFF1E4),
                onPrimaryContainer = Color(0xFF5A2404),
                secondary = Color(0xFF0F766E),
                tertiary = Color(0xFF0F766E),
                onTertiary = Color(0xFFFFFFFF),
                background = Color(0xFFFBF8F3),
                surface = Color(0xFFFBF8F3),
                surfaceContainerLow = Color(0xFFFFFFFF),
                surfaceContainer = Color(0xFFF4EDE3),
                outlineVariant = Color(0xFFE8DDD0)
            )
        }

        "blue", "indigo", "purple", "yellow" -> base

        "green" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFF8CD6B1),
                onPrimary = Color(0xFF003824),
                primaryContainer = Color(0xFF005136),
                onPrimaryContainer = Color(0xFFA8F2CC)
            )
        } else {
            base.copy(
                primary = Color(0xFF1D6B4A),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFA8F2CC),
                onPrimaryContainer = Color(0xFF002113)
            )
        }

        "orange" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFFFFB68D),
                onPrimary = Color(0xFF532200),
                primaryContainer = Color(0xFF763300),
                onPrimaryContainer = Color(0xFFFFDBC8)
            )
        } else {
            base.copy(
                primary = Color(0xFF9A4500),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFDBC8),
                onPrimaryContainer = Color(0xFF321200)
            )
        }

        "pink" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFFFFB1C4),
                onPrimary = Color(0xFF650028),
                primaryContainer = Color(0xFF8E0A3D),
                onPrimaryContainer = Color(0xFFFFD9E1)
            )
        } else {
            base.copy(
                primary = Color(0xFFB32656),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFD9E1),
                onPrimaryContainer = Color(0xFF3F0016)
            )
        }

        "red" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFFFFB4AB),
                onPrimary = Color(0xFF690005),
                primaryContainer = Color(0xFF93000A),
                onPrimaryContainer = Color(0xFFFFDAD6)
            )
        } else {
            base.copy(
                primary = Color(0xFFB3261E),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFDAD6),
                onPrimaryContainer = Color(0xFF410002)
            )
        }

        else -> if (darkTheme) SadaTealDarkColorScheme else SadaTealLightColorScheme
    }
}
