package com.chisara.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.chisara.app.ui.nav.ChiSaraNavHost
import com.chisara.app.ui.nav.Routes
import com.chisara.app.ui.theme.ChiSaraTheme
import com.chisara.app.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    // Event id delivered by tapping a blind notification; consumed once on start.
    private var pendingGuessEventId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingGuessEventId = extractEventId(intent)

        setContent {
            ChiSaraTheme {
                val vm: GameViewModel = viewModel()
                val navController = rememberNavController()
                val permissions by vm.permissions.collectAsStateWithLifecycle()

                // Re-check special-access permissions every time we resume from Settings.
                LaunchedEffect(Unit) { vm.refreshPermissions() }

                val startDestination = remember(permissions.allGranted) {
                    if (permissions.allGranted) Routes.HOME else Routes.ONBOARDING
                }

                ChiSaraNavHost(
                    navController = navController,
                    viewModel = vm,
                    startDestination = startDestination,
                    deepLinkEventId = pendingGuessEventId,
                    onDeepLinkConsumed = { pendingGuessEventId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingGuessEventId = extractEventId(intent)
    }

    private fun extractEventId(intent: Intent?): Long? {
        if (intent?.action != ACTION_GUESS) return null
        val id = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        return if (id >= 0) id else null
    }

    companion object {
        const val ACTION_GUESS = "com.chisara.app.action.GUESS"
        const val EXTRA_EVENT_ID = "com.chisara.app.extra.EVENT_ID"
    }
}
