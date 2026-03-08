package helmsman.client.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import helmsman.client.ui.theme.DeathStrandingColors

@Composable
fun GoldGradientCard(
    modifier: Modifier = Modifier,
    glowColor: Color = DeathStrandingColors.Gold,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.maxDimension * 0.8f
                    )
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    0f to glowColor.copy(alpha = 0.5f),
                    0.5f to glowColor.copy(alpha = 0.15f),
                    1f to glowColor.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeathStrandingColors.SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun StatusDot(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    val color = if (isOnline) DeathStrandingColors.SuccessGreen else DeathStrandingColors.ErrorRed
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(size * 2.2f)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f * scale))
                .align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun GoldButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeathStrandingColors.Gold,
            contentColor = DeathStrandingColors.DeepNavy,
            disabledContainerColor = DeathStrandingColors.DarkGold.copy(alpha = 0.3f),
            disabledContentColor = DeathStrandingColors.TextMuted
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.Bold, letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp))
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DeathStrandingColors.Gold)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = DeathStrandingColors.Gold,
                fontWeight = FontWeight.Bold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }
        action?.invoke()
    }
}

@Composable
fun EmptyStateMessage(icon: ImageVector, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(DeathStrandingColors.Gold.copy(alpha = 0.06f))
                .border(1.dp, DeathStrandingColors.Gold.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = DeathStrandingColors.Gold.copy(alpha = 0.4f))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = DeathStrandingColors.TextMuted)
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DeathStrandingColors.Gold, strokeWidth = 2.dp, modifier = Modifier.size(36.dp))
    }
}

@Composable
fun GoldChip(text: String, icon: ImageVector? = null, color: Color = DeathStrandingColors.Gold) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun HmTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DeathStrandingColors.Gold,
            unfocusedBorderColor = DeathStrandingColors.Border,
            cursorColor = DeathStrandingColors.Gold,
            focusedLabelColor = DeathStrandingColors.Gold,
            unfocusedLabelColor = DeathStrandingColors.TextMuted,
            focusedTextColor = DeathStrandingColors.TextPrimary,
            unfocusedTextColor = DeathStrandingColors.TextPrimary,
            disabledBorderColor = DeathStrandingColors.Border.copy(alpha = 0.5f),
            disabledTextColor = DeathStrandingColors.TextMuted,
            disabledLabelColor = DeathStrandingColors.TextMuted
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun HmErrorText(message: String) {
    Surface(color = DeathStrandingColors.ErrorRed.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp)) {
        Text(
            message,
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = DeathStrandingColors.ErrorRed
        )
    }
}
