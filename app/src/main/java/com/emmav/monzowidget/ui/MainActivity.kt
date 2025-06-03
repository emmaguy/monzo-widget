@file:OptIn(ExperimentalMaterial3Api::class)

package com.emmav.monzowidget.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.emmav.monzowidget.App
import com.emmav.monzowidget.BuildConfig
import com.emmav.monzowidget.R
import com.emmav.monzowidget.data.session.AuthStorage
import com.emmav.monzowidget.ui.LoginViewModel.LoginUiState
import com.emmav.monzowidget.ui.Utils.formatBalance
import com.emmav.monzowidget.ui.Utils.title
import com.emmav.monzowidget.ui.theme.MonzoWidgetTheme

class MainActivity : ComponentActivity() {

    private val loginViewModel by lazy {
        LoginViewModel(
            sessionRepository = App.Companion.instance.sessionRepository,
            authStorage = AuthStorage(App.Companion.instance.applicationContext),
            clientId = BuildConfig.MONZO_CLIENT_ID,
            clientSecret = BuildConfig.MONZO_CLIENT_SECRET,
            redirectUri = getString(R.string.callback_scheme) + "://" + getString(R.string.callback_host),
        )
    }
    private val homeViewModel by lazy {
        HomeViewModel(
            monzoRepository = App.Companion.instance.monzoRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent()
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                loginViewModel.onResume()
            }
        })
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
                            Text("Requires auth $uiState")
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
                        val uiState by homeViewModel.uiState.collectAsState()
                        HomeScreen(uiState)
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
    private fun HomeScreen(uiState: HomeViewModel.HomeUiState) {
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
                    when (uiState) {
                        HomeViewModel.HomeUiState.Loading -> LoadingScreen()
                        is HomeViewModel.HomeUiState.Loaded -> {
                            val accounts = uiState.accounts
                            if (accounts.isEmpty()) {
                                Text("No accounts found")
                            } else {
                                accounts.forEach { account ->
                                    ListItem(
                                        title = account.title(),
                                        subtitle = account.balance?.formatBalance(),
                                        icon = {
                                            val vector = when (account.ownerType) {
                                                "personal" -> Icons.Filled.Person
                                                "joint" -> Icons.Filled.Group
                                                "business" -> Icons.Filled.Work
                                                else -> Icons.Filled.AccountCircle
                                            }
                                            RowIcon(vector = vector)
                                        },
                                        onClick = { },
                                    )
                                    account.pots.forEach { pot ->
                                        ListItem(
                                            title = pot.name,
                                            subtitle = pot.balance?.formatBalance(),
                                            icon = {
                                                RowIcon(vector = Icons.Filled.Savings)
                                            },
                                            onClick = { }
                                        )
                                    }
                                }
                            }
                        }

                        is HomeViewModel.HomeUiState.Error -> {
                            Text(text = "Error: ${uiState.message}")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ListItem(
        icon: @Composable (() -> Unit),
        title: String,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
        onClick: (() -> Unit)? = null,
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onClick?.invoke() },
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon()
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun RowIcon(vector: ImageVector) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {

            Icon(
                imageVector = vector,
                contentDescription = "icon",
            )
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
