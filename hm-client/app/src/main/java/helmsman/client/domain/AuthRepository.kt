package helmsman.client.domain

import helmsman.client.data.local.AppDatabase
import helmsman.client.data.local.TokenEntity
import helmsman.client.data.remote.AuthInterceptor

class AuthRepository(
    private val db: AppDatabase,
    private val authInterceptor: AuthInterceptor
) {
    @Volatile
    private var cachedToken: String? = null

    suspend fun getToken(): String? {
        cachedToken?.let { return it }
        return db.tokenDao().getToken()?.token?.also {
            cachedToken = it
            authInterceptor.updateCache(it)
        }
    }

    suspend fun saveToken(token: String) {
        db.tokenDao().insertToken(TokenEntity(token = token))
        cachedToken = token
        authInterceptor.updateCache(token)
    }

    suspend fun clearToken() {
        db.tokenDao().clearToken()
        cachedToken = null
        authInterceptor.invalidateCache()
    }

    fun isAuthenticated(): Boolean = cachedToken != null
}
