package helmsman.client.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import helmsman.client.data.model.Server
import helmsman.client.data.model.ServerStatus
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServersViewModel(private val api: HelmsmanApi) : ViewModel() {

    private val _servers = MutableStateFlow<UiState<List<Server>>>(UiState.Idle)
    val servers: StateFlow<UiState<List<Server>>> = _servers.asStateFlow()

    private val _createState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val createState: StateFlow<UiState<Unit>> = _createState.asStateFlow()

    private val _serverStatuses = MutableStateFlow<Map<String, UiState<ServerStatus>>>(emptyMap())
    val serverStatuses: StateFlow<Map<String, UiState<ServerStatus>>> = _serverStatuses.asStateFlow()

    fun loadServers() {
        viewModelScope.launch {
            _servers.value = UiState.Loading
            try {
                val response = api.getServers()
                if (response.isSuccessful) {
                    _servers.value = UiState.Success(response.body() ?: emptyList())
                } else {
                    _servers.value = UiState.Error("Failed (${response.code()})")
                }
            } catch (e: Exception) {
                _servers.value = UiState.Error(e.localizedMessage ?: "Connection failed")
            }
        }
    }

    fun createServer(server: Server) {
        viewModelScope.launch {
            _createState.value = UiState.Loading
            try {
                val response = api.createServer(server)
                if (response.isSuccessful) {
                    _createState.value = UiState.Success(Unit)
                    loadServers()
                } else {
                    _createState.value = UiState.Error("Failed (${response.code()})")
                }
            } catch (e: Exception) {
                _createState.value = UiState.Error(e.localizedMessage ?: "Failed")
            }
        }
    }

    fun deleteServer(id: String) {
        viewModelScope.launch {
            try {
                val response = api.deleteServer(id)
                if (response.isSuccessful) loadServers()
            } catch (_: Exception) { }
        }
    }

    fun probeServer(id: String) {
        viewModelScope.launch {
            _serverStatuses.value = _serverStatuses.value + (id to UiState.Loading)
            try {
                val response = api.getServerStatus(id)
                if (response.isSuccessful) {
                    _serverStatuses.value = _serverStatuses.value +
                            (id to UiState.Success(response.body()!!))
                } else {
                    _serverStatuses.value = _serverStatuses.value +
                            (id to UiState.Error("Probe failed (${response.code()})"))
                }
            } catch (e: Exception) {
                _serverStatuses.value = _serverStatuses.value +
                        (id to UiState.Error(e.localizedMessage ?: "Probe failed"))
            }
        }
    }

    fun resetCreateState() { _createState.value = UiState.Idle }

    class Factory(private val api: HelmsmanApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ServersViewModel(api) as T
        }
    }
}
