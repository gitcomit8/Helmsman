package helmsman.client.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DeathStrandingDarkScheme = darkColorScheme(
    primary = DeathStrandingColors.Gold,
    onPrimary = DeathStrandingColors.DeepNavy,
    primaryContainer = DeathStrandingColors.DarkGold,
    onPrimaryContainer = DeathStrandingColors.PaleGold,

    secondary = DeathStrandingColors.Amber,
    onSecondary = DeathStrandingColors.DeepNavy,
    secondaryContainer = DeathStrandingColors.BurntGold,
    onSecondaryContainer = DeathStrandingColors.PaleGold,

    tertiary = DeathStrandingColors.InfoCyan,
    onTertiary = DeathStrandingColors.DeepNavy,

    background = DeathStrandingColors.DeepNavy,
    onBackground = DeathStrandingColors.TextPrimary,

    surface = DeathStrandingColors.DarkNavy,
    onSurface = DeathStrandingColors.TextPrimary,
    surfaceVariant = DeathStrandingColors.MidNavy,
    onSurfaceVariant = DeathStrandingColors.TextSecondary,

    surfaceContainerLowest = DeathStrandingColors.DeepNavy,
    surfaceContainerLow = DeathStrandingColors.SurfaceCard,
    surfaceContainer = DeathStrandingColors.SurfaceElevated,
    surfaceContainerHigh = DeathStrandingColors.Navy,
    surfaceContainerHighest = DeathStrandingColors.LightNavy,

    outline = DeathStrandingColors.Border,
    outlineVariant = DeathStrandingColors.Border.copy(alpha = 0.5f),

    error = DeathStrandingColors.ErrorRed,
    onError = Color.White,

    inverseSurface = DeathStrandingColors.TextPrimary,
    inverseOnSurface = DeathStrandingColors.DeepNavy,
    inversePrimary = DeathStrandingColors.DarkGold
)

@Composable
fun HelmsmanTheme(
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        else -> DeathStrandingDarkScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HelmsmanTypography,
        content = content
    )
}
