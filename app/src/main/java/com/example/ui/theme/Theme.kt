package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val KharchaDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = TextOnGold,
    primaryContainer = GoldDark,
    onPrimaryContainer = GoldLight,
    secondary = AmberVibrant,
    onSecondary = TextOnGold,
    secondaryContainer = SurfaceHighlight,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldCash,
    onTertiary = TextOnGold,
    tertiaryContainer = EmeraldDark,
    onTertiaryContainer = EmeraldCash,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderMedium,
    error = RedExpense,
    onError = TextPrimary
)

val KharchaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = KharchaDarkColorScheme,
        typography = Typography,
        shapes = KharchaShapes,
        content = content
    )
}
