package com.emmav.monzo.widget.feature.setupclient

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.emmav.monzo.widget.R
import com.emmav.monzo.widget.common.AppTheme
import com.emmav.monzo.widget.common.FullWidthButton
import com.emmav.monzo.widget.common.Info
import com.emmav.monzo.widget.common.openUrl
import com.emmav.monzo.widget.feature.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupClientActivity : AppCompatActivity() {
    private val viewModel: SetupClientViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Scaffold(topBar = {
                    TopAppBar(title = { Text(ContextAmbient.current.getString(R.string.setup_activity_title)) })
                }, bodyContent = {
                    val state by viewModel.state.observeAsState(SetupClientViewModel.State())
                    if (state.finished) {
                        startActivity(LoginActivity.buildIntent(ContextAmbient.current))
                        finish()
                    }
                    if (state.openCreateClientInBrowser) {
                        openUrl(
                            url = "https://developers.monzo.com",
                            toolbarColor = MaterialTheme.colors.primary
                        )
                    }
                    Content(
                        state = state,
                        clientIdChanged = { viewModel.onClientIdChanged(clientId = it) },
                        clientSecretChanged = { viewModel.onClientSecretChanged(clientSecret = it) },
                        hasExistingClientClicked = { viewModel.onHasExistingClientClicked() },
                        goToCreateClientClicked = { viewModel.onGoToCreateClientClicked() },
                        createClientClicked = { viewModel.onCreateClientClicked() },
                        submitClicked = { viewModel.onSubmitClicked() }
                    )
                })
            }
        }
    }

    companion object {
        fun buildIntent(context: Context): Intent {
            return Intent(context, SetupClientActivity::class.java)
        }
    }
}

@Preview
@Composable
private fun EnterClientDetailsPreview() {
    AppTheme {
        Surface {
            Content(
                state = SetupClientViewModel.State(uiState = SetupClientViewModel.UiState.ENTER_CLIENT_DETAILS),
                clientIdChanged = {},
                clientSecretChanged = {},
                hasExistingClientClicked = {},
                goToCreateClientClicked = {},
                createClientClicked = {},
                submitClicked = {}
            )
        }
    }
}

@Composable
private fun Content(
    state: SetupClientViewModel.State,
    clientIdChanged: (String) -> Unit,
    clientSecretChanged: (String) -> Unit,
    hasExistingClientClicked: () -> Unit,
    goToCreateClientClicked: () -> Unit,
    createClientClicked: () -> Unit,
    submitClicked: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ScrollableColumn(modifier = Modifier.align(Alignment.TopCenter)) {
            Spacer(modifier = Modifier.preferredHeight(height = 64.dp))
            Info(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                emoji = state.uiState.emoji,
                title = state.uiState.title,
                subtitle = state.uiState.subtitle
            )
            if (state.uiState == SetupClientViewModel.UiState.ENTER_CLIENT_DETAILS) {
                Input(
                    modifier = Modifier,
                    state = state,
                    clientIdChanged = clientIdChanged,
                    clientSecretChanged = clientSecretChanged
                )
            }
            Spacer(modifier = Modifier.height(height = 128.dp))
        }
        Actions(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            state = state,
            hasExistingClientClicked = hasExistingClientClicked,
            goToCreateClientClicked = goToCreateClientClicked,
            createClientClicked = createClientClicked,
            submitClicked = submitClicked
        )
    }
}

@Composable
private fun Input(
    modifier: Modifier,
    state: SetupClientViewModel.State,
    clientIdChanged: (String) -> Unit,
    clientSecretChanged: (String) -> Unit
) {
    Column(modifier = modifier.padding(top = 16.dp)) {
        TextField(value = state.clientId,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { clientIdChanged(it) },
            label = { Text(ContextAmbient.current.getString(R.string.setup_enter_client_id_hint)) }
        )
        TextField(value = state.clientSecret,
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Send,
            onValueChange = { clientSecretChanged(it) },
            label = { Text(ContextAmbient.current.getString(R.string.setup_enter_client_secret_hint)) }
        )
    }
}

@Composable
private fun Actions(
    modifier: Modifier,
    state: SetupClientViewModel.State,
    hasExistingClientClicked: () -> Unit,
    goToCreateClientClicked: () -> Unit,
    createClientClicked: () -> Unit,
    submitClicked: () -> Unit
) {
    Column(modifier = modifier) {
        when (state.uiState) {
            SetupClientViewModel.UiState.WELCOME -> {
                FullWidthButton(
                    title = R.string.setup_welcome_action_positive,
                    onClick = { hasExistingClientClicked() }
                )
                FullWidthButton(
                    title = R.string.setup_welcome_action_negative,
                    onClick = { createClientClicked() }
                )
            }
            SetupClientViewModel.UiState.CREATE_INSTRUCTIONS -> {
                FullWidthButton(
                    title = R.string.setup_info_action,
                    onClick = { goToCreateClientClicked() }
                )
            }
            SetupClientViewModel.UiState.ENTER_CLIENT_DETAILS -> {
                FullWidthButton(
                    title = R.string.setup_entered_client_details,
                    enabled = state.clientId.isNotBlank() && state.clientSecret.isNotBlank(),
                    onClick = { submitClicked() }
                )
            }
        }
    }
}
