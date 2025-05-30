@file:OptIn(ExperimentalMaterial3Api::class)

package com.emmav.monzowidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay

class MainActivity : ComponentActivity() {
    private val repository by lazy {
        val db = DataModule.createDb(
            context = App.instance.applicationContext,
        )
        SessionRepository(
            api = DataModule.create("https://api.monzo.com"),
            db = db.authStorage(),
        )
    }
    private val loginViewModel by lazy {
        LoginViewModel(
            sessionRepository = repository,
            clientId = BuildConfig.MONZO_CLIENT_ID,
            clientSecret = BuildConfig.MONZO_CLIENT_SECRET,
            redirectUri = getString(R.string.callback_scheme) + "://" + getString(R.string.callback_host),
        )
    }
    private val startupViewModel by lazy { StartupViewModel(sessionRepository = repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent()
        setContent {
            val backStack = remember { mutableStateListOf<StartupUiState>(StartupUiState.Unknown) }
            StartupScreen(viewModel = startupViewModel, backStack = backStack)
        }
    }

    private fun handleIntent() {
        // TODO When handling the intent, show ui
        intent?.data?.let { uri ->
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")
            if (code != null) {
                loginViewModel.exchangeCodeForToken(code, state)
            }
        }
    }

    @Composable
    fun StartupScreen(
        viewModel: StartupViewModel,
        backStack: SnapshotStateList<StartupUiState>,
    ) {
        val uiState by viewModel.uiState.collectAsState()

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (uiState) {
                    StartupUiState.HasAuth -> NavEntry(key) {
                        Text("haz auth token")
                    }

                    StartupUiState.RequiresAuth -> NavEntry(key) {
                        Column(modifier = Modifier.padding(top = 32.dp)) {
                            Text("Requires auth")
                            Button(onClick = {
                                backStack.add(StartupUiState.HasAuth)
                                loginViewModel.onStartAuth(this@MainActivity)
                            }) {
                                Text("Click to navigate")
                            }
                        }
                    }

                    is StartupUiState.Unknown -> NavEntry(key) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            })
    }
}
