package helmsman.client.ui.pairing

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.AuthRepository
import helmsman.client.domain.UiState
import helmsman.client.ui.components.GoldButton
import helmsman.client.ui.theme.DeathStrandingColors

@Composable
fun PairingScreen(
    api: HelmsmanApi,
    authRepository: AuthRepository,
    onPaired: () -> Unit
) {
    val viewModel: PairingViewModel = viewModel(
        factory = PairingViewModel.Factory(api, authRepository)
    )

    val pairingState by viewModel.pairingState.collectAsStateWithLifecycle()
    val otpDigits by viewModel.otpDigits.collectAsStateWithLifecycle()

    LaunchedEffect(pairingState) {
        if (pairingState is UiState.Success) {
            onPaired()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeathStrandingColors.DeepNavy,
                        Color(0xFF080C1A),
                        DeathStrandingColors.DeepNavy
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated helm icon
            val infiniteTransition = rememberInfiniteTransition(label = "glow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "icon_glow"
            )

            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = "Helmsman",
                modifier = Modifier.size(80.dp),
                tint = DeathStrandingColors.Gold.copy(alpha = glowAlpha)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "HELMSMAN",
                style = MaterialTheme.typography.headlineLarge,
                color = DeathStrandingColors.Gold,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connect to your daemon",
                style = MaterialTheme.typography.bodyLarge,
                color = DeathStrandingColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "PAIRING CODE",
                style = MaterialTheme.typography.labelMedium,
                color = DeathStrandingColors.Gold.copy(alpha = 0.7f),
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            OtpInputRow(
                digits = otpDigits,
                onDigitChange = viewModel::updateDigit,
                enabled = pairingState !is UiState.Loading
            )

            Spacer(modifier = Modifier.height(32.dp))

            GoldButton(
                text = if (pairingState is UiState.Loading) "CONNECTING..." else "PAIR DEVICE",
                onClick = { viewModel.submitOtp() },
                enabled = pairingState !is UiState.Loading && otpDigits.all { it.isNotEmpty() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (pairingState is UiState.Error) {
                Text(
                    text = (pairingState as UiState.Error).message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeathStrandingColors.ErrorRed,
                    textAlign = TextAlign.Center
                )
            }

            if (pairingState is UiState.Loading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    color = DeathStrandingColors.Gold,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun OtpInputRow(
    digits: List<String>,
    onDigitChange: (Int, String) -> Unit,
    enabled: Boolean
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        digits.forEachIndexed { index, digit ->
            OtpDigitBox(
                value = digit,
                onValueChange = { newValue ->
                    if (newValue.length <= 1) {
                        onDigitChange(index, newValue)
                        if (newValue.isNotEmpty() && index < 5) {
                            focusRequesters[index + 1].requestFocus()
                        }
                    } else if (newValue.length == 6) {
                        // Handle paste of full code
                        newValue.forEachIndexed { i, c ->
                            onDigitChange(i, c.toString())
                        }
                        focusManager.clearFocus()
                    }
                },
                focusRequester = focusRequesters[index],
                enabled = enabled
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}

@Composable
private fun OtpDigitBox(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    enabled: Boolean
) {
    val borderColor = if (value.isNotEmpty()) {
        DeathStrandingColors.Gold
    } else {
        DeathStrandingColors.Border
    }

    BasicTextField(
        value = value,
        onValueChange = { newVal ->
            val filtered = newVal.filter { it.isDigit() }
            onValueChange(filtered)
        },
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DeathStrandingColors.SurfaceCard)
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .focusRequester(focusRequester),
        enabled = enabled,
        textStyle = TextStyle(
            color = DeathStrandingColors.Gold,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        cursorBrush = SolidColor(DeathStrandingColors.Gold),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "•",
                        color = DeathStrandingColors.TextMuted,
                        fontSize = 24.sp
                    )
                }
                innerTextField()
            }
        }
    )
}
