package helmsman.client.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AmoledDark = darkColorScheme(
    primary               = Color(0xFF22D3EE),   // cyan-400
    onPrimary             = Color(0xFF000000),
    primaryContainer      = Color(0xFF083344),   // cyan-950
    onPrimaryContainer    = Color(0xFFCFFAFE),   // cyan-100
    secondary             = Color(0xFF818CF8),   // indigo-400
    onSecondary           = Color(0xFF000000),
    tertiary              = Color(0xFF34D399),   // emerald-400
    onTertiary            = Color(0xFF000000),
    background            = Color(0xFF000000),
    onBackground          = Color(0xFFFFFFFF),
    surface               = Color(0xFF0C0C0C),
    onSurface             = Color(0xFFFFFFFF),
    surfaceVariant        = Color(0xFF1A1A1A),
    onSurfaceVariant      = Color(0xFF737373),
    surfaceContainerLowest  = Color(0xFF000000),
    surfaceContainerLow     = Color(0xFF0C0C0C),
    surfaceContainer        = Color(0xFF141414),
    surfaceContainerHigh    = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF262626),
    outline               = Color(0xFF262626),
    outlineVariant        = Color(0xFF1A1A1A),
    error                 = Color(0xFFFC8181),
    onError               = Color(0xFF000000),
    errorContainer        = Color(0xFF450A0A),
    onErrorContainer      = Color(0xFFFECACA),
    inverseSurface        = Color(0xFFFFFFFF),
    inverseOnSurface      = Color(0xFF000000),
    inversePrimary        = Color(0xFF0891B2),
)

@Composable
fun HelmsmanTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = AmoledDark.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = AmoledDark.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars    = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides AmoledExtended) {
        MaterialTheme(
            colorScheme = AmoledDark,
            typography  = HelmsmanTypography,
            content     = content
        )
    }
}
