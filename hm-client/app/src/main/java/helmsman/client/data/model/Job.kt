package helmsman.client.data.model

import com.google.gson.annotations.SerializedName

data class Job(
    val id: String,
    val name: String,
    val schedule: String,
    @SerializedName("command_id") val commandId: String,
    @SerializedName("last_run") val lastRun: String? = null,
    @SerializedName("next_run") val nextRun: String
)

data class JobCreateRequest(
    val id: String,
    val name: String,
    val schedule: String,
    @SerializedName("command_id") val commandId: String
)
