package helmsman.client.data.remote

import helmsman.client.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val db: AppDatabase) : Interceptor {
    @Volatile
    private var cachedToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Skip auth for pairing and status endpoints
        val path = request.url.encodedPath
        if (path == "/pair" || path == "/status") {
            return chain.proceed(request)
        }

        val token = cachedToken ?: runBlocking {
            db.tokenDao().getToken()?.token?.also { cachedToken = it }
        }

        val authenticatedRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(authenticatedRequest)
    }

    fun invalidateCache() {
        cachedToken = null
    }

    fun updateCache(token: String) {
        cachedToken = token
    }
}
