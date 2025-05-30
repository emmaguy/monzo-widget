package com.emmav.monzowidget

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Implements OAuth 2.0 login flow for Monzo, as described here:
 * https://docs.monzo.com/#acquire-an-access-token
 */
class LoginViewModel(
    private val sessionRepository: SessionRepository,
    private val sessionPreferences: SessionPreferences,
    private val clientId: String,
    private val clientSecret: String,
    private val redirectUri: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onStartAuth(context: Context) {
        sessionPreferences.stateToken = UUID.randomUUID().toString()

        // 1. Redirect the user to Monzo to authorise your app
        val uri = Uri.Builder()
            .scheme("https")
            .authority("auth.monzo.com")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("state", sessionPreferences.stateToken)
            .build()
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, uri)
    }

    fun exchangeCodeForToken(code: String, state: String?) {
        _uiState.value = LoginUiState.Loading

        // 2. Monzo redirects the user back to your app with an authorization code (via [MainActivity.onCreate()])
        if (state != sessionPreferences.stateToken) {
            sessionPreferences.stateToken = "" // Clear the token to prevent getting stuck
            _uiState.value = LoginUiState.Error("Invalid state token")
        } else {
            sessionPreferences.stateToken = "" // Clear the state token after use

            viewModelScope.launch {
                // 3. Exchange the authorization code for an access token.
                sessionRepository.exchangeCodeForToken(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    authorizationCode = code,
                    redirectUri = redirectUri,
                ).onSuccess {
                    _uiState.value = LoginUiState.Success
                }.onFailure {
                    _uiState.value = LoginUiState.Error(
                        "Failed to exchange code for token: ${it.message ?: "unknown error"}"
                    )
                }
            }
        }
    }

    sealed class LoginUiState {
        data object Idle : LoginUiState()
        data object Loading : LoginUiState()
        data class Error(val message: String) : LoginUiState()
        data object Success : LoginUiState()
    }
}
