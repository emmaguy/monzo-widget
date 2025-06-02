package com.emmav.monzowidget.ui

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emmav.monzowidget.data.session.AuthStorage
import com.emmav.monzowidget.data.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Implements OAuth 2.0 login flow for Monzo, as described here:
 * https://docs.monzo.com/#acquire-an-access-token.
 *
 * Used on app process start to send the user to the right screen.
 */
class LoginViewModel(
    private val sessionRepository: SessionRepository,
    private val authStorage: AuthStorage,
    private val clientId: String,
    private val clientSecret: String,
    private val redirectUri: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState: StateFlow<LoginUiState> = _uiState

    init {
        viewModelScope.launch {
            val session = sessionRepository.getSession()
            if (session == null) {
                _uiState.value = LoginUiState.RequiresAuth(hasSession = false, hasSCA = false)
            } else {
                checkIfSCARequired()
            }
        }
    }

    fun checkIfSCARequired() {
        viewModelScope.launch {
            val authResult = sessionRepository.testAuthentication()
            if (authResult.isFailure) {
                _uiState.value = LoginUiState.RequiresAuth(hasSession = true, hasSCA = false)
            } else {
                _uiState.value = LoginUiState.LoginFinished
            }
        }
    }

    fun onStartAuth(context: Context) {
        authStorage.stateToken = UUID.randomUUID().toString()

        // 1. Redirect the user to Monzo to authorise your app
        val uri = Uri.Builder()
            .scheme("https")
            .authority("auth.monzo.com")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("state", authStorage.stateToken)
            .build()
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, uri)
    }

    fun exchangeCodeForToken(code: String, state: String?) {
        _uiState.value = LoginUiState.Loading

        // 2. Monzo redirects the user back to your app with an authorization code (via [MainActivity.onCreate()])
        if (state != authStorage.stateToken) {
            authStorage.stateToken = "" // Clear the token to prevent getting stuck
            _uiState.value = LoginUiState.Error("Invalid state token")
        } else {
            authStorage.stateToken = "" // Clear the state token after use

            viewModelScope.launch {
                // 3. Exchange the authorization code for an access token.
                sessionRepository.exchangeCodeForToken(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    authorizationCode = code,
                    redirectUri = redirectUri,
                ).onSuccess {
                    // We need the user to complete SCA in the official Monzo app before we can
                    // do much with this token.
                    checkIfSCARequired()
                }.onFailure {
                    _uiState.value = LoginUiState.Error(
                        "Failed to exchange code for token: ${it.message ?: "unknown error"}"
                    )
                }
            }
        }
    }

    fun onResume() {
        checkIfSCARequired()
    }

    sealed class LoginUiState {
        data object Loading : LoginUiState()
        data class RequiresAuth(val hasSession: Boolean, val hasSCA: Boolean) : LoginUiState()
        data class Error(val message: String) : LoginUiState()
        data object LoginFinished : LoginUiState()
    }
}
