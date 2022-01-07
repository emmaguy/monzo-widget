package com.emmav.monzo.widget.feature.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.emmav.monzo.widget.R
import com.emmav.monzo.widget.common.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    @Inject lateinit var vmFactory: LoginViewModel.Factory
    private val viewModel by assistedViewModel {
        vmFactory.create(
            redirectUri = getString(R.string.callback_url_scheme) + "://" + getString(R.string.callback_url_host)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val openUrlAndFinish: (String) -> Unit = { url ->
            openUrl(url = url)
            finish()
        }

        setContent {
            AppTheme {
                Scaffold(topBar = {
                    TopAppBar(title = { Text(LocalContext.current.getString(R.string.login_activity_title)) })
                }, content = {
                    val state by viewModel.state.observeAsState(LoginViewModel.State.Unknown())
                    (state as? LoginViewModel.State.RequestMagicLink)?.url?.let { openUrlAndFinish.invoke(it) }

                    Content(
                        state = state,
                        loginClicked = { viewModel.onLoginClicked() },
                        openMonzoApp = {
                            val monzoAppIntent = packageManager.getLaunchIntentForPackage("co.uk.getmondo")
                            if (monzoAppIntent != null) {
                                startActivity(monzoAppIntent)
                            } else {
                                Toast.makeText(
                                    this,
                                    R.string.login_requires_sca_monzo_not_installed,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        loggedIn = { finish() }
                    )
                })
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.data?.let {
            if (it.toString().startsWith(getString(R.string.callback_url_scheme))) {
                viewModel.onMagicLinkParamsReceived(
                    it.getQueryParameter("code")!!,
                    it.getQueryParameter("state")!!
                )
            }
        }
    }

    companion object {
        fun buildIntent(context: Context): Intent {
            return Intent(context, LoginActivity::class.java)
        }
    }
}

@Composable
private fun Content(
    state: LoginViewModel.State,
    loginClicked: () -> Unit,
    openMonzoApp: () -> Unit,
    loggedIn: () -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp)
    ) {
        val (info, loading, actions) = createRefs()
        Info(
            modifier = Modifier.constrainAs(info) {
                centerHorizontallyTo(parent)
                linkTo(top = parent.top, bottom = actions.top)
            },
            emoji = state.emoji,
            title = state.title,
            subtitle = state.subtitle
        )
        if (state.showLoading) {
            Box(modifier = Modifier.constrainAs(loading) {
                centerHorizontallyTo(parent)
                linkTo(top = parent.top, bottom = info.top)
            }) {
                CircularProgressIndicator()
            }
        }
        Actions(
            modifier = Modifier.constrainAs(actions) {
                bottom.linkTo(parent.bottom)
            },
            state = state,
            loginClicked = loginClicked,
            openMonzoApp = openMonzoApp,
            loggedIn = loggedIn
        )
    }
}


@Composable
private fun Actions(
    modifier: Modifier,
    state: LoginViewModel.State,
    loginClicked: () -> Unit,
    openMonzoApp: () -> Unit,
    loggedIn: () -> Unit,
) {
    Column(modifier = modifier) {
        when (state) {
            is LoginViewModel.State.Unauthenticated -> {
                FullWidthButton(
                    title = R.string.login_unauthed_action,
                    onClick = { loginClicked() }
                )
            }
            is LoginViewModel.State.RequiresStrongCustomerAuthentication -> {
                FullWidthButton(
                    title = R.string.login_requires_sca_action,
                    onClick = { openMonzoApp() }
                )
            }
            is LoginViewModel.State.Authenticated -> {
                FullWidthButton(
                    title = R.string.login_logged_in_action,
                    onClick = { loggedIn() }
                )
            }
            else -> {
                // Do nothing
            }
        }
    }
}