package helmsman.client.ui.commands

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import helmsman.client.data.model.CommandSpec
import helmsman.client.data.model.RunResult
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommandsViewModel(private val api: HelmsmanApi) : ViewModel() {

    private val _commands = MutableStateFlow<UiState<List<CommandSpec>>>(UiState.Idle)
    val commands: StateFlow<UiState<List<CommandSpec>>> = _commands.asStateFlow()

    private val _createState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val createState: StateFlow<UiState<Unit>> = _createState.asStateFlow()

    private val _runResult = MutableStateFlow<UiState<RunResult>>(UiState.Idle)
    val runResult: StateFlow<UiState<RunResult>> = _runResult.asStateFlow()

    fun loadCommands() {
        viewModelScope.launch {
            _commands.value = UiState.Loading
            try {
                val response = api.getCommands()
                if (response.isSuccessful) {
                    _commands.value = UiState.Success(response.body() ?: emptyList())
                } else {
                    _commands.value = UiState.Error("Failed (${response.code()})")
                }
            } catch (e: Exception) {
                _commands.value = UiState.Error(e.localizedMessage ?: "Connection failed")
            }
        }
    }

    fun createCommand(spec: CommandSpec) {
        viewModelScope.launch {
            _createState.value = UiState.Loading
            try {
                val response = api.createCommand(spec)
                if (response.isSuccessful) {
                    _createState.value = UiState.Success(Unit)
                    loadCommands()
                } else {
                    _createState.value = UiState.Error("Failed (${response.code()})")
                }
            } catch (e: Exception) {
                _createState.value = UiState.Error(e.localizedMessage ?: "Failed")
            }
        }
    }

    fun deleteCommand(id: String) {
        viewModelScope.launch {
            try {
                val response = api.deleteCommand(id)
                if (response.isSuccessful) loadCommands()
            } catch (_: Exception) { }
        }
    }

    fun runCommand(id: String) {
        viewModelScope.launch {
            _runResult.value = UiState.Loading
            try {
                val response = api.runCommand(id)
                if (response.isSuccessful) {
                    _runResult.value = UiState.Success(response.body()!!)
                } else {
                    _runResult.value = UiState.Error("Execution failed (${response.code()})")
                }
            } catch (e: Exception) {
                _runResult.value = UiState.Error(e.localizedMessage ?: "Failed")
            }
        }
    }

    fun resetCreateState() { _createState.value = UiState.Idle }
    fun resetRunResult() { _runResult.value = UiState.Idle }

    class Factory(private val api: HelmsmanApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommandsViewModel(api) as T
        }
    }
}
