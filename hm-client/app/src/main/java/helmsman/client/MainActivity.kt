package helmsman.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import helmsman.client.data.local.AppDatabase
import helmsman.client.data.remote.ApiClient
import helmsman.client.domain.AuthRepository
import helmsman.client.ui.navigation.HelmsmanNavGraph
import helmsman.client.ui.theme.HelmsmanTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val (api, authInterceptor) = ApiClient.getInstance(db)
        val authRepository = AuthRepository(db, authInterceptor)
        val isAuthenticated = runBlocking { authRepository.getToken() != null }

        setContent {
            HelmsmanTheme {
                val navController = rememberNavController()
                HelmsmanNavGraph(
                    navController   = navController,
                    isAuthenticated = isAuthenticated,
                    api             = api,
                    authRepository  = authRepository,
                    db              = db
                )
            }
        }
    }
}
