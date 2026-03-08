package helmsman.client.data.model

import com.google.gson.annotations.SerializedName

data class RunResult(
    @SerializedName("exit_code") val exitCode: Int?,
    val stdout: String,
    val stderr: String,
    @SerializedName("duration_ms") val durationMs: Long
)
