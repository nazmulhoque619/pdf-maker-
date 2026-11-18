package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = PureWhite,
    primaryContainer = IceBlue,
    onPrimaryContainer = DeepNavy,
    secondary = CrimsonRed,
    onSecondary = PureWhite,
    secondaryContainer = SoftRose,
    onSecondaryContainer = DeepCrimson,
    tertiary = DeepNavy,
    onTertiary = PureWhite,
    tertiaryContainer = CoolSlateSurface,
    onTertiaryContainer = DeepNavy,
    background = PorcelainWhite,
    onBackground = InkCharcoal,
    surface = PureWhite,
    onSurface = InkCharcoal,
    surfaceVariant = CoolSlateSurface,
    onSurfaceVariant = SlateMuted,
    outline = SlateMuted,
    outlineVariant = HairlineLight,
    error = CrimsonRed,
    onError = PureWhite,
    errorContainer = SoftRose,
    onErrorContainer = DeepCrimson
)

private val DarkColorScheme = darkColorScheme(
    primary = LuminousBlue,
    onPrimary = MidnightNavy,
    primaryContainer = DarkNavyContainer,
    onPrimaryContainer = IceBlue,
    secondary = CoralRed,
    onSecondary = MidnightNavy,
    secondaryContainer = DarkCrimsonContainer,
    onSecondaryContainer = SoftRose,
    tertiary = Color(0xFF93C5FD),
    onTertiary = MidnightNavy,
    tertiaryContainer = ElevatedNavyCard,
    onTertiaryContainer = CrispOffWhite,
    background = MidnightNavy,
    onBackground = CrispOffWhite,
    surface = DarkSlateNavy,
    onSurface = CrispOffWhite,
    surfaceVariant = ElevatedNavyCard,
    onSurfaceVariant = CoolGrayText,
    outline = CoolGrayText,
    outlineVariant = HairlineDark,
    error = CoralRed,
    onError = MidnightNavy,
    errorContainer = DarkCrimsonContainer,
    onErrorContainer = SoftRose
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmartPdfTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
