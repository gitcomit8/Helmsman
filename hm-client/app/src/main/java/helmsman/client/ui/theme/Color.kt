package helmsman.client.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Extra semantic colors not in Material ColorScheme */
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val info: Color,
    val codeBlock: Color,
    val onCode: Color,
)

val AmoledExtended = ExtendedColors(
    success   = Color(0xFF34D399),
    onSuccess = Color(0xFF000000),
    warning   = Color(0xFFFCD34D),
    info      = Color(0xFF7DD3FC),
    codeBlock = Color(0xFF111111),
    onCode    = Color(0xFF6EE7B7),
)

val LocalExtendedColors = staticCompositionLocalOf { AmoledExtended }
