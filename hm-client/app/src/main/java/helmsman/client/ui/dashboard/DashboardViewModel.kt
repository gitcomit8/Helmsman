package helmsman.client.ui.dashboard

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

class DashboardViewModel(private val api: HelmsmanApi) : ViewModel() {

    private val _daemonStatus = MutableStateFlow<UiState<String>>(UiState.Idle)
    val daemonStatus: StateFlow<UiState<String>> = _daemonStatus.asStateFlow()

    private val _servers = MutableStateFlow<UiState<List<Server>>>(UiState.Idle)
    val servers: StateFlow<UiState<List<Server>>> = _servers.asStateFlow()

    private val _serverStatuses = MutableStateFlow<Map<String, ServerStatus>>(emptyMap())
    val serverStatuses: StateFlow<Map<String, ServerStatus>> = _serverStatuses.asStateFlow()

    fun checkDaemonStatus() {
        viewModelScope.launch {
            _daemonStatus.value = UiState.Loading
            try {
                val response = api.getStatus()
                if (response.isSuccessful) {
                    val text = response.body()?.string() ?: "OK"
                    _daemonStatus.value = UiState.Success(text)
                } else {
                    _daemonStatus.value = UiState.Error("Daemon returned ${response.code()}")
                }
            } catch (e: Exception) {
                _daemonStatus.value = UiState.Error(e.localizedMessage ?: "Unreachable")
            }
        }
    }

    fun loadServers() {
        viewModelScope.launch {
            _servers.value = UiState.Loading
            try {
                val response = api.getServers()
                if (response.isSuccessful) {
                    val serverList = response.body() ?: emptyList()
                    _servers.value = UiState.Success(serverList)
                    serverList.forEach { probeServer(it.id) }
                } else {
                    _servers.value = UiState.Error("Failed to load servers (${response.code()})")
                }
            } catch (e: Exception) {
                _servers.value = UiState.Error(e.localizedMessage ?: "Connection failed")
            }
        }
    }

    private fun probeServer(serverId: String) {
        viewModelScope.launch {
            try {
                val response = api.getServerStatus(serverId)
                if (response.isSuccessful) {
                    response.body()?.let { status ->
                        _serverStatuses.value = _serverStatuses.value + (serverId to status)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun refresh() {
        checkDaemonStatus()
        loadServers()
    }

    class Factory(private val api: HelmsmanApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(api) as T
        }
    }
}
