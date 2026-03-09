package helmsman.client.ui.pairing

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helmsman.client.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.AuthRepository
import helmsman.client.domain.UiState
import helmsman.client.ui.components.PrimaryButton

@Composable
fun PairingScreen(api: HelmsmanApi, authRepository: AuthRepository, onPaired: () -> Unit) {
    val vm: PairingViewModel = viewModel(factory = PairingViewModel.Factory(api, authRepository))
    val state by vm.pairingState.collectAsStateWithLifecycle()
    val digits by vm.otpDigits.collectAsStateWithLifecycle()

    LaunchedEffect(state) { if (state is UiState.Success) onPaired() }

    val isError   = state is UiState.Error
    val isLoading = state is UiState.Loading

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Logo
            Image(
                painter = painterResource(R.drawable.ic_helmsman),
                contentDescription = "Helmsman",
                modifier = Modifier.size(96.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text("Helmsman", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Enter the 6-digit pairing code\nfrom your daemon startup log",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            OtpRow(
                digits   = digits,
                onChange = vm::updateDigit,
                enabled  = !isLoading,
                isError  = isError
            )

            Spacer(Modifier.height(8.dp))

            if (isError) {
                Text(
                    (state as UiState.Error).message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(28.dp))

            PrimaryButton(
                text    = if (isLoading) "Connecting…" else "Pair device",
                enabled = !isLoading && digits.all { it.isNotEmpty() },
                modifier = Modifier.fillMaxWidth(),
                onClick = vm::submitOtp
            )
        }
    }
}

@Composable
private fun OtpRow(digits: List<String>, onChange: (Int, String) -> Unit, enabled: Boolean, isError: Boolean) {
    val focusers = remember { List(6) { FocusRequester() } }
    val fm = LocalFocusManager.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        digits.forEachIndexed { i, d ->
            OtpBox(
                value = d,
                focusRequester = focusers[i],
                isError = isError,
                enabled = enabled,
                onValueChange = { v ->
                    if (v.length <= 1) {
                        onChange(i, v)
                        if (v.isNotEmpty() && i < 5) focusers[i + 1].requestFocus()
                    } else if (v.length == 6 && v.all { it.isDigit() }) {
                        v.forEachIndexed { j, c -> onChange(j, c.toString()) }
                        fm.clearFocus()
                    }
                }
            )
        }
    }
    LaunchedEffect(Unit) { focusers[0].requestFocus() }
}

@Composable
private fun OtpBox(
    value: String,
    focusRequester: FocusRequester,
    isError: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        value.isNotEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outline
    }
    val bgColor = when {
        focused || value.isNotEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .size(48.dp)
            .background(bgColor, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text("·", fontSize = 20.sp, color = MaterialTheme.colorScheme.outlineVariant, fontWeight = FontWeight.Bold)
                inner()
            }
        }
    )
}
