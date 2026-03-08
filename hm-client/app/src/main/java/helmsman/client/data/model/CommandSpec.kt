package helmsman.client.data.model

import com.google.gson.annotations.SerializedName

data class CommandSpec(
    val id: String,
    val name: String,
    val command: List<String>,
    @SerializedName("working_dir") val workingDir: String? = null,
    @SerializedName("timeout_seconds") val timeoutSeconds: Int? = null
)
