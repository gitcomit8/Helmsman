package helmsman.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.rememberNavController
import helmsman.client.data.local.AppDatabase
import helmsman.client.data.remote.ApiClient
import helmsman.client.domain.AuthRepository
import helmsman.client.ui.navigation.HelmsmanNavGraph
import helmsman.client.ui.theme.AppThemeMode
import helmsman.client.ui.theme.HelmsmanTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val android.content.Context.dataStore by preferencesDataStore(name = "settings")
private val THEME_MODE_KEY = intPreferencesKey("theme_mode")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val (api, authInterceptor) = ApiClient.getInstance(db)
        val authRepository = AuthRepository(db, authInterceptor)
        val isAuthenticated = runBlocking { authRepository.getToken() != null }

        setContent {
            val scope = rememberCoroutineScope()
            val themeModeIndex by dataStore.data
                .map { it[THEME_MODE_KEY] ?: 0 }
                .collectAsState(initial = 0)

            val themeMode = if (themeModeIndex == 1) AppThemeMode.AMOLED else AppThemeMode.ZINC

            val cycleTheme: () -> Unit = {
                scope.launch {
                    dataStore.edit { prefs ->
                        prefs[THEME_MODE_KEY] = ((prefs[THEME_MODE_KEY] ?: 0) + 1) % 2
                    }
                }
            }

            HelmsmanTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                HelmsmanNavGraph(
                    navController     = navController,
                    isAuthenticated   = isAuthenticated,
                    api               = api,
                    authRepository    = authRepository,
                    db                = db,
                    themeMode         = themeMode,
                    onCycleTheme      = cycleTheme
                )
            }
        }
    }
}
