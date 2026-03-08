package helmsman.client.data.remote

import helmsman.client.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface HelmsmanApi {

    // Health
    @GET("status")
    suspend fun getStatus(): Response<String>

    // Pairing
    @POST("pair")
    suspend fun pair(@Body request: PairRequest): Response<PairResponse>

    // Commands
    @GET("commands")
    suspend fun getCommands(): Response<List<CommandSpec>>

    @POST("commands")
    suspend fun createCommand(@Body command: CommandSpec): Response<Unit>

    @DELETE("commands/{id}")
    suspend fun deleteCommand(@Path("id") id: String): Response<Unit>

    // Command execution
    @POST("commands/{id}/run")
    suspend fun runCommand(@Path("id") id: String): Response<RunResult>

    // Jobs
    @GET("jobs")
    suspend fun getJobs(): Response<List<Job>>

    @POST("jobs")
    suspend fun createJob(@Body job: JobCreateRequest): Response<Unit>

    @DELETE("jobs/{id}")
    suspend fun deleteJob(@Path("id") id: String): Response<Unit>

    // Servers
    @GET("servers")
    suspend fun getServers(): Response<List<Server>>

    @POST("servers")
    suspend fun createServer(@Body server: Server): Response<Unit>

    @DELETE("servers/{id}")
    suspend fun deleteServer(@Path("id") id: String): Response<Unit>

    @GET("servers/{id}/status")
    suspend fun getServerStatus(@Path("id") id: String): Response<ServerStatus>
}
