package helmsman.client.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Fallback for devices running API 30 (minSdk) that lack dynamic color
private val MaterialYouFallback = darkColorScheme(
    primary               = Color(0xFF8B5CF6),   // violet-500 — matches logo
    onPrimary             = Color(0xFFFFFFFF),
    primaryContainer      = Color(0xFF3B0764),
    onPrimaryContainer    = Color(0xFFEDE9FE),
    secondary             = Color(0xFF60A5FA),
    onSecondary           = Color(0xFF000000),
    tertiary              = Color(0xFF34D399),
    onTertiary            = Color(0xFF000000),
    background            = Color(0xFF09090B),
    onBackground          = Color(0xFFF4F4F5),
    surface               = Color(0xFF18181B),
    onSurface             = Color(0xFFF4F4F5),
    surfaceVariant        = Color(0xFF27272A),
    onSurfaceVariant      = Color(0xFFA1A1AA),
    surfaceContainerLowest  = Color(0xFF09090B),
    surfaceContainerLow     = Color(0xFF18181B),
    surfaceContainer        = Color(0xFF1E1E22),
    surfaceContainerHigh    = Color(0xFF27272A),
    surfaceContainerHighest = Color(0xFF3F3F46),
    outline               = Color(0xFF3F3F46),
    outlineVariant        = Color(0xFF27272A),
    error                 = Color(0xFFF87171),
    onError               = Color(0xFFFFFFFF),
    errorContainer        = Color(0xFF7F1D1D),
    onErrorContainer      = Color(0xFFFECACA),
    inverseSurface        = Color(0xFFF4F4F5),
    inverseOnSurface      = Color(0xFF09090B),
    inversePrimary        = Color(0xFF6D28D9),
)

// ── AMOLED ───────────────────────────────────────────────────────────────────
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
    onSurfaceVariant      = Color(0xFF737373),   // neutral-500
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
fun HelmsmanTheme(
    themeMode: AppThemeMode = AppThemeMode.MATERIAL_YOU,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when (themeMode) {
        AppThemeMode.AMOLED       -> AmoledDark
        AppThemeMode.MATERIAL_YOU ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context)
            else MaterialYouFallback
    }
    val extended = if (themeMode == AppThemeMode.AMOLED) AmoledExtended else MaterialYouExtended

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

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = HelmsmanTypography,
            content     = content
        )
    }
}
