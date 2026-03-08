package helmsman.client.data.model

import com.google.gson.annotations.SerializedName

data class ServerStatus(
    val host: String,
    @SerializedName("ping_ms") val pingMs: Double?,
    @SerializedName("ping_error") val pingError: String?,
    val uname: String
)
