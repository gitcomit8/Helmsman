package helmsman.client.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import helmsman.client.data.model.PairRequest
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.AuthRepository
import helmsman.client.domain.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PairingViewModel(
    private val api: HelmsmanApi,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _pairingState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val pairingState: StateFlow<UiState<String>> = _pairingState.asStateFlow()

    private val _otpDigits = MutableStateFlow(List(6) { "" })
    val otpDigits: StateFlow<List<String>> = _otpDigits.asStateFlow()

    fun updateDigit(index: Int, value: String) {
        if (index !in 0..5) return
        val filtered = value.filter { it.isDigit() }.take(1)
        _otpDigits.value = _otpDigits.value.toMutableList().apply { set(index, filtered) }
    }

    fun submitOtp() {
        val code = _otpDigits.value.joinToString("")
        if (code.length != 6) {
            _pairingState.value = UiState.Error("Please enter all 6 digits")
            return
        }

        viewModelScope.launch {
            _pairingState.value = UiState.Loading
            try {
                val response = api.pair(PairRequest(code))
                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (token != null) {
                        authRepository.saveToken(token)
                        _pairingState.value = UiState.Success(token)
                    } else {
                        _pairingState.value = UiState.Error("Empty response from daemon")
                    }
                } else {
                    val msg = when (response.code()) {
                        403 -> "Invalid or already used pairing code"
                        else -> "Pairing failed (${response.code()})"
                    }
                    _pairingState.value = UiState.Error(msg)
                }
            } catch (e: Exception) {
                _pairingState.value = UiState.Error(
                    e.localizedMessage ?: "Connection failed"
                )
            }
        }
    }

    fun resetState() {
        _pairingState.value = UiState.Idle
    }

    class Factory(
        private val api: HelmsmanApi,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PairingViewModel(api, authRepository) as T
        }
    }
}
