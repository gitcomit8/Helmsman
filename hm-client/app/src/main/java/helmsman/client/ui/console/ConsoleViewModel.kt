package helmsman.client.ui.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import helmsman.client.data.local.AppDatabase
import helmsman.client.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

data class ConsoleLine(
    val stream: String, // "stdout", "stderr", "exit", "error", "system"
    val text: String
)

class ConsoleViewModel(private val db: AppDatabase) : ViewModel() {

    private val _lines = MutableStateFlow<List<ConsoleLine>>(emptyList())
    val lines: StateFlow<List<ConsoleLine>> = _lines.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _exitCode = MutableStateFlow<Int?>(null)
    val exitCode: StateFlow<Int?> = _exitCode.asStateFlow()

    private var webSocket: WebSocket? = null

    fun startStream(commandId: String) {
        _lines.value = emptyList()
        _isRunning.value = true
        _exitCode.value = null

        val client = ApiClient.getOkHttpClient(db)

        val (_, authInterceptor) = ApiClient.getInstance(db)
        val tokenValue = let {
            val field = authInterceptor.javaClass.getDeclaredField("cachedToken")
            field.isAccessible = true
            field.get(authInterceptor) as? String ?: ""
        }

        val request = Request.Builder()
            .url("ws://100.111.67.98:3000/commands/$commandId/stream")
            .header("Authorization", "Bearer $tokenValue")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                addLine(ConsoleLine("system", "Connected — streaming output..."))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val line = when {
                    text.startsWith("stdout: ") -> ConsoleLine("stdout", text.removePrefix("stdout: "))
                    text.startsWith("stderr: ") -> ConsoleLine("stderr", text.removePrefix("stderr: "))
                    text.startsWith("exit: ") -> {
                        val code = text.removePrefix("exit: ").trim().toIntOrNull() ?: -1
                        _exitCode.value = code
                        ConsoleLine("exit", "Process exited with code $code")
                    }
                    text.startsWith("error: ") -> ConsoleLine("error", text.removePrefix("error: "))
                    else -> ConsoleLine("stdout", text)
                }
                addLine(line)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _isRunning.value = false
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                addLine(ConsoleLine("error", t.localizedMessage ?: "WebSocket error"))
                _isRunning.value = false
            }
        })
    }

    private fun addLine(line: ConsoleLine) {
        _lines.value = _lines.value + line
    }

    fun disconnect() {
        webSocket?.cancel()
        webSocket = null
        _isRunning.value = false
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }

    class Factory(private val db: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConsoleViewModel(db) as T
        }
    }
}
