package helmsman.client.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import helmsman.client.data.model.Job
import helmsman.client.data.model.JobCreateRequest
import helmsman.client.data.remote.HelmsmanApi
import helmsman.client.domain.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JobsViewModel(private val api: HelmsmanApi) : ViewModel() {

    private val _jobs = MutableStateFlow<UiState<List<Job>>>(UiState.Idle)
    val jobs: StateFlow<UiState<List<Job>>> = _jobs.asStateFlow()

    private val _createState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val createState: StateFlow<UiState<Unit>> = _createState.asStateFlow()

    fun loadJobs() {
        viewModelScope.launch {
            _jobs.value = UiState.Loading
            try {
                val response = api.getJobs()
                if (response.isSuccessful) {
                    _jobs.value = UiState.Success(response.body() ?: emptyList())
                } else {
                    _jobs.value = UiState.Error("Failed (${response.code()})")
                }
            } catch (e: Exception) {
                _jobs.value = UiState.Error(e.localizedMessage ?: "Connection failed")
            }
        }
    }

    fun createJob(request: JobCreateRequest) {
        viewModelScope.launch {
            _createState.value = UiState.Loading
            try {
                val response = api.createJob(request)
                if (response.isSuccessful) {
                    _createState.value = UiState.Success(Unit)
                    loadJobs()
                } else {
                    _createState.value = UiState.Error("Failed (${response.code()})")
                }
            } catch (e: Exception) {
                _createState.value = UiState.Error(e.localizedMessage ?: "Failed")
            }
        }
    }

    fun deleteJob(id: String) {
        viewModelScope.launch {
            try {
                val response = api.deleteJob(id)
                if (response.isSuccessful) loadJobs()
            } catch (_: Exception) { }
        }
    }

    fun resetCreateState() { _createState.value = UiState.Idle }

    class Factory(private val api: HelmsmanApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return JobsViewModel(api) as T
        }
    }
}
