package helmsman.client.data.remote

import helmsman.client.data.local.AppDatabase
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://100.111.67.98:3000/"

    @Volatile
    private var INSTANCE: Pair<HelmsmanApi, AuthInterceptor>? = null

    fun getInstance(db: AppDatabase): Pair<HelmsmanApi, AuthInterceptor> {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: createClient(db).also { INSTANCE = it }
        }
    }

    private fun createClient(db: AppDatabase): Pair<HelmsmanApi, AuthInterceptor> {
        val authInterceptor = AuthInterceptor(db)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val gson = com.google.gson.GsonBuilder()
            .setLenient()
            .create()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val api = retrofit.create(HelmsmanApi::class.java)
        return Pair(api, authInterceptor)
    }

    fun getOkHttpClient(db: AppDatabase): OkHttpClient {
        val authInterceptor = getInstance(db).second
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // No timeout for WebSocket
            .build()
    }
}
