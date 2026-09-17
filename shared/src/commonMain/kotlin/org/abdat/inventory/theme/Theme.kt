package org.abdat.inventory.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariant,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = Outline,
    outlineVariant = OutlineVariant
)

val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainerHighest,
    onSurfaceVariant = DarkOnSurfaceVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

val LocalStockColors = staticCompositionLocalOf { StockCustomColors() }
val LocalStockSpacing = staticCompositionLocalOf { StockSpacing() }

object AppTheme {
    val colors: StockCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStockColors.current

    val spacing: StockSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalStockSpacing.current

    val strings: AppStrings
        @Composable
        @ReadOnlyComposable
        get() = LocalAppStrings.current

    val mode: ThemeMode
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeMode.current
}

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    language: AppLanguage = AppLanguage.FR,
    customColors: StockCustomColors? = null,
    spacing: StockSpacing = StockSpacing(),
    content: @Composable () -> Unit
) {
    val activeColors = customColors ?: if (themeMode == ThemeMode.DARK) {
        StockCustomColors(
            backgroundSlate = DarkBackgroundSlate,
            surfaceWhite = DarkSurfaceWhite,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary
        )
    } else {
        StockCustomColors()
    }

    val activeStrings = if (language == AppLanguage.EN) EnStrings else FrStrings
    val colorScheme = if (themeMode == ThemeMode.DARK) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalStockColors provides activeColors,
        LocalStockSpacing provides spacing,
        LocalAppStrings provides activeStrings,
        LocalThemeMode provides themeMode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
