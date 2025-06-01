@file:OptIn(ExperimentalMaterial3Api::class)

package com.emmav.monzowidget.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.emmav.monzowidget.App
import com.emmav.monzowidget.BuildConfig
import com.emmav.monzowidget.R
import com.emmav.monzowidget.data.DataModule
import com.emmav.monzowidget.data.session.AuthStorage
import com.emmav.monzowidget.data.session.SessionRepository
import com.emmav.monzowidget.ui.LoginViewModel.LoginUiState
import com.emmav.monzowidget.ui.theme.MonzoWidgetTheme

class MainActivity : ComponentActivity() {
    private val repository by lazy {
        val context = App.Companion.instance.applicationContext
        val db = DataModule.createDb(
            context = context,
        )
        SessionRepository(
            api = DataModule.create(
                context = context,
                sessionStorage = db.authStorage(),
                baseUrl = "https://api.monzo.com",
            ),
            db = db.authStorage(),
        )
    }
    private val loginViewModel by lazy {
        LoginViewModel(
            sessionRepository = repository,
            authStorage = AuthStorage(App.Companion.instance.applicationContext),
            clientId = BuildConfig.MONZO_CLIENT_ID,
            clientSecret = BuildConfig.MONZO_CLIENT_SECRET,
            redirectUri = getString(R.string.callback_scheme) + "://" + getString(R.string.callback_host),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent()
        setContent {
            MonzoWidgetTheme {
                val backStack = remember {
                    mutableStateListOf<AppScreens>(AppScreens.Login)
                }
                StartupScreen(backStack = backStack)
            }
        }
    }

    private fun handleIntent() {
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
        backStack: SnapshotStateList<AppScreens>,
    ) {
        val uiState by loginViewModel.uiState.collectAsState()

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
                when (uiState) {
                    LoginUiState.Loading -> NavEntry(key) {
                        LoadingScreen()
                    }

                    is LoginUiState.RequiresAuth -> NavEntry(key) {
                        Column(modifier = Modifier.padding(top = 32.dp)) {
                            Text("Requires auth")
                            Button(onClick = {
                                loginViewModel.onStartAuth(this@MainActivity)
                            }) {
                                Text("Click to navigate")
                            }
                        }
                    }

                    LoginUiState.LoginFinished -> NavEntry(key) {
                        backStack.clear()
                        backStack.add(AppScreens.Home)
                        HomeScreen()
                    }

                    is LoginUiState.Error -> NavEntry(key) {
                        Text(
                            text = "Error: ${(uiState as LoginUiState.Error).message}",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            })
    }

    @Composable
    private fun HomeScreen() {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {
                TopAppBar(
                    title = { Text("Monzo widget II") },
                    modifier = Modifier.statusBarsPadding()
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("haz auth token")
                }
            }
        }
    }

    @Composable
    private fun LoadingScreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

sealed class AppScreens {
    data object Home : AppScreens()
    data object Login : AppScreens()
}
