package helmsman.client.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode { MATERIAL_YOU, AMOLED }

/** Extra semantic colors not in Material ColorScheme */
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val info: Color,
    val codeBlock: Color,
    val onCode: Color,
)

// Used by Material You mode and as fallback on API < 31
val MaterialYouExtended = ExtendedColors(
    success   = Color(0xFF4ADE80),
    onSuccess = Color(0xFF052E16),
    warning   = Color(0xFFFBBF24),
    info      = Color(0xFF60A5FA),
    codeBlock = Color(0xFF1E1E22),
    onCode    = Color(0xFF86EFAC),
)

val AmoledExtended = ExtendedColors(
    success   = Color(0xFF34D399),
    onSuccess = Color(0xFF000000),
    warning   = Color(0xFFFCD34D),
    info      = Color(0xFF7DD3FC),
    codeBlock = Color(0xFF111111),
    onCode    = Color(0xFF6EE7B7),
)

val LocalExtendedColors = staticCompositionLocalOf { MaterialYouExtended }
