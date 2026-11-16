package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val MaquisDarkColorScheme = darkColorScheme(
    primary = MaquisEmerald,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = MaquisEmeraldDark,
    onPrimaryContainer = MaquisEmeraldGlow,
    secondary = MaquisAmber,
    onSecondary = Color(0xFF1E1B18),
    secondaryContainer = MaquisAmberDark,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = MaquisSky,
    onTertiary = MaquisBg,
    error = MaquisRose,
    onError = Color.White,
    errorContainer = MaquisRoseDark,
    onErrorContainer = Color(0xFFFCA5A5),
    background = MaquisBg,
    onBackground = MaquisTextPrimary,
    surface = MaquisSurface,
    onSurface = MaquisTextPrimary,
    surfaceVariant = MaquisSurfaceAlt,
    onSurfaceVariant = MaquisTextSecondary,
    outline = MaquisBorder
)

private val MaquisShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MaquisProTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = MaquisDarkColorScheme,
        typography = MaquisTypography,
        shapes = MaquisShapes,
        content = content
    )
}
