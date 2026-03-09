package helmsman.client.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Status dot with subtle pulse ─────────────────────────────────────────────

@Composable
fun StatusDot(isOnline: Boolean, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    val color = if (isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val inf = rememberInfiniteTransition(label = "dot")
    val alpha by inf.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(modifier = modifier.size(size).clip(CircleShape).background(color.copy(alpha = alpha)))
}

// ── Generic card shell ────────────────────────────────────────────────────────

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

// ── Primary button ────────────────────────────────────────────────────────────

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
        )
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// ── Ghost / outlined button ───────────────────────────────────────────────────

@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = tint)
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        action?.invoke()
    }
}

// ── Inline tag / badge ────────────────────────────────────────────────────────

@Composable
fun Tag(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp
        )
    }
}

// ── Shared form text field ────────────────────────────────────────────────────

@Composable
fun HmTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = singleLine,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            disabledBorderColor  = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor    = MaterialTheme.colorScheme.primary,
            cursorColor          = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun HmErrorText(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth()
    )
}

// ── Dismissible dialog shell ──────────────────────────────────────────────────

@Composable
fun HmDialog(
    title: String,
    confirmLabel: String,
    confirmEnabled: Boolean = true,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        title = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) },
        confirmButton = {
            PrimaryButton(
                text = if (isLoading) "Saving…" else confirmLabel,
                enabled = confirmEnabled && !isLoading,
                onClick = onConfirm
            )
        },
        dismissButton = {
            TextButton(onDismiss) { Text("Cancel") }
        }
    )
}
