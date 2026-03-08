package helmsman.client.ui.pairing

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
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
import helmsman.client.ui.theme.DeathStrandingColors

@Composable
fun PairingScreen(
    api: HelmsmanApi,
    authRepository: AuthRepository,
    onPaired: () -> Unit
) {
    val viewModel: PairingViewModel = viewModel(factory = PairingViewModel.Factory(api, authRepository))
    val pairingState by viewModel.pairingState.collectAsStateWithLifecycle()
    val otpDigits by viewModel.otpDigits.collectAsStateWithLifecycle()

    LaunchedEffect(pairingState) {
        if (pairingState is UiState.Success) onPaired()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DeathStrandingColors.DeepNavy, Color(0xFF030508), DeathStrandingColors.DarkNavy)
                )
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            DeathStrandingColors.Gold.copy(alpha = 0.04f + glow * 0.04f),
                            Color.Transparent
                        ),
                        radius = size.maxDimension * 0.7f,
                        center = Offset(size.width / 2, size.height * 0.35f)
                    ),
                    radius = size.maxDimension * 0.7f,
                    center = Offset(size.width / 2, size.height * 0.35f)
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with glow rings
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                Box(
                    modifier = Modifier.size(120.dp).clip(CircleShape)
                        .background(DeathStrandingColors.Gold.copy(alpha = 0.04f + glow * 0.04f))
                )
                Box(
                    modifier = Modifier.size(88.dp).clip(CircleShape)
                        .background(DeathStrandingColors.Gold.copy(alpha = 0.08f))
                        .border(1.dp, DeathStrandingColors.Gold.copy(alpha = 0.2f + glow * 0.15f), CircleShape)
                )
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape)
                        .background(DeathStrandingColors.Gold.copy(alpha = 0.12f))
                        .border(1.dp, DeathStrandingColors.Gold.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Hub,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = DeathStrandingColors.Gold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "HELMSMAN",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeathStrandingColors.Gold,
                letterSpacing = 10.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Remote Command Center",
                style = MaterialTheme.typography.bodyMedium,
                color = DeathStrandingColors.TextMuted,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(52.dp))

            // Section label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DeathStrandingColors.Border)
                Text(
                    "PAIRING CODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeathStrandingColors.TextMuted,
                    letterSpacing = 3.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = DeathStrandingColors.Border)
            }

            Spacer(modifier = Modifier.height(20.dp))

            OtpInputRow(
                digits = otpDigits,
                onDigitChange = viewModel::updateDigit,
                enabled = pairingState !is UiState.Loading,
                isError = pairingState is UiState.Error
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Enter the 6-digit code printed on daemon startup",
                style = MaterialTheme.typography.labelSmall,
                color = DeathStrandingColors.TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = { viewModel.submitOtp() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = pairingState !is UiState.Loading && otpDigits.all { it.isNotEmpty() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeathStrandingColors.Gold,
                    contentColor = DeathStrandingColors.DeepNavy,
                    disabledContainerColor = DeathStrandingColors.DarkGold.copy(alpha = 0.25f),
                    disabledContentColor = DeathStrandingColors.TextMuted
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                if (pairingState is UiState.Loading) {
                    CircularProgressIndicator(
                        color = DeathStrandingColors.DeepNavy,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Connecting...", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                } else {
                    Text("PAIR DEVICE", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pairingState is UiState.Error) {
                Surface(
                    color = DeathStrandingColors.ErrorRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeathStrandingColors.ErrorRed.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = (pairingState as UiState.Error).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = DeathStrandingColors.ErrorRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OtpInputRow(
    digits: List<String>,
    onDigitChange: (Int, String) -> Unit,
    enabled: Boolean,
    isError: Boolean
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        digits.forEachIndexed { index, digit ->
            OtpDigitBox(
                value = digit,
                onValueChange = { newVal ->
                    if (newVal.length <= 1) {
                        onDigitChange(index, newVal)
                        if (newVal.isNotEmpty() && index < 5) focusRequesters[index + 1].requestFocus()
                    } else if (newVal.length == 6 && newVal.all { it.isDigit() }) {
                        newVal.forEachIndexed { i, c -> onDigitChange(i, c.toString()) }
                        focusManager.clearFocus()
                    }
                },
                focusRequester = focusRequesters[index],
                enabled = enabled,
                isError = isError
            )
        }
    }
    LaunchedEffect(Unit) { focusRequesters[0].requestFocus() }
}

@Composable
private fun OtpDigitBox(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    enabled: Boolean,
    isError: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> DeathStrandingColors.ErrorRed
        value.isNotEmpty() -> DeathStrandingColors.Gold
        isFocused -> DeathStrandingColors.Gold.copy(alpha = 0.6f)
        else -> DeathStrandingColors.Border
    }
    val bgColor = when {
        value.isNotEmpty() -> DeathStrandingColors.Gold.copy(alpha = 0.08f)
        isFocused -> DeathStrandingColors.Gold.copy(alpha = 0.04f)
        else -> DeathStrandingColors.SurfaceCard
    }

    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused },
        enabled = enabled,
        textStyle = TextStyle(
            color = DeathStrandingColors.Gold,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        cursorBrush = SolidColor(DeathStrandingColors.Gold),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (value.isEmpty()) {
                    Text("_", color = DeathStrandingColors.Border, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                inner()
            }
        }
    )
}
